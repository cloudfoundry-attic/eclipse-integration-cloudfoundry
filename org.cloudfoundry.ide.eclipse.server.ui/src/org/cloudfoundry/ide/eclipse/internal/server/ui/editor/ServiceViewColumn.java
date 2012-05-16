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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

public enum ServiceViewColumn {
	Name(150), Type(100), Vendor(100), Caldecott(80);
	private int width;

	private ServiceViewColumn(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}
}