package com.ecommerce.controller;

import com.ecommerce.client.InventoryServiceClient;
import com.ecommerce.client.NotificationServiceClient;
import com.ecommerce.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {
    private final PaymentServiceClient paymentServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @PostMapping("/payment/fail")
    public ResponseEntity<?> simulatePaymentFailure(@RequestParam boolean enable) {
        paymentServiceClient.setSimulateFailure(enable);
        return ResponseEntity.ok(buildResponse("Payment Service", enable));
    }

    @PostMapping("/inventory/fail")
    public ResponseEntity<?> simulateInventoryFailure(@RequestParam boolean enable) {
        inventoryServiceClient.setSimulateFailure(enable);
        return ResponseEntity.ok(buildResponse("Inventory Service", enable));
    }

    @PostMapping("/notification/fail")
    public ResponseEntity<?> simulateNotificationFailure(@RequestParam boolean enable) {
        notificationServiceClient.setSimulateFailure(enable);
        return ResponseEntity.ok(buildResponse("Notification Service", enable));
    }

    private Map<String, Object> buildResponse(String service, boolean failureEnabled) {
        Map<String, Object> response = new HashMap<>();
        response.put("service", service);
        response.put("failureSimulation", failureEnabled ? "ENABLED" : "DISABLED");
        response.put("message", failureEnabled ? 
                service + " will now fail on requests" : 
                service + " is now operating normally");
        return response;
    }
}
