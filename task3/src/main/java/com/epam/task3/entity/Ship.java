package com.epam.task3.entity;

import com.epam.task3.util.ShipIdGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class Ship extends Thread implements Serializable {
    private static final long serialVersionUID = -3491118351213241146L;
    private static final Logger LOGGER = LogManager.getLogger();
    public static final int DEFAULT_LOAD_UNLOAD_SPEED = 50;
    public static final int EMPTY_SHIP_CONTAINER_QUANTITY = 0;
    private final long shipId;
    private int capacity;
    private boolean isFull;
    private int containerQuantity;
    private int loadUnloadSpeed;

    private State state;
    private final Port port;

    public enum State {
        CREATED, WAITING, PROCESSING, COMPLETED
    }

    {
        shipId = ShipIdGenerator.generate();
        state = State.CREATED;
        loadUnloadSpeed = DEFAULT_LOAD_UNLOAD_SPEED;
        port = Port.getInstance();
    }

    public Ship() {
    }

    public Ship(int capacity, boolean isFull) {
        this.capacity = capacity;
        this.isFull = isFull;
        if (isFull) {
            this.containerQuantity = capacity;
        }
    }

    public Ship(int capacity, int containerQuantity) {
        this.capacity = capacity;
        this.containerQuantity = containerQuantity;
        if (containerQuantity > EMPTY_SHIP_CONTAINER_QUANTITY) {
            isFull = true;
        }
    }

    public Ship(int capacity) {
        this.capacity = capacity;
        isFull = false;
    }

    public long getShipId() {
        return shipId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getContainerQuantity() {
        return containerQuantity;
    }

    public void setContainerQuantity(int containerQuantity) {
        this.containerQuantity = containerQuantity;
    }

    public State getShipState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean full) {
        isFull = full;
    }

    public int getLoadUnloadSpeed() {
        return loadUnloadSpeed;
    }

    public void setLoadUnloadSpeed(int loadUnloadSpeed) {
        this.loadUnloadSpeed = loadUnloadSpeed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        Ship ship = (Ship) o;
        return shipId == ship.shipId &&
                isFull == ship.isFull &&
                capacity == ship.capacity &&
                containerQuantity == ship.containerQuantity &&
                loadUnloadSpeed == ship.loadUnloadSpeed &&
                state == ship.state;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = Long.hashCode(shipId);
        result = result * prime + Boolean.hashCode(isFull);
        result = result * prime + capacity;
        result = result * prime + containerQuantity;
        result = result * prime + loadUnloadSpeed;
        result = result * prime + state.hashCode();
        return result;
    }

    @Override
    public String toString() {
        String className = this.getClass().getSimpleName();
        StringBuilder builder = new StringBuilder(className);
        builder.append("{shipId=").append(shipId).
                append(", isFull=").append(isFull).
                append(", capacity=").append(capacity).
                append(", containerQuantity=").append(containerQuantity).
                append(", state=").append(state).
                append('}');
        return builder.toString();
    }

    @Override
    public void run() {
        Pier pier = null;
        try {
            pier = port.getFreePier(this);
            process();
        } catch (InterruptedException exception) {
            LOGGER.warn("Ship#" + shipId + " is interrupted\t\t\t" + this, exception);
        } finally {
            if (pier != null) {
                port.releasePier(pier);
            }
        }
    }

    private void process() throws InterruptedException {
        state = State.PROCESSING;
        if (isFull) {
            LOGGER.info("Ship#" + shipId + " starts unload.\t\t\t" + this);
            unload();
        } else {
            LOGGER.info("Ship#" + shipId + " starts load.\t\t\t" + this);
            load();
        }
        state = State.COMPLETED;
        LOGGER.info("Ship#" + shipId + " COMPLETED.\t\t\t" + this);
    }

    private void unload() throws InterruptedException {
        int quantityToUnload;
        while (containerQuantity != 0) {
            quantityToUnload = Math.min(containerQuantity, loadUnloadSpeed);
            containerQuantity -= quantityToUnload;
            port.getContainerQuantity().getAndAdd(quantityToUnload);
            port.getReservedPlace().getAndAdd(-quantityToUnload);
            TimeUnit.MILLISECONDS.sleep(100);
            LOGGER.debug("Ship#" + shipId + ": Unloaded " + quantityToUnload + " containers.\t\t\t" + this);
        }
    }

    private void load() throws InterruptedException {
        int quantityToLoad;
        int missingQuantity;
        while (containerQuantity != capacity) {
            missingQuantity = capacity - containerQuantity;
            quantityToLoad = Math.min(missingQuantity, loadUnloadSpeed);
            containerQuantity += quantityToLoad;
            port.getContainerQuantity().getAndAdd(-quantityToLoad);
            port.getReservedContainerQuantity().getAndAdd(-quantityToLoad);
            TimeUnit.MILLISECONDS.sleep(100);
            LOGGER.debug("Ship#" + shipId + ": Loaded " + quantityToLoad + " containers.\t\t\t" + this);
        }
    }
}
