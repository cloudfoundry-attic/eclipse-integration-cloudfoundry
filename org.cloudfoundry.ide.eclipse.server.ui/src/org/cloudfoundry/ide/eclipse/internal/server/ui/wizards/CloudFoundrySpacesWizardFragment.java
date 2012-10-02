/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
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
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudSpaceChangeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

public class CloudFoundrySpacesWizardFragment extends WizardFragment {

	private final CloudSpaceChangeListener cloudSpaceChangeListener;

	private final CloudFoundryServer cloudServer;

	public CloudFoundrySpacesWizardFragment(CloudSpaceChangeListener cloudSpaceChangeListener,
			CloudFoundryServer cloudServer) {
		this.cloudSpaceChangeListener = cloudSpaceChangeListener;
		this.cloudServer = cloudServer;
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		CloudSpacesSelectionPart spacesPart = new CloudSpacesSelectionPart(cloudSpaceChangeListener, cloudServer,
				handle);
		return spacesPart.createComposite(parent);
	}

	public boolean isComplete() {
		return cloudSpaceChangeListener != null && cloudSpaceChangeListener.hasSetSpace();
	}

	@Override
	public boolean hasComposite() {
		return true;
	}

}
