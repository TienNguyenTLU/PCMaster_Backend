package com.edu.pcmaster.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.edu.pcmaster.models.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
	Optional<Product> findBySlug(String slug);

	@Query(value = "select * from products p "
			+ "where (:categoryId is null or p.category_id = :categoryId) "
			+ "and (:brandId is null or p.brand_id = :brandId) "
			+ "and (:keyword is null or :keyword = '' "
			+ "or p.name::text ilike concat('%', :keyword, '%') "
			+ "or p.description::text ilike concat('%', :keyword, '%'))",
			countQuery = "select count(*) from products p "
					+ "where (:categoryId is null or p.category_id = :categoryId) "
					+ "and (:brandId is null or p.brand_id = :brandId) "
					+ "and (:keyword is null or :keyword = '' "
					+ "or p.name::text ilike concat('%', :keyword, '%') "
					+ "or p.description::text ilike concat('%', :keyword, '%'))",
			nativeQuery = true)
	Page<Product> search(@Param("categoryId") Long categoryId,
						 @Param("brandId") Long brandId,
						 @Param("keyword") String keyword,
						 Pageable pageable);

	@Query(value = "select * from products p "
			+ "where (:componentType is null or p.specs ->> 'component_type' = :componentType) "
			+ "and (:socket is null or p.specs ->> 'socket' = :socket) "
			+ "and (:ramType is null or p.specs ->> 'ram_type' = :ramType)", nativeQuery = true)
	List<Product> findCompatibleComponents(@Param("componentType") String componentType,
									  @Param("socket") String socket,
									  @Param("ramType") String ramType);
}

