/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryClientConnectionTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProxyTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviourTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServicesTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtilTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentURLTest;
import org.cloudfoundry.ide.eclipse.internal.server.core.ServerCredentialsStoreTest;
import org.cloudfoundry.ide.eclipse.server.tests.sts.util.ManagedTestSuite;
import org.cloudfoundry.ide.eclipse.server.tests.sts.util.StsTestUtil;

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

		// These need to be enabled only if a light-weight http servlet is
		// included in the build. They have been commented out since CF 1.0.0
		// See STS-3159
		// XXX suite.addTestSuite(LocalCloudFoundryServerBehaviourTest.class);
		// suite.addTestSuite(CloudFoundryMockServerTest.class);
		// TODO: Enable when Caldecott is fixed post CF 1.5.1
		// suite.addTestSuite(CaldecottTunnelTest.class);

		if (!heartbeat) {
			// XXX fails for on build server for unknown reasons
			if (!StsTestUtil.isOnBuildSite()) {
				suite.addTestSuite(CloudFoundryServerBehaviourTest.class);
			}
		}

		suite.addTestSuite(CloudFoundryProxyTest.class);
		suite.addTestSuite(ServerCredentialsStoreTest.class);
		suite.addTestSuite(CloudFoundryServerTest.class);
		suite.addTestSuite(CloudUtilTest.class);

		suite.addTestSuite(DeploymentURLTest.class);
		suite.addTestSuite(CloudFoundryServicesTest.class);
		suite.addTestSuite(CloudFoundryClientConnectionTest.class);

		return suite;
	}

	public static Test experimentalSuite() {
		TestSuite suite = new ManagedTestSuite(AllCloudFoundryTests.class.getName());
		suite.addTestSuite(CloudFoundryServerBehaviourTest.class);
		return suite;
	}

}
