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

import org.cloudfoundry.client.lib.CloudInfo;

/**
 * 
 * Representation of a runtime type that also contains a label that may be used
 * by features like UI. The runtime type name should match names found in
 * CloudInfo for a particular server.
 * @see CloudInfo.Runtime
 */
public enum StandaloneRuntimeType {

	java("Java"), java7("Java 7"), node("Node"), node06("Node 06"), ruby18("Ruby 18"), ruby19("Ruby 19");
	private String label;

	private StandaloneRuntimeType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public StandaloneRuntimeType getStandaloneRuntimeType(String label) {
		for (StandaloneRuntimeType type : values()) {
			if (type.getLabel().equals(label)) {
				return type;
			}
		}
		return null;
	}
}
