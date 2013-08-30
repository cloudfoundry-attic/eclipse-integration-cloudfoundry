/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationUrlLookup;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudHostDomainUrlPart;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Application URL part that is aware that empty URLs are valid for standalone
 * apps, as URLs are optional.
 */
public class StandaloneAppUrlPart extends CloudHostDomainUrlPart {

	public StandaloneAppUrlPart(CloudApplicationUrlLookup urlLookUp) {
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
