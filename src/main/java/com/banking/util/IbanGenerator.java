package com.banking.util;

import java.util.concurrent.ThreadLocalRandom;

public class IbanGenerator {

    public static String generateSpanishIban() {
        StringBuilder sb = new StringBuilder("ES");
        for (int i = 0; i < 22; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }

        return sb.toString();
    }
}
