/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryURLNavigation;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;


public class SpringInsightSection extends ServerEditorSection {

	public void createSection(Composite parent) {

		// Don't create section if CF server is not the Pivotal CF server
		if (!CloudFoundryURLNavigation.canEnableCloudFoundryNavigation(getCloudFoundryServer())) {
			return;
		}
		super.createSection(parent);

		FormToolkit toolkit = getFormToolkit(parent.getDisplay());

		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		section.setText("Spring Insight");
		section.setExpanded(false);

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 5).applyTo(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);

		new GoToSpringLinkWidget(composite, toolkit).createControl();

	}

	protected CloudFoundryServer getCloudFoundryServer() {
		if (server != null) {
			return (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		}
		return null;
	}

}
