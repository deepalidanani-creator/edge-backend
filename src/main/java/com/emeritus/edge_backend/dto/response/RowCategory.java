package com.emeritus.edge_backend.dto.response;

public enum RowCategory {

	CORRECTION,
	CREATE,
	UPDATE,
	/**
	 * SKIP_OP
	 * Same as DB snapshot so no operation needed
	 */
	SKIP_OP
}
