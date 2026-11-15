package com.shortener.common;

import java.util.Arrays;

/** Base62 codec (0-9, a-z, A-Z) for non-negative longs. */
public final class Base62 {

    private static final char[] ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int BASE = 62;
    private static final int[] INDEX = new int[128];

    static {
        Arrays.fill(INDEX, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            INDEX[ALPHABET[i]] = i;
        }
    }

    private Base62() {}

    /** Encodes {@code value}, left-padded with '0' up to {@code minLength}. */
    public static String encode(long value, int minLength) {
        if (value < 0) throw new IllegalArgumentException("value must be >= 0");
        char[] buf = new char[Math.max(11, minLength)];   // 62^11 > 2^63
        int pos = buf.length;
        do {
            buf[--pos] = ALPHABET[(int) (value % BASE)];
            value /= BASE;
        } while (value > 0);
        while (buf.length - pos < minLength) {
            buf[--pos] = ALPHABET[0];
        }
        return new String(buf, pos, buf.length - pos);
    }

    public static String encode(long value) {
        return encode(value, 1);
    }

    public static long decode(String s) {
        if (s == null || s.isEmpty()) throw new IllegalArgumentException("empty input");
        long result = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int digit = c < 128 ? INDEX[c] : -1;
            if (digit < 0) throw new IllegalArgumentException("Invalid Base62 character: " + c);
            result = Math.addExact(Math.multiplyExact(result, BASE), digit);
        }
        return result;
    }

    public static boolean isValid(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 128 || INDEX[c] < 0) return false;
        }
        return true;
    }
}
