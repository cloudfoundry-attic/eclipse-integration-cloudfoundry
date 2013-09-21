/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryCredentialsPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudSpaceChangeHandler;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Credentials wizard page used to prompt users for credentials in case an
 * EXISTING server instance can no longer connect with the existing credentials.
 * This wizard page is not used in the new server wizard when creating a new
 * server instance.
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryCredentialsWizardPage extends WizardPage {

	private final CloudFoundryCredentialsPart credentialsPart;

	private CloudSpaceChangeHandler spaceChangeHandler;

	private CredentialsWizardUpdateHandler wizardUpdateHandler;

	protected CloudFoundryCredentialsWizardPage(CloudFoundryServer server) {
		super(server.getServer().getName() + " Credentials");
		spaceChangeHandler = new CloudSpaceChangeHandler(server);
		wizardUpdateHandler = new CredentialsWizardUpdateHandler(this);
		credentialsPart = new CloudFoundryCredentialsPart(server, spaceChangeHandler, wizardUpdateHandler, this);
	}

	public void createControl(Composite parent) {
		Control control = credentialsPart.createPart(parent);
		setControl(control);
	}

	@Override
	public boolean isPageComplete() {
		return wizardUpdateHandler.isValid();
	}

	public CloudSpaceChangeHandler getSpaceChangeHandler() {
		return spaceChangeHandler;
	}

	public boolean areSpacesResolved() {
		return spaceChangeHandler.getCurrentSpacesDescriptor() != null;
	}

	public boolean canFlipToNextPage() {
		// There should only be a next page for the spaces page if there is a
		// cloud space descriptor set
		return isPageComplete() && areSpacesResolved() && getNextPage() != null;
	}

}
