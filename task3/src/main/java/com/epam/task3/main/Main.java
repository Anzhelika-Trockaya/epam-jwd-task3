package com.epam.task3.main;

import com.epam.task3.creator.RandomShipCreator;
import com.epam.task3.entity.Ship;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        RandomShipCreator shipCreator = RandomShipCreator.getInstance();
        List<Ship> ships = shipCreator.createShips();
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (Ship ship : ships) {
            executorService.execute(ship);
        }
        executorService.shutdown();
    }
}
