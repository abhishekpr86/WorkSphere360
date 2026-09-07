package com.workflow360.system.api;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

	
	@GetMapping("/status")
	public ResponseEntity<SystemStatusResponse> getStatus() {

		SystemStatusResponse response = new SystemStatusResponse(

				"WorkFlow360 Backend", "UP", "MODULAR_MONOLITH", Instant.now());

		return ResponseEntity.ok(response);

	}

}
