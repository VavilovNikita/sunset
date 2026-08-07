package com.sunsetbeach.controller;

import com.sunsetbeach.api.MenuApi;
import com.sunsetbeach.model.MenuItem;
import com.sunsetbeach.model.MenuItemInput;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.service.MenuService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuController implements MenuApi {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Override
    public ResponseEntity<List<MenuItem>> listMenuItems() {
        return ResponseEntity.ok(menuService.list());
    }

    @Override
    public ResponseEntity<MenuItem> getMenuItem(String id) {
        return ResponseEntity.ok(menuService.getById(id));
    }

    @Override
    public ResponseEntity<MenuItem> createMenuItem(MenuItemInput menuItemInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.create(menuItemInput));
    }

    @Override
    public ResponseEntity<MenuItem> updateMenuItem(String id, MenuItemInput menuItemInput) {
        return ResponseEntity.ok(menuService.update(id, menuItemInput));
    }

    @Override
    public ResponseEntity<OkTrue> deleteMenuItem(String id) {
        menuService.delete(id);
        return ResponseEntity.ok(new OkTrue(true));
    }
}
