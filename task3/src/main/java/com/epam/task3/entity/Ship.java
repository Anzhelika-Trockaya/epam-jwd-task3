package com.epam.task3.entity;

import com.epam.task3.util.ShipIdGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Ship extends Thread implements Serializable {
    private static final long serialVersionUID = -3491118351213241146L;
    private static final Logger LOGGER = LogManager.getLogger();
    public static final int DEFAULT_CONTAINER_QUANTITY = 0;
    public static final int DEFAULT_LOAD_UNLOAD_SPEED = 50;
    private final long shipId;
    private int capacity;
    private int containerQuantity;
    private int loadUnloadSpeed;

    private State state;
    private Pier pier;
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

    public Ship(int capacity, int containerQuantity) {
        this.capacity = capacity;
        this.containerQuantity = containerQuantity;
    }

    public Ship(int capacity) {
        this.capacity = capacity;
        this.containerQuantity = DEFAULT_CONTAINER_QUANTITY;
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

    public int getLoadUnloadSpeed() {
        return loadUnloadSpeed;
    }

    public void setLoadUnloadSpeed(int loadUnloadSpeed) {
        this.loadUnloadSpeed = loadUnloadSpeed;
    }

    public Pier getPier() {
        return pier;
    }

    public void setPier(Pier pier) {
        this.pier = pier;
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
                capacity == ship.capacity &&
                containerQuantity == ship.containerQuantity &&
                loadUnloadSpeed == ship.loadUnloadSpeed &&
                state == ship.state &&
                pier != null ? pier.equals(ship.pier) : ship.pier == null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = Long.hashCode(shipId);
        result = result * prime + capacity;
        result = result * prime + containerQuantity;
        result = result * prime + loadUnloadSpeed;
        result = result * prime + state.hashCode();
        result = result * prime + (pier == null ? 0 : pier.hashCode());
        return result;
    }

    @Override
    public String toString() {
        String className = this.getClass().getSimpleName();
        StringBuilder builder = new StringBuilder(className);
        builder.append("{shipId=").append(shipId).
                append(", capacity=").append(capacity).
                append(", containerQuantity=").append(containerQuantity).
                append(", state=").append(state).
                append(", pier=").append(pier).
                append('}');
        return builder.toString();
    }

    @Override
    public void run() {
        Optional<Pier> optionalPier = port.getFreePier(this);
        if (optionalPier.isPresent()) {
            pier = optionalPier.get();
            LOGGER.info(this + " moored at the pier:" + pier);
            try {
                TimeUnit.SECONDS.sleep(1);
                process();
            } catch (InterruptedException exception) {
                LOGGER.warn(this + " is interrupted! ");
                port.releasePier(pier);
            }
        } else {
            LOGGER.warn("The ship is not assigned a pier. " + this);
        }
    }

    private void process() {
        state = State.PROCESSING;
        if (containerQuantity == 0) {
            LOGGER.info(this + " starts load.");
            load();
            state = State.COMPLETED;
            LOGGER.info(this + " loaded.");
        } else {
            LOGGER.info(this + " starts unload.");
            unload();
            state = State.COMPLETED;
            LOGGER.info(this + " unloaded.");
        }
        port.releasePier(pier);
    }

    private void unload() {
        int quantityToUnload;
        try {
            while (containerQuantity != 0) {
                quantityToUnload = Math.min(containerQuantity, loadUnloadSpeed);
                containerQuantity -= quantityToUnload;
                port.getContainerQuantity().getAndAdd(quantityToUnload);
                port.getReservedPlace().getAndAdd(-quantityToUnload);
                TimeUnit.SECONDS.sleep(2);
                LOGGER.info("Unloaded " + quantityToUnload + " containers. " + this);
            }
        } catch (InterruptedException e) {
            LOGGER.warn(this + " is interrupted! " + containerQuantity + " containers not unloaded!");
            port.releasePier(pier);
        }
    }

    private void load() {
        int quantityToLoad;
        int missingQuantity;
        try {
            while (containerQuantity != capacity) {
                missingQuantity = capacity - containerQuantity;
                quantityToLoad = Math.min(missingQuantity, loadUnloadSpeed);
                containerQuantity += quantityToLoad;
                port.getContainerQuantity().getAndAdd(-quantityToLoad);
                port.getReservedContainerQuantity().getAndAdd(-quantityToLoad);
                TimeUnit.SECONDS.sleep(2);
                LOGGER.info("Loaded " + quantityToLoad + " containers. " + this);
            }
        } catch (InterruptedException e) {
            missingQuantity = capacity - containerQuantity;
            LOGGER.warn(this + " is interrupted! " + missingQuantity + " containers not loaded!");
            port.releasePier(pier);
        }
    }
}
