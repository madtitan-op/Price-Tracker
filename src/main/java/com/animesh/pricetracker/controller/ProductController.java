package com.animesh.pricetracker.controller;

import com.animesh.pricetracker.model.TrackedProduct;
import com.animesh.pricetracker.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService prodService;

    @PostMapping("add")
    public ResponseEntity<?> addProduct(@RequestBody TrackedProduct product) {
        try {
            TrackedProduct tProduct = prodService.addProduct(product);
            return new ResponseEntity<>(tProduct, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    e.getMessage() + "\nDomains Currently Supported: " + prodService.getSupportedDomains().toString(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("check-price")
    public ResponseEntity<?> checkPrice(@RequestParam int pid) {

        double currentPrice = prodService.checkPrice(pid);
        if (currentPrice > 0) {
            return new ResponseEntity<>(currentPrice, HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>("Try again in some time", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("delete/{pid}")
    public ResponseEntity<?> deleteProduct(@PathVariable int pid) {
        prodService.deleteProduct(pid);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
