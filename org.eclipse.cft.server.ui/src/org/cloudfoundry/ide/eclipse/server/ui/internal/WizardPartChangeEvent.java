/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import org.cloudfoundry.ide.eclipse.server.core.internal.ValidationEvents;
import org.eclipse.core.runtime.IStatus;

public class WizardPartChangeEvent extends PartChangeEvent {

	private final boolean updateWizardButtons;

	public WizardPartChangeEvent(Object data, IStatus status, UIPart source, int type, boolean updateWizardButtons) {
		super(data, status, source, type);
		this.updateWizardButtons = updateWizardButtons;
	}

	public WizardPartChangeEvent(Object data, IStatus status, IEventSource<?> source, boolean updateWizardButtons) {
		super(data, status, source, ValidationEvents.EVENT_NONE);
		this.updateWizardButtons = updateWizardButtons;
	}

	public boolean updateWizardButtons() {
		return updateWizardButtons;
	}

}
