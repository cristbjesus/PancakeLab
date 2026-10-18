package org.pancakelab.model;

import org.junit.jupiter.api.Test;
import org.pancakelab.exception.UnknownBuildingException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BuildingRegistryTest {
    private final BuildingRegistry registry = new BuildingRegistry(Set.of(10, 20));

    @Test
    public void GivenRegisteredBuilding_WhenCreatingDeliveryAddress_ThenAddressCreatedWithCorrectData() {
        // exercise
        DeliveryAddress address = registry.addressFor(10, 5);

        // verify
        assertEquals(10, address.building());
        assertEquals(5, address.room());
    }

    @Test
    public void GivenUnregisteredBuilding_WhenCreatingDeliveryAddress_ThenUnknownBuildingExceptionThrown() {
        // exercise
        UnknownBuildingException exception =
                assertThrows(UnknownBuildingException.class, () -> registry.addressFor(30, 5));

        // verify
        assertEquals(30, exception.getBuilding());
    }

    @Test
    public void GivenBuildingAddedToSourceSetLater_WhenCreatingDeliveryAddress_ThenUnknownBuildingExceptionThrown() {
        // setup
        Set<Integer> buildings = new HashSet<>(Set.of(10));
        BuildingRegistry registryFromMutableSet = new BuildingRegistry(buildings);
        buildings.add(30);

        // exercise & verify
        assertThrows(UnknownBuildingException.class, () -> registryFromMutableSet.addressFor(30, 5));
    }
}
