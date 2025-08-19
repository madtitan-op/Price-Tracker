package com.animesh.pricetracker.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class TrackedProduct {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Double targetPrice;

    public TrackedProduct(String url, String userEmail, Double price) {
        this.url = url;
        this.userEmail = userEmail;
        this.targetPrice = price;
    }

    public TrackedProduct() {}

}
