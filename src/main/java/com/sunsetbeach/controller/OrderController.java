package com.sunsetbeach.controller;

import com.sunsetbeach.api.OrdersApi;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.OrderUpdateInput;
import com.sunsetbeach.model.PrintAttemptResult;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.security.CurrentUser;
import com.sunsetbeach.service.OrderService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController implements OrdersApi {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ResponseEntity<List<Order>> listOrders(
            OrderStatus status, Zone zone, String tableId, String bookingId, String staffId, LocalDate from, LocalDate to, String shiftId) {
        return ResponseEntity.ok(orderService.list(status, zone, tableId, bookingId, staffId, from, to, shiftId));
    }

    @Override
    public ResponseEntity<Order> getOrder(String id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @Override
    public ResponseEntity<Order> createOrder(OrderCreateInput orderCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(orderCreateInput, CurrentUser.id()));
    }

    @Override
    public ResponseEntity<Order> updateOrder(String id, OrderUpdateInput orderUpdateInput) {
        return ResponseEntity.ok(orderService.update(id, orderUpdateInput));
    }

    @Override
    public ResponseEntity<Order> addOrderItems(String id, List<OrderItemInput> orderItemInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.addItems(id, orderItemInput));
    }

    @Override
    public ResponseEntity<Order> updateOrderItem(String id, String itemId, OrderItemInput orderItemInput) {
        return ResponseEntity.ok(orderService.updateItem(id, itemId, orderItemInput));
    }

    @Override
    public ResponseEntity<Order> deleteOrderItem(String id, String itemId) {
        return ResponseEntity.ok(orderService.deleteItem(id, itemId));
    }

    @Override
    public ResponseEntity<Order> closeOrder(String id, CloseOrderInput closeOrderInput) {
        return ResponseEntity.ok(orderService.close(id, closeOrderInput, CurrentUser.id()));
    }

    @Override
    public ResponseEntity<Order> cancelOrder(String id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }

    @Override
    public ResponseEntity<PrintAttemptResult> printOrderPrebill(String id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.printPrebill(id));
    }
}
