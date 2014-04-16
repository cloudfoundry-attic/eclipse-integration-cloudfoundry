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

import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.cloudfoundry.client.lib.domain.InstanceStats.Usage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.AppStatsContentProvider.InstanceStatsAndInfo;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class AppStatsLabelProvider extends LabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof InstanceStatsAndInfo) {
			InstanceStatsAndInfo statsAndInfo = (InstanceStatsAndInfo) element;
			InstanceStats stats = statsAndInfo.getStats();

			Usage usage = stats.getUsage();
			double cpu = 0.0;
			String memory = "0M";
			String disk = "0M";
			if (usage != null) {
				cpu = usage.getCpu();
				memory = getFormattedMemory(usage.getMem() / 1024);
				disk = getFormattedMemory(usage.getDisk() / 1024);
			}
			switch (columnIndex) {
			case 0:
				return stats.getId();
			case 1:
				return stats.getHost();
			case 2:
				return stats.getPort() + "";
			case 3:
				return String.valueOf(cpu) + "% (" + String.valueOf(stats.getCores()) + ")";
			case 4:
				return memory + " (" + getFormattedMemory(stats.getMemQuota() / 1024) + ")";
			case 5:
				return disk + " (" + getFormattedMemory(stats.getDiskQuota() / 1024) + ")";
			case 6:
				return getFormattedDuration(Math.round(stats.getUptime()));
			}
		}
		return null;
	}

	/** Returns the time in the format: HHH:MM */
	private String getFormattedDuration(long duration) {
		if (duration <= 0) {
			return "0h:0m:0s"; //$NON-NLS-1$
		}

		long remainderSeconds = duration % 60;
		long totalMinutes = duration / 60;
		long remainderMinutes = totalMinutes % 60;
		long totalHours = totalMinutes / 60;
		// long remainderHours = totalHours % 24;
		// long totalDays = totalHours / 24;

		// String dayString = "" + totalDays;
		String hourString = "" + totalHours;
		String minuteString = "" + remainderMinutes;
		String secondString = "" + remainderSeconds;

		return hourString + "h:" + minuteString + "m:" + secondString + "s"; //$NON-NLS-1$
	}

	private String getFormattedMemory(double mem) {
		return String.valueOf(Math.round(mem * 10 / 1024) / 10) + "M";
	}

}
