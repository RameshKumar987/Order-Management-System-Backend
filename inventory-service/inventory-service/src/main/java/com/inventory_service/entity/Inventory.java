package com.inventory_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventories")
@Getter
@Setter

public class Inventory {

    @Id
    private Long productId;

    private int availableStock;

    private int reservedStock;
}
