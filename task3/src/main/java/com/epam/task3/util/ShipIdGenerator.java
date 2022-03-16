package com.epam.task3.util;

public class ShipIdGenerator {
    private static long currentId = 0;

    private ShipIdGenerator() {
    }

    public static long generate() {
        return ++currentId;
    }
}
