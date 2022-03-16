package com.epam.task3;

import com.epam.task3.util.PierIdGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Port implements Serializable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long serialVersionUID = 2845995552020563499L;
    public static Port instance;
    public static final int DEFAULT_PIER_QUANTITY = 3;
    public static final int DEFAULT_CAPACITY = 1000;
    public static final int DEFAULT_CONTAINER_QUANTITY = 500;
    public static final int DEFAULT_LOAD_UNLOAD_SPEED = 30;
    private static final ReentrantLock LOCK = new ReentrantLock(true);
    private final Condition condition = LOCK.newCondition();

    private Deque<Pier> availablePiers;
    private int capacity;
    private int loadUnloadSpeed;
    private AtomicInteger containerQuantity;
    private AtomicInteger reservedContainerQuantity;
    private AtomicInteger reservedPlace;


    public static Port getInstance() {
        try {
            LOCK.lock();
            if (instance == null) {
                instance = new Port();
            }
        } finally {
            LOCK.unlock();
        }
        return instance;
    }

    private Port() {
        capacity = DEFAULT_CAPACITY;
        containerQuantity = new AtomicInteger(DEFAULT_CONTAINER_QUANTITY);
        availablePiers = new ArrayDeque<>();
        for (int i = 0; i < DEFAULT_PIER_QUANTITY; i++) {
            Pier pier = new Pier();
            availablePiers.addLast(pier);
        }
        reservedContainerQuantity = new AtomicInteger();
        reservedPlace = new AtomicInteger();
        loadUnloadSpeed = DEFAULT_LOAD_UNLOAD_SPEED;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Deque<Pier> getAvailablePiers() {
        return availablePiers;
    }

    public void setAvailablePiers(Deque<Pier> availablePiers) {
        if (availablePiers != null) {
            this.availablePiers = availablePiers;
        }
    }

    public void addAvailablePier() {
        Pier pier = new Pier();
        availablePiers.addLast(pier);
    }

    public AtomicInteger getContainerQuantity() {
        return containerQuantity;
    }

    public void setContainerQuantity(AtomicInteger containerQuantity) {
        this.containerQuantity = containerQuantity;
    }

    //fixme equals, toString...

    public void acceptShip(Ship ship) {
        ship.setState(Ship.State.WAITING);
        LOGGER.info(ship + " is waiting");
        try {
            LOCK.lock();
            if (ship.getContainerQuantity() == 0) {
                while (getAvailableContainers() < ship.getCapacity()) {//fixme if?
                    condition.await();
                }
                reservedContainerQuantity.getAndAdd(ship.getCapacity());
            } else {
                while (getAvailablePlace() < ship.getContainerQuantity()) {//fixme if?
                    condition.await();
                }
                reservedPlace.getAndAdd(ship.getContainerQuantity());
            }
            Pier pier;
            while ((pier = availablePiers.poll()) == null) {
                LOGGER.info(ship + " is waiting: all pier are taken");
                TimeUnit.MILLISECONDS.sleep(1000);
            }
            ship.setPier(pier);
            LOGGER.info(ship + " moored at the pier:" + pier);
        } catch (InterruptedException e) {
            LOGGER.warn(ship + " was interrupted! " + e);
            releasePier(ship);
        } finally {
            condition.signalAll();
            LOCK.unlock();
        }
    }

    public void processShip(Ship ship) {
        ship.setState(Ship.State.PROCESSING);
        if (ship.getContainerQuantity() == 0) {
            LOGGER.info(ship + " starts load.");
            loadShip(ship);
        } else {
            LOGGER.info(ship + " starts unload.");
            unloadShip(ship);
        }
        ship.setState(Ship.State.COMPLETED);
        LOGGER.info("Completed. "+ship+"\n\t\t Available containers = "+getAvailableContainers()+" availablePlace = "+getAvailablePlace());//fixme
        releasePier(ship);
    }

    private void unloadShip(Ship ship) {
        int quantityToUnload;
        try {
            while (ship.getContainerQuantity() != 0) {
                TimeUnit.MILLISECONDS.sleep(200);
                quantityToUnload = Math.min(ship.getContainerQuantity(), loadUnloadSpeed);
                ship.setContainerQuantity(ship.getContainerQuantity() - quantityToUnload);
                containerQuantity.getAndAdd(quantityToUnload);
                reservedPlace.getAndAdd(-quantityToUnload);
                LOGGER.info("Unloaded " + quantityToUnload + " containers. " + ship);
            }
        } catch (InterruptedException e) {
            LOGGER.warn(ship + " is interrupted! " + ship.getContainerQuantity() + " containers not unloaded!");
            releasePier(ship);
        }
    }

    private void loadShip(Ship ship) {
        int quantityToLoad;
        int missingQuantity;
        try {
            while (ship.getContainerQuantity() != ship.getCapacity()) {
                TimeUnit.MILLISECONDS.sleep(200);
                missingQuantity = ship.getCapacity() - ship.getContainerQuantity();
                quantityToLoad = Math.min(missingQuantity, loadUnloadSpeed);
                ship.setContainerQuantity(ship.getContainerQuantity() + quantityToLoad);
                containerQuantity.getAndAdd(-quantityToLoad);
                reservedContainerQuantity.getAndAdd(-quantityToLoad);
                LOGGER.info("Loaded " + quantityToLoad + " containers. " + ship);
            }
        } catch (InterruptedException e) {
            missingQuantity = ship.getCapacity() - ship.getContainerQuantity();
            LOGGER.warn(ship + " is interrupted! " + missingQuantity + " containers not loaded!");
            releasePier(ship);
        }
    }

    private void releasePier(Ship ship) {
        Pier pier = ship.getPier();
        if (pier != null) {
            availablePiers.addLast(pier);
        }
    }


    private int getAvailableContainers() {
        return containerQuantity.get() - reservedContainerQuantity.get();
    }

    private int getAvailablePlace() {
        return capacity - containerQuantity.get() - reservedPlace.get();
    }

    public static class Pier implements Serializable {
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
}
