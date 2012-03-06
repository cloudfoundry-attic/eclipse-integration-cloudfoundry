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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.eclipse.wst.server.ui.internal.Messages;

@SuppressWarnings("restriction")

/**
 * This class exists simply to disable the publish section in the server editor. It doesn't create any widgets.
 * 
 * @author Terry Denney
 */
public class DisablePublishSection extends ServerEditorSection {

	public DisablePublishSection() {
	}
	
	@Override
	public void createSection(Composite parent) {
		Control[] children = parent.getChildren();
		
		for(Control child: children) {
			if (child instanceof Section) {
				Section section = (Section) child;
				if (Messages.serverEditorOverviewPublishSection.equals(section.getText())) {
					section.setEnabled(false);
					section.setText(Messages.serverEditorOverviewPublishSection + " (manually only)");
//					Control[] sectionChildren = section.getChildren();
//					for(Control sectionChild: sectionChildren) {
//						if (sectionChild instanceof Composite) {
//							Composite composite = (Composite) sectionChild;
//							Control[] compositeChildren = composite.getChildren();
//							boolean found = false;
//							
//							for(Control compositeChild: compositeChildren) {
//								if (compositeChild instanceof Button) {
//									Button button = (Button) compositeChild;
//									if (Messages.serverEditorOverviewAutoPublishEnabledResource.equals(button.getText()) ||
//										Messages.serverEditorOverviewAutoPublishEnabledBuild.equals(button.getText())) {
//										button.setEnabled(false);
//										button.setText(button.getText() + " (currently not supported)");
//										found = true;
//									}
//								}
//							}	
//							
//							if (found) {
//								composite.setEnabled(false);
//							}
//						}
//					}
				}
			}
		}
	}
}
