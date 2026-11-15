package com.shortener.create.app;

import com.shortener.common.ShortenerExceptions.KeyExhausted;
import com.shortener.create.infra.KgsClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Local in-memory pool of pre-generated keys. {@link #nextKey()} is a queue pop (no network);
 * a background thread refills the buffer when it drops below the low watermark.
 * Keys held by a crashed pod are simply lost, which is fine because the keyspace is huge.
 */
@Service
public class KeyReservationService {

    private static final Logger log = LoggerFactory.getLogger(KeyReservationService.class);

    private static final int BATCH_SIZE = 10_000;
    private static final int LOW_WATERMARK = 20_000;
    private static final int HIGH_WATERMARK = 100_000;

    private final KgsClient kgs;
    private final ConcurrentLinkedQueue<String> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger();
    private final AtomicBoolean refilling = new AtomicBoolean(false);
    private final ExecutorService refiller = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kgs-refill");
        t.setDaemon(true);
        return t;
    });

    public KeyReservationService(KgsClient kgs) {
        this.kgs = kgs;
    }

    @PostConstruct
    void warmUp() {
        triggerAsyncRefill();                                  // do not block startup if KGS is still booting
    }

    public String nextKey() {
        String key = buffer.poll();
        if (key == null) {
            refillBlocking();                                  // buffer ran dry: last-resort synchronous refill
            key = buffer.poll();
            if (key == null) throw new KeyExhausted("No keys available from KGS right now.");
        }
        if (size.decrementAndGet() < LOW_WATERMARK) {
            triggerAsyncRefill();
        }
        return key;
    }

    public int available() {
        return Math.max(0, size.get());
    }

    private void triggerAsyncRefill() {
        if (refilling.compareAndSet(false, true)) {
            refiller.submit(() -> {
                try {
                    refillBlocking();
                } finally {
                    refilling.set(false);
                }
            });
        }
    }

    private synchronized void refillBlocking() {
        try {
            while (size.get() < HIGH_WATERMARK) {
                List<String> batch = kgs.fetchBatch(BATCH_SIZE);
                if (batch.isEmpty()) break;
                buffer.addAll(batch);
                size.addAndGet(batch.size());
            }
        } catch (Exception e) {
            log.error("KGS refill failed (buffer={}): {}", size.get(), e.toString());
        }
    }

    @PreDestroy
    void shutdown() {
        refiller.shutdownNow();
    }
}
