# Payment System Implementation — Guidance for Claude Code

> This file is the authoritative spec to follow when implementing the Medion
> payment system. Read the **Codebase Reconciliation** section FIRST — the
> original spec was written against assumptions that do not match this repo.

---

## ⚠️ Codebase Reconciliation (READ FIRST)

The spec below (sections 1–19) was authored generically. Before following it
literally, apply these corrections derived from the actual codebase:

1. **Package layout.** The spec says `com.medion.entity`, `com.medion.service`,
   `com.medion.repository`, `com.medion.enums`, etc. The real base package is
   **`com.medion.hardwarestore`** and it is organized by feature *domain*, not
   by layer:
   - Entities + repositories + enums live under
     `com.medion.hardwarestore.domain.<feature>/`
     (e.g. `domain/payment/`, `domain/wallet/`, `domain/store/`).
   - Services live under `com.medion.hardwarestore.service`.
   - Controllers live under `com.medion.hardwarestore.controller.<feature>`.
   - External integrations live under
     `com.medion.hardwarestore.integration.payment`.
   - Exceptions live under `com.medion.hardwarestore.exception`.
   Map every spec path onto this structure; do NOT create a new `com.medion.*`
   top-level tree.

2. **Flyway version numbers.** The spec says `V4__Create_Payment_Tables.sql`.
   **`V4` (and up through `V16`) already exist.** The next migration must be
   **`V17__...`** and increment from there. Never reuse or rewrite an existing
   migration.

3. **Existing code overlaps — extend, do not duplicate.** These already exist:
   - `domain/payment/`: `Payment`, `PaymentProvider`, `PaymentRepository`,
     `PaymentStatus`, `StorePayment`, `StorePaymentRepository`.
   - `domain/wallet/`: `Wallet`, `WalletRepository`, `Transaction`,
     `TransactionRepository`, `Withdrawal`, `WithdrawalRepository`.
   - `domain/store/`: `SubscriptionType` enum (currently `COMMISSION`,
     `PREMIUM`), `Store`, `StoreStatus`.
   - `integration/payment/`: `MPesaService`, `MpesaConfig`,
     `MpesaGatewayService`, plus `AirtelMoneyService`, `PesapalService`,
     `PaymentProcessor`.
   Before creating any entity/service/enum from the spec, check whether an
   equivalent already exists and prefer extending it. Reconcile naming with
   the existing `SubscriptionType` (spec wants `MONTHLY_FLAT_RATE` /
   `COMMISSION_BASED` — decide whether to migrate the enum or add new values;
   flag this rather than silently forking a parallel type).

4. **Match existing conventions.** Follow the comment density, naming, Lombok
   usage, and repository idioms already present in `domain/*` and `service/*`.
   The build is Spring Boot 3.2.6, Java 17, Maven, PostgreSQL + Flyway,
   Lombok + MapStruct, JWT security, spring-dotenv.

5. **Redis is commented out in `pom.xml`.** The cache/redis starters are
   present but commented — uncommenting them (spec §3.1) is a real step.

Whenever the spec conflicts with the codebase, the **codebase wins**; note the
deviation in the commit/PR description instead of forcing the spec's shape.

---

## Executive Summary

Implement a complete payment processing system for the Medion marketplace with
M-Pesa integration, subscription management, wallet system, Redis caching, and
comprehensive error handling. Follow enterprise-grade patterns with clean code,
proper testing structure, and professional git commit practices.

## Payment Processing Flow

1. User initiates payment (order/subscription).
2. Request → PaymentController → PaymentService.
3. Generate STK Push via MpesaService → M-Pesa API.
4. Cache pending transaction → Redis (TTL: 10 mins).
5. User enters M-Pesa PIN on device.
6. M-Pesa sends callback to `/api/v1/payments/mpesa/callback`.
7. Validate callback signature → update transaction → Redis cache.
8. Activate subscription / credit wallet → emit events.
9. Notify user via email (async via event listener).

---

## 1. Database Schema & Migrations

Create Flyway migrations in `src/main/resources/db/migration/`.
**Next available version is `V17`** (V1–V16 already exist).

`V17__Create_Payment_Tables.sql` (rename per next free version):
- `payment_transactions` — status tracking, M-Pesa receipt numbers, retry counts.
- `subscriptions` — subscription type (MONTHLY_FLAT_RATE or COMMISSION_BASED),
  billing cycle tracking.
- `subscription_payments` — billing records.
- `vendor_wallets` — available/locked balances, earnings tracking.
- `wallet_transactions` — audit trail.
- `payment_retry_logs` — debugging failed payments.
- Indexes on `user_id`, `status`, `mpesa_receipt`, `external_reference`,
  `store_id`, `created_at`.

---

## 2. Core Domain Entities

