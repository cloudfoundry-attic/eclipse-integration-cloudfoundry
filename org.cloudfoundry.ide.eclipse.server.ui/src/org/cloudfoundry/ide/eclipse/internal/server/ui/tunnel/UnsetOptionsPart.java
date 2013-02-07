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

import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
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

/**
 * Prompt user for unset application options.
 */
public class UnsetOptionsPart extends AbstractPart {

	private final Map<String, String> variableToValue;

	public UnsetOptionsPart(Map<String, String> variableToValue) {
		this.variableToValue = variableToValue;
	}

	public Composite createPart(Composite parent) {

		Composite generalArea = new Composite(parent, SWT.NONE | SWT.SCROLL_LINE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(generalArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(generalArea);

		boolean error = true;

		if (variableToValue != null && !variableToValue.isEmpty()) {
			for (Entry<String, String> entry : variableToValue.entrySet()) {
				// Only create variable UI for those that do NOT have set values
				if (entry.getValue() == null && entry.getKey() != null) {
					createOptionLabel(entry.getKey(), generalArea);

					// found at least one option to set
					error = false;
				}
			}
		}

		if (error) {
			Label serverLabel = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, false).span(2, 0).applyTo(serverLabel);
			serverLabel.setText("No options found that need to be set");
		}
		
		validate(false);
		return generalArea;

	}

	protected void createOptionLabel(final String variable, Composite parent) {
		Label serverLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(serverLabel);
		serverLabel.setText(variable + ": ");

		final Text text = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);

		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				variableToValue.put(variable, text.getText());
				validate(true);
			}
		});
	}

	protected IStatus validate(boolean showError) {
		String missingValueVariable = null;
		IStatus status = Status.OK_STATUS;

		if (variableToValue != null) {
			for (Entry<String, String> entry : variableToValue.entrySet()) {
				if (entry.getValue() == null || ValueValidationUtil.isEmpty(entry.getValue())) {
					missingValueVariable = entry.getKey();
					break;
				}
			}
		}

		if (missingValueVariable != null) {
			status = CloudFoundryPlugin.getErrorStatus(showError ? missingValueVariable + " requires a value." : "");
		}

		notifyChange(new PartChangeEvent(null, status));
		return status;
	}

}
