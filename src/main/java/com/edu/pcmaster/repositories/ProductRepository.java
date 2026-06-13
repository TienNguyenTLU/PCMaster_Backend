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

	@Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand "
			+ "WHERE (:categoryId IS NULL OR p.category.id = :categoryId) "
			+ "AND (:brandId IS NULL OR p.brand.id = :brandId) "
			+ "AND (:keyword IS NULL OR :keyword = '' "
			+ "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))",
			countQuery = "SELECT count(p) FROM Product p "
					+ "WHERE (:categoryId IS NULL OR p.category.id = :categoryId) "
					+ "AND (:brandId IS NULL OR p.brand.id = :brandId) "
					+ "AND (:keyword IS NULL OR :keyword = '' "
					+ "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
					+ "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
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
	@Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand " +
		   "WHERE p.stock > 0 AND (" +
		   "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
		   "OR LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
		   "OR LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
	List<Product> searchByKeyword(@Param("keyword") String keyword);
}

