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
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class CloudFoundryConsolePageParticipant implements IConsolePageParticipant {

	private CloseConsoleAction closeAction;

	public void activated() {
		// ignore
	}

	public void deactivated() {
		// ignore
	}

	public void dispose() {
		// ignore
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public void init(IPageBookViewPage page, IConsole console) {
		closeAction = new CloseConsoleAction(console);

		IToolBarManager manager = page.getSite().getActionBars().getToolBarManager();
		manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeAction);
	}

}
