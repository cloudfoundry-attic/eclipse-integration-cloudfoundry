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
package org.cloudfoundry.ide.eclipse.internal.server.core.standalone;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.Staging;

public class StagingHandler {

	// Note: these are a temporary definition. They should be referenced
	// directly from the CloudApplication.
	public static final String MODEL_KEY = "model";

	public static final String STACK_KEY = "stack";

	public static final String COMMAND_KEY = "command";

	private final Map<String, String> stagingValues;

	private Staging staging;

	public StagingHandler(Map<String, String> stagingValues) {
		this.stagingValues = stagingValues;
	}

	public Staging getStaging() {
		if (staging == null && stagingValues != null) {
			String framework = stagingValues.get(MODEL_KEY);
			if (framework != null) {
				staging = new Staging(framework);
				staging.setCommand(stagingValues.get(COMMAND_KEY));
				staging.setRuntime(stagingValues.get(STACK_KEY));
			}

		}
		return staging;
	}
}
