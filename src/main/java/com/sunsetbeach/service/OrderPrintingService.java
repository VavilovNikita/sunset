package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.entity.PrinterEntity;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.PrintJobMapper;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PrintAttemptResult;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrinterCodepage;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds and queues the three Order-triggered print documents (kitchen/bar ticket, pre-bill,
 * guest receipt). Kitchen/bar tickets never carry prices; pre-bill/receipt always do. All three
 * resolve {@code OrderItem.menuItemId} -&gt; name with one batched lookup, same pattern as
 * {@code BookingService.listPosOrders}/{@code ShiftService.exportCsv} - never one query per line
 * item. Every public method here swallows its own failures (mirrors {@link EmailService}):
 * a printer being down must never fail the order operation that triggered the print.
 */
@Service
public class OrderPrintingService {

    private static final Logger log = LoggerFactory.getLogger(OrderPrintingService.class);

    private final PrintService printService;
    private final PrintJobMapper printJobMapper;
    private final MenuItemRepository menuItemRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;

    public OrderPrintingService(
            PrintService printService,
            PrintJobMapper printJobMapper,
            MenuItemRepository menuItemRepository,
            TableRepository tableRepository,
            UserRepository userRepository) {
        this.printService = printService;
        this.printJobMapper = printJobMapper;
        this.menuItemRepository = menuItemRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
    }

    /**
     * OPEN -&gt; SENT: splits line items by {@link MenuItemEntity#getDepartment()} and prints one
     * ticket per department that (a) has at least one item and (b) has an active printer - an
     * order with only drinks never touches the kitchen printer, and a department with no active
     * printer configured is silently skipped (same "best effort" contract as everything else
     * here).
     */
    public void printTickets(OrderEntity order, List<OrderItemEntity> items) {
        try {
            if (items.isEmpty()) {
                return;
            }
            Map<String, MenuItemEntity> menuItemsById = resolveMenuItems(items);
            Map<MenuDepartment, List<OrderItemEntity>> byDepartment = items.stream()
                    .collect(Collectors.groupingBy(item -> menuDepartmentOf(item, menuItemsById)));

            for (Map.Entry<MenuDepartment, List<OrderItemEntity>> entry : byDepartment.entrySet()) {
                PrinterDepartment printerDepartment =
                        entry.getKey() == MenuDepartment.BAR ? PrinterDepartment.BAR : PrinterDepartment.KITCHEN;
                printService.findActivePrinter(printerDepartment).ifPresent(printer -> {
                    byte[] payload = buildTicketPayload(order, entry.getValue(), menuItemsById, printer.getCodepage(), printerDepartment);
                    printService.queueAndAttempt(
                            printer,
                            PrintDocumentType.KITCHEN_TICKET,
                            (printerDepartment == PrinterDepartment.BAR ? "Bar ticket" : "Kitchen ticket") + " — Order #" + shortId(order.getId()),
                            payload);
                });
            }
        } catch (Exception e) {
            log.error("printTickets failed for order {}", order.getId(), e);
        }
    }

    /** {@code POST /orders/{id}/print-prebill} - no-op result (not an exception) when there's no active CASHIER printer. */
    public PrintAttemptResult printPrebill(OrderEntity order, List<OrderItemEntity> items) {
        try {
            PrinterEntity printer = printService.findActivePrinter(PrinterDepartment.CASHIER).orElse(null);
            if (printer == null) {
                return new PrintAttemptResult(false);
            }
            Map<String, MenuItemEntity> menuItemsById = resolveMenuItems(items);
            byte[] payload = buildPriceListPayload(
                    "PRE-BILL", order, items, menuItemsById, printer.getCodepage(), null, null);
            PrintJobEntity job = printService.queueAndAttempt(
                    printer, PrintDocumentType.PREBILL, "Pre-bill — Order #" + shortId(order.getId()), payload);
            return new PrintAttemptResult(true).job(printJobMapper.toDto(job));
        } catch (Exception e) {
            log.error("printPrebill failed for order {}", order.getId(), e);
            return new PrintAttemptResult(false);
        }
    }

    /** {@code POST /orders/{id}/close} - fire-and-forget, same as {@link #printTickets}. */
    public void printGuestReceipt(OrderEntity order, List<OrderItemEntity> items, PaymentEntity payment, BookingEntity booking) {
        try {
            printService.findActivePrinter(PrinterDepartment.CASHIER).ifPresent(printer -> {
                Map<String, MenuItemEntity> menuItemsById = resolveMenuItems(items);
                byte[] payload = buildPriceListPayload(
                        "GUEST RECEIPT", order, items, menuItemsById, printer.getCodepage(), payment, booking);
                printService.queueAndAttempt(
                        printer, PrintDocumentType.GUEST_RECEIPT, "Guest receipt — Order #" + shortId(order.getId()), payload);
            });
        } catch (Exception e) {
            log.error("printGuestReceipt failed for order {}", order.getId(), e);
        }
    }

