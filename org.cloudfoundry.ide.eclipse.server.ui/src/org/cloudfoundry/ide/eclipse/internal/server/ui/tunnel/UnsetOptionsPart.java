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
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CommandOption;
import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener.PartChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class UnsetOptionsPart extends AbstractPart {

	private final List<CommandOption> unsetOptions;

	public UnsetOptionsPart(List<CommandOption> unsetOptions) {
		this.unsetOptions = unsetOptions;
	}

	public Composite createControl(Composite parent) {

		Composite generalArea = new Composite(parent, SWT.NONE | SWT.SCROLL_LINE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(generalArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(generalArea);

		if (unsetOptions == null || unsetOptions.isEmpty()) {
			Label serverLabel = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, false).span(2, 0).applyTo(serverLabel);
			serverLabel.setText("No options found that need to be set");

		}
		else {
			for (CommandOption option : unsetOptions) {
				createOptionLabel(option, generalArea);
			}
		}
		return generalArea;

	}

	protected void createOptionLabel(final CommandOption option, Composite parent) {
		Label serverLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(serverLabel);
		serverLabel.setText(option.getOption() + ": ");

		final Text text = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);

		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				option.setValue(text.getText());
				validate();
			}
		});
	}

	protected IStatus validate() {
		CommandOption unsetOption = null;
		IStatus status = Status.OK_STATUS;

		if (unsetOptions != null) {
			for (CommandOption option : unsetOptions) {
				if (!CommandOption.isOptionValueSet(option)) {
					unsetOption = option;
					break;
				}
			}
		}

		if (unsetOption != null) {
			status = CloudFoundryPlugin.getErrorStatus(unsetOption.getOption() + " requires a value.");
		}

		notifyChange(new PartChangeEvent(null, status));
		return status;
	}

}
