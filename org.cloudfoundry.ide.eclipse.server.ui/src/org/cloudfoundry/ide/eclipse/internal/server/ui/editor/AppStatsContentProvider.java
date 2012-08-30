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
