package com.shortener.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CommonTests {

    @Test
    void base62RoundTrip() {
        for (long v : new long[]{0, 1, 61, 62, 12345, 3_521_614_606_207L, Long.MAX_VALUE}) {
            assertEquals(v, Base62.decode(Base62.encode(v)));
        }
        assertEquals("0000001", Base62.encode(1, 7));
        assertFalse(Base62.isValid("ab-c"));
    }

    @Test
    void scramblerIsInjectiveAndInRange() {
        KeyScrambler s = new KeyScrambler(42L);
        Set<Long> seen = new HashSet<>();
        for (long i = 0; i < 200_000; i++) {
            long p = s.permute(i);
            assertTrue(p >= 0 && p < KeyScrambler.KEYSPACE);
            assertTrue(seen.add(p), "collision at " + i);
        }
        assertEquals(7, s.toKey(123).length());
    }
}
