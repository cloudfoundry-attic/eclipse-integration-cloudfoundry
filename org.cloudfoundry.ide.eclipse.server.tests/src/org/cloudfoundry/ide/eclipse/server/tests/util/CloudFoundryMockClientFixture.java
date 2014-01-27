/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.tests.util;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryClientFactory;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryMockClientFixture.CFClientMockedRestTemplate;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryMockClientFixture.TestConnectionDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.springframework.web.client.RestTemplate;

/**
 * Sets up a Cloud Foundry client in the Cloud Foundry feature that contains a
 * client with a mocked rest template, thus avoiding the need of connecting to
 * an actual server.
 * 
 * Note that this MUST be called and initialised prior to starting any server
 * test harness
 * 
 */
public class CloudFoundryMockClientFixture {

	public CloudFoundryClientFactory setCloudFoundryClientFactory() {
		// set Cloud Foundry client factory that generates a client with a
		// mocked rest template
		CloudFoundryClientFactory factory = new CloudFoundryClientFactory() {

			public CloudFoundryClient getNonUAACloudFoundryOperations(String userName, String password, URL url) {

				try {
					return new CFClientMockedRestTemplate(userName, password, url);
				}
				catch (MalformedURLException e) {
					CloudFoundryPlugin.logError(e);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError(e);
				}
				return null;
			}

			public CloudFoundryClient getCloudFoundryOperations(String cloudControllerUrl) throws MalformedURLException {
				try {
					return new CFClientMockedRestTemplate(cloudControllerUrl);
				}
				catch (MalformedURLException e) {
					CloudFoundryPlugin.logError(e);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError(e);
				}
				return null;
			}

		};

		CloudFoundryPlugin.setCloudFoundryClientFactory(factory);
		return factory;
	}

	class CFClientMockedRestTemplate extends CloudFoundryClient {

		public CFClientMockedRestTemplate(String cloudControllerUrl) throws MalformedURLException, CoreException {
			this(null, null, new URL(cloudControllerUrl));
		}

		public CFClientMockedRestTemplate(String userName, String password, URL url) throws MalformedURLException,
				CoreException {
			super(new CloudCredentials(userName, password), url);

			initMockedRestTemplate(userName, password, url);
		}

		protected void initMockedRestTemplate(String email, String password, URL cloudControllerUrl)
				throws CoreException {
			Class<?> cls = CloudFoundryClient.class;
			Field restTemplateField;

			try {
				restTemplateField = cls.getDeclaredField("restTemplate");

				RestTemplate mockedTemplate = new MockRestTemplate(new TestConnectionDescriptor(cloudControllerUrl,
						email, null, password));

				// Set the mocked template
				restTemplateField.setAccessible(true);
				restTemplateField.set(this, mockedTemplate);

			}
			catch (SecurityException e) {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
			}
			catch (NoSuchFieldException e) {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
			}
			catch (IllegalAccessException e) {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
			}

		}

	}

	class TestConnectionDescriptor {

		public final URL controllerURL;

		public final String email;

		public final String token;

		public final String password;

		public TestConnectionDescriptor(URL controllerURL, String email, String token, String password) {

			this.controllerURL = controllerURL;
			this.email = email;
			this.token = token;
			this.password = password;
		}

	}

}