Create JPA entities under `com.medion.hardwarestore.domain.<feature>` (spec
said `com.medion.entity` — remap). Reuse/extend existing entities where present.

### 2.1 PaymentTransaction
- Fields: id, user, amount, mpesaReceiptNumber, mpesaTransactionId,
  status (enum), paymentType (enum), externalReferenceId, phoneNumber,
  currency (default KES), retryCount, maxRetries, errorMessage, initiatedAt,
  completedAt, expiresAt.
- Timestamps: createdAt, updatedAt via `@CreationTimestamp` / `@UpdateTimestamp`.
- `@PrePersist` sets initiatedAt and expiresAt (10-minute default expiry).
- Lazy-load relationship with User.

### 2.2 Subscription
- Fields: id, store (OneToOne), subscriptionType (enum), monthlyFlatRate,
  commissionPercentage, status (enum), currentBillingCycleStart/End,
  autoRenewal (default true), lastPaymentDate, nextPaymentDate,
  cancellationReason.
- Timestamps: createdAt, updatedAt.

### 2.3 VendorWallet
- Fields: id, store (OneToOne), availableBalance, lockedBalance, totalEarnings,
  totalCommissionsPaid, lastWithdrawalDate. Timestamps: createdAt, updatedAt.
- NOTE: a `Wallet` already exists under `domain/wallet` — reconcile before
  adding a parallel `VendorWallet`.

### 2.4 WalletTransaction
- Fields: id, vendorWallet (ManyToOne), amount, transactionType (enum),
  referenceId, referenceType, description, status, balanceBefore, balanceAfter.
- Timestamp: createdAt.
- NOTE: a `Transaction` already exists under `domain/wallet` — reconcile.

### 2.5 Enums (under the relevant `domain/*` package)
- **PaymentStatus**: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED,
  EXPIRED, REFUNDED. (A `PaymentStatus` already exists — extend it.)
- **PaymentType**: SUBSCRIPTION, PRODUCT_ORDER, VENDOR_WITHDRAWAL.
- **SubscriptionType**: MONTHLY_FLAT_RATE, COMMISSION_BASED. (Existing enum has
  COMMISSION / PREMIUM — reconcile explicitly.)
- **SubscriptionStatus**: ACTIVE, SUSPENDED, CANCELLED, EXPIRED,
  PENDING_ACTIVATION.

### 2.6 Repositories (`domain/<feature>`, extend `JpaRepository`)
- `PaymentTransactionRepository`: `findByExternalReferenceId`,
  `findByMpesaReceiptNumber`.
- `SubscriptionRepository`: `findByStoreId`,
  `findByAutoRenewalAndNextPaymentDateBefore`.
- `VendorWalletRepository`: `findByStoreId`.
- `WalletTransactionRepository`:
  `findByVendorWalletStoreIdOrderByCreatedAtDesc(Long, Pageable)`.
- `PaymentRetryLogRepository`.

---

## 3. Redis Cache Configuration

### 3.1 Dependencies
Uncomment in `pom.xml`: `spring-boot-starter-cache` and
`spring-boot-starter-data-redis` (currently commented out).

### 3.2 `config/CacheConfig.java`
- `@Configuration` + `@EnableCaching`.
- Beans: `LettuceConnectionFactory redisConnectionFactory()`;
  `RedisCacheManager cacheManager(...)` (10-min default TTL, `StringRedisSerializer`
  keys, `GenericJackson2JsonRedisSerializer` values);
  `RedisTemplate<String,Object> redisTemplate(...)` (String key/hashKey,
  Jackson value/hashValue).

---

## 4. MpesaService (`integration/payment`)

Reconcile with existing `MPesaService` / `MpesaConfig` / `MpesaGatewayService`.

Config injection: `mpesa.consumer-key`, `consumer-secret`, `passkey`,
`shortcode`, `is-live` (default false), `callback-url`. Define sandbox + live
Auth and STK Push URLs as constants.

Methods:
- `MpesaStkPushResponse initiateStkPush(phone, amount, accountReference, paymentTransactionId)`
  — get token, build request, POST with Bearer token, cache
  checkoutRequestID→paymentTransactionId (10 min), return on code "0" else
  throw `MpesaException`; log + `recordRetryLog()`.
- `String getAccessToken()` — check Redis `mpesa:access_token`; else base64
  `consumerKey:consumerSecret`, POST with Basic auth, cache token 3500s.
- `boolean validateCallbackSignature(signature, body)` — SHA-256 → base64 → compare.
- `String generatePassword()` — base64(shortcode+passkey+timestamp).
- `String generateTimestamp()` — `yyyyMMddHHmmss`.
- `String generateCallbackSignature(body)` — SHA-256 → base64.
- `void cacheTransaction(id, checkoutRequestId)` — key
  `mpesa:transaction:<id>`, 10-min TTL.
