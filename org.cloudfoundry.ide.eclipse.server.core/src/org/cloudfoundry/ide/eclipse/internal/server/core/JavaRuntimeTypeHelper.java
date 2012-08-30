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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudInfo;

public class JavaRuntimeTypeHelper {

	private final CloudFoundryServer cloudServer;

	public JavaRuntimeTypeHelper(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	/**
	 * Returns the Java runtimes supported by the given cloud server based on
	 * the local Java runtime definitions. If no Java runtimes are found,
	 * returns empty list
	 * @param actualRuntimes
	 * @return Java runtimes supported by cloud server, or empty list. Never
	 * null.
	 */

	/**
	 * Always returns a non-null list. May be empty
	 * @return
	 */
	public List<RuntimeType> getRuntimeTypes() {

		List<CloudInfo.Runtime> actualTypes = cloudServer.getBehaviour().getRuntimes();

		if (actualTypes == null) {
			return Collections.emptyList();
		}

		List<RuntimeType> appTypes = new ArrayList<RuntimeType>();
		
		Set<String> runtimeIds = new HashSet<String>();
		for (CloudInfo.Runtime actualRuntime : actualTypes) {
			runtimeIds.add(actualRuntime.getName());
		}
		
		RuntimeType[] expectedTypes = { RuntimeType.java, RuntimeType.java7 };


		// Check whether the expected types still are found in the server
		// runtime list. Only show runtime types that
		// match those that are actually supported in the server
		for (RuntimeType expectedType : expectedTypes) {
			if (runtimeIds.contains(expectedType.name())) {
				appTypes.add(expectedType);
			}
		}

		return appTypes;
	}

}
