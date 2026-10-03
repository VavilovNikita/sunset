package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.RestaurantMap;
import com.sunsetbeach.model.RestaurantMapTable;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.TableRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the restaurant map lists and what it calls occupied - the two facts a waiter reads off it.
 * SPA-zone tables belong to the spa map, never here; "occupied" is exactly the POS board's own
 * OPEN/SENT set, so a PAID or CANCELLED order leaves its table free.
 */
@SpringBootTest
@Transactional
class RestaurantMapServiceTests extends AbstractIntegrationTest {

    @Autowired
    private RestaurantMapService restaurantMapService;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private OrderRepository orderRepository;

    private TableEntity createTable(Zone zone) {
        TableEntity table = new TableEntity();
        table.setZone(zone);
        table.setLabel("Map Test " + UUID.randomUUID());
        table.setCapacity(4);
        table.setActive(true);
        return tableRepository.saveAndFlush(table);
    }

    private OrderEntity createOrder(TableEntity table, OrderStatus status) {
        OrderEntity order = new OrderEntity();
        order.setTableId(table.getId());
        order.setStatus(status);
        return orderRepository.saveAndFlush(order);
    }

    private static RestaurantMapTable find(RestaurantMap map, TableEntity table) {
        return map.getTables().stream().filter(t -> t.getTableId().equals(table.getId())).findFirst().orElse(null);
    }

    @Test
    void get_listsNonSpaTablesWithTheirOpenAndSentOrdersOnly() {
        TableEntity spaTable = createTable(Zone.SPA);
        TableEntity busy = createTable(Zone.RESTAURANT);
        TableEntity free = createTable(Zone.BAR);
        free.setPositionX(new BigDecimal("0.25"));
        free.setPositionY(new BigDecimal("0.75"));
        tableRepository.saveAndFlush(free);

        OrderEntity open = createOrder(busy, OrderStatus.OPEN);
        OrderEntity sent = createOrder(busy, OrderStatus.SENT);
        createOrder(free, OrderStatus.PAID);
        createOrder(free, OrderStatus.CANCELLED);
        createOrder(spaTable, OrderStatus.OPEN);

        RestaurantMap map = restaurantMapService.get();

        assertThat(find(map, spaTable)).isNull();
        assertThat(find(map, busy).getOpenOrderIds()).containsExactlyInAnyOrder(open.getId(), sent.getId());
        RestaurantMapTable freeDto = find(map, free);
        assertThat(freeDto.getOpenOrderIds()).isEmpty();
        assertThat(freeDto.getZone()).isEqualTo(Zone.BAR);
        assertThat(freeDto.getPositionX().get()).isEqualByComparingTo("0.25");
        assertThat(freeDto.getPositionY().get()).isEqualByComparingTo("0.75");
    }
}
