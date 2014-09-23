/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.UIPart;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Prompt user to set both command option and environment value variables e.g.
 * if an environment variable is defined as HOST=${host}, this will prompt the
 * user for a value for "host"
 */
public class SetValueVariablesPart extends UIPart {

	private final Map<String, String> optionsValueVariables;

	private final Map<String, String> envVarsValueVariables;

	public SetValueVariablesPart(Map<String, String> variableToValue, Map<String, String> envVarsValueVariables) {
		this.optionsValueVariables = variableToValue;
		this.envVarsValueVariables = envVarsValueVariables;
	}

	public Composite createPart(Composite parent) {

		Composite generalArea = new Composite(parent, SWT.NONE | SWT.SCROLL_LINE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(generalArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(generalArea);


		boolean hasOptionsToSet = createValueInputArea(optionsValueVariables, generalArea, Messages.SetValueVariablesPart_TEXT_COMMAND_OPT);
		boolean hasEnvironmentVariablesToSet = createValueInputArea(envVarsValueVariables, generalArea, Messages.SetValueVariablesPart_TEXT_ENV_VAR);

		if (!hasOptionsToSet && !hasEnvironmentVariablesToSet) {
			Label serverLabel = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, false).span(2, 0).applyTo(serverLabel);
			serverLabel.setText(Messages.SetValueVariablesPart_LABEL_NO_COMMAND_OR_ENVAR);
		}

		validate(false);
		return generalArea;

	}

	protected boolean createValueInputArea(Map<String, String> valueVariableMap, Composite parent, String labelName) {

		if (valueVariableMap == null) {
			return false;
		}

		List<String> unsetValues = new ArrayList<String>();

		for (Entry<String, String> entry : valueVariableMap.entrySet()) {
			// Only create variable UI for those that do NOT have set values
			if (entry.getValue() == null && entry.getKey() != null) {
				unsetValues.add(entry.getKey());
			}
		}

		if (unsetValues.isEmpty()) {
			return false;
		}
		else {
			Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, false).span(2, 0).applyTo(label);
			label.setText(labelName);
			for (String variable : unsetValues) {
				createValueVariableControl(variable, parent, valueVariableMap);
			}
			return true;
		}
	}

	protected void createValueVariableControl(final String variable, Composite parent,
			final Map<String, String> valueVariables) {
		Label serverLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(serverLabel);
		serverLabel.setText(variable + ": "); //$NON-NLS-1$

		final Text text = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);

		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				valueVariables.put(variable, text.getText());
				validate(true);
			}
		});
	}

	protected IStatus validate(boolean showError) {
		String missingValueVariable = null;
		IStatus status = Status.OK_STATUS;

		if (optionsValueVariables != null) {
			for (Entry<String, String> entry : optionsValueVariables.entrySet()) {
				if (entry.getValue() == null || ValueValidationUtil.isEmpty(entry.getValue())) {
					missingValueVariable = entry.getKey();
					break;
				}
			}
		}
		
		if (missingValueVariable == null && envVarsValueVariables != null) {
			for (Entry<String, String> entry : envVarsValueVariables.entrySet()) {
				if (entry.getValue() == null || ValueValidationUtil.isEmpty(entry.getValue())) {
					missingValueVariable = entry.getKey();
					break;
				}
			}
		}

		if (missingValueVariable != null) {
			status = CloudFoundryPlugin.getErrorStatus(showError ? NLS.bind(missingValueVariable, Messages.SetValueVariablesPart_ERROR_VALIDATE) : ""); //$NON-NLS-1$ 
		}

		notifyStatusChange(status);
		return status;
	}

}
