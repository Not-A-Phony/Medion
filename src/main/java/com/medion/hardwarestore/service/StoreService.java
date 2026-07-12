package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
import com.medion.hardwarestore.domain.store.StoreStatus;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.Role;
import com.medion.hardwarestore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private static final int EARTH_RADIUS_KM = 6371;

//    @Cacheable(value = "stores")
    public List<Store> getAllActiveStores() {
        return storeRepository.findAll().stream()
                .filter(Store::getIsActive)
                .filter(s -> s.getStatus() == StoreStatus.APPROVED)
                .toList();
    }

    public List<Store> getAllStores() {
        return storeRepository.findAll();
    }

    public Store getStoreById(UUID id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + id));
    }

//    @CacheEvict(value = "stores", allEntries = true)
    public Store createStore(Store store, UUID ownerId) {
        store.setOwnerId(ownerId);
        return storeRepository.save(store);
    }
    
    public List<Store> getStoresByOwnerId(UUID ownerId) {
        return storeRepository.findByOwnerId(ownerId);
    }

//    @CacheEvict(value = "stores", allEntries = true)
    public Store updateStore(UUID id, Store updatedDetails, User user) {
        Store existingStore = getStoreById(id);
        if (user.getRole() == Role.STORE_VENDOR && !user.getId().equals(existingStore.getOwnerId())) {
            throw new com.medion.hardwarestore.exception.BusinessException("You can only update your own store");
        }
        existingStore.setName(updatedDetails.getName());
        existingStore.setAddress(updatedDetails.getAddress());
        existingStore.setLatitude(updatedDetails.getLatitude());
        existingStore.setLongitude(updatedDetails.getLongitude());
        if (updatedDetails.getSubscriptionType() != null) {
            existingStore.setSubscriptionType(updatedDetails.getSubscriptionType());
        }
        if (updatedDetails.getCommissionRate() != null) {
            existingStore.setCommissionRate(updatedDetails.getCommissionRate());
        }
        return storeRepository.save(existingStore);
    }

//    @CacheEvict(value = "stores", allEntries = true)
    public void deleteStore(UUID id, User user) {
        Store existingStore = getStoreById(id);
        if (user.getRole() == Role.STORE_VENDOR && !user.getId().equals(existingStore.getOwnerId())) {
            throw new com.medion.hardwarestore.exception.BusinessException("You can only delete your own store");
        }
        existingStore.setIsActive(false);
        storeRepository.save(existingStore);
    }
    
//    @CacheEvict(value = "stores", allEntries = true)
    public Store approveStore(UUID id) {
        Store existingStore = getStoreById(id);
        existingStore.setStatus(StoreStatus.APPROVED);
        return storeRepository.save(existingStore);
    }
    
//    @CacheEvict(value = "stores", allEntries = true)
    public Store rejectStore(UUID id) {
        Store existingStore = getStoreById(id);
        existingStore.setStatus(StoreStatus.REJECTED);
        return storeRepository.save(existingStore);
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
