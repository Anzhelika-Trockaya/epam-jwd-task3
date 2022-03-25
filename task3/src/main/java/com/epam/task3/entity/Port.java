package com.epam.task3.entity;

import com.epam.task3.property.ProjectProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Port implements Serializable {
    private static final long serialVersionUID = 2845995552020563499L;
    private static final Logger LOGGER = LogManager.getLogger();
    public static Port instance;
    private static final AtomicBoolean isInitialized = new AtomicBoolean();
    private static final CountDownLatch initialisingLatch = new CountDownLatch(1);
    public static final double MAX_LOAD_PERCENTAGE = 0.75;
    public static final double MIN_LOAD_PERCENTAGE = 0.25;
    public static final double OPTIMAL_LOAD_PERCENTAGE = 0.5;
    public static final int REGULATOR_DELAY_MILLISECONDS = 100;
    public static final int REGULATOR_PERIOD_MILLISECONDS = 500;
    private static final ReentrantLock lock = new ReentrantLock(true);
    private final Condition condition = lock.newCondition();

    private final Deque<Pier> availablePiers;
    private final Deque<Pier> occupiedPiers;
    private final int capacity;
    private final AtomicInteger containerQuantity;
    private final AtomicInteger reservedContainerQuantity;
    private final AtomicInteger reservedPlace;

    public static Port getInstance() {
        if (instance == null) {
            while (isInitialized.compareAndSet(false, true)) {
                instance = new Port();
                initialisingLatch.countDown();
            }
            try {
                initialisingLatch.await();
            } catch (InterruptedException e) {
                LOGGER.warn(Thread.currentThread() + " is interrupted! ", e);
            }
        }
        return instance;
    }

    private Port() {
        ProjectProperties properties = ProjectProperties.getInstance();
        availablePiers = new ArrayDeque<>();
        int pierQuantity = properties.getPierQuantity();
        for (int i = 0; i < pierQuantity; i++) {
            Pier pier = new Pier();
            availablePiers.addLast(pier);
        }
        capacity = properties.getPortCapacity();
        containerQuantity = new AtomicInteger(properties.getPortContainerQuantity());
        occupiedPiers = new ArrayDeque<>();
        reservedContainerQuantity = new AtomicInteger();
        reservedPlace = new AtomicInteger();
        LOGGER.info("Port created.\t\t\t" + this);
        startRegulator();
    }

    private class TimerPortRegulator extends TimerTask {
        @Override
        public void run() {
            LOGGER.info("TimerPortRegulator is running...");
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
                        LOGGER.info(delta + " containers were brought to the port.\t\t\t" + Port.this);
                    } else if (realContainerQuantity > maxContainerQuantity) {
                        int notReservedContainers = containerQuantity.get() - reservedContainerQuantity.get();
                        if (notReservedContainers > 0) {
                            int delta = realContainerQuantity - optimalContainerQuantity;
                            if (notReservedContainers < delta) {
                                delta = notReservedContainers;
                            }
                            containerQuantity.getAndAdd(-delta);
                            LOGGER.info(delta + " containers were taken from the port.\t\t\t" + Port.this);
                        }
                    }
                } finally {
                    condition.signalAll();
                    lock.unlock();
                }
            }
            LOGGER.info("TimerPortRegulator finished running.");
        }
    }

    public Pier getFreePier(Ship ship) throws InterruptedException {
        Pier pier;
        ship.setState(Ship.State.WAITING);
        LOGGER.info("Ship#" + ship.getShipId() + " is waiting\t\t\t" + ship);
        try {
            lock.lock();
            while (availablePiers.size() < 1 || !reserveContainersOrPlaceForShip(ship)) {
                LOGGER.info("Ship#" + ship.getShipId() + " continues to wait.\t\t\t" + ship + " " + this);
                condition.await();
            }
            pier = availablePiers.poll();
            occupiedPiers.addLast(pier);
        } finally {
            lock.unlock();
        }
        LOGGER.info("Ship#" + ship.getShipId() + " moored at the pier:" + pier + "\t\t\t" + ship);
        return pier;
    }

    public void releasePier(Pier pier) {
        try {
            lock.lock();
            occupiedPiers.remove(pier);
            availablePiers.addLast(pier);
            LOGGER.info(pier + " is free.\t\t\t" + this);
        } finally {
            condition.signalAll();
            lock.unlock();
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

    private void startRegulator() {
        TimerPortRegulator regulator = new TimerPortRegulator();
        Timer timer = new Timer(true);
        timer.schedule(regulator, REGULATOR_DELAY_MILLISECONDS, REGULATOR_PERIOD_MILLISECONDS);
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
        if (ship.isFull()) {
            quantity = ship.getContainerQuantity();
            isReserved = reservePlace(quantity);
        } else {
            quantity = ship.getCapacity();
            isReserved = reserveContainers(quantity);
        }
        return isReserved;
    }

    private boolean reserveContainers(int quantity) {
        int availableContainers = countAvailableContainers();
        if (quantity <= availableContainers) {
            reservedContainerQuantity.getAndAdd(quantity);
            LOGGER.debug("Reserved " + quantity + " containers.");
            return true;
        }
        return false;
    }

    private boolean reservePlace(int quantity) {
        int availablePlace = countAvailablePlace();
        if (quantity <= availablePlace) {
            reservedPlace.getAndAdd(quantity);
            LOGGER.debug("Reserved " + quantity + " places for containers.");
            return true;
        }
        return false;
    }
}
