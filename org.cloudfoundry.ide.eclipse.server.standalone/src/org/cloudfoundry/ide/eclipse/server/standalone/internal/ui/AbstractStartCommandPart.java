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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.ui;

import org.cloudfoundry.ide.eclipse.server.standalone.internal.ui.StartCommandPartFactory.IStartCommandPartListener;
import org.eclipse.swt.widgets.Composite;

/**
 * Base part that allows a UI part to be defined to set a Java start command
 * 
 */
public abstract class AbstractStartCommandPart implements StartCommandPart {
	protected final Composite parent;

	protected final IStartCommandPartListener listener;

	private Composite composite;

	protected AbstractStartCommandPart(Composite parent,
			IStartCommandPartListener listener) {
		this.parent = parent;
		this.listener = listener;
	}

	public Composite getComposite() {
		if (composite == null) {
			composite = createComposite();
		}
		return composite;
	}

	abstract protected Composite createComposite();

}