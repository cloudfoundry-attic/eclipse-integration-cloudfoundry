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
 *     IBM - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import org.cloudfoundry.ide.eclipse.server.core.AbstractCloudFoundryUrl;

/**
 * Represents a user defined CloudFoundry server Url which is used
 * in several sections of the code.
 */
public class UserDefinedCloudFoundryUrl extends AbstractCloudFoundryUrl {
	private boolean selfSigned;

	public UserDefinedCloudFoundryUrl(String name, String url, boolean selfSigned) {
		super (name, url, null, null);
		this.selfSigned = selfSigned;
	}
	
	/**
	 * Must always return true
	 */
	@Override
	public boolean getUserDefined() {
		return true;
	}
	
	/**
	 * Returns true or false, depending on the value of self-signed boolean passed
	 * in constructor
	 */
	@Override
	public boolean getSelfSigned() {
		return selfSigned;
	}
}
