/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ServerCredentialsValidationStatics;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * UI Part that sends notifications on changes in the UI Part controls.
 */
public abstract class UIPart {

	private List<IPartChangeListener> listeners = new ArrayList<IPartChangeListener>();

	public void addPartChangeListener(IPartChangeListener listener) {
		if (listener != null && !listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	/**
	 * Notify any listeners that a chance has occurred in the UI part. The
	 * Change event should contain all the information necessary for the
	 * listeners to react accordingly.
	 * 
	 * @param changeEvent
	 */
	protected void notifyChange(PartChangeEvent changeEvent) {
		for (IPartChangeListener listener : listeners) {
			listener.handleChange(changeEvent);
		}
	}

	/**
	 * Notify any listeners that a change has occurred in the UI Part. This
	 * notification is accomplished via a status. E.g., an error status
	 * indicates that some change in the UI part resulted in an error (e.g.
	 * invalid value entered in one of the part's controls). Setting a null
	 * status is equivalent to an OK status.
	 * @param status reflecting change in UI part
	 */
	protected void notifyStatusChange(IStatus status) {
		notifyStatusChange(null, status, ServerCredentialsValidationStatics.EVENT_NONE);
	}

	/**
	 * Notify any listeners that a change has occurred in the UI Part. This
	 * notification is accomplished via a status. E.g., an error status
	 * indicates that some change in the UI part resulted in an error (e.g.
	 * invalid value entered in one of the part's controls). Setting a null
	 * status is equivalent to an OK status. The event type provides additional
	 * information to the listeners on what the change was.
	 * @param status reflecting change in UI part
	 * @param eventType a type associated with the change in the UI part that a
	 * listener may use for additional handling
	 */
	protected void notifyStatusChange(IStatus status, int eventType) {
		notifyStatusChange(null, status, eventType);
	}

	/**
	 * Notify any listeners that a change has occurred in the UI Part. This
	 * notification is accomplished via a status. E.g., an error status
	 * indicates that some change in the UI part resulted in an error (e.g.
	 * invalid value entered in one of the part's controls). Setting a null
	 * status is equivalent to an OK status. The object data may be the result
	 * of a change in the UI part (e.g. a set value)
	 * @param data event change value, e.g., a set value in a control.
	 * @param status reflecting change in UI part
	 */
	protected void notifyStatusChange(Object data, IStatus status) {
		notifyStatusChange(data, status, ServerCredentialsValidationStatics.EVENT_NONE);
	}

	/**
	 * Notify any listeners that a change has occurred in the UI Part. This
	 * notification is accomplished via a status. E.g., an error status
	 * indicates that some change in the UI part resulted in an error (e.g.
	 * invalid value entered in one of the part's controls). Setting a null
	 * status is equivalent to an OK status. The object data may be the result
	 * of a change in the UI part (e.g. a set value)
	 * @param data event change value, e.g., a set value in a control.
	 * @param status reflecting change in UI part
	 * @param eventType a type associated with the change in the UI part that a
	 * listener may use for additional handling
	 */
	protected void notifyStatusChange(Object data, IStatus status, int eventType) {

		if (status == null) {
			status = Status.OK_STATUS;
		}

		notifyChange(new PartChangeEvent(data, status, this, eventType));
	}

	abstract public Control createPart(Composite parent);

}
