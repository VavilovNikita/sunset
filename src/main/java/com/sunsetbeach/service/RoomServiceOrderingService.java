package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.GuestOrderMapper;
import com.sunsetbeach.model.GuestOrderView;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.OrderUpdateInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Room-service ordering for a verified, {@code CHECKED_IN} guest - see the {@code GuestOrder} tag's
 * own openapi.yaml description. Deliberately its own service, not folded into {@link OrderService}
 * (already large and multi-concern) or {@link GuestAccountService} (that's identity, not
 * ordering): this one owns exactly one thing, the gate ("is this booking this guest's own, and
 * currently checked in") in front of the same {@link OrderService} machinery every other order
 * path already uses. A room-service order is an ordinary {@code Order} with {@code bookingId} set
 * and no {@code tableId} - ownership here is defined purely by that link, re-checked on every
 * write (see {@link #submit}/{@link #addItems}'s own javadoc for why "once at creation" isn't
 * enough) - and, to keep this door scoped to genuinely room-service orders (never a dine-in tab or
 * a spa-billed order that merely happens to carry this guest's {@code bookingId} as a staff
 * pre-fill - see {@code Order.bookingId}'s own description), {@code tableId} must still be null.
 *
 * <p>Every failure - unknown booking/order, wrong owner, not checked in - collapses to the same
 * generic {@link NotFoundException}, same "a guessing attempt learns nothing" discipline as {@link
 * OrderService#requireGuestAccess}.
 */
@Service
public class RoomServiceOrderingService {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookingRepository bookingRepository;
    private final GuestOrderMapper guestOrderMapper;
    private final OrderPrintingService orderPrintingService;

    public RoomServiceOrderingService(
            OrderService orderService,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            BookingRepository bookingRepository,
            GuestOrderMapper guestOrderMapper,
            OrderPrintingService orderPrintingService) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.bookingRepository = bookingRepository;
        this.guestOrderMapper = guestOrderMapper;
        this.orderPrintingService = orderPrintingService;
    }

    /**
     * The guest's own "place my order" action - there's no waiter to separately confirm and send
     * the way dine-in staff do (see {@code OrdersApi.updateOrder}'s {@code OPEN -> SENT}
     * transition), so this doesn't stop at {@code OPEN}: create (guest-opened - {@code
     * openedByUserId} null, see {@link OrderService#create}), add the items, then transition
     * straight to {@code SENT} via {@link OrderService#update}, which already dispatches the
     * kitchen/bar ticket - never a second implementation of that.
     */
    @Transactional
    public GuestOrderView submit(String guestEmail, String bookingId, List<OrderItemInput> items) {
        requireCheckedInOwnBooking(guestEmail, bookingId);

        Order created = orderService.create(new OrderCreateInput().bookingId(bookingId), null);
        orderService.addItems(created.getId(), items);
        orderService.update(created.getId(), new OrderUpdateInput().status(OrderStatus.SENT));
        return toGuestOrderView(created.getId());
    }

    /**
     * A second (or later) round on an already-open room-service order. Re-verifies ownership and
     * {@code CHECKED_IN} again, not just at {@link #submit} - a guest can be checked out between
     * two room-service actions, and further item-adding on an already-open order must stop even
     * though the order itself is still {@code OPEN}/{@code SENT}. Delegates straight into {@link
     * OrderService#addItems} - the same merge-into-unsent-line and re-order-ticket-print behavior
     * staff and dine-in QR ordering get.
     */
    @Transactional
    public GuestOrderView addItems(String guestEmail, String orderId, List<OrderItemInput> items) {
        requireOwnRoomServiceOrder(guestEmail, orderId, true);
        orderService.addItems(orderId, items);
        return toGuestOrderView(orderId);
    }

    /** Ownership check only, no {@code CHECKED_IN} requirement - a guest should still see their own room-service receipt after checkout. */
    @Transactional(readOnly = true)
    public GuestOrderView getById(String guestEmail, String orderId) {
        requireOwnRoomServiceOrder(guestEmail, orderId, false);
        return toGuestOrderView(orderId);
    }

    private BookingEntity requireCheckedInOwnBooking(String guestEmail, String bookingId) {
        BookingEntity booking = bookingId == null ? null : bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !ownedByGuest(booking, guestEmail) || booking.getOccupancyStatus() != OccupancyStatus.CHECKED_IN) {
            throw new NotFoundException("Booking not found");
        }
        return booking;
    }

    private OrderEntity requireOwnRoomServiceOrder(String guestEmail, String orderId, boolean requireCheckedIn) {
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getTableId() != null || order.getBookingId() == null) {
            throw new NotFoundException("Order not found");
        }
        BookingEntity booking = bookingRepository.findById(order.getBookingId()).orElse(null);
        if (booking == null || !ownedByGuest(booking, guestEmail)) {
            throw new NotFoundException("Order not found");
        }
        if (requireCheckedIn && booking.getOccupancyStatus() != OccupancyStatus.CHECKED_IN) {
            throw new NotFoundException("Order not found");
        }
        return order;
    }

    private static boolean ownedByGuest(BookingEntity booking, String guestEmail) {
        return booking.getGuestEmail() != null && booking.getGuestEmail().equalsIgnoreCase(guestEmail);
    }

    private GuestOrderView toGuestOrderView(String orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(orderId);
        return guestOrderMapper.toDto(order, items, orderPrintingService.describeLocation(order));
    }
}
