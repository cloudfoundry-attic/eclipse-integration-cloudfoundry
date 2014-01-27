/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRegistry.Priority;
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
public class ApplicationProvider extends FrameworkProvider<IApplicationDelegate> {

	private static final String MODULE_ID_ELEMENT = "moduleID";

	private static final String PRIORITY_ATTR = "priority";

	private static final String ID_ATTRIBUTE = "id";

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
