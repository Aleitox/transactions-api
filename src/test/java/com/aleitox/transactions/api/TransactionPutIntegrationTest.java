package com.aleitox.transactions.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionPutIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void putTransactions_createsHierarchyFromSpecExample() throws Exception {
		mockMvc.perform(put("/transactions/10")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 5000, "type": "cars"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));

		mockMvc.perform(put("/transactions/11")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 10000, "type": "shopping", "parent_id": 10}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));

		mockMvc.perform(put("/transactions/12")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 5000, "type": "shopping", "parent_id": 11}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}

}
