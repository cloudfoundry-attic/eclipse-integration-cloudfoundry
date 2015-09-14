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
package org.cloudfoundry.ide.eclipse.server.core;

import java.util.List;

/**
 * Enforces the structure that a Url Provider must follow
 * so it can be used to provide dynamic Cloud Urls through
 * branding extension.
 * 
 * <b>: IMPORTANT: It is highly recommended for classes that 
 * implement this interface to use a cache mechanism when calculating
 * the urls that will be returned, specially of those obtained
 * dynamically (as for example, from a web service), since the
 * retrieval methods could be invoked multiple times and no cache
 * is implemented at the base level. </b>
 */
public interface ICloudFoundryUrlProvider {
	/**
	 * Provides the default Url for the current
	 * Cloud Foundry server type.
	 * 
	 * This default Url will take precedence over any value
	 * provided through the static plugin.xml.
	 * 
	 * If for some reason, two default Urls are provided for
	 * the same contribution (one dynamically through an 
	 * {@link ICloudFoundryUrlProvider}, then another one
	 * through a static contribution in plugin.xml, the 
	 * static one will be added to the list of non-default
	 * Urls when processing this contribution. 
	 *  
	 * @return an AbstractCloudFoundryUrl or null if no default
	 * Url is given by this provider.
	 */
	public abstract AbstractCloudFoundryUrl getDefaultUrl ();
	
	/**
	 * Provides a list of additional Urls for the current
	 * Cloud Foundry server type.
	 * 
	 * @return a List of (non default) AbstractCloudFoundryUrl
	 */
	public abstract List <AbstractCloudFoundryUrl> getNonDefaultUrls ();
}
