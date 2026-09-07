package com.workflow360.system.api;

import java.time.Instant;

public record SystemStatusResponse(String application,String status,String architecture,Instant timestamp) {
	
	

}
