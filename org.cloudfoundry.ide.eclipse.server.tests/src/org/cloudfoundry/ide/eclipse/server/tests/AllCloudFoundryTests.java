/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.cloudfoundry.ide.eclipse.internal.server.core.CaldecottTunnelTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryClientTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryConsoleTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviourTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServicesTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtilTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentURLTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.ServerCredentialsStoreTest;
import org.cloudfoundry.ide.eclipse.server.tests.sts.util.ManagedTestSuite;

/**
 * @author Steffen Pingel
 */
public class AllCloudFoundryTests {

	public static final String PLUGIN_ID = "org.cloudfoundry.ide.eclipse.server.tests";

	public static Test suite() {
		return suite(false);
	}

	public static Test suite(boolean heartbeat) {
		TestSuite suite = new ManagedTestSuite(AllCloudFoundryTests.class.getName());
		if (!heartbeat) {
			// XXX fails for on build server for unknown reasons
			// if (!StsTestUtil.isOnBuildSite()) {
			suite.addTestSuite(CloudFoundryServerBehaviourTest.class);
			// }
		}
		suite.addTestSuite(ServerCredentialsStoreTest.class);
		suite.addTestSuite(CloudFoundryServerTest.class);
		// XXX suite.addTestSuite(LocalCloudFoundryServerBehaviourTest.class);
		suite.addTestSuite(CloudUtilTest.class);
		// suite.addTestSuite(CloudFoundryMockServerTest.class);
		suite.addTestSuite(DeploymentURLTest.class);
		suite.addTestSuite(CloudFoundryServicesTest.class);
		suite.addTestSuite(CloudFoundryConsoleTest.class);
		suite.addTestSuite(CloudFoundryClientTest.class);
		suite.addTestSuite(CaldecottTunnelTest.class);
		return suite;
	}

	public static Test experimentalSuite() {
		TestSuite suite = new ManagedTestSuite(AllCloudFoundryTests.class.getName());
		suite.addTestSuite(CloudFoundryServerBehaviourTest.class);
		return suite;
	}

}
