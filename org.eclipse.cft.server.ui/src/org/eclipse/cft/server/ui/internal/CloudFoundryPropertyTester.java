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
package org.eclipse.cft.server.ui.internal;

import java.util.List;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.debug.CloudFoundryProperties;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.viewers.StructuredSelection;
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
		// Reusing this property tester to test visibility and enablement of the 'migrated' popup menu commands/handlers.
		// The receiver can be a structured selection (TreeSelection) or a List.
		
		IServerModule serverModule = null;
		IServer server = null;
		CloudFoundryServer cloudFoundryServer = null;
		// Handle the TreeSelection
		if (receiver instanceof StructuredSelection) {
			Object obj = ((StructuredSelection)receiver).getFirstElement();
			if (obj instanceof IServer) {
			   server = (IServer) obj;
			   cloudFoundryServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
			} else if (obj instanceof IServerModule) {
			   serverModule = (IServerModule)obj;
			   cloudFoundryServer = getCloudFoundryServer(serverModule);
			}
		} // Handle the List
		else if (receiver instanceof List) {
			List<?> arr = (List<?>) receiver;
			if (!arr.isEmpty()) {
			  Object obj = arr.get(0);
			  if (obj instanceof IServer) {
				 server = (IServer) obj;
				 cloudFoundryServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
			  }
			}
		} else {  // This is the default behaviour, as before
		    serverModule = getServerModule(receiver);
			if (serverModule != null) {
			   cloudFoundryServer = getCloudFoundryServer(serverModule);
			}
		}
		// Only perform property testing for Cloud Foundry servers.
		if (cloudFoundryServer != null) {
			CloudFoundryProperties cfProperty = CloudFoundryProperties.valueOf(property);
				if (cfProperty != null && expectedValue instanceof Boolean) {
				return ((Boolean) expectedValue).booleanValue() == cfProperty.testProperty(serverModule != null ? serverModule.getModule() : null,
						cloudFoundryServer);
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
