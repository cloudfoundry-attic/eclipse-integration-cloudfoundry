package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.Arrays;
import java.util.Collections;

import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugModeType;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryMockClientFixture;
import org.eclipse.wst.server.core.IServer;


public class CloudFoundryMockDebugTest extends CloudFoundryDebugTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		new CloudFoundryMockClientFixture().setCloudFoundryClientFactory();
		testServlet = harness.startMockServer();
	}

	public void testLocalCloudConnect() throws Exception {
		serverBehavior.connect(null);
		assertEquals(IServer.STATE_STARTED, server.getServerState());
		assertEquals(Collections.emptyList(), Arrays.asList(server.getModules()));
	}

	public void testDebugStartSuspendModule() throws Exception {

		assertStarted(DebugModeType.SUSPEND);

	}
}
