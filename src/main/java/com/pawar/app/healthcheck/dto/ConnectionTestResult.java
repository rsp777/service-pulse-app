// Issue #26: Connection test result model [Author: Ravindra Singh Pawar / Antigravity]
package com.pawar.app.healthcheck.dto;

public record ConnectionTestResult(boolean connected, String message) {
}
