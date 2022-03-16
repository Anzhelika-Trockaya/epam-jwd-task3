package com.epam.task3;

import java.io.Serializable;

public class Ship extends Thread implements Serializable {
    private static final long serialVersionUID = -3491118351213241146L;
    public static final int DEFAULT_CONTAINER_QUANTITY = 0;
    private final long shipId;
    private int capacity;
    private int containerQuantity;
    private State state;
    private Port.Pier pier;

    public enum State {
        CREATED, WAITING, PROCESSING, COMPLETED
    }

    {
        shipId = ShipIdGenerator.generate();
        state = State.CREATED;
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

    public Port.Pier getPier() {
        return pier;
    }

    public void setPier(Port.Pier pier) {
        this.pier = pier;
    }

    //fixme equals.....


    @Override
    public String toString() {//fixme StringBulder
        return "Ship{" +
                "shipId=" + shipId +
                ", capacity=" + capacity +
                ", containerQuantity=" + containerQuantity +
                ", state=" + state +
                ", pier=" + pier +
                '}';
    }

    public void goToPort() {
        Port port = Port.getInstance();
        port.acceptShip(this);
        port.processShip(this);
    }

    @Override
    public void run() {
        goToPort();
    }
}
