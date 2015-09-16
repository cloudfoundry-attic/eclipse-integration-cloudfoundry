/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License,
 * Version 2.0 (the "License�); you may not use this file except in compliance
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
package org.eclipse.cft.server.tests.core;

import java.net.URL;

import junit.framework.TestCase;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryLoginHandler;
import org.eclipse.cft.server.tests.sts.util.StsTestUtil;
import org.eclipse.cft.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.cft.server.tests.util.CloudFoundryTestFixture.CredentialProperties;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;

/**
 * @author Terry Denney
 */
public class CloudFoundryClientConnectionTest extends TestCase {

	public void testConnectToNonSecureUrl() throws Exception {

		String url = getTestFixture().getUrl();

		URL ur = new URL(url);
		String host = ur.getHost();
		String httpUrl = "http://" + host;

		CredentialProperties credentials = getTestFixture().getCredentials();

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password,
				credentials.organization, credentials.space, httpUrl, getTestFixture().getSelfSignedCertificate());

		new CloudFoundryLoginHandler(client).login(new NullProgressMonitor());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);
	}

	public void testConnectToSecureUrl() throws Exception {
		String url = getTestFixture().getUrl();

		URL ur = new URL(url);
		String host = ur.getHost();
		String httpUrl = "https://" + host;

		CredentialProperties credentials = getTestFixture().getCredentials();

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password,
				credentials.organization, credentials.space, httpUrl, getTestFixture().getSelfSignedCertificate());

		new CloudFoundryLoginHandler(client).login(new NullProgressMonitor());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);
	}

	public void testValidCredentials() throws Exception {

		CredentialProperties credentials = getTestFixture().getCredentials();

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password,
				credentials.organization, credentials.space, getTestFixture().getUrl(), getTestFixture()
						.getSelfSignedCertificate());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);

	}

	public void testValidCredentialsLoginHandler() throws Exception {

		CredentialProperties credentials = getTestFixture().getCredentials();

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password,
				credentials.organization, credentials.space, getTestFixture().getUrl(), getTestFixture()
						.getSelfSignedCertificate());

		CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client);

		operationsHandler.login(new NullProgressMonitor());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);

	}

	public void testInvalidUsername() throws Exception {
		CredentialProperties credentials = getTestFixture().getCredentials();

		String invalidUsername = "invalid@username";

		CloudFoundryException cfe = null;
		try {
			StsTestUtil.createStandaloneClient(invalidUsername, credentials.password, credentials.organization,
					credentials.space, getTestFixture().getUrl(), getTestFixture().getSelfSignedCertificate());
		}
		catch (CloudFoundryException e) {
			cfe = e;
		}

		assertNotNull(cfe);
		assertTrue(CloudErrorUtil.getCloudFoundryErrorMessage(cfe).contains("403"));
	}

	public void testInvalidPassword() throws Exception {
		CredentialProperties credentials = getTestFixture().getCredentials();

		String invalidPassword = "wrongpassword";

		CloudFoundryException cfe = null;
		try {
			StsTestUtil.createStandaloneClient(credentials.userEmail, invalidPassword, credentials.organization,
					credentials.space, getTestFixture().getUrl(), getTestFixture().getSelfSignedCertificate());
		}
		catch (CloudFoundryException e) {
			cfe = e;
		}

		assertNotNull(cfe);
		assertTrue(CloudErrorUtil.getCloudFoundryErrorMessage(cfe).contains("403"));
	}

	public void testInvalidOrg() throws Exception {
		CredentialProperties credentials = getTestFixture().getCredentials();

		String wrongOrg = "wrongorg";

		IllegalArgumentException error = null;
		try {
			StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password, wrongOrg,
					credentials.space, getTestFixture().getUrl(), getTestFixture().getSelfSignedCertificate());
		}
		catch (IllegalArgumentException e) {
			error = e;
		}

		assertNotNull(error);
		assertTrue(error.getMessage().toLowerCase().contains("no matching organization and space"));
	}

	public void testInvalidSpace() throws Exception {
		CredentialProperties credentials = getTestFixture().getCredentials();

		String wrongSpace = "wrongSpace";

		IllegalArgumentException error = null;
		try {
			StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password, credentials.organization,
					wrongSpace, getTestFixture().getUrl(), getTestFixture().getSelfSignedCertificate());
		}
		catch (IllegalArgumentException e) {
			error = e;
		}

		assertNotNull(error);
		assertTrue(error.getMessage().toLowerCase().contains("no matching organization and space"));
	}

	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}
}
