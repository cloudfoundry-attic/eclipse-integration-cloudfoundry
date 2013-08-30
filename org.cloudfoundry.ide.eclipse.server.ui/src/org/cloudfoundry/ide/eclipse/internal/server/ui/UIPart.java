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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * UI Part that sends notifications on changes in the UI Part controls.
 */
public abstract class UIPart {

	private IPartChangeListener listener;

	public void addPartChangeListener(IPartChangeListener listener) {
		this.listener = listener;
	}

	protected void notifyChange(PartChangeEvent changeEvent) {
		if (listener != null) {
			listener.handleChange(changeEvent);
		}
	}

	protected IPartChangeListener getListener() {
		return listener;
	}

	/**
	 * Setting a null status is equivalent to an OK status. Setting an error
	 * status without a message may allow listeners to react to the error (e.g.
	 * disabling controls like a "Finish" or "OK" button) but not display an
	 * error message. To display an error message, a non-null error message is
	 * required.
	 * @param status
	 */
	protected void notifyStatusChange(IStatus status) {
		notifyStatusChange(null, status);
	}

	/**
	 * Setting a null status is equivalent to an OK status. Setting an error
	 * status without a message may allow listeners to react to the error (e.g.
	 * disabling controls like a "Finish" or "OK" button) but not display an
	 * error message. To display an error message, a non-null error message is
	 * required.
	 * @param status
	 */
	protected void notifyStatusChange(Object data, IStatus status) {

		if (status == null) {
			status = Status.OK_STATUS;
		}

		notifyChange(new PartChangeEvent(data, status, this));
	}

	abstract public Control createPart(Composite parent);

}
