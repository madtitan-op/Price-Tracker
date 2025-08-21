package com.animesh.pricetracker.dto;

public record ProdResponseDTO(
        String sid,
        String site,
        String url,
        Double targetPrice
) {
}
