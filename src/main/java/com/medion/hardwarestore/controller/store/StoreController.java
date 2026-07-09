package com.medion.hardwarestore.controller.store;

import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final UserRepository userRepository;

    public record StoreDto(UUID id, String name, String address, Double latitude, Double longitude) {}
    public record CreateStoreRequest(
            @NotBlank(message = "Store name is required") String name, 
            @NotBlank(message = "Store address is required") String address, 
            @NotNull(message = "Latitude is required") Double latitude, 
            @NotNull(message = "Longitude is required") Double longitude, 
            com.medion.hardwarestore.domain.store.SubscriptionType subscriptionType) {}

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
    public ResponseEntity<StoreDto> createStore(@Valid @RequestBody CreateStoreRequest request) {
        User user = getCurrentUser();
        Store store = Store.builder()
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .subscriptionType(request.subscriptionType() != null ? request.subscriptionType() : com.medion.hardwarestore.domain.store.SubscriptionType.COMMISSION)
                .isActive(true)
                .build();
        
        Store savedStore = storeService.createStore(store, user.getId());
        return ResponseEntity.ok(mapToDto(savedStore));
    }
    
    @GetMapping("/my-store")
    public ResponseEntity<StoreDto> getMyStore() {
        User user = getCurrentUser();
        Store store = storeService.getStoreByOwnerId(user.getId());
        return ResponseEntity.ok(mapToDto(store));
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

    private User getCurrentUser() {
        return userRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    User dummy = User.builder()
                            .username("guest")
                            .email("guest@example.com")
                            .firstName("Guest")
                            .lastName("User")
                            .role(com.medion.hardwarestore.domain.user.Role.CUSTOMER)
                            .password("nopassword")
                            .build();
                    return userRepository.save(dummy);
                });
    }
}
