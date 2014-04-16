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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider for ApplicationsMasterPart
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class TreeContentProvider implements ITreeContentProvider {

	private Object[] elements;

	public TreeContentProvider() {
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
