package com.animesh.pricetracker.dto;

public record ProdRequestDTO(
        String url,
        String userEmail,
        Double targetPrice
) {
}
