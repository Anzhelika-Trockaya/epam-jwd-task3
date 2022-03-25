package com.epam.task3.property;

import com.epam.task3.exception.PortException;
import com.epam.task3.util.ResourcePathUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ProjectProperties {
    private static final Logger LOGGER = LogManager.getLogger();
    private static ProjectProperties instance;
    private static final String PROPERTIES_FILE_NAME = "port.properties";
    private static final int DEFAULT_PORT_CAPACITY = 1000;
    private static final int DEFAULT_PORT_CONTAINER_QUANTITY = 500;
    private static final int DEFAULT_PIER_QUANTITY = 2;
    private static final int DEFAULT_SHIP_QUANTITY = 5;

    private int portCapacity;
    private int portContainerQuantity;
    private int pierQuantity;
    private int shipQuantity;

    public static ProjectProperties getInstance() {
        if (instance == null) {
            instance = new ProjectProperties();
        }
        return instance;
    }

    private ProjectProperties() {
        try {
            loadFromFile();
            LOGGER.info("Properties loaded. " + this);
        } catch (IOException | PortException exception) {
            LOGGER.warn("Properties not loaded! Default values are used. " + this, exception);
            defaultInitialize();
        }
    }

    public int getPortCapacity() {
        return portCapacity;
    }

    public int getPortContainerQuantity() {
        return portContainerQuantity;
    }

    public int getPierQuantity() {
        return pierQuantity;
    }

    public int getShipQuantity() {
        return shipQuantity;
    }

    @Override
    public String toString() {
        String className = this.getClass().getSimpleName();
        StringBuilder builder = new StringBuilder(className);
        builder.append("{portCapacity=").append(portCapacity).
                append(", portContainerQuantity=").append(portContainerQuantity).
                append(", pierQuantity=").append(pierQuantity).
                append(", shipQuantity=").append(shipQuantity).append('}');
        return builder.toString();
    }

    private void loadFromFile() throws PortException, IOException {
        String filePath;
        filePath = ResourcePathUtil.getResourcePath(PROPERTIES_FILE_NAME);
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            Properties properties = new Properties();
            properties.load(fileInputStream);
            pierQuantity = Integer.parseInt(properties.getProperty("pierQuantity"));
            portCapacity = Integer.parseInt(properties.getProperty("capacity"));
            portContainerQuantity = Integer.parseInt(properties.getProperty("containerQuantity"));
            shipQuantity = Integer.parseInt(properties.getProperty("shipQuantity"));
        }
    }

    private void defaultInitialize() {
        portCapacity = DEFAULT_PORT_CAPACITY;
        portContainerQuantity = DEFAULT_PORT_CONTAINER_QUANTITY;
        pierQuantity = DEFAULT_PIER_QUANTITY;
        shipQuantity = DEFAULT_SHIP_QUANTITY;
    }
}
