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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudApplicationUrlPart;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Application URL part that is aware that empty URLs are valid for standalone
 * apps, as URLs are optional.
 */
public class StandaloneAppUrlPart extends CloudApplicationUrlPart {

	public StandaloneAppUrlPart(ApplicationUrlLookupService urlLookUp) {
		super(urlLookUp);
	}

	@Override
	protected void notifyURLChanged(String appURL, IStatus status) {
		// For standalone, empty URLs are valid, as URLs are optional
		if (ValueValidationUtil.isEmpty(appURL)) {
			status = Status.OK_STATUS;
		}
		notifyStatusChange(appURL, status);
	}
}
