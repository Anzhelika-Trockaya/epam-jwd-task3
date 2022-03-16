package com.epam.task3;

public class ShipIdGenerator {
    private static long currentId = 0;

    private ShipIdGenerator() {
    }

    public static long generate() {
        return ++currentId;
    }
}
