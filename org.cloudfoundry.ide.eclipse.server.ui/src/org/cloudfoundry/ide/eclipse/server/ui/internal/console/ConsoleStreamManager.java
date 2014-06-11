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
package org.cloudfoundry.ide.eclipse.server.ui.internal.console;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.log.LogContentType;

public class ConsoleStreamManager {

	private static ConsoleStreamManager current;

	private List<ConsoleStreamProvider> providers;

	public static ConsoleStreamManager getCurrent() {
		if (current == null) {
			current = new ConsoleStreamManager();
		}
		return current;
	}

	public ConsoleStream getStream(LogContentType type) {
		if (type == null) {
			return null;
		}

		if (providers == null) {
			providers = loadProviders();
		}

		ConsoleStreamProvider supportedProvider = null;
		int i = 0;
		while (supportedProvider == null && i < providers.size()) {
			ConsoleStreamProvider provider = providers.get(i);
			LogContentType[] supportedTypes = provider.getSupportedTypes();
			for (LogContentType tp : supportedTypes) {
				if (tp.equals(type)) {
					supportedProvider = provider;
					break;
				}
			}
			i++;
		}

		return supportedProvider != null ? supportedProvider.getStream(type) : null;
	}

	public List<ConsoleStreamProvider> loadProviders() {
		List<ConsoleStreamProvider> providers = new ArrayList<ConsoleStreamProvider>();
		providers.add(new StdStreamProvider());
		providers.add(new TraceStreamProvider());
		providers.add(new ApplicationLogStreamProvider());
		return providers;
	}

}
