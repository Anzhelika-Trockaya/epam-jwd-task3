package com.epam.task3;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Port port = Port.getInstance();
        List<Ship> ships = new ArrayList<>();
        ships.add(new Ship(100));
        ships.add(new Ship(250, 232));
        ships.add(new Ship(80));
        ships.add(new Ship(150, 100));
        ships.add(new Ship(200, 180));
        ships.add(new Ship(195));
        ships.add(new Ship(84));
        for (Ship ship : ships) {
            ship.start();
        }
    }
}
