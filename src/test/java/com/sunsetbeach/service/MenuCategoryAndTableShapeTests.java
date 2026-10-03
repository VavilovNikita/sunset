package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.MenuItem;
import com.sunsetbeach.model.MenuItemInput;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.TableInput;
import com.sunsetbeach.model.TableShape;
import com.sunsetbeach.model.Zone;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Menu categories are free text and the POS draws one tab per distinct spelling, so a category
 * differing from an existing one only by case or spacing must be refused rather than become a
 * second tab. Also: a menu item needs no description, and a table keeps the shape it was given.
 */
@SpringBootTest
@Transactional
class MenuCategoryAndTableShapeTests extends AbstractIntegrationTest {

    @Autowired private MenuService menuService;
    @Autowired private TableService tableService;

    private static MenuItemInput item(String category) {
        return new MenuItemInput("Item " + UUID.randomUUID().toString().substring(0, 8), category, new BigDecimal("100.00"));
    }

    @Test
    void aCategoryDifferingOnlyByCaseOrSpacing_isRefused_namingTheExistingOne() {
        String existing = "Cocktails " + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        menuService.create(item(existing));

        assertThatThrownBy(() -> menuService.create(item(existing.toLowerCase())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(existing);
        assertThatThrownBy(() -> menuService.create(item("  " + existing.toLowerCase().replace(" ", "   ") + " ")))
                .isInstanceOf(ValidationException.class);
        // Spacing alone normalizes onto the existing spelling - the same tab, not a new one.
        assertThat(menuService.create(item("  " + existing.replace(" ", "   ") + " ")).getCategory()).isEqualTo(existing);
    }

    @Test
    void theExactExistingSpelling_andANewName_areAccepted() {
        String existing = "Mocktails " + UUID.randomUUID().toString().substring(0, 6);
        menuService.create(item(existing));

        assertThat(menuService.create(item(existing)).getCategory()).isEqualTo(existing);
        assertThat(menuService.create(item(" Brand  new " + existing + " ")).getCategory()).isEqualTo("Brand new " + existing);
    }

    @Test
    void anItemWithoutADescription_isSavedWithAnEmptyOne() {
        MenuItem created = menuService.create(item("Sides " + UUID.randomUUID()));

        assertThat(created.getDescription()).isEmpty();
    }

    @Test
    void aTableKeepsItsShape_andDefaultsToRound() {
        Table round = tableService.create(new TableInput(Zone.RESTAURANT, "Shape test A", 4));
        Table rectangle = tableService.create(new TableInput(Zone.RESTAURANT, "Shape test B", 8).shape(TableShape.RECTANGLE));

        assertThat(round.getShape()).isEqualTo(TableShape.ROUND);
        assertThat(rectangle.getShape()).isEqualTo(TableShape.RECTANGLE);
        assertThat(rectangle.getCapacity()).isEqualTo(8);
    }
}
