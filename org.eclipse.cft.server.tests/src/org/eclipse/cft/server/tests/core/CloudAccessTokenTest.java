/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
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

import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryLoginHandler;
import org.eclipse.cft.server.tests.AllCloudFoundryTests;
import org.eclipse.cft.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * This tests possible token access errors. Since it may not run consistently,
 * it is not part of {@link AllCloudFoundryTests}
 *
 */
public class CloudAccessTokenTest extends AbstractCloudFoundryTest {

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}

	public void testFailingRequestAccessTokenErrorClient() throws Exception {
		CloudFoundryOperations client = harness.createExternalClient();
		CloudFoundryLoginHandler handler = new CloudFoundryLoginHandler(client);

		OAuth2AccessToken token = handler.login(new NullProgressMonitor());
		assertNotNull(token);
		assertFalse(token.isExpired());

		List<CloudApplication> apps = client.getApplications();
		assertNotNull(apps);

		longWait();

		Exception error = null;
		try {
			client.getApplications();
		}
		catch (Exception e) {
			error = e;
		}

		assertNotNull(
				"Expected timeout or token access exception from client. Run test again and increase timeout if necessary",
				error);
		assertTrue("Expected access or auth exception from handler", handler.shouldAttemptClientLogin(error));

		OAuth2AccessDeniedException oauthError = error instanceof OAuth2AccessDeniedException ? (OAuth2AccessDeniedException) error
				: (OAuth2AccessDeniedException) error.getCause();

		assertNotNull("Expected OAuth2AccessDeniedException", oauthError);

		token = handler.login(new NullProgressMonitor(), 3, 2000);
		assertNotNull(token);
		assertFalse(token.isExpired());

		apps = client.getApplications();
		assertNotNull(apps);
	}

	protected void longWait() throws CoreException {
		waitForTimeout(20 * 60 * 1000, 60 * 1000);
	}

	protected void waitForTimeout(int total, int interval) throws CoreException {

		int wait = total;

		if (wait <= 0) {
			return;
		}
		if (interval < 1 || interval > wait) {
			interval = wait;
		}

		// Test the Server behaviour API that checks if application is running
		System.out.println("Total time waiting: " + wait / 1000 + "s");
		System.out.println("Interval: " + interval / 1000 + "s");

		for (; wait > 0;) {
			System.out.println("Time remaining for timeout test: " + wait);
			wait -= interval;
			try {
				Thread.sleep(interval);
			}
			catch (InterruptedException e) {
				throw CloudErrorUtil.toCoreException(
						"Failed timeout test. Interrupted while waiting: " + e.getMessage(), e);
			}
		}
	}

}
