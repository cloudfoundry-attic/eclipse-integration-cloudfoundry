/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

public enum StdContentType implements IContentType {

	STD_OUT("local_std_out"), STD_ERROR("local_std_error");

	private final String id;

	private StdContentType(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
