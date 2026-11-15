package com.shortener.common;

/**
 * Keyed bijective permutation over [0, 62^7) built from a 4-round Feistel network on a 42-bit
 * domain plus cycle-walking. Sequential counters become non-guessable 7-char Base62 keys with
 * ZERO collisions (the mapping is one-to-one).
 */
public final class KeyScrambler {

    public static final long KEYSPACE = 3_521_614_606_208L;   // 62^7
    public static final int KEY_LENGTH = 7;
    private static final int HALF_BITS = 21;                  // 2 x 21 = 42-bit domain (2^42 > 62^7)
    private static final long MASK = (1L << HALF_BITS) - 1;
    private static final int ROUNDS = 4;

    private final long[] roundKeys = new long[ROUNDS];

    public KeyScrambler(long secretSeed) {
        long x = secretSeed;
        for (int i = 0; i < ROUNDS; i++) {
            x = mix(x + 0x9E3779B97F4A7C15L);
            roundKeys[i] = x;
        }
    }

    /** One-to-one map of [0, KEYSPACE) onto itself. */
    public long permute(long id) {
        if (id < 0 || id >= KEYSPACE) {
            throw new IllegalArgumentException("id outside keyspace: " + id);
        }
        long v = id;
        do {
            v = feistel(v);
        } while (v >= KEYSPACE);                              // cycle-walk back into the keyspace
        return v;
    }

    public String toKey(long counter) {
        return Base62.encode(permute(counter), KEY_LENGTH);
    }

    private long feistel(long v) {
        long left = (v >>> HALF_BITS) & MASK;
        long right = v & MASK;
        for (int i = 0; i < ROUNDS; i++) {
            long next = left ^ (mix(right ^ roundKeys[i]) & MASK);
            left = right;
            right = next;
        }
        return (left << HALF_BITS) | right;
    }

    private static long mix(long z) {                         // SplitMix64 finalizer
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
