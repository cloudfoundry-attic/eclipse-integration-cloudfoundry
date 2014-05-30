/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.ide.eclipse.server.core.internal.ValidationEvents;
import org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.WizardStatusHandler;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableContext;

/**
 * A validation event handler around a {@link ServerValidator} that acts both as
 * a listener for incoming validation requests as well as a notifier of
 * validation completion.
 * <p/>
 * As a listener, the validation handler will get notified when there are
 * changes to account values (e.g. username changed or different cloud space
 * selected), or when a user has explicitly requested a server authorisation
 * (e.g., a user has pressed a "Validate" UI control), and if necessary, perform
 * an account validation. Validations may be both local or can be server
 * authorisations, depending on the type of event received by this wrapper.
 * <p/>
 * As a notifier, once a validation is completed, the handler will notify
 * registered listeners that validation has been performed.
 * <p/>
 * The handler also keeps tracks of errors based on event types. This allows
 * multiple notifiers, like different UI parts or other components, that fire
 * events, to log errors.
 * <p/>
 * IMPORTANT NOTE: errors are tracked by event type. Therefore in order to CLEAR
 * an error, an {@link IStatus#OK} event with the SAME event type must be fired,
 * whether by the same notifier that fired the initial error, or any other
 * notifier.
 * <p/>
 * For example, a credentials UI part fires an event type VALUE_FILLED, with an
 * {@link IStatus#ERROR}, due to a missing value like a password in a UI
 * control. Once this error has been corrected (password filled), the same
 * VALUE_FILLED event must be fired with an {@link IStatus#OK} in order to clear
 * that error. This event need not be fired by the same UI part that generated
 * the initial error, it can be fired by some other component (maybe one that
 * reads a stored password value), as long as the event type REMAINS the same.
 * <p/>
 * Otherwise if an OK status is fired with a different event type, or example,
 * PASSWORD_FILLED, the error will NOT be cleared.
 * <p/>
 * Therefore event types in {@link PartChangeEvent} should NOT represent an
 * error condition (e.g missing values). Rather they should indicate:
 * <p/>
 * 1. An action that was performed, like values being entered.
 * <p/>
 * 2. A validation request (e.g. validate locally or validate against a server).
 * <p/>
 * To indicate an error status associated with an event type, use the
 * {@link IStatus} value in the {@link PartChangeEvent}
 */
public class ValidationEventHandler implements IPartChangeListener {

	private ServerValidator validator;

	private Map<Integer, PartChangeEvent> trackedStatus = new HashMap<Integer, PartChangeEvent>();

	private Set<IPartChangeListener> listeners = new LinkedHashSet<IPartChangeListener>();

	private Set<WizardStatusHandler> statusHandlers = new LinkedHashSet<WizardStatusHandler>();

	public ValidationEventHandler(ServerValidator validator) {
		this.validator = validator;
	}

	public ValidationEventHandler() {

	}

	public synchronized void updateValidator(ServerValidator validator) {
		this.validator = validator;
	}

	/**
	 * 
	 * @param handler wizard status handler that gets notified when an event
	 * status is received. This is different than a validation listener, in the
	 * sense that the status handler gets notified only when a status is meant
	 * to be displayed in a wizard, regardless of the event type, whereas a
	 * validation listener only gets notified if a validation process completed.
	 */
	public synchronized void addStatusHandler(WizardStatusHandler handler) {
		if (handler != null) {
			statusHandlers.add(handler);
		}
	}

	/*
	 * 
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.server.ui.internal.IPartChangeListener#
	 * handleChange
	 * (org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent)
	 */
	public synchronized void handleChange(PartChangeEvent event) {
		if (validator == null || event == null) {
			return;
		}

		// See if the incoming event triggers a validation. If so the validation
		// will
		// generate its own event
		PartChangeEvent validationEvent = getValidationEvent(event);

		if (validationEvent != null) {
			fireValidationEvent(validationEvent);
			event = validationEvent;
		}

		// Track the event to see if it is an error, or clears an existing error
		trackEvent(event);

		// Notify status handlers of the event
		handleStatus(event);
	}

	public synchronized void validate(IRunnableContext context) {
		handleChange(new PartChangeEvent(context, Status.OK_STATUS, null, ValidationEvents.SERVER_AUTHORISATION));
	}

