package com.workflow360.common.api;

import java.util.List;

public record PageResponse<T>(
		List content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {

}
