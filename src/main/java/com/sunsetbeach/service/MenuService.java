package com.sunsetbeach.service;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.MenuItemMapper;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.MenuItem;
import com.sunsetbeach.model.MenuItemInput;
import com.sunsetbeach.repository.MenuItemRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemMapper menuItemMapper;
    private final AuditLogService auditLogService;

    public MenuService(MenuItemRepository menuItemRepository, MenuItemMapper menuItemMapper, AuditLogService auditLogService) {
        this.menuItemRepository = menuItemRepository;
        this.menuItemMapper = menuItemMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<MenuItem> list() {
        return menuItemRepository.findAll().stream().map(menuItemMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MenuItem getById(String id) {
        return menuItemMapper.toDto(findOrThrow(id));
    }

    /**
     * The dine-in QR ordering menu ({@code GET /public/menu}) - available, non-SPA items only.
     * Same SPA exclusion {@code OrderPrintingService#printTickets} applies when deciding what
     * gets a kitchen/bar ticket: a treatment is never something a guest picks off a menu screen.
     * Separate from {@link #list()} so the unfiltered staff catalog view is never affected.
     */
    @Transactional(readOnly = true)
    public List<MenuItem> listPublic() {
        return menuItemRepository.findAll().stream()
                .filter(MenuItemEntity::isAvailable)
                .filter(item -> item.getDepartment() != MenuDepartment.SPA)
                .map(menuItemMapper::toDto)
                .toList();
    }

    @Transactional
    public MenuItem create(MenuItemInput input) {
        requireNoNearDuplicateCategory(input.getCategory());
        MenuItemEntity entity = new MenuItemEntity();
        menuItemMapper.applyInput(entity, input);
        MenuItemEntity saved = menuItemRepository.saveAndFlush(entity);
        auditLogService.record(AuditAction.MENU_ITEM_CREATED, AuditEntityType.MENU_ITEM, saved.getId(), "Menu item " + saved.getName() + " created");
        return menuItemMapper.toDto(saved);
    }

    @Transactional
    public MenuItem update(String id, MenuItemInput input) {
        MenuItemEntity entity = findOrThrow(id);
        requireNoNearDuplicateCategory(input.getCategory());
        String oldName = entity.getName();
        menuItemMapper.applyInput(entity, input);
        MenuItemEntity saved = menuItemRepository.save(entity);

        StringBuilder summary = new StringBuilder("Menu item ").append(oldName).append(" updated");
        if (!Objects.equals(oldName, saved.getName())) {
            summary.append(" (renamed to ").append(saved.getName()).append(")");
        }
        auditLogService.record(AuditAction.MENU_ITEM_UPDATED, AuditEntityType.MENU_ITEM, saved.getId(), summary.toString());

        return menuItemMapper.toDto(saved);
    }

    @Transactional
    public void delete(String id) {
        MenuItemEntity entity = findOrThrow(id);
        try {
            menuItemRepository.deleteById(id);
            menuItemRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This menu item has existing order lines and can't be deleted.");
        }
        auditLogService.record(AuditAction.MENU_ITEM_DELETED, AuditEntityType.MENU_ITEM, id, "Menu item " + entity.getName() + " deleted");
    }

    /**
     * Category is free text, and the POS builds one tab per distinct spelling - so "cocktails"
     * typed next to an existing "Cocktails" used to become a second tab. A spelling that differs
     * from one already in use only by letter case or spacing is refused, naming the existing one;
     * the exact existing spelling (and any genuinely new name) is accepted. Existing near-duplicate
     * pairs from before this check are left alone - an item keeps its current spelling on edit.
     */
    private void requireNoNearDuplicateCategory(String rawCategory) {
        if (rawCategory == null) {
            return;
        }
        String category = MenuItemMapper.normalizeCategory(rawCategory);
        String key = category.toLowerCase(Locale.ROOT);
        Set<String> existing = menuItemRepository.findAll().stream()
                .map(MenuItemEntity::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        if (existing.contains(category)) {
            return;
        }
        existing.stream()
                .filter(c -> MenuItemMapper.normalizeCategory(c).toLowerCase(Locale.ROOT).equals(key))
                .findFirst()
                .ifPresent(match -> {
                    throw ValidationException.field("category", "A category \"" + match + "\" already exists - use that one instead.");
                });
    }

    private MenuItemEntity findOrThrow(String id) {
        return menuItemRepository.findById(id).orElseThrow(() -> new NotFoundException("Menu item not found"));
    }
}
