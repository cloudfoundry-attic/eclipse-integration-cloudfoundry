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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider for ApplicationsMasterPart
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class ApplicationsMasterPartContentProvider implements ITreeContentProvider {

	private Object[] elements;

	public ApplicationsMasterPartContentProvider() {
	}

	public void dispose() {
	}

	public Object[] getChildren(Object parentElement) {
		return null;
	}

	public Object[] getElements(Object inputElement) {
		return elements;
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		return false;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof Object[]) {
			elements = (Object[]) newInput;
		}
	}

}
