package com.aleitox.transactions.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TransactionErrorHandlingIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void put_returns400WhenParentDoesNotExist() throws Exception {
		mockMvc.perform(put("/transactions/2001")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 5000, "type": "cars", "parent_id": 9999}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Parent does not exist: 9999"));
	}

	@Test
	void put_returns400WhenParentWouldCreateCycle() throws Exception {
		mockMvc.perform(put("/transactions/2010")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 5000, "type": "cars"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(put("/transactions/2011")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 10000, "type": "shopping", "parent_id": 2010}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(put("/transactions/2010")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 5000, "type": "cars", "parent_id": 2011}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Cycle detected for transaction: 2010"));
	}

	@Test
	void getSum_returns404WithErrorBodyWhenTransactionDoesNotExist() throws Exception {
		mockMvc.perform(get("/transactions/sum/9999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Transaction not found: 9999"));
	}

	@Test
	void getTypes_returns400WhenTypeIsBlankAfterTrim() throws Exception {
		mockMvc.perform(get("/transactions/types/   "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("type must not be empty"));
	}

	@Test
	void put_returns400WhenBeanValidationFails() throws Exception {
		mockMvc.perform(put("/transactions/2003")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"type": "cars"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("amount: must not be null"));
	}

}
