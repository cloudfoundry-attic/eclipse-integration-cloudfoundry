/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui.actions;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;

public abstract class MenuActionHandler<T> {

	private Class<T> selectionClass;

	protected MenuActionHandler(Class<T> selectionClass) {
		this.selectionClass = selectionClass;
	}

	public List<IAction> getActions(Object context) {
		T selection = getSelectionFromContext(context);
		if (selection != null) {
			return getActionsFromSelection(selection);
		}
		return Collections.emptyList();
	}

	abstract protected List<IAction> getActionsFromSelection(T selection);

	/**
	 * Returns a list of applicable context menu actions based on the given
	 * context. Always returns a non-null list, although the list may be empty
	 * if no actions are applicable to the given context.
	 * 
	 * @param context to evaluate for the creation of actions. If null, an
	 * attempt will be made to obtain the context directly from the Servers view
	 * @return non-null list of actions corresponding to the given evaluation
	 * context. May be empty if context is invalid.
	 */
	@SuppressWarnings("unchecked")
	protected T getSelectionFromContext(Object context) {
		T selection = null;

		// First check if the context is an evaluation context, and attempt to
		// obtain the server module from the context
		if (context instanceof IEvaluationContext) {

			Object evalContext = ((IEvaluationContext) context).getDefaultVariable();

			if (evalContext instanceof List<?>) {
				List<?> content = (List<?>) evalContext;
				if (!content.isEmpty()) {
					Object obj = content.get(0);
					if (selectionClass.isAssignableFrom(obj.getClass())) {
						selection = (T) obj;
					}
				}
			}
		}

		// Failed to get context selection from context.
		// Try the servers view directly
		if (selection == null) {

			IStructuredSelection strucSelection = CloudUiUtil.getServersViewSelection();
			if (strucSelection != null && !strucSelection.isEmpty()) {
				Object selectObj = strucSelection.getFirstElement();
				if (selectionClass.isAssignableFrom(selectObj.getClass())) {
					selection = (T) selectObj;
				}
			}
		}

		return selection;
	}

}
