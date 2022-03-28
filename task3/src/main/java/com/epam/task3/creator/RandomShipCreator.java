package com.epam.task3.creator;

import com.epam.task3.entity.Port;
import com.epam.task3.entity.Ship;
import com.epam.task3.property.ProjectProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomShipCreator {
    private static final Logger LOGGER = LogManager.getLogger();
    private static RandomShipCreator instance;
    private static final double MAX_SHIP_CAPACITY_PERCENTAGE = 0.25;
    private static final double MIN_SHIP_CAPACITY_PERCENTAGE = 0.05;
    private final Port port;
    private final Random random;
    private final int numberOfShip;

    public static RandomShipCreator getInstance() {
        if (instance == null) {
            instance = new RandomShipCreator();
        }
        return instance;
    }

    private RandomShipCreator() {
        port = Port.getInstance();
        ProjectProperties properties = ProjectProperties.getInstance();
        this.numberOfShip = properties.getShipQuantity();
        random = new Random();
    }

    public List<Ship> createShips() {
        List<Ship> ships = new ArrayList<>(numberOfShip);
        Ship currentShip;
        for (int i = 0; i < numberOfShip; i++) {
            currentShip = createShip();
            ships.add(currentShip);
        }
        LOGGER.info("Ships created. " + ships);
        return ships;
    }

    private Ship createShip() {
        boolean isShipFull = random.nextBoolean();
        int capacity = generateCapacity();
        return new Ship(capacity, isShipFull);
    }

    private int generateCapacity() {
        int minValue = (int) Math.ceil(MIN_SHIP_CAPACITY_PERCENTAGE * port.getCapacity());
        int maxValue = (int) Math.floor(MAX_SHIP_CAPACITY_PERCENTAGE * port.getCapacity());
        int difference = maxValue - minValue;
        return minValue + random.nextInt(++difference);
    }
}
