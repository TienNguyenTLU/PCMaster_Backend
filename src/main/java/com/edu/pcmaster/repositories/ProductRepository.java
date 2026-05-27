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

	@Query(value = "SELECT p.* FROM products p "
			+ "LEFT JOIN categories c ON p.category_id = c.id "
			+ "LEFT JOIN brands b ON p.brand_id = b.id "
			+ "WHERE (:categorySlug IS NULL OR :categorySlug = '' OR c.slug = :categorySlug "
			+ "   OR c.parent_id IN (SELECT id FROM categories WHERE slug = :categorySlug)) "
			+ "AND (:brandSlug IS NULL OR :brandSlug = '' OR b.name ILIKE CONCAT('%', :brandSlug, '%')) "
			+ "AND (:maxPrice IS NULL OR p.price <= :maxPrice) "
			+ "AND (:keyword IS NULL OR :keyword = '' "
			+ "   OR p.name ILIKE CONCAT('%', :keyword, '%') "
			+ "   OR p.description ILIKE CONCAT('%', :keyword, '%')) "
			+ "AND p.stock > 0 "
			+ "LIMIT 8", nativeQuery = true)
	List<Product> findProductsForChatbot(@Param("categorySlug") String categorySlug,
										@Param("brandSlug") String brandSlug,
										@Param("maxPrice") java.math.BigDecimal maxPrice,
										@Param("keyword") String keyword);
}

