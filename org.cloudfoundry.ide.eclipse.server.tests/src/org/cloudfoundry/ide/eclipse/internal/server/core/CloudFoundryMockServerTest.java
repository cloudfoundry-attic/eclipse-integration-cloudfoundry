package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryMockClientFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.runtime.NullProgressMonitor;


public class CloudFoundryMockServerTest extends AbstractCloudFoundryTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		new CloudFoundryMockClientFixture().setCloudFoundryClientFactory();
		testServlet = harness.startMockServer();
	}

	public void testValidate() throws Exception {
		// Test that client connection occurs without exceptions
		CloudFoundryServerBehaviour.validate(harness.getUrl(), "user", "password", new NullProgressMonitor());

	}

	@Override
	protected Harness createHarness() {
		return CloudFoundryTestFixture.LOCAL.harness();
	}

}
