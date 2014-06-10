/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryClientConnectionTest;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryProxyTest;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServerBehaviourTest;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServerTest;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServicesTest;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudUtilTest;
import org.cloudfoundry.ide.eclipse.server.core.internal.DeploymentURLTest;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerCredentialsStoreTest;
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
