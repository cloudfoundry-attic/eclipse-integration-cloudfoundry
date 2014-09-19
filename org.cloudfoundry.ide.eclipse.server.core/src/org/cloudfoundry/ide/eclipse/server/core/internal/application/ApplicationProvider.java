/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.application;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.AbstractApplicationDelegate;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ApplicationRegistry.Priority;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * 
 * A wrapper around a application contribution from the extension point:
 * 
 * org.cloudfoundry.ide.eclipse.server.core.application
 * 
 * Reads the attributes and application delegate from the extension point
 * configuration element, and contains additional API to access the extension
 * point attributes, like provider ID.
 * 
 */
public class ApplicationProvider extends FrameworkProvider<AbstractApplicationDelegate> {

	private static final String MODULE_ID_ELEMENT = "moduleID"; //$NON-NLS-1$

	private static final String PRIORITY_ATTR = "priority"; //$NON-NLS-1$

	private static final String ID_ATTRIBUTE = "id"; //$NON-NLS-1$

	private Priority priority;

	private List<String> moduleIDs;

	public ApplicationProvider(IConfigurationElement configurationElement, String extensionPointID) {
		super(configurationElement, extensionPointID);
	}

	public Priority getPriority() {

		if (priority == null && configurationElement != null) {
			priority = getPriority(configurationElement.getAttribute(PRIORITY_ATTR));
		}
		return priority;

	}

	/**
	 * List of Eclipse WST module IDs that this provider supports.
	 */
	public List<String> getModuleIDs() {
		if (moduleIDs == null) {
			// Initialise only once, even if failures occur with the
			// configuration element
			moduleIDs = new ArrayList<String>();

			if (configurationElement != null) {
				IConfigurationElement[] moduleIDElements = configurationElement.getChildren(MODULE_ID_ELEMENT);
				if (moduleIDElements != null) {
					for (IConfigurationElement element : moduleIDElements) {
						String value = element.getAttribute(ID_ATTRIBUTE);
						if (value != null) {
							moduleIDs.add(value);
						}
					}
				}
			}
		}
		return moduleIDs;
	}

	private static Priority getPriority(String priority) {
		for (Priority pr : Priority.values()) {
			if (pr.name().equals(priority)) {
				return pr;
			}
		}

		return null;
	}

}
