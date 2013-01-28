package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains command definitions with additional information that should not be
 * serialised, like predefined commands per service.
 * 
 */
public class CommandDefinitionsWithPredefinition implements ITunnelServiceCommands {

	private final TunnelServiceCommands original;

	private final PredefinedServiceCommands predefined;

	private List<ServerService> wrapped;

	public CommandDefinitionsWithPredefinition(TunnelServiceCommands commands, PredefinedServiceCommands predefined) {
		this.original = commands;
		this.predefined = predefined;

	}

	public TunnelServiceCommands getSerialisableCommands() {
		List<ServerService> serverServices = getServices();
		List<ServerService> serialisable = new ArrayList<ServerService>();

		for (ServerService service : serverServices) {
			if (service instanceof ServerServiceWithPredefinitions) {
				serialisable.add(((ServerServiceWithPredefinitions) service).getOriginal());
			}
			else {
				serialisable.add(service);
			}
		}
		original.setServices(serialisable);
		return original;
	}

	public List<ServerService> getServices() {
		// Wrap the services with services that have predefined definitions

		if (wrapped == null) {
			List<ServerService> existingServices = original.getServices();

			wrapped = new ArrayList<ServerService>();
			for (ServerService existing : existingServices) {
				wrapped.add(new ServerServiceWithPredefinitions(existing, predefined));
			}
		}

		return wrapped;
	}

	public void setServices(List<ServerService> services) {
		original.setServices(services);
	}

}
