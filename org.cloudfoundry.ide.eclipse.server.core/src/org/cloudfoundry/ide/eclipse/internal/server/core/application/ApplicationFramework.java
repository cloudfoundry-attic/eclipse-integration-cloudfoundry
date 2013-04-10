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
 * 
 * Representation of an application framework like Spring, Grails, and Java Web.
 * 
 * 
 */
public class ApplicationFramework {
	private final String framework;

	private final String displayName;

	public ApplicationFramework(String framework, String displayName) {
		this.framework = framework;
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getFramework() {
		return framework;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
		result = prime * result + ((framework == null) ? 0 : framework.hashCode());
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
		ApplicationFramework other = (ApplicationFramework) obj;
		if (displayName == null) {
			if (other.displayName != null) {
				return false;
			}
		}
		else if (!displayName.equals(other.displayName)) {
			return false;
		}
		if (framework == null) {
			if (other.framework != null) {
				return false;
			}
		}
		else if (!framework.equals(other.framework)) {
			return false;
		}
		return true;
	}

}
