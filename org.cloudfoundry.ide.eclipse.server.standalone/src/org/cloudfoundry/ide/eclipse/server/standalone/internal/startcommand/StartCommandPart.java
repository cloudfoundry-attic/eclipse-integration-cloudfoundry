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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.startcommand;

import org.cloudfoundry.ide.eclipse.internal.server.ui.UIPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Base part that allows a UI part to be defined to set a Java start command
 * 
 */
public abstract class StartCommandPart extends UIPart {
	private final Composite parent;

	private Control composite;

	protected StartCommandPart(Composite parent) {
		this.parent = parent;
	}

	public Control getComposite() {
		if (composite == null) {
			composite = createPart(parent);
		}
		return composite;
	}

	/**
	 * Tells the part to update the start command from current values of in the
	 * UI control and notify listeners with the revised start command
	 */
	abstract public void updateStartCommand();

}