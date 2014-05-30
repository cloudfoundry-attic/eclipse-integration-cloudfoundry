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
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import org.eclipse.core.runtime.IStatus;

/**
 * Event fired by a component, like a UI part, when state in that component has
 * changed.
 */
public class PartChangeEvent {

	private final IStatus status;

	private final IEventSource<?> source;

	private final Object data;

	private final int type;

	public PartChangeEvent(Object data, IStatus status, IEventSource<?> source, int type) {
		this.source = source;
		this.status = status;
		this.data = data;

		this.type = type;
	}

	public IEventSource<?> getSource() {
		return source;
	}

	public IStatus getStatus() {
		return status;
	}

	public int getType() {
		return type;
	}

	public Object getData() {
		return data;
	}

	@Override
	public String toString() {
		return getStatus() != null ? getStatus().toString() : super.toString();
	}

}