    private Map<String, MenuItemEntity> resolveMenuItems(List<OrderItemEntity> items) {
        List<String> ids = items.stream().map(OrderItemEntity::getMenuItemId).distinct().toList();
        return menuItemRepository.findAllById(ids).stream().collect(Collectors.toMap(MenuItemEntity::getId, m -> m));
    }

    private static MenuDepartment menuDepartmentOf(OrderItemEntity item, Map<String, MenuItemEntity> menuItemsById) {
        MenuItemEntity menuItem = menuItemsById.get(item.getMenuItemId());
        return menuItem != null ? menuItem.getDepartment() : MenuDepartment.KITCHEN;
    }

    private byte[] buildTicketPayload(
            OrderEntity order,
            List<OrderItemEntity> items,
            Map<String, MenuItemEntity> menuItemsById,
            PrinterCodepage codepage,
            PrinterDepartment department) {
        EscPosBuilder b = new EscPosBuilder(codepage);
        b.center(true)
                .bold(true)
                .line(department == PrinterDepartment.BAR ? "BAR TICKET" : "KITCHEN TICKET")
                .bold(false)
                .center(false);
        b.line(describeLocation(order));
        b.line("Time: " + TimestampFormat.readable(LocalDateTime.now()));
        b.line("Waiter: " + resolveWaiterLabel(order.getOpenedByUserId()));
        b.divider();
        for (OrderItemEntity item : items) {
            MenuItemEntity menuItem = menuItemsById.get(item.getMenuItemId());
            String name = menuItem != null ? menuItem.getName() : "Unknown item";
            b.line(item.getQuantity() + "x " + name);
            if (item.getNote() != null && !item.getNote().isBlank()) {
                b.line("   note: " + item.getNote());
            }
        }
        return b.cutAndBuild();
    }

    /**
     * Shared by pre-bill and guest receipt - both are "items with prices + total", differing
     * only in the trailing block: nothing (pre-bill) vs. payment method / room-charge note
     * (receipt, when {@code payment} is non-null).
     */
    private byte[] buildPriceListPayload(
            String title,
            OrderEntity order,
            List<OrderItemEntity> items,
            Map<String, MenuItemEntity> menuItemsById,
            PrinterCodepage codepage,
            PaymentEntity payment,
            BookingEntity booking) {
        EscPosBuilder b = new EscPosBuilder(codepage);
        b.center(true).bold(true).line(title).bold(false).center(false);
        b.line(describeLocation(order));
        b.line("Time: " + TimestampFormat.readable(LocalDateTime.now()));
        b.divider();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemEntity item : items) {
            MenuItemEntity menuItem = menuItemsById.get(item.getMenuItemId());
            String name = menuItem != null ? menuItem.getName() : "Unknown item";
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);
            b.twoColumn(item.getQuantity() + "x " + name, PriceFormat.asDecimalString(lineTotal));
        }
        b.divider();
        b.twoColumn("TOTAL", PriceFormat.asDecimalString(total));

        if (payment != null) {
            b.divider();
            if (payment.getMethod() == PaymentMethod.ROOM_CHARGE && booking != null) {
                b.line("Charged to room: " + booking.getRoom().getName());
                b.line("Guest: " + booking.getGuestName());
                b.line("Booking #" + shortId(booking.getId()));
            } else {
                b.line("Payment: " + payment.getMethod().getValue());
            }
        }
        return b.cutAndBuild();
    }

    private String describeLocation(OrderEntity order) {
        if (order.getTableId() != null) {
            return tableRepository
                    .findById(order.getTableId())
                    .map(t -> zoneLabel(t.getZone()) + " – " + t.getLabel())
                    .orElseGet(() -> "Order #" + shortId(order.getId()));
        }
        if (order.getGuestName() != null && !order.getGuestName().isBlank()) {
            return "Takeaway – " + order.getGuestName();
        }
        return "Order #" + shortId(order.getId());
    }

    private String resolveWaiterLabel(String userId) {
        if (userId == null) {
            return "—";
        }
        return userRepository.findById(userId).map(u -> u.getEmail()).orElse(userId);
    }

    private static String zoneLabel(Zone zone) {
        return switch (zone) {
            case RESTAURANT -> "Restaurant";
            case BAR -> "Bar";
            case SPA -> "Spa";
            case POOL -> "Pool";
            case ROOM_SERVICE -> "Room service";
        };
    }

    private static String shortId(String id) {
        return (id.length() > 8 ? id.substring(0, 8) : id).toUpperCase();
    }
}
