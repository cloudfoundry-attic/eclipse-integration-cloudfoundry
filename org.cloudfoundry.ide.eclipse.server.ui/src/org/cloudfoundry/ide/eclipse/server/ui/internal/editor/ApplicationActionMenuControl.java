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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationAction;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * 
 * @author Nieraj Singh
 * 
 */
public class ApplicationActionMenuControl {

	private Composite parent;

	private ToolItem viewMenuButton;

	private ApplicationAction selectedType;

	private String selectionButtonLabel;

	private Image selectionButtonImage;

	private Button applicationActionButton;

	private List<IButtonMenuListener> listeners;

	private List<ApplicationAction> actions;

	private FormToolkit toolkit;

	private ToolBar toolBar;

	private Composite buttonComposite;

	public ApplicationActionMenuControl(Composite parent, ApplicationAction[] actions, ApplicationAction defaultValue,
			String selectionButtonLabel, Image selectionButtonImage, FormToolkit toolkit) {
		this.parent = parent;
		selectedType = defaultValue;
		listeners = new ArrayList<IButtonMenuListener>();
		this.actions = actions != null ? Arrays.asList(actions) : null;
		this.selectionButtonLabel = selectionButtonLabel;
		this.selectionButtonImage = selectionButtonImage;
		this.toolkit = toolkit;
	}

	/**
	 * May be null
	 * @return
	 */
	public Button getSelectionButton() {
		return applicationActionButton;
	}

	public void setCompositeLayoutData(Object data) {
		if (buttonComposite != null) {
			buttonComposite.setLayoutData(data);
		}
	}

	public void setVisible(boolean isVisible) {
		if (applicationActionButton != null) {
			applicationActionButton.setVisible(isVisible);

		}
		if (toolBar != null) {
			toolBar.setVisible(isVisible);
		}

		if (buttonComposite != null) {
			buttonComposite.setVisible(isVisible);
		}

	}

	protected Button createButton(Composite parent, String text, Image image, int type) {
		Button button = toolkit != null ? toolkit.createButton(parent, text, type) : new Button(parent, type);
		button.setText(text);
		if (image != null) {
			button.setImage(image);
		}

		return button;
	}

	protected void notifyListeners() {
		for (IButtonMenuListener listener : listeners) {
			listener.widgetSelected(selectedType);
		}
	}

	protected void adaptControl(Control control) {
		if (toolkit != null) {
			control.setBackground(toolkit.getColors().getBackground());
			control.setForeground(toolkit.getColors().getForeground());
		}

	}

	protected ToolBarManager createToolBarManager(Composite parent) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolBar = toolBarManager.createControl(parent);

		// Adapt it to a form if a form tool kit is specified
		adaptControl(toolBar);

		GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.BEGINNING).applyTo(toolBar);
		return toolBarManager;
	}

	public void createControl() {

		buttonComposite = toolkit != null ? toolkit.createComposite(parent, SWT.NONE) : new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(new Point(0, 0)).numColumns(2).equalWidth(false).margins(0, 0)
				.applyTo(buttonComposite);

		applicationActionButton = createButton(buttonComposite, selectionButtonLabel, selectionButtonImage, SWT.FLAT);
		applicationActionButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				notifyListeners();
			}

		});

		GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.BEGINNING).applyTo(applicationActionButton);
		setDefaultTooltipMessage();

		// Do not create toolbar and menu if there is only one or less actions
		// in the given actionlist
		if (actions != null && actions.size() > 1) {

			final MenuManager viewMenuManager = new MenuManager();

			ToolBarManager manager = createToolBarManager(buttonComposite);
			toolBar = manager.getControl();

			viewMenuButton = new ToolItem(toolBar, SWT.PUSH, 0);

			viewMenuButton.setImage(CloudFoundryImages.getImage(CloudFoundryImages.MENU_VIEW_ENABLED));
			viewMenuButton.setDisabledImage(CloudFoundryImages.getImage(CloudFoundryImages.MENU_VIEW_DISABLED));

			viewMenuButton.setToolTipText(Messages.ApplicationActionMenuControl_TEXT_SELECT_MODE);
			viewMenuButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					showViewMenu(viewMenuManager, toolBar);
				}
			});

			for (ApplicationAction action : actions) {
				viewMenuManager.add(new MenuAction(action));
			}
		}

		setVisible(true);

	}

	protected void showViewMenu(MenuManager manager, ToolBar toolBar) {
		// don't show if the debug button is disabled.
		if (!isVisible()) {
			return;
		}

		Menu menu = manager.createContextMenu(applicationActionButton);
		applicationActionButton.setMenu(menu);

		Rectangle bounds = toolBar.getBounds();
		// Position the menu near the toolitem
		Point topRight = new Point(bounds.x + bounds.x / 10, bounds.height);
		topRight = applicationActionButton.toDisplay(topRight);
		menu.setLocation(topRight.x, topRight.y);

		menu.setVisible(true);

	}

	public boolean isVisible() {
		if (applicationActionButton != null) {
			return applicationActionButton.isVisible();
		}
		return false;
	}

	public IButtonMenuListener addMenuListener(IButtonMenuListener listener) {
		if (listener != null && !listeners.contains(listener)) {
			listeners.add(listener);
			return listener;
		}
		return null;
	}

	public interface IButtonMenuListener {

		public void widgetSelected(ApplicationAction actionType);

	}

	public void setSelectedAction(ApplicationAction selectedType) {
		this.selectedType = selectedType;
		setDefaultTooltipMessage();
	}

	protected class MenuAction extends Action {

		private ApplicationAction type;

		public MenuAction(ApplicationAction type) {
			this.type = type;
			setText(type.getDisplayName() != null ? type.getDisplayName() : type.name());
		}

		public void run() {
			setSelectedAction(type);
			notifyListeners();
		}

	}

	protected String getLabel(ApplicationAction action) {
		return action.getDisplayName();
	}

	public void setDefaultTooltipMessage() {

		if (applicationActionButton != null) {
			applicationActionButton.setToolTipText(NLS.bind(Messages.ApplicationActionMenuControl_TEXT_SELECT_MODE_FOR, selectedType.getDisplayName()));
		}

	}

}
