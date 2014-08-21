/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
 *     Keith Chong, IBM - Support more general branded server type IDs via org.eclipse.ui.menus
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudUiUtil;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IURLProvider;

public class OpenHomePageCommand extends BaseCommandHandler {

	private String SELECTION_TEXT = "Select the module to launch home page from";
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		initializeSelection(event);

		if (selectedServer != null) {
			CloudFoundryApplicationModule cloudModule = getSelectedCloudAppModule();
			if (cloudModule != null) {
				int state = cloudModule.getState();
				// Based on property testers, this should already be started
				if (state == IServer.STATE_STARTED) {
					IURLProvider cloudServer = (IURLProvider)selectedServer.loadAdapter(IURLProvider.class, null);

					CloudFoundryServer cfs = (CloudFoundryServer)selectedServer.loadAdapter(CloudFoundryServer.class, null);
					
					String contextRoot = null;
					if (cfs != null){
						// IModule[][] because IModule[] is the correct representation of module structure
						IModule[][] launchables = cfs.getLaunchableModules(selectedModule);

						if (launchables != null){
							if (launchables.length == 1){
								contextRoot = cfs.getLaunchableModuleContextRoot(launchables[0]);
							}
							else if (launchables.length > 1 ){
								
								List<String> selectionOptions = new ArrayList<String>();
								Map<String, String> index = new HashMap<String, String>();
								for (int i = 0; i < launchables.length; i++){
									String option = "";
									for (int j = 0; j < launchables[i].length; j++){
										option += launchables[i][j].getName() + "/";
									}
									if (option.endsWith("/")){
										option = option.substring(0, option.length() - 1);
									}
									selectionOptions.add(option);
									index.put(option, String.valueOf(i));
								}
								
								ElementListSelectionDialog dialog = new ElementListSelectionDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), new LabelProvider());
								dialog.setTitle(SELECTION_TEXT);
								dialog.setElements(selectionOptions.toArray());


								if (dialog.open() != Window.OK) { 	
									if (dialog.getReturnCode() == Window.CANCEL){
										CloudFoundryPlugin.logWarning("User pressed cancel on selection dialog");
										return null;
									}
									CloudFoundryPlugin.logError(("Failed to open the Open Home Page selection dialog"));
									return null;
								}

								Object[] result = dialog.getResult();
								contextRoot = cfs.getLaunchableModuleContextRoot(launchables[Integer.valueOf(index.get(result[0]))]); 
							}
						}
					}
					
					try {
						URL homePageUrl = cloudServer.getModuleRootURL(selectedModule);
						if (contextRoot != null){
							homePageUrl = new URL(homePageUrl, contextRoot);
						}
						
						if (homePageUrl != null) {
							CloudUiUtil.openUrl(homePageUrl.toExternalForm());
						}
						else {
							CloudFoundryPlugin.logError("homePageUrl is null, unable to launch the Home Page URL");
							return null;
						}
					}
					catch (Exception e) {
						CloudFoundryPlugin.logError("Cannot launch the home page URL", e);
						return null;
					}					
				}
			}
		}
		return null;
	}
	
	private CloudFoundryApplicationModule getSelectedCloudAppModule() {
		CloudFoundryServer cloudServer = (CloudFoundryServer) selectedServer
				.loadAdapter(CloudFoundryServer.class, null);
		return cloudServer.getExistingCloudModule(selectedModule);
	}


}
