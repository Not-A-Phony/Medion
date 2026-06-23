package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreService storeService;

    private Store nairobiStore;
    private Store mombasaStore;

    @BeforeEach
    void setUp() {
        nairobiStore = Store.builder()
                .id(UUID.randomUUID())
                .name("Nairobi Central Hardware")
                .latitude(-1.286389)
                .longitude(36.817223)
                .isActive(true)
                .build();

        mombasaStore = Store.builder()
                .id(UUID.randomUUID())
                .name("Mombasa Coastal Hardware")
                .latitude(-4.043477)
                .longitude(39.668206)
                .isActive(true)
                .build();
    }

    @Test
    void testFindNearestStore_ShouldReturnNairobiStore_WhenUserIsInNairobi() {
        when(storeRepository.findAll()).thenReturn(List.of(nairobiStore, mombasaStore));

        // User is somewhere in Nairobi (Westlands)
        double userLat = -1.266667;
        double userLon = 36.800000;

        Store nearestStore = storeService.findNearestStore(userLat, userLon);

        assertEquals("Nairobi Central Hardware", nearestStore.getName());
    }

    @Test
    void testFindNearestStore_ShouldReturnMombasaStore_WhenUserIsInMombasa() {
        when(storeRepository.findAll()).thenReturn(List.of(nairobiStore, mombasaStore));

        // User is somewhere near Mombasa
        double userLat = -4.000000;
        double userLon = 39.600000;

        Store nearestStore = storeService.findNearestStore(userLat, userLon);

        assertEquals("Mombasa Coastal Hardware", nearestStore.getName());
    }
}
