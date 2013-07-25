/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import static org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ApplicationInstanceServiceColumn.*;

/**
 * Label provider for CloudService
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class ServicesLabelProvider extends LabelProvider implements ITableLabelProvider {



	public ServicesLabelProvider() {

	}

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}
	
	public ApplicationInstanceServiceColumn[] getServiceViewColumn() {
		return new ApplicationInstanceServiceColumn[] { Name, Vendor, Plan, Version };
	}

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof CloudService) {
			CloudService service = (CloudService) element;
			switch (columnIndex) {
			case 0:
				return service.getName();
			case 1:
				return service.getLabel();
			case 2:
				return service.getPlan();
			case 3:
				return service.getVersion();
			}
		}
		return null;
	}

}
