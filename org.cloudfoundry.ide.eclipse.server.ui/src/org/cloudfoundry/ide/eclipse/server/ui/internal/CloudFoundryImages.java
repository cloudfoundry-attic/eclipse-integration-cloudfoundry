/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import java.net.MalformedURLException;
import java.net.URL;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryBrandingExtensionPoint;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 */
public class CloudFoundryImages {

	private static final URL baseURL = CloudFoundryServerUiPlugin.getDefault().getBundle().getEntry("/icons/"); //$NON-NLS-1$

	private final static String OBJ = "obj16/"; //$NON-NLS-1$

	public static final ImageDescriptor EDIT = create(OBJ, "edit.gif"); //$NON-NLS-1$

	public static final ImageDescriptor TUNNEL_EXTERNAL_TOOLS = create(OBJ, "external_tools.gif"); //$NON-NLS-1$

	public static final ImageDescriptor JDBC_DATA_TOOLS = create(OBJ, "jdbc_16.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DISCONNECT = create(OBJ, "disconnect.png"); //$NON-NLS-1$

	public static final ImageDescriptor CONNECT = create(OBJ, "connect.png"); //$NON-NLS-1$

	public static final ImageDescriptor NEW_SERVICE = create(OBJ, "service_new.png"); //$NON-NLS-1$

	public static final ImageDescriptor OBJ_APPLICATION = create(OBJ, "application.png"); //$NON-NLS-1$

	public static final ImageDescriptor OBJ_MULTI = create(OBJ, "multi.png"); //$NON-NLS-1$

	public static final ImageDescriptor OBJ_PUBLISH = create(OBJ, "publish.png"); //$NON-NLS-1$

	public static final ImageDescriptor OBJ_SERVICE = create(OBJ, "service.png"); //$NON-NLS-1$

	public static final ImageDescriptor OBJ_SINGLE = create(OBJ, "single.png"); //$NON-NLS-1$

	public static final ImageDescriptor OBJ_SPRINGSOURCE = create(OBJ, "springsource_obj.png"); //$NON-NLS-1$

	public static final ImageDescriptor OVERLAY_REFRESH = create(OBJ, "refresh_overlay.png"); //$NON-NLS-1$

	public static final ImageDescriptor REFRESH = create(OBJ, "refresh.gif"); //$NON-NLS-1$

	public static final ImageDescriptor REMOVE = create(OBJ, "remove.gif"); //$NON-NLS-1$

	public static final ImageDescriptor OVERLAY_ERROR = create(OBJ, "error_overlay.png"); //$NON-NLS-1$

	public static final ImageDescriptor RESTART_DEBUG_MODE = create(OBJ, "restart_debug.png"); //$NON-NLS-1$

	public static final ImageDescriptor RESTART = create(OBJ, "restart.png"); //$NON-NLS-1$

	public static final ImageDescriptor PUSH = create(OBJ, "push.gif"); //$NON-NLS-1$

	public static final ImageDescriptor MENU_VIEW_ENABLED = create(OBJ, "view_menu_elcl.gif"); //$NON-NLS-1$

	public static final ImageDescriptor MENU_VIEW_DISABLED = create(OBJ, "view_menu_dlcl.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DEBUG = create(OBJ, "debug_exc.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DEFAULT_WIZARD_BANNER = create(OBJ, "default_wizard_banner.png"); //$NON-NLS-1$

	private static ImageRegistry imageRegistry;

	public static ImageDescriptor create(String prefix, String name) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(prefix, name));
		}
		catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	/**
	 * Lazily initializes image map.
	 */
	public static Image getImage(ImageDescriptor imageDescriptor) {
		ImageRegistry imageRegistry = getImageRegistry();

		Image image = imageRegistry.get("" + imageDescriptor.hashCode()); //$NON-NLS-1$
		if (image == null) {
			image = imageDescriptor.createImage();
			imageRegistry.put("" + imageDescriptor.hashCode(), image); //$NON-NLS-1$
		}
		return image;
	}

	public static ImageDescriptor getWizardBanner(String serverTypeId) {
		IConfigurationElement config = CloudFoundryBrandingExtensionPoint.getConfigurationElement(serverTypeId);
		String wizBanner = CloudFoundryBrandingExtensionPoint.getWizardBannerPath(serverTypeId);
		if (config != null && wizBanner != null && wizBanner.trim().length() > 0) {
			String bundle = config.getContributor().getName();
			return AbstractUIPlugin.imageDescriptorFromPlugin(bundle, wizBanner);
		}
		return null;
	}

	private static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			imageRegistry = new ImageRegistry();
		}

		return imageRegistry;
	}

	private static URL makeIconFileURL(String prefix, String name) throws MalformedURLException {
		if (baseURL == null) {
			throw new MalformedURLException();
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append("full"); //$NON-NLS-1$
		buffer.append('/');
		buffer.append(prefix);
		buffer.append('/');
		buffer.append(name);
		return new URL(baseURL, buffer.toString());
	}

}
