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

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandOption;
import org.eclipse.jface.wizard.Wizard;

public class UnsetOptionsWizard extends Wizard {

	private final List<CommandOption> unsetOptions;

	public UnsetOptionsWizard(List<CommandOption> unsetOptions) {
		this.unsetOptions = unsetOptions;
		setWindowTitle("Set Command Option Values");
	}

	public void addPages() {
		UnsetOptionsWizardPage page = new UnsetOptionsWizardPage();
		addPage(page);
	}

	public List<CommandOption> getCommandOptions() {
		return unsetOptions;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
