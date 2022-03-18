package com.epam.task3.main;

import com.epam.task3.creator.RandomShipCreator;
import com.epam.task3.entity.Ship;
import com.epam.task3.regulator.TimerPortRegulator;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        TimerPortRegulator regulator = new TimerPortRegulator();
        Timer timer = new Timer(true);
        timer.schedule(regulator, 200, 7000);
        RandomShipCreator shipCreator = RandomShipCreator.getInstance();
        List<Ship> ships = shipCreator.createShips();
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (Ship ship : ships) {
            executorService.execute(ship);
        }
        executorService.shutdown();
    }
}
