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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.AppStatsContentProvider.InstanceStatsAndInfo;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.wst.server.core.IModule;


/**
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class CloudFoundryViewerSorter extends ViewerSorter {

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof InstanceStatsAndInfo && e1 instanceof InstanceStatsAndInfo) {
			InstanceStats stats1 = ((InstanceStatsAndInfo) e1).getStats();
			InstanceStats stats2 = ((InstanceStatsAndInfo) e2).getStats();
			return stats1.getId().compareTo(stats2.getId());
		}
		if (e1 instanceof CloudService && e2 instanceof CloudService) {
			CloudService service1 = (CloudService) e1;
			CloudService service2 = (CloudService) e2;
			return service1.getName().compareTo(service2.getName());
		}
		if (e1 instanceof IModule && e2 instanceof IModule) {
			IModule m1 = (IModule) e1;
			IModule m2 = (IModule) e2;
			return m1.getName().compareTo(m2.getName());
		}
		return super.compare(viewer, e1, e2);
	}

}