	/**
	 * 
	 * @param listener to be notified after a validation event has been
	 * completed.
	 */
	public synchronized void addValidationListener(IPartChangeListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	public synchronized void removeValidationListener(IPartChangeListener listener) {
		listeners.remove(listener);
	}

	public synchronized boolean isOK() {
		return getNextNonOKEvent() == null;
	}

	/**
	 * 
	 * @return the next non {@link IStatus#OK} event that is tracked, or null if
	 * there are no remaining non-OK events (i.e all errors have been cleared)
	 */
	public synchronized IStatus getNextNonOKEvent() {
		PartChangeEvent event = getNextTrackedEvent();
		return event != null ? event.getStatus() : null;
	}

	protected void fireValidationEvent(PartChangeEvent event) {
		if (event == null) {
			return;
		}

		Object source = event.getSource() != null ? event.getSource().getSource() : null;
		for (IPartChangeListener listener : listeners) {
			// Do not notify the same source that fired the event
			if (source != listener) {
				listener.handleChange(event);
			}
		}
	}

	protected PartChangeEvent getNextTrackedEvent() {
		for (PartChangeEvent event : trackedStatus.values()) {
			return event;
		}
		return null;
	}

	protected ValidationStatus validate(IRunnableContext context, int eventType) {
		ValidationStatus status = null;
		switch (eventType) {

		// Any local credential changes (e.g. changes in username or password in
		// the UI), should only result in credentials validation, but not cloud
		// space
		// validation, as the latter is only performed after an explicit server
		// validation event is received
		case ValidationEvents.CREDENTIALS_FILLED:
			status = validator.validate(false, false, context);
			break;
		// Indicates that a request was made to explicitly validate credentials
		// against the server
		case ValidationEvents.SERVER_AUTHORISATION:
			status = validator.validate(true, true, context);
			break;
		// Any general validation event should only be a local validation.
		// Remove
		// server authorisations are handled separately above.
		case ValidationEvents.VALIDATION:
			status = validator.validate(false, true, context);
			break;
		}
		return status;

	}

	protected void handleStatus(PartChangeEvent event) {
		if (event == null || event.getStatus() == null || event.getStatus().isOK()) {
			PartChangeEvent errorEvent = getNextTrackedEvent();
			// Be sure to check for null error event as to not overwrite the
			// incoming OK
			// status event.
			if (errorEvent != null) {
				event = errorEvent;
			}
		}
		for (WizardStatusHandler handler : statusHandlers) {
			handler.handleChange(event);
		}
	}

	/**
	 * Examines the given event to determine if validation should occur. If so,
	 * it will generate a separate Validation event after the validation has
	 * been completed.
	 * <p/>
	 * Returns null if the incoming event does NOT trigger a validation
	 * operation.
	 * <p/>
	 * Validation can only occur if the incoming event is {@link IStatus#OK}.
	 * <p/>
	 * Otherwise, if there is an issue indicated by the incoming event status,
	 * that should be resolved first before validation can occur (e.g. missing
	 * password, invalid server URL, invalid space selection, etc..)
	 * @param event
	 * @return Validation event IFF the incoming event triggers a validation
	 * operation. Otherwise, return null. It does NOT return the original event.
	 */
	protected PartChangeEvent getValidationEvent(PartChangeEvent event) {

		// Do not validate if the incoming event is NOT OK.
		if (event.getStatus() == null || !event.getStatus().isOK()) {
			return null;
		}
		int eventType = event.getType();

		IRunnableContext context = event.getData() instanceof IRunnableContext ? (IRunnableContext) event.getData()
				: null;
		ValidationStatus validationStatus = validate(context, eventType);
		PartChangeEvent validationEvent = null;
		if (validationStatus instanceof IAdaptable) {
			validationEvent = (PartChangeEvent) ((IAdaptable) validationStatus).getAdapter(PartChangeEvent.class);
		}
		else {
			validationEvent = new PartChangeEvent(null, validationStatus != null ? validationStatus.getStatus() : null,
					new EventSource<ValidationEventHandler>(this), ValidationEvents.VALIDATION);
		}
		return validationEvent;
	}

	protected void trackEvent(PartChangeEvent event) {
		// Check the status and determine if it is an error
		IStatus status = event.getStatus();

		// Process the status to determine if it is an error. If so, keep track
		// of it. Errors can only
		// be "cleared" by the same event source that generated the error.
		if (status != null) {
			// Only one error per event type is logged.

			if (!status.isOK()) {
				trackedStatus.put(event.getType(), event);
			}
			else {
				trackedStatus.remove(event.getType());
			}
		}
	}
}
