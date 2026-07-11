package com.sol.ecopulse;

import com.sol.ecopulse.repository.AbstractPostgisIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: the full application context boots against a real PostGIS container.
 * Inherits the Testcontainers setup so it does not depend on a locally-running database.
 */
class EcopulseBackendApplicationTests extends AbstractPostgisIntegrationTest {

	@Test
	void contextLoads() {
	}

}
