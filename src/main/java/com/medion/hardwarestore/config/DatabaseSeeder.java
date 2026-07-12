package com.medion.hardwarestore.config;

import com.medion.hardwarestore.domain.category.Category;
import com.medion.hardwarestore.domain.category.CategoryRepository;
import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.product.ProductRepository;
import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
import com.medion.hardwarestore.domain.store.StoreStatus;
import com.medion.hardwarestore.domain.user.Role;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static final Random random = new Random();
    private static final Faker faker = new Faker();

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // Setup Users (keep existing ones or create if none exist)
        setupUsers();

        // If products exist, clear them to reseed properly
        if (productRepository.count() > 0 || storeRepository.count() > 0) {
            log.info("Purging existing stub products and stores...");
            jdbcTemplate.execute("TRUNCATE TABLE reviews CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE order_items CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE store_payments CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE store_followers CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE products CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE stores CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE categories CASCADE");
        }

        log.info("Starting massive data seeding (18 Stores, 500 Products)...");

        // 1. Create Categories
        List<Category> categories = createCategories();

        // 2. Generate 18 Stores
        List<User> vendors = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STORE_VENDOR)
                .toList();

        if (vendors.isEmpty()) {
            throw new RuntimeException("No vendors found to assign stores.");
        }

        Map<Store, String> storeCategoryMap = generateStores(vendors);

        // 3. Generate 500 Products across the 18 Stores
        generateProducts(storeCategoryMap);

        log.info("Database seeding completed successfully!");
    }

    private void setupUsers() {
        if (userRepository.findByEmail("admin@medion.com").isEmpty()) {
            User admin = User.builder()
                    .email("admin@medion.com")
                    .password(passwordEncoder.encode("password123"))
                    .firstName("Super")
                    .lastName("Admin")
                    .username("admin")
                    .role(Role.ADMIN)
                    .build();
            userRepository.saveAndFlush(admin);
        }

        if (userRepository.findByEmail("vendor1@medion.com").isEmpty()) {
            User vendor1 = User.builder()
                    .email("vendor1@medion.com")
                    .password(passwordEncoder.encode("password123"))
                    .firstName("Bob")
                    .lastName("Builder")
                    .username("bob_builder")
                    .role(Role.STORE_VENDOR)
                    .build();
            userRepository.saveAndFlush(vendor1);
        }

        if (userRepository.findByEmail("vendor2@medion.com").isEmpty()) {
            User vendor2 = User.builder()
                    .email("vendor2@medion.com")
                    .password(passwordEncoder.encode("password123"))
                    .firstName("Alice")
                    .lastName("Smith")
                    .username("alice_smith")
                    .role(Role.STORE_VENDOR)
                    .build();
            userRepository.saveAndFlush(vendor2);
        }
    }

    private List<Category> createCategories() {
        List<Category> list = new ArrayList<>();
        
        // Requested New Categories
        list.add(createCategory("Local Services", "local-services", "SERVICE", true, "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=800&q=80"));
        list.add(createCategory("Furniture", "furniture", "PRODUCT", true, "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800&q=80"));
        list.add(createCategory("Electronics", "electronics", "PRODUCT", true, "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=800&q=80"));
        list.add(createCategory("Computers & Phones", "computers-phones", "PRODUCT", true, "https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=800&q=80"));
        list.add(createCategory("Kitchenware", "kitchenware", "PRODUCT", true, "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=800&q=80"));
        list.add(createCategory("Clothes & Shoes", "clothes-shoes", "PRODUCT", true, "https://images.unsplash.com/photo-1445205170230-053b83016050?w=800&q=80"));
        list.add(createCategory("Tools & Building Materials", "tools-building-materials", "PRODUCT", true, "https://images.unsplash.com/photo-1584284852158-768f5c357731?w=800&q=80"));

        // Keep existing ones for store seeding compat
        list.add(createCategory("Power Tools", "power-tools", "PRODUCT", true, "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=800&q=80"));
        list.add(createCategory("Hand Tools", "hand-tools", "PRODUCT", true, "https://images.unsplash.com/photo-1586864387967-d02ef85d93e8?w=800&q=80"));
        list.add(createCategory("Lumber & Building", "lumber-building", "PRODUCT", true, "https://images.unsplash.com/photo-1584284852158-768f5c357731?w=800&q=80"));
        list.add(createCategory("Plumbing", "plumbing", "PRODUCT", true, "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800&q=80"));
        list.add(createCategory("Electrical", "electrical", "PRODUCT", true, "https://images.unsplash.com/photo-1555664424-778a1e5e1b48?w=800&q=80"));
        list.add(createCategory("Hardware & Fasteners", "hardware-fasteners", "PRODUCT", true, "https://images.unsplash.com/photo-1508873699372-7aeab60b44ab?w=800&q=80"));
        
        // Service Categories
        list.add(createCategory("Plumbing Services", "plumbing-services", "SERVICE", true, "https://images.unsplash.com/photo-1607427293702-036933b41fa6?w=800&q=80"));
        list.add(createCategory("Electrical Services", "electrical-services", "SERVICE", true, "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800&q=80"));
        list.add(createCategory("Carpentry Services", "carpentry-services", "SERVICE", true, "https://images.unsplash.com/photo-1621644788350-fc89b70bb19d?w=800&q=80"));
        
        return list;
    }

    private Category createCategory(String name, String slug, String type, boolean isFeatured, String imageUrl) {
        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .type(type)
                .isFeatured(isFeatured)
                .status("ACTIVE")
                .imageUrl(imageUrl)
                .build();
        return categoryRepository.save(category);
    }

    private Map<Store, String> generateStores(List<User> vendors) {
        String[] storeNames = {
                "Apex Building Supplies", "Titan Construction Materials", "Prime Lumber Hub", // Building
                "Ace Hand Tools", "Precision Tool Works", "Reliable Handyman Supplies", // Hand Tools
                "VoltEdge Electricals", "Spark & Wire Tech", "MegaVolt Supplies", // Electrical
                "AquaFlow Plumbing", "PipeMasters Hub", "HydroTech Sanitary", // Plumbing
                "ProGear Power Tools", "MaxDrill Equipment", "ForcePower Machinery", // Power Tools
                "SolidFast Hardware", "Nut & Bolt Depo", "IronClad Fasteners", // Fasteners
                "Pro Fix Plumbing", "Quick Leak Repairs", "Master Plumbers KE", // Plumbing Services
                "Bright Sparks Electrical", "Wire Wizards", "Safe Voltage Services", // Electrical Services
                "WoodCraft Artisans", "Prime Carpentry", "Custom Woodworks" // Carpentry Services
        };

        String[] storeTypes = {
                "Lumber & Building", "Lumber & Building", "Lumber & Building",
                "Hand Tools", "Hand Tools", "Hand Tools",
                "Electrical", "Electrical", "Electrical",
                "Plumbing", "Plumbing", "Plumbing",
                "Power Tools", "Power Tools", "Power Tools",
                "Hardware & Fasteners", "Hardware & Fasteners", "Hardware & Fasteners",
                "Plumbing Services", "Plumbing Services", "Plumbing Services",
                "Electrical Services", "Electrical Services", "Electrical Services",
                "Carpentry Services", "Carpentry Services", "Carpentry Services"
        };

        Map<Store, String> storeCategoryMap = new java.util.LinkedHashMap<>();

        for (int i = 0; i < storeNames.length; i++) {
            User vendor = vendors.get(i % vendors.size());
            
            // Generate realistic Nairobi/Kenya coordinates for variety
            double lat = -1.2921 + (random.nextDouble() * 0.1 - 0.05);
            double lon = 36.8219 + (random.nextDouble() * 0.1 - 0.05);

            String logoUrl = "https://ui-avatars.com/api/?name=" + storeNames[i].replace(" ", "+") + "&background=random&size=200";
            String bannerUrl = getRandomImageForCategory(storeTypes[i]);
            String bio = "Welcome to " + storeNames[i] + ". We are dedicated to providing the best " + storeTypes[i] + " in Nairobi. Customer satisfaction is our priority.";
            List<String> adsUrls = List.of(getRandomImageForCategory(storeTypes[i]), getRandomImageForCategory(storeTypes[i]));

            Store store = Store.builder()
                    .name(storeNames[i])
                    .address(faker.address().streetAddress() + ", Nairobi")
                    .latitude(lat)
                    .longitude(lon)
                    .ownerId(vendor.getId())
                    .status(StoreStatus.APPROVED)
                    .averageRating(4.0 + random.nextDouble())
                    .reviewCount(random.nextInt(500) + 10)
                    .isFeatured(random.nextBoolean())
                    .isActive(true)
                    .logoUrl(logoUrl)
                    .bannerUrl(bannerUrl)
                    .bio(bio)
                    .adsUrls(adsUrls)
                    .build();
            
            store = storeRepository.saveAndFlush(store);
            storeCategoryMap.put(store, storeTypes[i]);
            
            // Follow logic mock (10-50 random followers per store, simulating via jdbc since we just need the count for analytics, or creating entities)
            int followersCount = random.nextInt(40) + 10;
            for(int j=0; j<followersCount; j++) {
                jdbcTemplate.update("INSERT INTO store_followers (store_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                        store.getId(), vendor.getId()); // Using vendor id just for mock since we only have a few users
            }
        }
        return storeCategoryMap;
    }

    private void generateProducts(Map<Store, String> storeCategoryMap) {
        int productsPerStore = 500 / storeCategoryMap.size(); // ~27
        int totalCreated = 0;

        for (Map.Entry<Store, String> entry : storeCategoryMap.entrySet()) {
            Store store = entry.getKey();
            String storeCategory = entry.getValue();

            for (int i = 0; i < productsPerStore; i++) {
                String productName = generateRealisticProductName(storeCategory);
                String description = faker.lorem().paragraph(4) + "\n\nFeatures:\n- " + faker.commerce().material() + "\n- Durable build\n- Professional Grade";
                String sku = faker.bothify("???-####-???").toUpperCase();
                double price = 10.0 + (500.0 - 10.0) * random.nextDouble();
                
                String imgUrl = getRandomImageForCategory(storeCategory);

                Product product = Product.builder()
                        .name(productName)
                        .description(description)
                        .sku(sku)
                        .price(BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP))
                        .currency("KES")
                        .stockQuantity(random.nextInt(200) + 5)
                        .store(store)
                        .isActive(true)
                        .averageRating(3.5 + (random.nextDouble() * 1.5))
                        .reviewCount(random.nextInt(100))
                        .imageUrls(List.of(imgUrl))
                        .build();

                productRepository.save(product);
                totalCreated++;
            }
        }
        
        log.info("Total products created: {}", totalCreated);
    }

    private String generateRealisticProductName(String category) {
        return switch (category) {
            case "Power Tools" -> faker.options().option("DeWalt 20V MAX Cordless Drill", "Makita 18V Angle Grinder", "Milwaukee M18 Impact Driver", "Bosch Laser Level", "Ryobi Cordless Saw", "Craftsman Router", "Hitachi Hammer Drill", "Ridgid Circular Saw", "Black+Decker Jigsaw");
            case "Hand Tools" -> faker.options().option("Stanley 25-Foot Tape Measure", "Crescent 10-Inch Adjustable Wrench", "Irwin Vise-Grip Pliers", "Estwing 16 oz Claw Hammer", "Klein Tools Screwdriver Set", "Channellock Pliers", "Husky Socket Set", "Kobalt Utility Knife", "GearWrench Ratchet");
            case "Lumber & Building" -> faker.options().option("Premium 2x4 Lumber Pine", "Drywall Sheet 4x8", "Plywood 3/4 inch", "Portland Cement 50lb", "Concrete Mix 80lb", "Roofing Shingles Bundle", "Fiberglass Insulation Roll", "Treated Decking Board", "MDF Board 4x8");
            case "Plumbing" -> faker.options().option("PVC Pipe 10ft Schedule 40", "Moen Kitchen Faucet", "Delta Shower Head", "Brass Ball Valve 3/4 inch", "Kohler Toilet", "Water Heater 50 Gallon", "Garbage Disposal 1/2 HP", "Copper Pipe 10ft", "Plumber's Putty");
            case "Electrical" -> faker.options().option("Romex Wire 250ft 12/2", "Leviton Outlet Receptacle", "Square D 20 Amp Breaker", "LED Recessed Light 6-inch", "GFCI Outlet", "Electrical Tape Pack", "Conduit 10ft 1/2 inch", "Switch Plate Cover", "Wire Connectors Assortment");
            case "Hardware & Fasteners" -> faker.options().option("Galvanized Screws 2-inch", "Hex Bolts Assortment", "Heavy Duty Hinges", "Padlock Master Lock", "Cabinet Knobs Set", "Drawer Slides 20-inch", "Gate Latch", "Nails 3-inch Box", "Drywall Anchors Set");
            case "Plumbing Services" -> faker.options().option("Pipe Leak Repair", "Water Heater Installation", "Drain Unclogging", "Toilet Repair & Replacement", "Full Bathroom Plumbing Installation");
            case "Electrical Services" -> faker.options().option("House Wiring & Rewiring", "Circuit Breaker Replacement", "Lighting Fixture Installation", "Electrical Fault Diagnostics", "Generator Installation");
            case "Carpentry Services" -> faker.options().option("Custom Cabinet Making", "Door & Window Frame Installation", "Wooden Floor Repair", "Roof Truss Construction", "Furniture Restoration");
            default -> faker.commerce().productName();
        };
    }

    private String getRandomImageForCategory(String category) {
        String[] powerTools = {
            "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=800&q=80",
            "https://images.unsplash.com/photo-1572981779307-38b8cabb2407?w=800&q=80",
            "https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=800&q=80"
        };
        String[] handTools = {
            "https://images.unsplash.com/photo-1586864387967-d02ef85d93e8?w=800&q=80",
            "https://images.unsplash.com/photo-1540104539488-92a51bbc0410?w=800&q=80",
            "https://images.unsplash.com/photo-1558591710-4b4a1ae0f04d?w=800&q=80",
            "https://images.unsplash.com/photo-1533280801048-fb9e26e0e02c?w=800&q=80"
        };
        String[] building = {
            "https://images.unsplash.com/photo-1584284852158-768f5c357731?w=800&q=80",
            "https://images.unsplash.com/photo-1503387762-592deb58ef4e?w=800&q=80",
            "https://images.unsplash.com/photo-1621644788350-fc89b70bb19d?w=800&q=80"
        };
        String[] plumbing = {
            "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800&q=80",
            "https://images.unsplash.com/photo-1607427293702-036933b41fa6?w=800&q=80"
        };
        String[] electrical = {
            "https://images.unsplash.com/photo-1555664424-778a1e5e1b48?w=800&q=80",
            "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800&q=80"
        };
        String[] hardware = {
            "https://images.unsplash.com/photo-1508873699372-7aeab60b44ab?w=800&q=80",
            "https://images.unsplash.com/photo-1622345512215-081ba831efaf?w=800&q=80"
        };

        return switch (category) {
            case "Power Tools" -> powerTools[random.nextInt(powerTools.length)];
            case "Hand Tools" -> handTools[random.nextInt(handTools.length)];
            case "Lumber & Building" -> building[random.nextInt(building.length)];
            case "Plumbing", "Plumbing Services" -> plumbing[random.nextInt(plumbing.length)];
            case "Electrical", "Electrical Services" -> electrical[random.nextInt(electrical.length)];
            case "Hardware & Fasteners" -> hardware[random.nextInt(hardware.length)];
            case "Carpentry Services" -> building[random.nextInt(building.length)];
            default -> handTools[0];
        };
    }
}
