package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.PriceRangeInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DB-backed (real dev Postgres), NOT {@code @Transactional} - see {@link BookingAuditLogTests}'s
 * javadoc for why (AuditLogService.record commits independently of this test's transaction).
 */
@SpringBootTest
class RoomPricingAuditLogTests {

    @Autowired
    private RoomService roomService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdRoomIds = new java.util.ArrayList<>();

    @BeforeEach
    void setUpSecurityContext() {
        StaffPrincipal principal = new StaffPrincipal("manager-actor", "audit-pricing-test@example.com", Role.MANAGER);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_MANAGER"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String roomId : createdRoomIds) {
            auditLogRepository.deleteAll(entriesFor(roomId));
            roomRepository.deleteById(roomId);
        }
    }

    private List<AuditLogEntity> entriesFor(String roomId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == AuditEntityType.ROOM && roomId.equals(e.getEntityId()))
                .toList();
    }

    private RoomEntity createRoom(BigDecimal basePrice) {
        RoomEntity room = new RoomEntity();
        room.setName("Audit Pricing Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by RoomPricingAuditLogTests");
        room.setCapacity(2);
        room.setBasePrice(basePrice);
        RoomEntity saved = roomRepository.saveAndFlush(room);
        createdRoomIds.add(saved.getId());
        return saved;
    }

    private static RoomInput roomInputFrom(RoomEntity room, BigDecimal newBasePrice) {
        return new RoomInput(room.getName(), room.getDescription(), room.getCapacity(), newBasePrice);
    }

    @Test
    void updateRoom_basePriceChanged_writesRoomPriceChangedEntry() {
        RoomEntity room = createRoom(new BigDecimal("1000.00"));

        roomService.update(room.getId(), roomInputFrom(room, new BigDecimal("1200.00")));

        List<AuditLogEntity> entries = entriesFor(room.getId());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getAction()).isEqualTo(AuditAction.ROOM_PRICE_CHANGED);
        assertThat(entries.get(0).getSummary()).contains("1000.00").contains("1200.00");
    }

    @Test
    void updateRoom_basePriceUnchanged_writesNoEntry() {
        RoomEntity room = createRoom(new BigDecimal("1000.00"));

        // Same price, only the description changes - must not be reported as a price change.
        RoomInput input = new RoomInput(room.getName(), "Updated description", room.getCapacity(), new BigDecimal("1000.00"));
        roomService.update(room.getId(), input);

        assertThat(entriesFor(room.getId())).isEmpty();
    }

    @Test
    void setPricing_writesRateOverrideChangedEntry() {
        RoomEntity room = createRoom(new BigDecimal("1000.00"));
        LocalDate from = LocalDate.now().plusDays(30);
        LocalDate to = LocalDate.now().plusDays(32);

        pricingService.setPricing(room.getId(), new PriceRangeInput(from.toString(), to.toString(), new BigDecimal("1500")));

        List<AuditLogEntity> entries = auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.RATE_OVERRIDE_CHANGED && room.getId().equals(e.getEntityId()))
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getEntityType()).isEqualTo(AuditEntityType.ROOM);
        assertThat(entries.get(0).getSummary()).contains("1500").contains(from.toString()).contains(to.toString());
    }
}
