package com.sunsetbeach.entity;

import com.sunsetbeach.model.MenuDepartment;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "MenuItem")
public class MenuItemEntity {

    @Id
    @UuidGenerator
    private String id;

    private String name;

    private String description;

    private String category;

    /** Print routing (kitchen vs bar ticket) - independent of the free-text `category` display grouping. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MenuDepartment department = MenuDepartment.KITCHEN;

    @jakarta.persistence.Column(precision = 10, scale = 2)
    private BigDecimal price;

    private boolean isAvailable = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public MenuDepartment getDepartment() {
        return department;
    }

    public void setDepartment(MenuDepartment department) {
        this.department = department;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
