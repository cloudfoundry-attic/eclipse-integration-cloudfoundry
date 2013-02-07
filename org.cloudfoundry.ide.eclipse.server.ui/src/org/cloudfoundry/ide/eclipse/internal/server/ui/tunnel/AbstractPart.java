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
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener.PartChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public abstract class AbstractPart {

	private IPartChangeListener listener;

	public void addPartChangeListener(IPartChangeListener listener) {
		this.listener = listener;
	}

	protected void notifyChange(IPartChangeListener.PartChangeEvent changeEvent) {
		if (listener != null) {
			listener.handleChange(changeEvent);
		}
	}

	/**
	 * Setting a null status is equivalent to an OK status. Setting an error
	 * status without a message may allow listeners to react to the error (e.g.
	 * disabling controls like a "Finish" or "OK" button) but not display an
	 * error message. To display an error message, a non-null error message is
	 * required.
	 * @param status
	 */
	protected void setStatus(IStatus status) {

		if (status == null) {
			status = Status.OK_STATUS;
		}

		notifyChange(new PartChangeEvent(null, status));
	}
	
	/**
	 * Returns the current shell or null.
	 * @return
	 */
	protected Shell getShell() {
		Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		return shell;
	}
	
	abstract public Control createPart(Composite parent);

}
