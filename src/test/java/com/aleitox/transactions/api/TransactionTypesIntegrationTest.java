package com.aleitox.transactions.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
class TransactionTypesIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void getTypes_returnsIdsForEachTypeFromSpecExample() throws Exception {
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

		mockMvc.perform(get("/transactions/types/cars"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]").value(10));

		mockMvc.perform(get("/transactions/types/shopping"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", containsInAnyOrder(11, 12)));
	}

	@Test
	void getTypes_isCaseInsensitive() throws Exception {
		mockMvc.perform(put("/transactions/100")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 5000, "type": "cars"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/transactions/types/CaRs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]").value(100));
	}

}
