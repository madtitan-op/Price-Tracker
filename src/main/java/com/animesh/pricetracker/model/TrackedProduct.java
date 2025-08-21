package com.animesh.pricetracker.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class TrackedProduct {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String sid;     //id of product on their respective site

    @Column(nullable = false)
    private String site;    // site on which product is listed

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Double targetPrice;

    public TrackedProduct(String sid, String site, String url, String userEmail, Double targetPrice) {
        this.sid = sid;
        this.site = site;
        this.url = url;
        this.userEmail = userEmail;
        this.targetPrice = targetPrice;
    }

    public TrackedProduct() {}

}
