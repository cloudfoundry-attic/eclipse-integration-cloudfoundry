/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryURLNavigation;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPart;

public class GoToSpringInsightAction extends AbstractCloudFoundryServerAction {

	
	protected void serverSelectionChanged(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule, IAction action) {
		if (CloudFoundryURLNavigation.canEnableCloudFoundryNavigation(cloudServer)) {
			action.setEnabled(true);
			return;
		}
		action.setEnabled(false);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// Nothing
	}

	@Override
	void doRun(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule, IAction action) {
		CloudFoundryURLNavigation.INSIGHT_URL.navigate();
	}

}
