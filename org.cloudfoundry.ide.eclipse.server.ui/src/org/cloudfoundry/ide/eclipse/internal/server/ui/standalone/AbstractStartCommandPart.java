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
package org.cloudfoundry.ide.eclipse.internal.server.ui.standalone;

import org.cloudfoundry.ide.eclipse.internal.server.ui.standalone.StartCommandPartFactory.IStartCommandPartListener;
import org.eclipse.swt.widgets.Composite;

public abstract class AbstractStartCommandPart implements StartCommandPart {
	protected final Composite parent;

	protected final IStartCommandPartListener listener;

	private Composite composite;

	protected AbstractStartCommandPart(Composite parent, IStartCommandPartListener listener) {
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