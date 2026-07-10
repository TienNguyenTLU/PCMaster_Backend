package com.edu.pcmaster;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.beans.factory.annotation.Autowired;
import com.edu.pcmaster.services.EmbeddingIngestionService;
import com.edu.pcmaster.services.RagChatService;
import java.util.List;

@SpringBootTest
@ActiveProfiles("dev")
@org.springframework.transaction.annotation.Transactional
class PcmasterApplicationTests {

	@Autowired
	private EmbeddingIngestionService embeddingIngestionService;

	@Autowired
	private RagChatService ragChatService;

	@Autowired
	private com.edu.pcmaster.repositories.ProductRepository productRepository;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Autowired
	private org.springframework.ai.vectorstore.VectorStore vectorStore;

	@Test
	@org.springframework.test.annotation.Commit
	void updateStockTo40() {
		int count = jdbcTemplate.update("UPDATE products SET stock = 40");
		System.out.println("[TEST] Updated stock to 40 for " + count + " products.");
	}

	@Test
	void contextLoads() {
	}

	@Test
	@org.springframework.test.annotation.Commit
	void testReindex() {
		int count = embeddingIngestionService.reindexAll();
		System.out.println("[TEST] Reindexed products count: " + count);
	}

	@Test
	void testCategorySearch() {
		String[] types = {"CPU", "MAINBOARD", "RAM", "GPU", "STORAGE", "PSU", "CASE", "COOLER"};
		for (String type : types) {
			String filterExpr = String.format("componentType == '%s'", type);
			org.springframework.ai.vectorstore.SearchRequest searchRequest = 
				org.springframework.ai.vectorstore.SearchRequest.builder()
					.query(type + " Build PC gaming")
					.topK(5)
					.filterExpression(filterExpr)
					.build();
			try {
				var docs = vectorStore.similaritySearch(searchRequest);
				System.out.println("[TEST_SEARCH] Category " + type + " returned: " + docs.size() + " documents.");
				for (var doc : docs) {
					System.out.println("  - " + doc.getMetadata().get("name") + " | componentType=" + doc.getMetadata().get("componentType") + " | productId=" + doc.getMetadata().get("productId"));
				}
			} catch (Exception e) {
				System.err.println("[TEST_SEARCH] Search failed for " + type + ": " + e.getMessage());
			}
		}
	}



	@Test
	void testChatFlow() {
		var products = productRepository.findAll();
		System.out.println("[TEST] Available products in DB:");
		products.stream().limit(10).forEach(p -> {
			System.out.printf(" - ID: %d | Name: %s | Brand: %s | Category: %s%n",
					p.getId(), p.getName(),
					p.getBrand() != null ? p.getBrand().getName() : "None",
					p.getCategory() != null ? p.getCategory().getName() : "None");
		});

		if (products.isEmpty()) {
			System.out.println("[TEST] No products found in DB, skipping chat flow test.");
			return;
		}

		
		String targetName = products.get(0).getName();
		String query = String.format("Mẫu %s hiện tại giá bao nhiêu, còn hàng không em?", targetName);
		System.out.println("[TEST] Chatting with query: " + query);

		var response = ragChatService.chat(query, List.of(), "consult");
		System.out.println("[TEST] AI Response:\n" + response.message());
		System.out.println("[TEST] Recommended Products:");
		response.recommendedProducts().forEach(p -> {
			System.out.printf(" - %s | Price: %s | Stock: %d%n", p.name(), p.price(), p.stock());
		});
	}


}


