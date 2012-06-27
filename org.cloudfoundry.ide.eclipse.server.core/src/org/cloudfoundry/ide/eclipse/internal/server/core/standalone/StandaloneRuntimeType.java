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

public enum StandaloneRuntimeType {

	Java("java"), Node("node"), Node06("node06"), Ruby18("ruby18"), Ruby19("ruby19");
	private String id;

	private StandaloneRuntimeType(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
