package com.epam.task3.regulator;

import com.epam.task3.entity.Port;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.TimerTask;

public class TimerPortRegulator extends TimerTask {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void run() {
        Port port = Port.getInstance();
        LOGGER.info("TimerPortRegulator is running...");
        port.regulateContainersQuantity();
    }
}
