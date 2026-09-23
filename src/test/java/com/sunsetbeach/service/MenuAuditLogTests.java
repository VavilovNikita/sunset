package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.MenuItem;
import com.sunsetbeach.model.MenuItemInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
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
class MenuAuditLogTests extends AbstractIntegrationTest {

    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdItemIds = new java.util.ArrayList<>();

    @BeforeEach
    void setUpSecurityContext() {
        StaffPrincipal principal = new StaffPrincipal("admin-actor", "audit-admin-test@example.com", Role.ADMIN);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String itemId : createdItemIds) {
            auditLogRepository.deleteAll(entriesFor(itemId));
            menuItemRepository.findById(itemId).ifPresent(menuItemRepository::delete);
        }
    }

    private List<AuditLogEntity> entriesFor(String itemId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == AuditEntityType.MENU_ITEM && itemId.equals(e.getEntityId()))
                .toList();
    }

    @Test
    void createUpdateDeleteMenuItem_writeExpectedEntries() {
        MenuItem created = menuService.create(new MenuItemInput("Audit Mojito " + UUID.randomUUID(), "test item", "Test", new BigDecimal("100.00")));
        createdItemIds.add(created.getId());

        List<AuditLogEntity> createdEntries =
                entriesFor(created.getId()).stream().filter(e -> e.getAction() == AuditAction.MENU_ITEM_CREATED).toList();
        assertThat(createdEntries).hasSize(1);
        assertThat(createdEntries.get(0).getSummary()).contains(created.getName());

        String newName = "Renamed Mojito " + UUID.randomUUID();
        menuService.update(created.getId(), new MenuItemInput(newName, "test item", "Test", new BigDecimal("120.00")));

        List<AuditLogEntity> updatedEntries =
                entriesFor(created.getId()).stream().filter(e -> e.getAction() == AuditAction.MENU_ITEM_UPDATED).toList();
        assertThat(updatedEntries).hasSize(1);
        assertThat(updatedEntries.get(0).getSummary()).contains("renamed").contains(newName);

        String itemId = created.getId();
        menuService.delete(itemId);
        createdItemIds.remove(itemId);

        List<AuditLogEntity> deletedEntries =
                entriesFor(itemId).stream().filter(e -> e.getAction() == AuditAction.MENU_ITEM_DELETED).toList();
        assertThat(deletedEntries).hasSize(1);
        auditLogRepository.deleteAll(entriesFor(itemId));
    }
}
