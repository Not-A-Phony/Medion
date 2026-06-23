package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
import com.medion.hardwarestore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private static final int EARTH_RADIUS_KM = 6371;

    public List<Store> getAllActiveStores() {
        return storeRepository.findAll().stream()
                .filter(Store::getIsActive)
                .toList();
    }

    public Store getStoreById(UUID id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + id));
    }

    public Store createStore(Store store) {
        return storeRepository.save(store);
    }

    // Use Haversine formula to find the nearest store
    public Store findNearestStore(double userLat, double userLon) {
        List<Store> stores = getAllActiveStores();
        if (stores.isEmpty()) {
            throw new ResourceNotFoundException("No active stores available.");
        }

        return stores.stream()
                .min(Comparator.comparingDouble(store -> calculateHaversineDistance(userLat, userLon, store.getLatitude(), store.getLongitude())))
                .orElseThrow(() -> new ResourceNotFoundException("Could not calculate nearest store"));
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
                   
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
