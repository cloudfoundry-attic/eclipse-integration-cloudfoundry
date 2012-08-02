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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;

/**
 * UI part that creates the controls for a particular start command type.
 * Listener can be registered that handles changes to the start command value
 * based on UI control changes.
 */
public interface StartCommandPart {
	/**
	 * Gets the Composite instance that will later be used for possible updates.
	 * A new composite should not be created every time this method is called
	 * 
	 * 
	 * @return
	 */
	public Composite getComposite();

	public void update(IProgressMonitor monitor);

}