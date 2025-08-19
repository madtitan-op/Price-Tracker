package com.animesh.pricetracker.repository;

import com.animesh.pricetracker.model.TrackedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<TrackedProduct, Integer> {
}
