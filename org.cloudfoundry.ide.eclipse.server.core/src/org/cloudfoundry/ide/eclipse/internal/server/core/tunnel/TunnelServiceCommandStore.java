/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Typically there should only be one store instance available per session.
 * 
 */
public class TunnelServiceCommandStore {

	public static final String SERVICE_COMMANDS_PREF = CloudFoundryPlugin.PLUGIN_ID + ".service.tunnel.commands";

	private final static ObjectMapper mapper = new ObjectMapper();

	private TunnelServiceCommands cachedCommands;

	private final PredefinedServiceCommands predefinedCommands;

	public TunnelServiceCommandStore(PredefinedServiceCommands predefinedCommands) {
		this.predefinedCommands = predefinedCommands;
	}

	public synchronized ITunnelServiceCommands getTunnelServiceCommands() throws CoreException {
		loadCommandsFromStore();

		// Cached commands are the serialisable version. Return a subtype with
		// additional information that is not persisted,
		// like pre-defined commands

		return cachedCommands != null && predefinedCommands != null ? new CommandDefinitionsWithPredefinition(
				cachedCommands, predefinedCommands) : cachedCommands;
	}

	protected void loadCommandsFromStore() throws CoreException {
		String storedValue = CloudFoundryPlugin.getDefault().getPreferences().get(SERVICE_COMMANDS_PREF, null);
		cachedCommands = parseAndUpdateTunnelServiceCommands(storedValue);
	}

	protected TunnelServiceCommands parseAndUpdateTunnelServiceCommands(String json) throws CoreException {
		TunnelServiceCommands commands = null;
		if (json != null) {
			try {
				commands = mapper.readValue(json, TunnelServiceCommands.class);
			}
			catch (IOException e) {

				CloudFoundryPlugin.logError("Error while reading Java Map from JSON response: ", e);
			}
		}

		if (commands == null) {
			// initialise commands for the first time
			commands = new TunnelServiceCommands();

			// Set a default terminal for the command
			CommandTerminal defaultTerminal = CommandTerminal.getDefaultOSTerminal();
			if (defaultTerminal != null) {
				commands.setDefaultTerminal(defaultTerminal);
			}

			ServiceCommandManager manager = new ServiceCommandManager(commands);
			manager.addServices(ServiceInfo.values());

			if (predefinedCommands != null) {
				manager.addPredefinedCommands(predefinedCommands);
			}

		}

		return commands;
	}

	public synchronized String storeServerServiceCommands(ITunnelServiceCommands commands) throws CoreException {

		String serialisedCommands = null;

		// Resolve the commands that need to be persisted, in case they are part
		// of a wrapper
		// that has information that should not be serialised
		if (commands instanceof CommandDefinitionsWithPredefinition) {
			cachedCommands = ((CommandDefinitionsWithPredefinition) commands).getSerialisableCommands();
		}
		else if (commands instanceof TunnelServiceCommands) {
			cachedCommands = (TunnelServiceCommands) commands;
		}
		if (cachedCommands != null) {
			if (mapper.canSerialize(cachedCommands.getClass())) {
				try {
					serialisedCommands = mapper.writeValueAsString(cachedCommands);
				}
				catch (IOException e) {
					throw new CoreException(CloudFoundryPlugin.getErrorStatus(
							"Error while serialising Java Map from JSON response: ", e));
				}
			}
			else {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus("Value of type "
						+ cachedCommands.getClass().getName() + " can not be serialized to JSON."));
			}
		}

		if (serialisedCommands != null) {
			IEclipsePreferences prefs = CloudFoundryPlugin.getDefault().getPreferences();
			prefs.put(SERVICE_COMMANDS_PREF, serialisedCommands);
			try {
				prefs.flush();
			}
			catch (BackingStoreException e) {
				CloudFoundryPlugin.logError(e);
			}

		}

		return serialisedCommands;

	}

	public synchronized List<ServiceCommand> getCommandsForService(CloudService cloudService, boolean forceLoad)
			throws CoreException {
		List<ServiceCommand> commands = new ArrayList<ServiceCommand>();
		if (forceLoad) {
			loadCommandsFromStore();
		}
		if (cachedCommands != null && cachedCommands.getServices() != null) {
			String vendor = CloudUtil.getServiceVendor(cloudService);
			for (ServerService service : cachedCommands.getServices()) {
				if (service.getServiceInfo().getVendor().equals(vendor)) {
					commands = service.getCommands();
					break;
				}
			}
		}
		return commands;
	}

	public static TunnelServiceCommandStore getCurrentStore() {
		return CloudFoundryPlugin.getDefault().getTunnelCommandsStore();
	}

}
