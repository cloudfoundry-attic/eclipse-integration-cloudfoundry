/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.eclipse.cft.server.ui.internal.editor;

import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider for showing instances given an ApplicatinoStats
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class AppStatsContentProvider implements ITreeContentProvider {
	
	public static class InstanceStatsAndInfo {
		
		private InstanceStats stats;
		
		private InstanceInfo info;
		
		public InstanceStatsAndInfo(InstanceStats stats, InstanceInfo info) {
			this.stats = stats;
			this.info = info;
		}
		
		public InstanceStats getStats() {
			return stats;
		}
		
		public InstanceInfo getInfo() {
			return info;
		}
	}

	private InstanceStatsAndInfo[] statsAndInfos;

	public AppStatsContentProvider() {
	}

	public void dispose() {
	}

	public Object[] getChildren(Object parentElement) {
		return null;
	}

	public Object[] getElements(Object inputElement) {
		return statsAndInfos;
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		return false;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof InstanceStatsAndInfo[]) {
			statsAndInfos = (InstanceStatsAndInfo[]) newInput;
		}
	}

}
