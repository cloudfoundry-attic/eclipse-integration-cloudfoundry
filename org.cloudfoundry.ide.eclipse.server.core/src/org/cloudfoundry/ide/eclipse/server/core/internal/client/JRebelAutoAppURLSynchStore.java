/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.eclipse.osgi.util.NLS;

public class JRebelAutoAppURLSynchStore extends ServerPropertyStore {

	public static final String PREF_ID = CloudFoundryPlugin.PLUGIN_ID + ".jrebel.auto.appurl.synch"; //$NON-NLS-1$

	private String errorMessage;

	private final String serverUrl;

	public JRebelAutoAppURLSynchStore(String serverUrl, String org, String space) {
		super(new ServerProperty(serverUrl, org, space));
		this.serverUrl = serverUrl;
	}

	@Override
	protected String getPropertyID() {
		return PREF_ID;
	}

	@Override
	protected String getError() {
		if (errorMessage == null) {
			errorMessage = NLS.bind(Messages.ERROR_FAILED_ACCESS_JREBEL_PREF, serverUrl);
		}
		return errorMessage;
	}
}
