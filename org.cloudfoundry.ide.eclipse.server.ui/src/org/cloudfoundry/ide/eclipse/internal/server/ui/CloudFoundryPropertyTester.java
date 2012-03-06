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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;


/**
 * Test various properties for enabling/disabling UI and commands for Cloud
 * Foundry functionality like debugging.
 * 
 * 
 * Property enum value name must match the registered property name in the
 * property tester extension point
 * @author Nieraj Singh
 * 
 */
public class CloudFoundryPropertyTester extends PropertyTester {

	public boolean test(Object receiver, String property, Object[] arg2, Object expectedValue) {
		IServerModule serverModule = getServerModule(receiver);

		if (serverModule != null) {
			CloudFoundryServer cloudFoundryServer = getCloudFoundryServer(serverModule);
			// Only perform property testing for Cloud Foundry servers.
			if (cloudFoundryServer != null) {
				CloudFoundryProperties cfProperty = CloudFoundryProperties.valueOf(property);

				if (cfProperty != null && expectedValue instanceof Boolean) {

					IModule[] modules = serverModule.getModule();
					return ((Boolean) expectedValue).booleanValue() == cfProperty.testProperty(modules,
							cloudFoundryServer);
				}
			}
		}
		return false;

	}

	protected CloudFoundryServer getCloudFoundryServer(IServerModule serverModule) {

		if (serverModule != null) {
			IServer server = serverModule.getServer();

			return (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		}
		return null;
	}

	protected IServerModule getServerModule(Object context) {
		Object toCheck = context;

		if (toCheck instanceof EvaluationContext) {
			toCheck = ((EvaluationContext) toCheck).getDefaultVariable();
		}

		if (toCheck instanceof List<?>) {
			List<?> content = (List<?>) toCheck;
			if (!content.isEmpty()) {
				Object obj = content.get(0);
				if (obj instanceof IServerModule) {
					return (IServerModule) obj;
				}
			}
		}
		return null;
	}

}
