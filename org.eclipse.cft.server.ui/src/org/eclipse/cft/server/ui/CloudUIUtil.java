/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others
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
 *     IBM Corporation - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui;

import java.util.List;

import org.eclipse.cft.server.core.AbstractCloudFoundryUrl;
import org.eclipse.cft.server.core.internal.spaces.CloudOrgsAndSpaces;
import org.eclipse.cft.server.ui.internal.CloudServerUIUtil;
import org.eclipse.cft.server.ui.internal.CloudUiUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.operation.IRunnableContext;

/**
 * Wrapper class that exposes as API some internal
 * methods to be used outside of the current plugin.
 */
public class CloudUIUtil {

	/**
	 * Open the given URL on a browser
	 * @param url the URL value to be launched.
	 */
	public static void openUrl(String url) {
		CloudUiUtil.openUrl(url);
	}
	
	/**
	 * Returns the list of all available urls (default and non-default)
	 * for the given server type. 
	 * 
	 * @param serverTypeId the server type id to look for in the records. 
	 * @return a list of AbstractCloudFoundryUrl containing all known
	 * 		urls for the current server type id.
	 * @throws CoreException basically if operation is cancelled.
	 */
	public static List<AbstractCloudFoundryUrl> getAllUrls(String serverTypeId) throws CoreException {
		return CloudServerUIUtil.getAllUrls(serverTypeId, null);
	}
	
	/**
	 * Returns the available default url (if there is one)
	 * for the given server type. 
	 * 
	 * @param serverTypeId the server type id to look for in the records. 
	 * @return an AbstractCloudFoundryUrl representing the default url
	 * 		or null if there isn't one.
	 * @throws CoreException basically if operation is cancelled.
	 */
	public static AbstractCloudFoundryUrl getDefaultUrl(String serverTypeId) throws CoreException {
		return CloudServerUIUtil.getDefaultUrl(serverTypeId, null);
	}
	
	/**
	 * Returns the list of all non-default Urls (provided by the framework or user-defined)
	 * for the given server type. 
	 * 
	 * @param serverTypeId the server type id to look for in the records. 
	 * @return a list of AbstractCloudFoundryUrl containing all known
	 * 		non-default Urls.
	 * @throws CoreException basically if operation is cancelled.
	 */
	public static List<AbstractCloudFoundryUrl> getUrls(String serverTypeId) throws CoreException {
		return CloudServerUIUtil.getUrls(serverTypeId, null);
	}
	
	/**
	 * Returns all cloud spaces given the input information, to create connections
	 * to a Cloud Url server.
	 * 
	 * Runnable context can be null. If so, default Eclipse progress service
	 * will be used as a runnable context. Display URL should be true if the
	 * display URL is passed. If so, and attempt will be made to parse the
	 * actual URL.
	 * 
	 * @param userName must not be null
	 * @param password must not be null
	 * @param urlText must not be null. Can be either display or actual URL
	 * @param displayURL true if URL is display URL
	 * @param selfSigned true if connecting to a self-signing server. False otherwise
	 * @param context may be optional
	 * @return spaces descriptor, or null if it couldn't be determined
	 * @throws CoreException
	 */
	public static CloudOrgsAndSpaces getCloudSpaces(String userName, String password, String urlText,
			boolean displayURL, boolean selfSigned, IRunnableContext context) throws CoreException {
		return CloudUiUtil.getCloudSpaces(userName, password, urlText, displayURL, selfSigned, context);
	}
}
