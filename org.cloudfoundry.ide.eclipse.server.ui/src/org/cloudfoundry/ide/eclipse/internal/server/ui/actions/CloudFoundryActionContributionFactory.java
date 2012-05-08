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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.menus.IMenuService;

/**
 * Contributes debug context menu actions to the Server view based on valid
 * context selections. This is invoked any time a user right-clicks on the
 * Servers view, and therefore context selection checks should be fast.
 * 
 */
public class CloudFoundryActionContributionFactory extends AbstractMenuContributionFactory {

	private static final MenuActionHandler<?>[] ACTION_HANDLERS = new MenuActionHandler<?>[] {
			new DebugMenuActionHandler(), new ServerMenuActionHandler() };

	public CloudFoundryActionContributionFactory() {
		super("popup:org.eclipse.wst.server.ui.ServersView?before=additions", null);
	}

	@Override
	protected List<IAction> getActions(IMenuService menuService) {

		// Return all context menu actions that are applicable to the given
		// context
		Object context = menuService.getCurrentState();
		List<IAction> actions = new ArrayList<IAction>();
		for (MenuActionHandler<?> actionHandler : ACTION_HANDLERS) {
			List<IAction> handlerActions = actionHandler.getActions(context);
			if (handlerActions != null && !handlerActions.isEmpty()) {
				actions.addAll(handlerActions);
			}
		}
		return actions;
	}

}
