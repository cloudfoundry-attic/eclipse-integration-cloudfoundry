/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
 *     IBM - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.verify.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	public static String CloudFoundryEclipseToolsErrorTitle;
	
	public static String UnsupportedJavaVersion;

	private static final String BUNDLE_NAME = Activator.PLUGIN_ID + ".internal.Messages"; //$NON-NLS-1$
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Empty
	}
}
