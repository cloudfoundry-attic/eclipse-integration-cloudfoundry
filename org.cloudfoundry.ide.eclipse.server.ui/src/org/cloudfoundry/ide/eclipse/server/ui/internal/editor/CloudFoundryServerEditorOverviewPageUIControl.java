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

package org.cloudfoundry.ide.eclipse.server.ui.internal.editor;

import java.beans.PropertyChangeEvent;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryBrandingExtensionPoint;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.AbstractUIControl;
import org.eclipse.wst.server.ui.editor.ServerEditorOverviewPageModifier;

public class CloudFoundryServerEditorOverviewPageUIControl extends ServerEditorOverviewPageModifier {

	public void createControl(UI_LOCATION location, Composite parent) {
		// Do nothing
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

			if (curNewValue instanceof IServerType && isSupportedServerType((IServerType)curNewValue)) {
				// Disable the host name field.
				if (controlListener != null) {
					controlMap.put(PROP_HOSTNAME, new UIControlEntry(false, null));
					fireUIControlChangedEvent();
				}
			} else {
				// Enable the host name field.
				if (controlListener != null) {
					controlMap.put(PROP_HOSTNAME, new UIControlEntry(true,
									null));
					fireUIControlChangedEvent();
				}
			}
		}
	}
	
	private boolean isSupportedServerType(IServerType serverType) {
		if (serverType == null) {
			return false;
		}
		return CloudFoundryBrandingExtensionPoint.getServerTypeIds().contains(serverType.getId());
	}

	public void setServerWorkingCopy(IServerWorkingCopy curServerWc) {
		super.setServerWorkingCopy(curServerWc);
	}

}