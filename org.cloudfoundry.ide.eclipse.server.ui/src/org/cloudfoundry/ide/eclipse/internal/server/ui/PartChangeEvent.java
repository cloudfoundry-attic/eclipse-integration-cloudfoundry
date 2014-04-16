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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.cloudfoundry.ide.eclipse.internal.server.core.ServerCredentialsValidationStatics;
import org.eclipse.core.runtime.IStatus;

/**
 * Event fired by a UI part when value changes have occured in the part's
 * controls
 */
public class PartChangeEvent {

	private final IStatus status;

	private final Object data;

	private final UIPart source;

	private final int type;



	public PartChangeEvent(Object data, IStatus status, UIPart source) {
		this(data, status, source, ServerCredentialsValidationStatics.EVENT_NONE);
	}

	public PartChangeEvent(Object data, IStatus status, UIPart source, int type) {
		this.data = data;
		this.status = status;
		this.source = source;
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public UIPart getSource() {
		return source;
	}

	public IStatus getStatus() {
		return status;
	}

	public Object getData() {
		return data;
	}

}