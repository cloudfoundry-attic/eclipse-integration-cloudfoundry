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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.startcommand;

import java.util.List;

/**
 * Defines a Standalone start command for a given runtime type. A start command
 * may be defined by multiple start command types. For example, a Java start
 * command may defined a java application start command "java ..." or a script
 * file.
 * <p/>
 * If defining multiple start command definitions, a default start command type
 * can also be specified.
 */
public abstract class StartCommand {

	/**
	 * The start command in the form that it would be used to start the
	 * application.
	 */
	abstract public String getStartCommand();

	abstract public StartCommandType getDefaultStartCommandType();

	abstract public List<StartCommandType> getStartCommandTypes();

	abstract public String getArgs();

}
