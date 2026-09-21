package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.MenuItem;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /public/menu} (via {@code MenuService#listPublic}) must only ever show a guest
 * items they can actually order - unavailable items and SPA-department items (treatments are
 * booked as a {@code SpaAppointment}, never picked off a menu screen) are excluded, mirroring
 * the same SPA exclusion {@code OrderPrintingService#printTickets} applies. {@code MenuService#list}
 * (the staff catalog) must stay unfiltered - this is a separate method, not a shared query with
 * a hidden filter flag.
 */
@SpringBootTest
@Transactional
class MenuServiceListPublicTests extends AbstractIntegrationTest {

    @Autowired
    private MenuService menuService;

    @Autowired
    private com.sunsetbeach.repository.MenuItemRepository menuItemRepository;

    private MenuItemEntity save(String name, MenuDepartment department, boolean available) {
        MenuItemEntity entity = new MenuItemEntity();
        entity.setName(name);
        entity.setDescription("test item");
        entity.setCategory("Test");
        entity.setDepartment(department);
        entity.setPrice(new BigDecimal("100.00"));
        entity.setAvailable(available);
        return menuItemRepository.saveAndFlush(entity);
    }

    @Test
    void listPublic_excludesUnavailableAndSpaItems_butIncludesOrdinaryAvailableOnes() {
        save("Unavailable Burger", MenuDepartment.KITCHEN, false);
        save("Spa Treatment", MenuDepartment.SPA, true);
        MenuItemEntity mojito = save("Mojito", MenuDepartment.BAR, true);

        List<MenuItem> publicMenu = menuService.listPublic();

        assertThat(publicMenu).extracting(MenuItem::getId).containsExactly(mojito.getId());
    }

    @Test
    void list_staffCatalog_isNeverFilteredByListPublic() {
        save("Unavailable Burger", MenuDepartment.KITCHEN, false);
        save("Spa Treatment", MenuDepartment.SPA, true);
        save("Mojito", MenuDepartment.BAR, true);

        List<MenuItem> staffMenu = menuService.list();

        assertThat(staffMenu).hasSize(3);
    }
}
