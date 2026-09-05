package com.postoffice.repository;

import com.postoffice.model.PostalService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostalServiceRepository extends JpaRepository<PostalService, Long> {
    Optional<PostalService> findBySlug(String slug);
    Optional<PostalService> findByNameIgnoreCase(String name);
}
