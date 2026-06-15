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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionSumIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void getSum_returnsTransitiveSumFromSpecExample() throws Exception {
		mockMvc.perform(put("/transactions/10")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 5000, "type": "cars"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(put("/transactions/11")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 10000, "type": "shopping", "parent_id": 10}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(put("/transactions/12")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 5000, "type": "shopping", "parent_id": 11}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/transactions/sum/10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sum").value(20000.0));

		mockMvc.perform(get("/transactions/sum/11"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sum").value(15000.0));
	}

	@Test
	void getSum_returns404WhenTransactionDoesNotExist() throws Exception {
		mockMvc.perform(get("/transactions/sum/999"))
				.andExpect(status().isNotFound());
	}

}
