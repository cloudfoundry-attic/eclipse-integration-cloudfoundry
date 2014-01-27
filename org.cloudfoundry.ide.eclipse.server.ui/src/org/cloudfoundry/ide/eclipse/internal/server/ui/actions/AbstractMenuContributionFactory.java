/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.menus.AbstractContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.services.IServiceLocator;

public abstract class AbstractMenuContributionFactory extends AbstractContributionFactory {

	public AbstractMenuContributionFactory(String location, String namespace) {
		super(location, namespace);
	}

	@Override
	public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
		IMenuService menuService = (IMenuService) serviceLocator.getService(IMenuService.class);
		if (menuService == null) {
			CloudFoundryPlugin
					.logError("Unable to retrieve Eclipse menu service. Cannot add Cloud Foundry context menus.");
			return;
		}

		List<IAction> debugActions = getActions(menuService);
		for (IAction action : debugActions) {
			additions.addContributionItem(new ActionContributionItem(action), new Expression() {
				public EvaluationResult evaluate(IEvaluationContext context) {
					return EvaluationResult.TRUE;
				}

				public void collectExpressionInfo(ExpressionInfo info) {
				}
			});
		}
	}
	
	protected abstract List<IAction> getActions(IMenuService menuService);

}
