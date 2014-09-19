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

import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

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
				if (Messages.DisablePublishSection_TEXT_PUBLISHING.equals(section.getText())) {
					section.setEnabled(false);
					section.setText(NLS.bind(Messages.DisablePublishSection_TEXT_PUBLISHING, Messages.DisablePublishSection_MANUAL));
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
