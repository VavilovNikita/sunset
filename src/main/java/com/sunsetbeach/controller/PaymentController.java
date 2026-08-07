package com.sunsetbeach.controller;

import com.sunsetbeach.api.PaymentsApi;
import com.sunsetbeach.model.PaymentsSummary;
import com.sunsetbeach.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController implements PaymentsApi {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public ResponseEntity<PaymentsSummary> getPaymentsSummary(String from, String to) {
        return ResponseEntity.ok(paymentService.getSummary(from, to));
    }
}
