package com.medion.hardwarestore.controller.store;

import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    public record StoreDto(UUID id, String name, String address, Double latitude, Double longitude) {}
    public record CreateStoreRequest(String name, String address, Double latitude, Double longitude) {}

    @GetMapping
    public ResponseEntity<List<StoreDto>> getAllStores() {
        List<StoreDto> stores = storeService.getAllActiveStores().stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDto> getStoreById(@PathVariable UUID id) {
        Store store = storeService.getStoreById(id);
        return ResponseEntity.ok(mapToDto(store));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreDto> createStore(@RequestBody CreateStoreRequest request) {
        Store store = Store.builder()
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .isActive(true)
                .build();
        
        Store savedStore = storeService.createStore(store);
        return ResponseEntity.ok(mapToDto(savedStore));
    }

    @GetMapping("/nearest")
    public ResponseEntity<StoreDto> getNearestStore(
            @RequestParam double lat,
            @RequestParam double lon) {
        Store nearest = storeService.findNearestStore(lat, lon);
        return ResponseEntity.ok(mapToDto(nearest));
    }

    private StoreDto mapToDto(Store store) {
        return new StoreDto(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getLatitude(),
                store.getLongitude()
        );
    }
}
