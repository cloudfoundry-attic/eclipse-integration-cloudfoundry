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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryURLNavigation;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPart;


public class GoToSpringInsightAction extends AbstractCloudFoundryServerAction {

	public void run(IAction action) {
		CloudFoundryURLNavigation.INSIGHT_URL.navigate();
	}

	protected void serverSelectionChanged(IAction action) {
		if (CloudFoundryURLNavigation.canEnableCloudFoundryNavigation(getCloudFoundryServer())) {
			action.setEnabled(true);
			return;
		}
		action.setEnabled(false);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// Nothing
	}

}
