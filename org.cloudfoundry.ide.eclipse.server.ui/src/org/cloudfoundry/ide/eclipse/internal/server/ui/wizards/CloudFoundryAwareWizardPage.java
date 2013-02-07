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

import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;

public abstract class CloudFoundryAwareWizardPage extends WizardPage {

	protected CloudFoundryAwareWizardPage(String pageName, String title, String description, ImageDescriptor banner) {
		super(pageName);
		if (title != null) {
			setTitle(title);
		}
		if (description != null) {
			setDescription(description);
		}
		if (banner == null) {
			banner = CloudFoundryImages.DEFAULT_WIZARD_BANNER;
		}
		setImageDescriptor(banner);

	}

}
