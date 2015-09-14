/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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

package org.cloudfoundry.ide.eclipse.server.ui.internal;

import org.cloudfoundry.ide.eclipse.server.core.internal.log.HttpTracer;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page to enable/disable Cloud Foundry HTTP verbose tracing.
 */
public class CloudFoundryTracePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private boolean isTracingEnabled;

	public CloudFoundryTracePreferencePage() {
		setPreferenceStore(CloudFoundryServerUiPlugin.getDefault().getPreferenceStore());
	}

	public void init(IWorkbench workbench) {
		// Do nothing
	}

	@Override
	protected Control createContents(Composite parent) {

		Composite topComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(topComposite);
		GridDataFactory.fillDefaults().grab(true, true);

		final Button enableTracing = new Button(topComposite, SWT.CHECK);
		GridDataFactory.fillDefaults().grab(false, false);
		enableTracing.setText(Messages.LABEL_ENABLE_TRACING);
		enableTracing.setToolTipText(Messages.TOOLTIP_ENABLE_TRACING);

		isTracingEnabled = HttpTracer.getCurrent().isEnabled();

		enableTracing.setSelection(isTracingEnabled);

		enableTracing.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				isTracingEnabled = enableTracing.getSelection();
			}

		});

		return topComposite;
	}

	@Override
	protected void performApply() {
		HttpTracer.getCurrent().enableTracing(isTracingEnabled);
		super.performApply();
	}

	@Override
	public boolean performOk() {
		HttpTracer.getCurrent().enableTracing(isTracingEnabled);
		return super.performOk();
	}
}
