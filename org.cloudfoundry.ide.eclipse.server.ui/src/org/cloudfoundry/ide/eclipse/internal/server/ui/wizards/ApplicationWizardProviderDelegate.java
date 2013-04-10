/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.application.IApplicationDelegate;

public class ApplicationWizardProviderDelegate {

	private final IApplicationDelegate applicationDelegate;

	private final IApplicationWizardDelegate wizardDelegate;

	public ApplicationWizardProviderDelegate(IApplicationDelegate applicationDelegate,
			IApplicationWizardDelegate wizardDelegate) {
		this.applicationDelegate = applicationDelegate;
		this.wizardDelegate = wizardDelegate;
	}

	public IApplicationDelegate getApplicationDelegate() {
		return applicationDelegate;
	}

	public IApplicationWizardDelegate getWizardDelegate() {
		return wizardDelegate;
	}

}
