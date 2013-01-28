/*******************************************************************************
 * Copyright (c) 2012 - 2013 VMware, Inc.
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

import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServerService;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.ServiceCommand;

public class EditCommandDisplayPart extends AddCommandDisplayPart {

	private final String originalDisplayName;

	public EditCommandDisplayPart(ServerService service, ServiceCommand serviceCommand) {
		super(service, serviceCommand);
		originalDisplayName = serviceCommand.getExternalApplication().getDisplayName();
	}

	protected String getValidationMessage() {
		String message = null;

		/*
		 * Check that if editing the display name, the original display name is
		 * NOT counted when checking if another command already exists with the
		 * same display name. Display names must be unique for each command.
		 */
		if (ValueValidationUtil.isEmpty(locationVal)) {
			message = "No command executable location specified.";
		}
		else if (ValueValidationUtil.isEmpty(displayNameVal)) {
			message = "No command display name specified.";
		}
		else {
			List<ServiceCommand> existingCommands = getService().getCommands();
			if (existingCommands != null) {
				for (ServiceCommand command : existingCommands) {
					String otherCommandName = command.getExternalApplication().getDisplayName();
					if (!otherCommandName.equals(originalDisplayName) && otherCommandName.equals(displayNameVal)) {
						message = "Another command with the same display name already exists. Please select another display name.";
						break;
					}
				}
			}
		}

		return message;
	}

}
