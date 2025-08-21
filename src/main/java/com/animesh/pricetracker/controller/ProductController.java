package com.animesh.pricetracker.controller;

import com.animesh.pricetracker.dto.ProdRequestDTO;
import com.animesh.pricetracker.dto.ProdResponseDTO;
import com.animesh.pricetracker.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService prodService;

    @PostMapping("add")
    public ResponseEntity<?> addProduct(@RequestBody ProdRequestDTO requestDTO) {
        try {
            ProdResponseDTO responseDTO = prodService.addProduct(requestDTO);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    e.getMessage() + "\nDomains Currently Supported: " + prodService.getSupportedDomains().toString(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("all")
    public ResponseEntity<?> getAllProducts() {
        List<ProdResponseDTO> products= prodService.getAllProducts();
        if (!products.isEmpty())
            return new ResponseEntity<>(products, HttpStatus.OK);

        else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
