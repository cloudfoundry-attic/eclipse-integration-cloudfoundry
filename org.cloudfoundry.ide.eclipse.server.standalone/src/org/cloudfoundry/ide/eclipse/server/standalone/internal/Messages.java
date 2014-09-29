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
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/

package org.cloudfoundry.ide.eclipse.server.standalone.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.cloudfoundry.ide.eclipse.server.standalone.internal.Messages"; //$NON-NLS-1$

	public static String JavaCloudFoundryArchiver_ERROR_CREATE_CF_ARCHIVE;

	public static String JavaCloudFoundryArchiver_ERROR_CREATE_PACKAGED_FILE;

	public static String JavaCloudFoundryArchiver_ERROR_CREATE_TEMP_DIR;

	public static String JavaCloudFoundryArchiver_ERROR_JAVA_APP_PACKAGE;

	public static String JavaCloudFoundryArchiver_ERROR_NO_JAVA_PROJ_RESOLVED;

	public static String JavaCloudFoundryArchiver_ERROR_NO_MAIN;

	public static String JavaCloudFoundryArchiver_ERROR_NO_MAIN_CLASS_IN_MANIFEST;

	public static String JavaCloudFoundryArchiver_ERROR_MANIFEST_NOT_ACCESSIBLE;
	
	public static String JavaCloudFoundryArchiver_ERROR_FAILED_READ_MANIFEST;

	public static String JavaCloudFoundryArchiver_ERROR_NO_PACKAGE_FRAG_ROOTS;

	public static String JavaCloudFoundryArchiver_ERROR_NO_PACKAGED_FILE_CREATED;

	public static String JavaCloudFoundryArchiver_ERROR_REPACKAGE_SPRING;

	public static String JavaTypeUIAdapter_JOB_JAVA_ASSIST;

	public static String ProjectExplorerMenuFactory_JOB_DISABLE;

	public static String ProjectExplorerMenuFactory_JOB_ENABLE;

	public static String ProjectExplorerMenuFactory_LABEL_CONVERT_TEXT;
	public static String ProjectExplorerMenuFactory_LABEL_CONVERT_TOOLTIP;
	public static String ProjectExplorerMenuFactory_LABEL_REMOVE_TEXT;
	public static String ProjectExplorerMenuFactory_LABEL_REMOVE_TOOLTIP;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
