/*******************************************************************************
 * Copyright (c) 2012 - 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
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

	private static final URL baseURL = CloudFoundryServerUiPlugin.getDefault().getBundle().getEntry("/icons/");

	private final static String OBJ = "obj16/";

	public static final ImageDescriptor EDIT = create(OBJ, "edit.gif");

	public static final ImageDescriptor TUNNEL_EXTERNAL_TOOLS = create(OBJ, "external_tools.gif");
	
	public static final ImageDescriptor JDBC_DATA_TOOLS = create(OBJ, "jdbc_16.gif");

	public static final ImageDescriptor DISCONNECT = create(OBJ, "disconnect.png");

	public static final ImageDescriptor CONNECT = create(OBJ, "connect.png");

	public static final ImageDescriptor NEW_SERVICE = create(OBJ, "service_new.png");

	public static final ImageDescriptor OBJ_APPLICATION = create(OBJ, "application.png");

	public static final ImageDescriptor OBJ_MULTI = create(OBJ, "multi.png");

	public static final ImageDescriptor OBJ_PUBLISH = create(OBJ, "publish.png");

	public static final ImageDescriptor OBJ_SERVICE = create(OBJ, "service.png");

	public static final ImageDescriptor OBJ_SINGLE = create(OBJ, "single.png");

	public static final ImageDescriptor OBJ_SPRINGSOURCE = create(OBJ, "springsource_obj.png");

	public static final ImageDescriptor OVERLAY_REFRESH = create(OBJ, "refresh_overlay.png");

	public static final ImageDescriptor REFRESH = create(OBJ, "refresh.gif");

	public static final ImageDescriptor REMOVE = create(OBJ, "remove.gif");

	public static final ImageDescriptor OVERLAY_ERROR = create(OBJ, "error_overlay.png");

	public static final ImageDescriptor RESTART_DEBUG_MODE = create(OBJ, "restart_debug.png");

	public static final ImageDescriptor RESTART = create(OBJ, "restart.png");

	public static final ImageDescriptor MENU_VIEW_ENABLED = create(OBJ, "view_menu_elcl.gif");

	public static final ImageDescriptor MENU_VIEW_DISABLED = create(OBJ, "view_menu_dlcl.gif");

	public static final ImageDescriptor DEBUG = create(OBJ, "debug_exc.gif");

	public static final ImageDescriptor DEFAULT_WIZARD_BANNER = create(OBJ, "default_wizard_banner.png");

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

		Image image = imageRegistry.get("" + imageDescriptor.hashCode());
		if (image == null) {
			image = imageDescriptor.createImage();
			imageRegistry.put("" + imageDescriptor.hashCode(), image);
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
		buffer.append("full");
		buffer.append('/');
		buffer.append(prefix);
		buffer.append('/');
		buffer.append(name);
		return new URL(baseURL, buffer.toString());
	}

}
