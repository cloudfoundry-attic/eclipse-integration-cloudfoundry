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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.server.core.IApplicationDelegate;

/**
 * 
 * This contains a reference to the core-level application delegate, which
 * contains API to push an application to a CF server. Since a wizard delegate
 * does NOT require a map to a core level application delegate, the link between
 * the two is not pushed up to the parent.
 */
public abstract class ApplicationWizardDelegate implements IApplicationWizardDelegate {

	private IApplicationDelegate appDelegate;

	public void setApplicationDelegate(IApplicationDelegate appDelegate) {
		this.appDelegate = appDelegate;
	}

	/**
	 * Corresponding core level application delegate that contains API for
	 * pushing an app to a CF server. This may be null, as a wizard delegate may
	 * not be mapped to an app delegate (in the event it uses a default app
	 * delegate from the CF Application framework) .
	 * @return Corresponding Application delegate, if it exists, or null.
	 */
	public IApplicationDelegate getApplicationDelegate() {
		return appDelegate;
	}

}
