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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class CloudFoundryPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public void init(IWorkbench workbench) {
		//
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite mainArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(mainArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(mainArea);

		Label serviceLabel = new Label(mainArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(serviceLabel);
		serviceLabel.setText("Expand the tree to select a specific Cloud Foundry feature");

		return mainArea;
	}

}
