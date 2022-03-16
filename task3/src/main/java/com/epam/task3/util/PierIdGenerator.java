package com.epam.task3.util;

public class PierIdGenerator {
    public static long currentId = 0;

    private PierIdGenerator() {
    }

    public static long generate() {
        return ++currentId;
    }
}
