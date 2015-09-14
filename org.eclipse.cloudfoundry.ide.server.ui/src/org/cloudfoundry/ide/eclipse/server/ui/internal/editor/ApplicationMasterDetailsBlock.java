/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.editor;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.ui.internal.actions.EditorAction.RefreshArea;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wst.server.core.IModule;

/**
 * Contains the UI for the "Applications and Services" tab in the editor
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class ApplicationMasterDetailsBlock extends MasterDetailsBlock implements IDetailsPageProvider {

	private final CloudFoundryServer cloudServer;

	private ApplicationDetailsPart detailsPart;

	private final CloudFoundryApplicationsEditorPage editorPage;

	private ApplicationMasterPart masterPart;

	public ApplicationMasterDetailsBlock(CloudFoundryApplicationsEditorPage editorPage, CloudFoundryServer cloudServer) {
		this.editorPage = editorPage;
		this.cloudServer = cloudServer;
	}

	@Override
	public void createContent(IManagedForm managedForm) {
		super.createContent(managedForm);
		// Set the width allocation for both the master and detail parts.
		// Fix for STS-2995
		sashForm.setWeights(new int[] { 50, 50 });
	}

	public IModule getCurrentModule() {
		return getMasterPart().getCurrentModule();
	}

	public ApplicationDetailsPart getDetailsPage() {
		if (detailsPart == null) {
			detailsPart = new ApplicationDetailsPart(editorPage, cloudServer);
		}
		return detailsPart;
	}

	public ApplicationMasterPart getMasterPart() {
		return masterPart;
	}

	public IDetailsPage getPage(Object key) {
		return getDetailsPage();
	}

	public Object getPageKey(Object object) {
		if (object == null) {
			return null;
		}
		// always use same details page
		return getClass();
	}

	public void refreshUI(RefreshArea area) {
		if (area == RefreshArea.MASTER || area == RefreshArea.ALL) {
			masterPart.refreshUI();
		}

		if (area == RefreshArea.DETAIL || area == RefreshArea.ALL) {
			if (detailsPart != null) {
				detailsPart.refreshUI();
			}
		}
	}

	@Override
	protected void createMasterPart(IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();
		Composite container = toolkit.createComposite(parent);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		masterPart = new ApplicationMasterPart(editorPage, managedForm, container, cloudServer);
		managedForm.addPart(masterPart);
		masterPart.createContents();
	}

	@Override
	protected void createToolBarActions(IManagedForm managedForm) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		detailsPart.setPageProvider(this);
	}

}
