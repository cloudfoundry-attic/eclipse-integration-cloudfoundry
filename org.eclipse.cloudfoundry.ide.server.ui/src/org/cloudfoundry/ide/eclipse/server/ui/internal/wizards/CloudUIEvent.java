/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import org.cloudfoundry.ide.eclipse.server.ui.internal.IEventSource;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;

public class CloudUIEvent implements IEventSource<CloudUIEvent> {
	public static final CloudUIEvent APP_NAME_CHANGE_EVENT = new CloudUIEvent(
			Messages.CloudUIEvent_TEXT_APP_NAME_CHANGE);

	public static final CloudUIEvent APPLICATION_URL_CHANGED = new CloudUIEvent(
			Messages.CloudUIEvent_TEXT_APP_URL_CHANGE);

	public static final CloudUIEvent BUILD_PACK_URL = new CloudUIEvent(Messages.CloudUIEvent_TEXT_BUILDPACK);

	public static final CloudUIEvent MEMORY = new CloudUIEvent(Messages.COMMONTXT_MEM);

	private final String name;

	public CloudUIEvent(String name) {
		this.name = name;
	}

	@Override
	public CloudUIEvent getSource() {
		return this;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}

}
