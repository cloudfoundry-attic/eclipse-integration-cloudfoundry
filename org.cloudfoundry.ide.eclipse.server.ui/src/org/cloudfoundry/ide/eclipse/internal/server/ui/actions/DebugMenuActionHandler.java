/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugCommandBuilder;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.DebugMenuActionHandler.DebugAction.DebugActionDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.ui.IServerModule;

/**
 * Creates Cloud Foundry debug actions based on a given context. Valid context
 * should include a server module and a cloud foundry server as a bare minimum.
 * 
 */
public class DebugMenuActionHandler extends MenuActionHandler<IServerModule> {

	protected DebugMenuActionHandler() {
		super(IServerModule.class);
	}

	public static final String DEBUG_ACTION_ID = "org.cloudfoundry.ide.eclipse.server.ui.action.debug";

	public static final String CONNECT_TO_DEBUGGER_ACTION_ID = "org.cloudfoundry.ide.eclipse.server.ui.action.connectToDebugger";

	public static final String DEBUG_TOOLTIP_TEXT = "Debug the selected application";

	public static final String CONNECT_TO_DEBUBGGER_TOOTIP_TEXT = "Connect the debugger to the deployed application";

	protected ApplicationAction getApplicationAction(IServerModule serverModule, CloudFoundryServer cloudFoundryServer) {

		IModule[] module = serverModule.getModule();
		if (CloudFoundryProperties.isApplicationRunningInDebugMode.testProperty(module, cloudFoundryServer)
				&& !CloudFoundryProperties.isConnectedToDebugger.testProperty(module, cloudFoundryServer)) {
			return ApplicationAction.CONNECT_TO_DEBUGGER;
		}
		else if (CloudFoundryProperties.isModuleStopped.testProperty(module, cloudFoundryServer)
				&& CloudFoundryProperties.isDebugEnabled.testProperty(module, cloudFoundryServer)) {
			return ApplicationAction.DEBUG;
		}
		return null;
	}

	static class DebugAction extends Action {

		protected final DebugActionDescriptor descriptor;

		public DebugAction(DebugActionDescriptor descriptor) {
			this.descriptor = descriptor;
			setActionValues();
		}

		protected void setActionValues() {
			ActionUIValues values = new ActionUIValues(descriptor.applicationAction);
			setText(values.getName());
			setImageDescriptor(CloudFoundryImages.DEBUG);
			setToolTipText(values.getToolTipText());
			setEnabled(true);
		}

		public void run() {
			final CloudFoundryServer cloudFoundryServer = descriptor.getCloudFoundryServer();
			final ApplicationAction debugAction = descriptor.getApplicationAction();
			Job job = new Job(debugAction.getDisplayName()) {

				protected IStatus run(IProgressMonitor monitor) {

					IModule[] modules = descriptor.getServerModule().getModule();

					switch (debugAction) {
					case DEBUG:
						new DebugCommandBuilder(modules, cloudFoundryServer).getDefaultDeployInDebugModeCommand().run(
								monitor);
						break;
					case CONNECT_TO_DEBUGGER:
						new DebugCommandBuilder(modules, cloudFoundryServer).getDebugCommand(
								ApplicationAction.CONNECT_TO_DEBUGGER, null).run(monitor);
						break;
					}

					return Status.OK_STATUS;
				}
			};

			job.schedule();
		}

		static class ActionUIValues {

			private String actionID;

			private String actionName;

			private String toolTipText;

			public ActionUIValues(ApplicationAction action) {
				setValuesFromApplicationAction(action);
			}

			protected void setValuesFromApplicationAction(ApplicationAction action) {
				switch (action) {
				case DEBUG:
					actionID = DEBUG_ACTION_ID;
					toolTipText = DEBUG_TOOLTIP_TEXT;
					actionName = ApplicationAction.DEBUG.getDisplayName();
					break;
				case CONNECT_TO_DEBUGGER:
					actionID = CONNECT_TO_DEBUGGER_ACTION_ID;
					toolTipText = CONNECT_TO_DEBUBGGER_TOOTIP_TEXT;
					actionName = ApplicationAction.CONNECT_TO_DEBUGGER.getDisplayName();
					break;
				}
			}

			public String getActionID() {
				return actionID;
			}

			public String getToolTipText() {
				return toolTipText;
			}

			public String getName() {
				return actionName;
			}

		}

		static class DebugActionDescriptor {

			private final IServerModule serverModule;

			private final ApplicationAction applicationAction;

			private final CloudFoundryServer cloudFoundryServer;

			public DebugActionDescriptor(IServerModule serverModule, CloudFoundryServer cloudServerFoundry,
					ApplicationAction applicationAction) {

				this.serverModule = serverModule;
				this.applicationAction = applicationAction;
				this.cloudFoundryServer = cloudServerFoundry;

			}

			public IServerModule getServerModule() {
				return serverModule;
			}

			public ApplicationAction getApplicationAction() {
				return applicationAction;
			}

			public CloudFoundryServer getCloudFoundryServer() {
				return cloudFoundryServer;
			}

		}
	}

	@Override
	protected List<IAction> getActionsFromSelection(IServerModule serverModule) {
		CloudFoundryServer cloudFoundryServer = (CloudFoundryServer) serverModule.getServer().loadAdapter(
				CloudFoundryServer.class, null);
		if (cloudFoundryServer == null) {
			return Collections.emptyList();
		}

		ApplicationAction debugAction = getApplicationAction(serverModule, cloudFoundryServer);
		if (debugAction == null) {
			return Collections.emptyList();
		}

		// For now only handle one action per context request.
		DebugActionDescriptor descriptor = new DebugActionDescriptor(serverModule, cloudFoundryServer, debugAction);
		DebugAction menuAction = new DebugAction(descriptor);
		List<IAction> actions = new ArrayList<IAction>();
		actions.add(menuAction);
		return actions;
	}
}
