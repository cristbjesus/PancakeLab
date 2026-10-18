package org.pancakelab.model;

import org.pancakelab.exception.UnknownBuildingException;

import java.util.Set;

public class BuildingRegistry {
    private final Set<Integer> buildings;

    public BuildingRegistry(Set<Integer> buildings) {
        this.buildings = Set.copyOf(buildings);
    }

    public DeliveryAddress addressFor(int building, int room) {
        if (!buildings.contains(building)) {
            throw new UnknownBuildingException(building);
        }
        return new DeliveryAddress(building, room);
    }
}