- `void recordRetryLog(id, responseBody, errorCode, errorMessage)`.
Inject: `RestTemplate`, `PaymentRetryLogRepository`, `RedisTemplate`.

---

## 5. PaymentService (`service`)

`@Service @Slf4j @Transactional`.
- `initiateSubscriptionPayment(user, amount, phone, storeId)` — validate;
  reject if active subscription exists; create PENDING SUBSCRIPTION txn;
  `generateExternalReference(storeId)`; save; `initiateStkPush`; cache
  `payment:subscription:<checkoutId>`→storeId (10 min); return PaymentResponse;
  wrap `MpesaException` as `PaymentException`.
- `handleMpesaCallback(req)` — extract checkoutId/resultCode/resultDesc; look up
  cached payment ID; on success set COMPLETED + receipt + completedAt, activate
  subscription or process order, publish `PaymentCompletedEvent`; on failure set
  FAILED + errorMessage, publish `PaymentFailedEvent`.
- `processSubscriptionActivation(txn, checkoutId)` — resolve storeId + type from
  cache; find-or-create subscription; ACTIVE; billing cycle now → now+1mo;
  set rates by type (flat rate = amount; commission = 5.0).
- `creditVendorWallet(storeId, amount, refType, refId)` — commission-based
  deducts commission; increment balances/earnings; write WalletTransaction(s)
  with balanceBefore/After (separate txn for commission deduction).
- `getPaymentStatus(id)` — `@Cacheable("payment-status", key="#transactionId")`.
- Helpers: `generateExternalReference` → `SUB-<storeId>-<ts>`;
  `generateAccountReference` → `STORE<storeId>`.
Inject: the payment/subscription/wallet repos, `MpesaService`, `RedisTemplate`,
`ApplicationEventPublisher`.

---

## 6. SubscriptionService (`service`)

`@Service @Slf4j @Transactional`.
- `getAvailablePlans()` `@Cacheable("subscription-plans")` — Professional Plan
  (MONTHLY_FLAT_RATE, 9999 KES, 100% retention) + Starter Plan
  (COMMISSION_BASED, 5%).
- `getSubscriptionStatus(storeId)` — hasSubscription flag + full details.
- `cancelSubscription(storeId, reason)` — status CANCELLED + reason.
- `processSubscriptionRenewals()` `@Scheduled(cron="0 0 1 * * *")` — renew due
  auto-renewals; per-subscription error logging.
- `processSubscriptionRenewal(sub)` (private) — build SubscriptionPayment,
  invoice number, due date now+3d.
- Helpers: `calculateCommissionDue` (return ZERO for now, TODO);
  `generateInvoiceNumber` → `INV-SUB-<id>-<ts>`.

---

## 7. WalletService (`service`)

`@Service @Slf4j @Transactional`.
- `createWallet(storeId)` — balances ZERO.
- `getWalletBalance(storeId)` `@Cacheable("wallet-balance", key="#storeId")`.
- `withdrawFunds(storeId, amount, bankAccountNumber)` — validate amount > 0 and
  available ≥ amount; move available→locked; WalletTransaction WITHDRAWAL/PENDING;
  cache `withdrawal:<txnId>` (24h); return WithdrawalResponse.
- `getTransactionHistory(storeId, pageable)` `@Transactional(readOnly=true)`.
- `invalidateWalletCache(storeId)` — delete `wallet-balance::<storeId>`.
- Helper: `mapToResponse(tx)`.

---

## 8. Controllers (`controller.<feature>`)

- **PaymentController** `@RequestMapping("/api/v1/payments")`:
  `POST /subscription/initiate`, `POST /mpesa/callback`,
  `GET /{transactionId}/status`.
  NOTE: a `PaymentController` already exists — extend it.
- **SubscriptionController** `@RequestMapping("/api/v1/subscriptions")`:
  `GET /plans`, `GET /status/{storeId}`, `DELETE /{storeId}/cancel`.
- **WalletController** `@RequestMapping("/api/v1/wallet")`:
  `GET /{storeId}/balance`, `POST /{storeId}/withdraw`,
  `GET /{storeId}/transactions` (page default 0, size 20).
  NOTE: a `WalletController` already exists — extend it.

---

## 9. Exception Handling (`exception`)

- `PaymentException`, `MpesaException`, `WalletException` — each extends
  `RuntimeException` with `(message)` and `(message, cause)` constructors.
- `GlobalExceptionHandler` `@RestControllerAdvice @Slf4j`:
  PaymentException → 400 `PAYMENT_ERROR`; MpesaException → 503 `MPESA_ERROR`
  (generic message); WalletException → 400 `WALLET_ERROR`. Each with
  `ErrorResponse{status, message, timestamp}`.

---

## 10. Event-Driven Architecture (`event`)

