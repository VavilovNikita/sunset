package com.sunsetbeach.service;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.MenuItemMapper;
import com.sunsetbeach.model.MenuItem;
import com.sunsetbeach.model.MenuItemInput;
import com.sunsetbeach.repository.MenuItemRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemMapper menuItemMapper;

    public MenuService(MenuItemRepository menuItemRepository, MenuItemMapper menuItemMapper) {
        this.menuItemRepository = menuItemRepository;
        this.menuItemMapper = menuItemMapper;
    }

    @Transactional(readOnly = true)
    public List<MenuItem> list() {
        return menuItemRepository.findAll().stream().map(menuItemMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MenuItem getById(String id) {
        return menuItemMapper.toDto(findOrThrow(id));
    }

    @Transactional
    public MenuItem create(MenuItemInput input) {
        MenuItemEntity entity = new MenuItemEntity();
        menuItemMapper.applyInput(entity, input);
        return menuItemMapper.toDto(menuItemRepository.saveAndFlush(entity));
    }

    @Transactional
    public MenuItem update(String id, MenuItemInput input) {
        MenuItemEntity entity = findOrThrow(id);
        menuItemMapper.applyInput(entity, input);
        return menuItemMapper.toDto(menuItemRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        if (!menuItemRepository.existsById(id)) {
            throw new NotFoundException("Menu item not found");
        }
        try {
            menuItemRepository.deleteById(id);
            menuItemRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This menu item has existing order lines and can't be deleted.");
        }
    }

    private MenuItemEntity findOrThrow(String id) {
        return menuItemRepository.findById(id).orElseThrow(() -> new NotFoundException("Menu item not found"));
    }
}
