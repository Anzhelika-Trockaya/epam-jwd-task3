package com.epam.task3;

import java.util.ArrayList;
import java.util.List;

public class Main2 {
    public static void main(String[] args) {
        Port port = Port.getInstance();
        List<Ship> ships = new ArrayList<>();
        ships.add(new Ship(250, 250));
        ships.add(new Ship(250, 250));
        ships.add(new Ship(100, 100));
        for (Ship ship : ships) {
            ship.start();
        }
    }
}
