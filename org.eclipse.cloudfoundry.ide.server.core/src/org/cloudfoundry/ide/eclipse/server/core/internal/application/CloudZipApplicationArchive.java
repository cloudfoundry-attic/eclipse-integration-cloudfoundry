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
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal.application;

import java.io.IOException;
import java.util.zip.ZipFile;

import org.cloudfoundry.client.lib.archive.ZipApplicationArchive;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.eclipse.core.runtime.CoreException;

public class CloudZipApplicationArchive extends ZipApplicationArchive implements
		CloudApplicationArchive {

	protected final ZipFile zipFile;

	public CloudZipApplicationArchive(ZipFile zipFile) {
		super(zipFile);
		this.zipFile = zipFile;
	}

	@Override
	public void close() throws CoreException {
		try {
			if (zipFile != null) {
				zipFile.close();
			}
		} catch (IOException e) {
			throw CloudErrorUtil.toCoreException(e);
		}
	}
}
