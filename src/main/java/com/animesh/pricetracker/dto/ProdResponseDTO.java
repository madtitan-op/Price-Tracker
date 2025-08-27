package com.animesh.pricetracker.dto;

public record ProdResponseDTO(
        Integer id,
        String sid,
        String site,
        String url,
        Double targetPrice
) {
}
