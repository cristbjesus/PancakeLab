package org.pancakelab.exception;

public class UnknownBuildingException extends PancakeLabException {
    private final int building;

    public UnknownBuildingException(int building) {
        super("Building %d is not registered.".formatted(building));
        this.building = building;
    }

    public int getBuilding() {
        return building;
    }
}
