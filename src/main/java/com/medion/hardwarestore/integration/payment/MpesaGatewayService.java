package com.medion.hardwarestore.integration.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpesaGatewayService {

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    // We'll use a webhook URL that the frontend/backend can expose using ngrok, but for now we default to localhost
    @Value("${mpesa.callback-url:http://localhost:8080/api/v1/payments/mpesa-webhook}")
    private String callbackUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String OAUTH_URL = "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";
    private static final String STK_PUSH_URL = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest";

    public String getAccessToken() {
        try {
            String credentials = consumerKey + ":" + consumerSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(encodedCredentials); // Sets Authorization: Basic base64(...)

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(OAUTH_URL, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to generate MPESA Access Token", e);
            throw new RuntimeException("Failed to generate MPESA Access Token", e);
        }
        return null;
    }

    /**
     * Initiates an STK Push to the specified phone number.
     * 
     * @param phoneNumber The phone number format (2547XXXXXXXX)
     * @param amount The amount to charge
     * @param accountReference The Merchant Reference (e.g. Order ID)
     * @return boolean indicating if the push was successfully sent to Safaricom
     */
    public String initiateStkPush(String phoneNumber, long amount, String accountReference) {
        String token = getAccessToken();
        if (token == null) {
            return null;
        }

        try {
            // Format phoneNumber (must be 254...)
            String formattedPhone = formatPhoneNumber(phoneNumber);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = Base64.getEncoder().encodeToString((shortcode + passkey + timestamp).getBytes(StandardCharsets.UTF_8));

            Map<String, Object> payload = new HashMap<>();
            payload.put("BusinessShortCode", shortcode);
            payload.put("Password", password);
            payload.put("Timestamp", timestamp);
            payload.put("TransactionType", "CustomerPayBillOnline");
            payload.put("Amount", amount);
            payload.put("PartyA", formattedPhone);
            payload.put("PartyB", shortcode);
            payload.put("PhoneNumber", formattedPhone);
            payload.put("CallBackURL", callbackUrl);
            payload.put("AccountReference", accountReference);
            payload.put("TransactionDesc", "Hardware Store Payment");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(STK_PUSH_URL, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("STK Push successfully initiated for {}", accountReference);
                return (String) response.getBody().get("CheckoutRequestID");
            } else {
                log.error("STK Push failed: {}", response.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("Error sending STK Push", e);
            return null;
        }
    }

    private String formatPhoneNumber(String phone) {
        // Safaricom Daraja expects 2547XXXXXXXX or 2541XXXXXXXX
        phone = phone.replaceAll("[^0-9]", "");
        if (phone.startsWith("0")) {
            return "254" + phone.substring(1);
        } else if (phone.startsWith("+")) {
            return phone.substring(1);
        } else if (phone.startsWith("7") || phone.startsWith("1")) {
            return "254" + phone;
        }
        return phone;
    }
}
