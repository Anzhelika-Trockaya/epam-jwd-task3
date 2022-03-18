package com.epam.task3.creator;

import com.epam.task3.entity.Port;
import com.epam.task3.entity.Ship;
import com.epam.task3.exception.PortException;
import com.epam.task3.util.ResourcePathUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class RandomShipCreator {
    private static final Logger LOGGER = LogManager.getLogger();
    private static RandomShipCreator instance;
    private static final double MAX_SHIP_CAPACITY_PERCENTAGE = 0.3;
    private static final double MIN_SHIP_CAPACITY_PERCENTAGE = 0.05;
    private static final String PROPERTIES_FILE_NAME = "port.properties";
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
            this.numberOfShip = Integer.parseInt(properties.getProperty("shipQuantity"));
            random = new Random();
        } catch (IOException exception) {
            LOGGER.fatal("Properties not loaded!", exception);
            throw new RuntimeException(exception);//todo?
        }
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
        boolean fullShip = random.nextBoolean();
        int capacity = generateCapacity();
        Ship ship;
        if (fullShip) {
            ship = new Ship(capacity, capacity);
        } else {
            ship = new Ship(capacity);
        }
        return ship;
    }

    private int generateCapacity() {
        int minValue = (int) Math.ceil(MIN_SHIP_CAPACITY_PERCENTAGE * port.getCapacity());
        int maxValue = (int) Math.floor(MAX_SHIP_CAPACITY_PERCENTAGE * port.getCapacity());
        int difference = maxValue - minValue;
        return minValue + random.nextInt(++difference);
    }
}
