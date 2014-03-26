/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/


package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.beans.PropertyChangeEvent;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.AbstractUIControl;
import org.eclipse.wst.server.ui.wizard.ServerCreationWizardPageExtension;


public class CloudFoundryServerWizardUIControl extends
		ServerCreationWizardPageExtension {

	private String previousHostname = null;

	public void createControl(UI_POSITION position, Composite parent) {
		// Do nothing
	}
	
	private boolean isSupportedServerType(IServerType serverType) {
		if (serverType == null) {
			return false;
		}
		return CloudFoundryBrandingExtensionPoint.getServerTypeIds().contains(serverType.getId());
	}

	/**
	 * Allow UI Control to react based on a property change and change the UI
	 * control.
	 * 
	 * @param event
	 *            property change event that describes the change.
	 */
	public void handlePropertyChanged(PropertyChangeEvent event) {
		if (event != null
				&& AbstractUIControl.PROP_SERVER_TYPE.equals(event
						.getPropertyName())) {
			Object curNewValue = event.getNewValue();
			Object curOldValue = event.getOldValue();

			// Only enable user cloud.
			if (curOldValue != curNewValue) {
				if (curNewValue instanceof IServerType) {
					if (isSupportedServerType((IServerType)curNewValue)) {
						// Disable the host name field
						if (controlListener != null) {
							controlMap.put(PROP_HOSTNAME, new UIControlEntry(false, null));
							previousHostname = controlListener
									.getControlStringValue(PROP_HOSTNAME);
							fireUIControlChangedEvent();
						}
					} else if (isSupportedServerType((IServerType)curOldValue)) {
						// Re-enable the host name field.
						if (controlListener != null) {
							controlMap.put(PROP_HOSTNAME, new UIControlEntry(true, previousHostname));
							previousHostname = null;
							fireUIControlChangedEvent();
						}
					}
				}
			}
		}
	}

	public void setServerWorkingCopy(IServerWorkingCopy curServerWc) {
		super.setServerWorkingCopy(curServerWc);
	}

}