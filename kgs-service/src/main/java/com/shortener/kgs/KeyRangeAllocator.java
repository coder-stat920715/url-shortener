package com.shortener.kgs;

import com.shortener.common.KeyScrambler;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Leases ID ranges from a ZooKeeper-backed atomic counter and turns each ID into a scrambled
 * 7-char Base62 key. A crashed node simply loses the unused remainder of its range; ranges are
 * NEVER reclaimed or reused (that is how duplicates would appear).
 */
@Service
public class KeyRangeAllocator {

    private final DistributedAtomicLong counter;
    private final KeyScrambler scrambler;
    private final long rangeSize;

    private long next;
    private long end;

    public KeyRangeAllocator(CuratorFramework curator,
                             KeyScrambler scrambler,
                             @Value("${kgs.range-size:1000000}") long rangeSize) {
        this.counter = new DistributedAtomicLong(curator, "/kgs/counter", new ExponentialBackoffRetry(100, 5));
        this.scrambler = scrambler;
        this.rangeSize = rangeSize;
    }

    public synchronized List<String> nextBatch(int size) throws Exception {
        List<String> keys = new ArrayList<>(size);
        while (keys.size() < size) {
            if (next >= end) {
                leaseNewRange();
            }
            keys.add(scrambler.toKey(next++));
        }
        return keys;
    }

    private void leaseNewRange() throws Exception {
        AtomicValue<Long> v = counter.add(rangeSize);          // atomic compare-and-set in ZooKeeper
        if (!v.succeeded()) {
            throw new IllegalStateException("ZooKeeper range lease failed");
        }
        next = v.preValue();
        end = v.postValue();
    }
}
