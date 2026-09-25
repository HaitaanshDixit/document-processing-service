package com.suretyseven.docprocessing.util;

import java.security.SecureRandom;

public final class IdGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
    }

    public static String newDocumentId() {
        StringBuilder suffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            suffix.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return "DOC-" + suffix;
    }
}
