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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryURLNavigation;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;


public class GoToSpringLinkWidget extends LinkWidget {

	public static final String NAVIGATION_LABEL = "Go to Spring Insight";

	public GoToSpringLinkWidget(Composite parent, FormToolkit toolKit) {
		super(parent, NAVIGATION_LABEL, CloudFoundryURLNavigation.INSIGHT_URL.getLocation(), toolKit);
	}

	protected void navigate() {
		CloudFoundryURLNavigation.INSIGHT_URL.navigate();
	}
}