- `PaymentCompletedEvent` (paymentTransactionId, userId, amount, paymentType).
- `PaymentFailedEvent` (paymentTransactionId, failureReason).
- `PaymentEventListener` `@Component @Slf4j`: `@EventListener onPaymentCompleted`
  / `onPaymentFailed` — call EmailService if present, else log.

---

## 11. DTOs (`dto` or feature-local)

- Requests: `InitiateSubscriptionPaymentRequest`, `MpesaCallbackRequest`,
  `WithdrawalRequest`.
- Responses: `PaymentResponse`, `SubscriptionStatusResponse`, `SubscriptionPlan`,
  `WalletBalanceResponse`, `WithdrawalResponse`, `WalletTransactionResponse`,
  `ErrorResponse`.
- M-Pesa: `MpesaStkPushRequest`, `MpesaStkPushResponse`,
  `MpesaAccessTokenResponse`.

---

## 12. Configuration — `application.yml`

```yaml
mpesa:
  consumer-key: ${MPESA_CONSUMER_KEY}
  consumer-secret: ${MPESA_CONSUMER_SECRET}
  passkey: ${MPESA_PASSKEY}
  shortcode: ${MPESA_SHORTCODE}
  is-live: ${MPESA_IS_LIVE:false}
  callback-url: ${APP_PAYMENT_CALLBACK_URL}
```

---

## 13. Environment Configuration

- `.env.example` (already exists — extend): DB, JWT, M-Pesa (sandbox),
  Redis (host/port), Cloudinary, encryption key, email (optional), callback URL,
  with comments on where to obtain each credential.
- **Never commit `.env`.** Ensure `.gitignore` covers `.env`, `.env.*`,
  `*.pem`, `*.key`, `*.jks`, `*.p12`. All secrets via env vars — never hardcode.
  Production: use cloud secrets management.

---

## 14. Git Commit Strategy (Conventional Commits)

Commit incrementally, roughly in this order:
- `feat: add payment transaction entity and Flyway migrations`
- `feat: add subscription domain entity with billing cycle management`
- `feat: add vendor wallet and wallet transaction entities`
- `feat: enable and configure Redis caching infrastructure`
- `feat: implement M-Pesa service with STK push and access token caching`
- `feat: implement payment service with transaction and subscription lifecycle`
- `feat: implement wallet service with balance and withdrawal management`
- `feat: implement subscription service with auto-renewal scheduling`
- `feat: add payment, subscription, and wallet REST controllers`
- `feat: implement custom exception handling and global error responses`
- `feat: add event-driven payment notifications`
- `test: add unit tests for M-Pesa service`
- `test: add integration tests for payment callback flow`
- `test: add wallet service tests`
- `docs: add payment integration API documentation`
- `chore: add .env.example with payment configuration`

Only commit/push when the user asks.

---

## 15. Testing (`src/test/java/com/medion/hardwarestore`)

- Unit: `PaymentServiceTest`, `MpesaServiceTest`, `WalletServiceTest`,
  `SubscriptionServiceTest`.
- Integration: end-to-end payment flow with callback; subscription activation;
  wallet balance updates after successful payment.

---

## 16. Key Implementation Principles

- **Security**: no hardcoded secrets; validate all payment input; validate
  M-Pesa callback signatures before processing; hash bank account numbers; never
  log phone numbers or payment credentials.
- **Database**: Flyway only (no direct DDL); indexes on hot columns; lazy loading;
  service-layer transaction boundaries.
- **Caching TTLs**: access token 3500s; transaction lookup 10 min; payment status
  10 min; subscription plans no expiry (invalidate on change); wallet balance
  5 min (invalidate on update).
- **M-Pesa**: sandbox by default; switch on `MPESA_IS_LIVE=true`; exponential
  backoff on retries; log requests/responses; handle timeouts; validate callbacks.
- **Subscriptions**: MONTHLY_FLAT_RATE (100% sales) and COMMISSION_BASED (5%);
  auto-renewal daily at 1 AM; Redis-held payment context; commission computed at
  wallet-credit time.
- **Wallets**: lock funds during withdrawal; record every transaction; commission
  deductions as separate transactions; invalidate cache on balance changes.

---

## 17. Performance

HikariCP pooling (default); index user_id/status/created_at/next_payment_date;
batch renewals; cache plans + access tokens; lazy relationships.

---

## 18. Deployment Checklist

Migrations tested locally; Redis reachable; M-Pesa sandbox creds set; callback URL
publicly reachable; DB backups; error monitoring; API docs; load test (100
concurrent payments); security audit; `.env` gitignored, `.env.example` committed.

---

## 19. Post-Implementation Tasks

Update API docs; Postman collection; payment-failure monitoring; renewal alerting;
admin reconciliation dashboard; M-Pesa webhook retry; payment analytics dashboard.
