package com.pawar.sop.http.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpService {

	private final RestTemplate restTemplate;

	public HttpService() {
		this(new RestTemplate());
	}

	HttpService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public ResponseEntity<String> restCall(String requestBody, String url, HttpMethod method,
			Map<String, String> headers, Map<String, ?> uriVariables) {
		HttpHeaders httpHeaders = new HttpHeaders();
		if (headers != null && !headers.isEmpty()) {
			httpHeaders.setAll(headers);
		}
		HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, httpHeaders);
		if (uriVariables == null || uriVariables.isEmpty()) {
			return restTemplate.exchange(url, method, requestEntity, String.class);
		}
		return restTemplate.exchange(url, method, requestEntity, String.class, uriVariables);
	}
}
