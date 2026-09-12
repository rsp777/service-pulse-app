package com.pawar.sop.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;

class HttpServiceTest {

	@Test
	void restCallReturnsResponseBodyFromRemoteService() {
		RestTemplate restTemplate = new RestTemplate();
		HttpService httpService = new HttpService(restTemplate);
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo("http://localhost:8080/health"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("UP", MediaType.TEXT_PLAIN));

		ResponseEntity<String> response = httpService.restCall(null, "http://localhost:8080/health",
				HttpMethod.GET, null, null);

		assertEquals("UP", response.getBody());
		server.verify();
	}
}
