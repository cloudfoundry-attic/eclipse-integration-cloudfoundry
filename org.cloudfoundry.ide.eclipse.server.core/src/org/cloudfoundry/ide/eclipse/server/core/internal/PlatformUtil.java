/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import org.eclipse.core.runtime.Platform;

public class PlatformUtil {

	private static String os;

	/**
	 * 
	 * @return OS as defined by Platform
	 * @see Platform
	 */
	public static String getOS() {

		if (os == null) {

			os = Platform.getOS();

			if (os != Platform.OS_MACOSX && os != Platform.OS_LINUX) {
				String osName = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
				if (osName != null && osName.startsWith("windows")) { //$NON-NLS-1$
					os = Platform.OS_WIN32;
				}
			}

		}

		return os;
	}

}
