package com.pawar.todo.amt.converter;

import org.springframework.stereotype.Component;

import com.pawar.todo.amt.constants.HttpMethodName;
@Component
public class HttpMethodNameConverter {
	public HttpMethodName toEnum(String status) {
		return HttpMethodName.valueOf(status.toUpperCase());
	}

	public String toString(HttpMethodName httpMethodName) {
		return httpMethodName.name();
	}
}