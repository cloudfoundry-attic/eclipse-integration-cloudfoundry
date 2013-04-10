/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

/**
 * Representation of a runtime that should be used when starting an application
 * in a Cloud Foundry server. An example would be "java" or "java7".
 * 
 */
public class ApplicationRuntime {

	private final String runtime;

	private final String displayName;

	public ApplicationRuntime(String runtime, String displayName) {
		this.runtime = runtime;
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getRuntime() {
		return runtime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
		result = prime * result + ((runtime == null) ? 0 : runtime.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		ApplicationRuntime other = (ApplicationRuntime) obj;
		if (displayName == null) {
			if (other.displayName != null) {
				return false;
			}
		}
		else if (!displayName.equals(other.displayName)) {
			return false;
		}
		if (runtime == null) {
			if (other.runtime != null) {
				return false;
			}
		}
		else if (!runtime.equals(other.runtime)) {
			return false;
		}
		return true;
	}

}
