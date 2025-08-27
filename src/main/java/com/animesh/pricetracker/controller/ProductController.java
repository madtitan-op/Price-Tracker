package com.animesh.pricetracker.controller;

import com.animesh.pricetracker.dto.ProdRequestDTO;
import com.animesh.pricetracker.dto.ProdResponseDTO;
import com.animesh.pricetracker.exception.ProductAlreadyExists;
import com.animesh.pricetracker.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
@Tag(name = "Product Tracker API", description = "Endpoints for managing tracked products")
public class ProductController {

    private final ProductService prodService;

    @Operation(summary = "Add a new product to track",
            description = "Accepts a product URL, target price, and email. The URL is expanded and validated against supported domains before being saved.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product added successfully",
                    content = { @Content(schema = @Schema(implementation = ProdResponseDTO.class), mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Product already exists or the domain is not supported",
                    content = { @Content(schema = @Schema()) })
    })
    @PostMapping("add")
    public ResponseEntity<?> addProduct(@RequestBody ProdRequestDTO requestDTO) {
        try {
            ProdResponseDTO responseDTO = prodService.addProduct(requestDTO);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (ProductAlreadyExists e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    "\nDomains Currently Supported: " + prodService.getSupportedDomains().toString(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @Operation(summary = "Get all tracked products",
            description = "Retrieves a list of all products currently being monitored by the service.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of products",
            content = { @Content(schema = @Schema(implementation = ProdResponseDTO.class), mediaType = "application/json") })
    @GetMapping("all")
    public ResponseEntity<?> getAllProducts() {
        List<ProdResponseDTO> products= prodService.getAllProducts();

        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @Operation(summary = "Manually check a product's price",
            description = "Triggers an immediate price check for a single product using its internal database ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Price checked successfully",
                    content = { @Content(schema = @Schema(type = "number", format = "double")) }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Could not scrape the price",
                    content = { @Content(schema = @Schema()) })
    })
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

    @Operation(summary = "Delete a tracked product",
            description = "Stops tracking a product and removes it from the database using its product ID (e.g., ASIN or FSN).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("delete/{pid}")
    public ResponseEntity<?> deleteProduct(@PathVariable int pid) {
        prodService.deleteProduct(pid);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
