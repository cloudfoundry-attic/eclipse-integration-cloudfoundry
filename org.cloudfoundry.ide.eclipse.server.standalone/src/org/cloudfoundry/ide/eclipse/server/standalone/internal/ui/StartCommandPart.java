/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import org.cloudfoundry.ide.eclipse.internal.server.ui.UIPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Base part that allows a UI part to be defined to set a Java start command
 * 
 */
public abstract class StartCommandPart extends UIPart {
	private final Composite parent;

	private Control composite;

	protected StartCommandPart(Composite parent) {
		this.parent = parent;
	}

	public Control getComposite() {
		if (composite == null) {
			composite = createPart(parent);
		}
		return composite;
	}

	/**
	 * Tells the part to update the start command from current values of in the
	 * UI control and notify listeners with the revised start command
	 */
	abstract public void updateStartCommand();

}