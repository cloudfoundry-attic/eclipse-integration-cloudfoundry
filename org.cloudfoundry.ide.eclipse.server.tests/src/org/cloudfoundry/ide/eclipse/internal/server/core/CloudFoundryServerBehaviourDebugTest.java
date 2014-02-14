/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.Arrays;
import java.util.Collections;

import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugModeType;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

public class CloudFoundryServerBehaviourDebugTest extends CloudFoundryDebugTest {

	public void testLocalCloudConnect() throws Exception {
		serverBehavior.connect(null);
		assertEquals(IServer.STATE_STARTED, server.getServerState());
		assertEquals(Collections.emptyList(), Arrays.asList(server.getModules()));
	}

	public void testDebugStartSuspendModule() throws Exception {

		assertStarted(DebugModeType.SUSPEND, "debugStartSuspend");

	}

	/**
	 * Start in suspend mode, restart in suspend mode
	 * @throws Exception
	 */
	public void testDebugSuspendRestartSuspendModule() throws Exception {
		String prefix = "debugSuspendRestart";
		assertStarted(DebugModeType.SUSPEND, prefix);

		// Restart Module
		assertRestarted(DebugModeType.SUSPEND, prefix);

	}

	/**
	 * Start in Suspend mode, restart in Regular mode
	 * @throws Exception
	 */
	public void testDebugSuspendRestartRegularModule() throws Exception {

		String prefix = "debugSuspendRestartRegular";
		assertStarted(DebugModeType.SUSPEND, prefix);

		// Restart Module in regular mode

		IModule[] modules = REGULAR_RESTART.launch(prefix);

		// Verify that the application was deployed in the correct Debug Mode
		assertIsOfDebugModeType(modules, null);

	}

	/**
	 * Start in regular mode, restart in debug suspend mode.
	 * @throws Exception
	 */
	public void testRegularStartRestartSuspendModule() throws Exception {

		String prefix = "regularStartRestartSuspended";
		IModule[] modules = REGULAR_START.launch(prefix);

		// Verify that the application was deployed in regular mode
		assertIsOfDebugModeType(modules, null);

		// Verify it has started
		int moduleState = server.getModuleState(modules);
		assertEquals(IServer.STATE_STARTED, moduleState);

		// Restart Module

		assertRestarted(DebugModeType.SUSPEND, prefix);

	}

}
