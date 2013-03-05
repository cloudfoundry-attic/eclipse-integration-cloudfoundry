/*******************************************************************************
 * Copyright (c) 2012 - 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudSpacesSelectionPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudSpaceChangeHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

public class CloudFoundrySpacesWizardFragment extends WizardFragment {

	private final CloudSpaceChangeHandler spaceChangeHandler;

	private final CloudFoundryServer cloudServer;

	public CloudFoundrySpacesWizardFragment(CloudSpaceChangeHandler spaceChangeHandler,
			CloudFoundryServer cloudServer) {
		this.spaceChangeHandler = spaceChangeHandler;
		this.cloudServer = cloudServer;
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		CloudSpacesSelectionPart spacesPart = new CloudSpacesSelectionPart(spaceChangeHandler, cloudServer,
				handle);
		return spacesPart.createComposite(parent);
	}

	public boolean isComplete() {
		return spaceChangeHandler != null && spaceChangeHandler.hasSetSpace();
	}

	@Override
	public boolean hasComposite() {
		return true;
	}

}
