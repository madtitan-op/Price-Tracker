package com.animesh.pricetracker.exception;

import java.io.IOException;

public class ScrapingFailedException extends IOException {
    public ScrapingFailedException(String message) {
        super(message);
    }
}
