/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import java.util.List;

import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.wizard.Wizard;

/**
 * 
 */
public class SelectMainTypeWizard extends Wizard {

	private final String serverID;

	private final List<IType> mainTypes;

	private SelectMainTypeWizardPage page;

	public SelectMainTypeWizard(String serverID, List<IType> mainTypes) {
		this.serverID = serverID;
		this.mainTypes = mainTypes;
	}

	@Override
	public void addPages() {
		page = new SelectMainTypeWizardPage(mainTypes,
				CloudFoundryImages.getWizardBanner(serverID));
		addPage(page);
	}

	public boolean performFinish() {
		return page != null && page.getSelectedMainType() != null;
	}

	public IType getSelectedMainType() {
		return page != null ? page.getSelectedMainType() : null;
	}
}
