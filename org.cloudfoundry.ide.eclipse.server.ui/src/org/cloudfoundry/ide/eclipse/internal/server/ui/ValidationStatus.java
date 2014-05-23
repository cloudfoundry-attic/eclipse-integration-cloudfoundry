/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.cloudfoundry.ide.eclipse.internal.server.core.ValidationEvents;
import org.eclipse.core.runtime.IStatus;

/**
 * Contains the {@link IStatus} of a validation operation, along with a
 * validation event type indicating an event that either occurred during or
 * after the validation process.
 *
 */
public class ValidationStatus {

	private final IStatus status;

	private final int eventType;

	public ValidationStatus(IStatus status, int eventType) {
		this.status = status;
		this.eventType = eventType;
	}

	public IStatus getStatus() {
		return status;
	}

	/**
	 * @return an event defined in {@link ValidationEvents}
	 */
	public int getEventType() {
		return eventType;
	}

	@Override
	public String toString() {
		return getStatus() != null ? getStatus().toString() : null;
	}
}