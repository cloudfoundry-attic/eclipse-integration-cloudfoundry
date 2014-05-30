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
package org.cloudfoundry.ide.eclipse.server.core.internal.debug;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;

/**
 * Handles debugger termination events.
 * 
 */
public class ConnectToDebuggerListener implements IDebugEventSetListener {

	private Object eventSource;

	private DebugCommand command;

	public ConnectToDebuggerListener(DebugCommand command, Object eventSource) {
		this.eventSource = eventSource;
		this.command = command;
	}

	public void handleDebugEvents(DebugEvent[] events) {
		if (events != null) {
			int size = events.length;
			for (int i = 0; i < size; i++) {

				if (events[i].getKind() == DebugEvent.TERMINATE) {

					Object source = events[i].getSource();

					// THe event source should be a thread as remote debugging
					// does not launch a separate local process
					// However, multiple threads may be associated with the
					// debug target, so to check if an app
					// is disconnected from the debugger, additional termination
					// checks need to be performed on the debug target
					// itself
					if (source instanceof IThread) {
						IDebugTarget debugTarget = ((IThread) source).getDebugTarget();

						// Be sure to only handle events from the debugger
						// source that generated the termination event. Do not
						// handle
						// any other application that is currently connected to
						// the debugger.
						if (eventSource.equals(debugTarget) && debugTarget.isDisconnected()) {

							DebugPlugin.getDefault().removeDebugEventListener(this);
							command.removeFromConnectionRegister();
							ICloudFoundryDebuggerListener listener = command.getListener();
							if (listener != null) {
								listener.handleDebuggerTermination();
							}
							return;
						}
					}

				}
			}
		}
	}

}