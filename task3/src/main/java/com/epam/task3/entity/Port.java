package com.epam.task3.entity;

import com.epam.task3.exception.PortException;
import com.epam.task3.util.ResourcePathUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Port implements Serializable {
    private static final long serialVersionUID = 2845995552020563499L;
    private static final Logger LOGGER = LogManager.getLogger();
    public static Port instance;
    private static final AtomicBoolean isInitialized = new AtomicBoolean();
    public static final double MAX_LOAD_PERCENTAGE = 0.75;
    public static final double MIN_LOAD_PERCENTAGE = 0.25;
    public static final double OPTIMAL_LOAD_PERCENTAGE = 0.5;
    private static final String PROPERTIES_FILE_NAME = "port.properties";
    private static final ReentrantLock lock = new ReentrantLock(true);
    private final Condition condition = lock.newCondition();

    private final Deque<Pier> availablePiers;
    private final Deque<Pier> occupiedPiers;
    private final int capacity;
    private final AtomicInteger containerQuantity;
    private final AtomicInteger reservedContainerQuantity;
    private final AtomicInteger reservedPlace;


    public static Port getInstance() {
        if (!isInitialized.get()) {
            try {
                lock.lock();
                if (instance == null) {
                    instance = new Port();
                    isInitialized.set(true);
                }
            } finally {
                lock.unlock();
            }
        }
        return instance;
    }

    private Port() {
        String filePath;
        try {
            filePath = ResourcePathUtil.getResourcePath(PROPERTIES_FILE_NAME);
        } catch (PortException exception) {
            LOGGER.fatal("Properties file not found!", exception);
            throw new RuntimeException(exception);//todo?
        }
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            Properties properties = new Properties();
            properties.load(fileInputStream);
            availablePiers = new ArrayDeque<>();
            int pierQuantity = Integer.parseInt(properties.getProperty("pierQuantity"));
            for (int i = 0; i < pierQuantity; i++) {
                Pier pier = new Pier();
                availablePiers.addLast(pier);
            }
            capacity = Integer.parseInt(properties.getProperty("capacity"));
            containerQuantity = new AtomicInteger(Integer.parseInt(properties.getProperty("containerQuantity")));
            occupiedPiers = new ArrayDeque<>();
            reservedContainerQuantity = new AtomicInteger();
            reservedPlace = new AtomicInteger();
            LOGGER.info("Port created. " + this);
        } catch (IOException exception) {
            LOGGER.fatal("Properties not loaded!", exception);
            throw new RuntimeException(exception);//todo?
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public AtomicInteger getContainerQuantity() {
        return containerQuantity;
    }

    public AtomicInteger getReservedContainerQuantity() {
        return reservedContainerQuantity;
    }

    public AtomicInteger getReservedPlace() {
        return reservedPlace;
    }

    public Optional<Pier> getFreePier(Ship ship) {
        ship.setState(Ship.State.WAITING);
        LOGGER.info(ship + " is waiting");
        try {
            lock.lock();
            while (availablePiers.size() < 1 || !reserveContainersOrPlaceForShip(ship)) {
                LOGGER.info(ship + " is waiting. " + this);
                condition.await();
            }
            Pier pier = availablePiers.poll();
            if (pier != null) {
                occupiedPiers.addLast(pier);
            }
            return pier != null ? Optional.of(pier) : Optional.empty();
        } catch (InterruptedException e) {
            LOGGER.warn(ship + " was interrupted! " + e);
            Pier pier = ship.getPier();
            if (pier != null) {
                releasePier(pier);
            }
        } finally {
            lock.unlock();
        }
        return Optional.empty();
    }

    public void releasePier(Pier pier) {
        try {
            lock.lock();
            availablePiers.addLast(pier);
            occupiedPiers.remove(pier);
            LOGGER.info(pier + " is free. " + this);
        } finally {
            condition.signalAll();
            lock.unlock();
        }
    }

    public void regulateContainersQuantity() {
        int maxContainerQuantity = (int) (capacity * MAX_LOAD_PERCENTAGE);
        int minContainerQuantity = (int) (capacity * MIN_LOAD_PERCENTAGE);
        int realContainerQuantity = containerQuantity.get() + reservedPlace.get();
        if (realContainerQuantity < minContainerQuantity || realContainerQuantity > maxContainerQuantity) {
            try {
                lock.lock();
                realContainerQuantity = containerQuantity.get() + reservedPlace.get();
                int optimalContainerQuantity = (int) (capacity * OPTIMAL_LOAD_PERCENTAGE);
                if (realContainerQuantity < minContainerQuantity) {
                    int delta = optimalContainerQuantity - realContainerQuantity;
                    containerQuantity.getAndAdd(delta);
                    LOGGER.info(delta + " containers were brought to the port. " + this);
                } else if (realContainerQuantity > maxContainerQuantity) {
                    int notReservedContainers = containerQuantity.get() - reservedContainerQuantity.get();
                    if (notReservedContainers > 0) {
                        int delta = realContainerQuantity - optimalContainerQuantity;
                        if (notReservedContainers < delta) {
                            delta = notReservedContainers;
                        }
                        containerQuantity.getAndAdd(-delta);
                        LOGGER.info(delta + " containers were taken from the port. " + this);
                    }
                }
            } finally {
                condition.signalAll();
                lock.unlock();
            }
        }
    }

    @Override
    public String toString() {
        String className = this.getClass().getSimpleName();
        StringBuilder builder = new StringBuilder(className);
        builder.append("{capacity=").append(capacity).
                append(", availablePiers=").append(availablePiers).
                append(", occupiedPiers=").append(occupiedPiers).
                append(", containerQuantity=").append(containerQuantity).
                append(", reservedContainerQuantity=").append(reservedContainerQuantity).
                append(", reservedPlace=").append(reservedPlace).
                append(", condition=").append(condition).
                append('}');
        return builder.toString();
    }

    private int countAvailableContainers() {
        return containerQuantity.get() - reservedContainerQuantity.get();
    }

    private int countAvailablePlace() {
        return capacity - containerQuantity.get() - reservedPlace.get();
    }

    private boolean reserveContainersOrPlaceForShip(Ship ship) {
        boolean isReserved;
        int quantity;
        if (ship.getContainerQuantity() == 0) {
            quantity = ship.getCapacity();
            isReserved = reserveContainers(quantity);
        } else {
            quantity = ship.getContainerQuantity();
            isReserved = reservePlace(quantity);
        }
        return isReserved;
    }

    private boolean reserveContainers(int quantity) {
        int availableContainers = countAvailableContainers();
        if (quantity <= availableContainers) {
            reservedContainerQuantity.getAndAdd(quantity);
            return true;
        }
        return false;
    }

    private boolean reservePlace(int quantity) {
        int availablePlace = countAvailablePlace();
        if (quantity <= availablePlace) {
            reservedPlace.getAndAdd(quantity);
            return true;
        }
        return false;
    }
}
