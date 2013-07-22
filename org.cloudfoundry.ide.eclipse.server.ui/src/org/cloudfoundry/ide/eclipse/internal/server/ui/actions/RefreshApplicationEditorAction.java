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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.wst.server.core.IModule;



/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class RefreshApplicationEditorAction extends CloudFoundryEditorAction {

	public RefreshApplicationEditorAction(CloudFoundryApplicationsEditorPage editorPage) {
		this(editorPage, RefreshArea.ALL);
	}

	public RefreshApplicationEditorAction(CloudFoundryApplicationsEditorPage editorPage, RefreshArea area) {
		super(editorPage, area);
		
		setImageDescriptor(CloudFoundryImages.REFRESH);
		setText("Refresh");
	}

	@Override
	public String getJobName() {
		return "Refresh application";
	}

	@Override
	public IStatus performAction(IProgressMonitor monitor) {
		// no action needed, just need to refresh editor through super class'
		// implementation
		return Status.OK_STATUS;
	}

	@Override
	protected void display404Error(IStatus status) {
		IModule currentModule = getEditorPage().getMasterDetailsBlock().getCurrentModule();
		if (currentModule != null) {
			getEditorPage().setMessage(
				"Local module is not yet deployed. Cannot refresh with server.",
				IMessageProvider.WARNING);
		}
		else {
			getEditorPage().setMessage(
				"Status is not up to date with server. Refresh needed.",
				IMessageProvider.WARNING);
		}
	}
	
	@Override
	protected boolean shouldLogException(CoreException e) {
		return !CloudErrorUtil.isNotFoundException(e);
	}
	
}
