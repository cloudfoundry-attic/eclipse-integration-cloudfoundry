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

import org.cloudfoundry.client.lib.CloudService;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for CloudService
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class ServicesLabelProvider extends LabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof CloudService) {
			CloudService service = (CloudService) element;
			switch (columnIndex) {
			case 0:
				return service.getName();
			case 1:
				return service.getType();
			case 2:
				return service.getVendor();
			case 3:
				return service.getVersion();
//			case 4:
//				return service.getTier();
			}
		}
		return null;
	}

}
