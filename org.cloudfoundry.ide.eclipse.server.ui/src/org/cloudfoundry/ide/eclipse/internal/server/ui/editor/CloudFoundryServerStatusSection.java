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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudServerListener;
import org.cloudfoundry.ide.eclipse.internal.server.core.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
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
	
	private Label statusLabel;

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
		section.setText("Server Status");
		section.setExpanded(true);

		composite = toolkit.createComposite(section);
		section.setClient(composite);

		GridLayoutFactory.fillDefaults().numColumns(4).margins(10, 5).applyTo(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);

		nameLabel = toolkit.createLabel(composite, "");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(nameLabel);
		nameLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		
		statusLabel = toolkit.createLabel(composite, "");
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(statusLabel);
		
		connectButton = toolkit.createButton(composite, "Connect", SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(connectButton);
		
		connectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Job job = new Job("Connect server") {
					
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							cfServer.getBehaviour().connect(monitor);
						}
						catch (CoreException e) {
							StatusManager.getManager().handle(new Status(Status.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, "Failed to perform server editor action", e), StatusManager.LOG);
							return Status.CANCEL_STATUS;
						}
						
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		});
		
		disconnectButton = toolkit.createButton(composite, "Disconnect", SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(disconnectButton);
		
		disconnectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Job job = new Job("Connect server") {
					
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							cfServer.getBehaviour().disconnect(monitor);
						}
						catch (CoreException e) {
							StatusManager.getManager().handle(new Status(Status.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, "Failed to perform server editor action", e), StatusManager.LOG);
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
				nameLabel.setText(cfServer.getServer().getName() + ": ");
				
				int s = cfServer.getServer().getServerState();
				String statusString = "Not connected";
				if (s == IServer.STATE_STARTED) {
					statusString = "Connected";
				}
				statusLabel.setText(statusString);
				
				connectButton.setEnabled(s != IServer.STATE_STARTED);
				disconnectButton.setEnabled(s == IServer.STATE_STARTED);
				
				composite.layout(true);
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
