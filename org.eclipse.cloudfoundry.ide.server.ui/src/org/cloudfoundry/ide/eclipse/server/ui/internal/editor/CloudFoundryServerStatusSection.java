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
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerListener;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;



/** 
 * @author Terry Denney
 */
public class CloudFoundryServerStatusSection extends ServerEditorSection implements CloudServerListener, IServerListener {

	private CloudFoundryServer cfServer;
	
	private Text statusLabel;

	private Button connectButton;

	private Button disconnectButton;

	private Label nameLabel;

	private Composite composite;

	@Override
	public void createSection(Composite parent) {
		super.createSection(parent);

		FormToolkit toolkit = getFormToolkit(parent.getDisplay());

		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		section.setText(Messages.CloudFoundryServerStatusSection_TEXT_SERV_STAT);
		section.setExpanded(true);

		composite = toolkit.createComposite(section);
		section.setClient(composite);

		GridLayoutFactory.fillDefaults().numColumns(4).margins(10, 5).applyTo(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);

		nameLabel = toolkit.createLabel(composite, ""); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(nameLabel);
		nameLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		
		// Temporary switch for the border style to "no border", so this is drawn properly
		int borderStyleBackup = toolkit.getBorderStyle();
		try {
			toolkit.setBorderStyle(SWT.NULL);
			statusLabel = toolkit.createText(composite, "", SWT.READ_ONLY); //$NON-NLS-1$
		} finally {
			// Make sure under every circumstance, the previous border is restored
			toolkit.setBorderStyle(borderStyleBackup);
		}
		
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(statusLabel);
		
		connectButton = toolkit.createButton(composite, Messages.CloudFoundryServerStatusSection_TEXT_CONN_BUTTON, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(connectButton);
		
		connectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Job job = new Job(Messages.CloudFoundryServerStatusSection_JOB_CONN_SERVER) {
					
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							cfServer.getBehaviour().connect(monitor);
						}
						catch (CoreException e) {
							StatusManager.getManager().handle(new Status(Status.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, "", e), StatusManager.LOG); //$NON-NLS-1$
							return Status.CANCEL_STATUS;
						}
						
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		});
		
		disconnectButton = toolkit.createButton(composite, Messages.CloudFoundryServerStatusSection_TEXT_DISCONN_BUTTON, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(disconnectButton);
		
		disconnectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Job job = new Job(Messages.CloudFoundryServerStatusSection_JOB_CONN_SERVER) {
					
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							cfServer.getBehaviour().disconnect(monitor);
						}
						catch (CoreException e) {
							StatusManager.getManager().handle(new Status(Status.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, "", e), StatusManager.LOG); //$NON-NLS-1$
							return Status.CANCEL_STATUS;
						}
						
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		});
		
		update();
		
		ServerEventHandler.getDefault().addServerListener(this);
		cfServer.getServer().addServerListener(this);
	}
	
	private void update() {
		if (cfServer == null) {
			return;
		}
		
		Display.getDefault().asyncExec(new Runnable() {
			
			public void run() {
				
				if (composite != null && !composite.isDisposed()) {
					nameLabel.setText(cfServer.getServer().getName() + ": "); //$NON-NLS-1$
					
					int s = cfServer.getServer().getServerState();
					String statusString = Messages.CloudFoundryServerStatusSection_TEXT_NOT_CONNECTED;
					if (s == IServer.STATE_STARTED) {
						statusString = Messages.CloudFoundryServerStatusSection_TEXT_CONNECTED;
					}
					statusLabel.setText(statusString);
					
					connectButton.setEnabled(s != IServer.STATE_STARTED);
					disconnectButton.setEnabled(s == IServer.STATE_STARTED);
					
					composite.layout(true);
				}
			}
		});
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		if (server != null) {
			cfServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		}
	}

	public void serverChanged(CloudServerEvent event) {
		update();
	}
	
	public void serverChanged(ServerEvent event) {
		update();
	}

	@Override
	public void dispose() {
		cfServer.getServer().removeServerListener(this);
		ServerEventHandler.getDefault().removeServerListener(this);
	}

}
