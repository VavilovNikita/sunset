package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** The one hotel logo every layout that shows it prints - a singleton row, see V137. */
@Entity
@Table(name = "PrintLogo")
public class PrintLogoEntity {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    private byte[] logoPng;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public byte[] getLogoPng() { return logoPng; }
    public void setLogoPng(byte[] logoPng) { this.logoPng = logoPng; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
