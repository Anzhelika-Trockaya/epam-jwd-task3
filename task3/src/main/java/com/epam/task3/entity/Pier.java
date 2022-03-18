package com.epam.task3.entity;

import com.epam.task3.util.PierIdGenerator;

import java.io.Serializable;

public class Pier implements Serializable {
    private static final long serialVersionUID = 5879642603016240336L;
    private final long pierId;

    public Pier() {
        pierId = PierIdGenerator.generate();
    }

    public long getPierId() {
        return pierId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        Pier pier = (Pier) o;
        return pierId == pier.pierId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(pierId);
    }

    @Override
    public String toString() {
        String className = this.getClass().getSimpleName();
        StringBuilder builder = new StringBuilder(className);
        builder.append("{pierId=").append(pierId).append('}');
        return builder.toString();
    }
}
