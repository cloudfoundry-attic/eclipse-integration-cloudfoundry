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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.eclipse.core.runtime.IStatus;

/**
 * Used for UI parts that require listeners to be notified when control values
 * and state change.
 */
public interface IPartChangeListener {

	public void handleChange(PartChangeEvent event);

	public static class PartChangeEvent {

		private final IStatus status;

		private final Object data;

		public PartChangeEvent(Object data, IStatus status) {
			this.data = data;
			this.status = status;
		}

		public IStatus getStatus() {
			return status;
		}

		public Object getData() {
			return data;
		}

	}

}
