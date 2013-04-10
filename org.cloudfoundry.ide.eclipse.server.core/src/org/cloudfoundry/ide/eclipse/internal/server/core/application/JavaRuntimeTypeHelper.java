/*******************************************************************************
 * Copyright (c) 2012, 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;

public class JavaRuntimeTypeHelper {

	public static final String JAVA_6_LABEL = "Java 6";

	public static final String JAVA_7_LABEL = "Java 7";

	private final CloudFoundryServer cloudServer;

	public JavaRuntimeTypeHelper(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	/**
	 * Always returns a non-null list. Java 6 is the default runtime
	 * @return
	 */
	public List<ApplicationRuntime> getRuntimeTypes() {

		List<CloudInfo.Runtime> actualTypes = cloudServer.getBehaviour().getRuntimes();
		List<ApplicationRuntime> appTypes = new ArrayList<ApplicationRuntime>();

		if (actualTypes != null && !actualTypes.isEmpty()) {
			for (CloudInfo.Runtime actualRuntime : actualTypes) {
				String runtimeName = actualRuntime.getName();
				String displayName = runtimeName;
				if ("java".equals(runtimeName)) {
					displayName = JAVA_6_LABEL;
					appTypes.add(new ApplicationRuntime(runtimeName, displayName));
				}
				else if ("java7".equals(runtimeName)) {
					displayName = JAVA_7_LABEL;
					appTypes.add(new ApplicationRuntime(runtimeName, displayName));
				}

			}
		}
		else {
			appTypes.add(new ApplicationRuntime("java", JAVA_6_LABEL));
		}

		return appTypes;
	}

}
