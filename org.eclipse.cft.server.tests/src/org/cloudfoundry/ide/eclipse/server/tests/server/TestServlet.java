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
package org.cloudfoundry.ide.eclipse.server.tests.server;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 */
public class TestServlet

// FIXNS: Commented out because of STS-3159
// extends HttpServlet

{

	public static class Response {

		private final int status;

		private final String message;

		private final String body;

		public Response(int status, String message, String body) {
			this.status = status;
			this.message = message;
			this.body = body;
		}

	}

	private static final long serialVersionUID = 1L;

	private static TestServlet instance;

	public static TestServlet getInstance() {
		return instance;
	}

	private final List<Response> responses = new LinkedList<TestServlet.Response>();

	public TestServlet() {
		instance = this;
	}

	public void addResponse(Response response) {
		responses.add(response);
	}

	// FIXNS: Commented out because of STS-3159
	// @Override
	// protected void doGet(HttpServletRequest request, HttpServletResponse
	// response) throws ServletException, IOException {
	// response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	// }
	//
	// @Override
	// protected void doPost(HttpServletRequest request, HttpServletResponse
	// response) throws ServletException,
	// IOException {
	// response.setContentType("application/json;charset=utf-8");
	// if (responses.size() > 0) {
	// Response mockReposonse = responses.remove(0);
	// response.sendError(mockReposonse.status, mockReposonse.message);
	// response.getWriter().append(mockReposonse.body);
	// }
	// else {
	// response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
	// }
	// }

}
