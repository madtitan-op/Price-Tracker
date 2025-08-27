package com.animesh.pricetracker.exception;

import java.io.IOException;

public class ProductAlreadyExists extends IOException {
    public ProductAlreadyExists(String message) {
        super(message);
    }
}
