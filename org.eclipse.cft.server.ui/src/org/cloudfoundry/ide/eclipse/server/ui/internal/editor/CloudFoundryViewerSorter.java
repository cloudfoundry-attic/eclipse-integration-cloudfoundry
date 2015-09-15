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
package org.cloudfoundry.ide.eclipse.server.ui.internal.editor;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.AppStatsContentProvider.InstanceStatsAndInfo;
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
