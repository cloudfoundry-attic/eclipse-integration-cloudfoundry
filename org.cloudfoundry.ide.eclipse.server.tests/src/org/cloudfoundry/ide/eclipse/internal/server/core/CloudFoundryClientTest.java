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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import junit.framework.TestCase;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.junit.Assert;

/**
 * @author Terry Denney
 */
public class CloudFoundryClientTest extends TestCase {

	public void testConnectToNonSecureUrl() throws Exception {
		CloudFoundryClient client = CloudFoundryPlugin.getDefault().getCloudFoundryClientFactory()
				.getCloudFoundryClient("http://api.cloudfoundry.com");

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);
	}

	public void testConnectToSecureUrl() throws Exception {
		CloudFoundryClient client = CloudFoundryPlugin.getDefault().getCloudFoundryClientFactory()
				.getCloudFoundryClient("https://api.cloudfoundry.com");
		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);

	}
}
