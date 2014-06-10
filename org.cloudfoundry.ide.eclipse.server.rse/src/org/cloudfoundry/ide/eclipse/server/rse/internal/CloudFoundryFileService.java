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
package org.cloudfoundry.ide.eclipse.server.rse.internal;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.files.AbstractFileService;
import org.eclipse.rse.services.files.IHostFile;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

/**
 * @author Leo Dos Santos
 */
public class CloudFoundryFileService extends AbstractFileService implements ICloudFoundryFileService {

	private List<AccountResource> accounts;

	private List<ApplicationResource> applications;

	private IHost host;

	public CloudFoundryFileService(IHost host) {
		super();
		this.host = host;
	}

	public void copy(String srcParent, String srcName, String tgtParent, String tgtName, IProgressMonitor monitor)
			throws SystemMessageException {
		// TODO Auto-generated method stub

	}

	public void copyBatch(String[] srcParents, String[] srcNames, String tgtParent, IProgressMonitor monitor)
			throws SystemMessageException {
		// TODO Auto-generated method stub

	}

	public IHostFile createFile(String remoteParent, String fileName, IProgressMonitor monitor)
			throws SystemMessageException {
		// TODO Auto-generated method stub
		return null;
	}

	public IHostFile createFolder(String remoteParent, String folderName, IProgressMonitor monitor)
			throws SystemMessageException {
		// TODO Auto-generated method stub
		return null;
	}

	public void delete(String remoteParent, String fileName, IProgressMonitor monitor) throws SystemMessageException {
		// TODO Auto-generated method stub

	}

	public void download(String remoteParent, String remoteFile, File localFile, boolean isBinary, String hostEncoding,
			IProgressMonitor monitor) throws SystemMessageException {
		Object[] array = parseNestedFiles(remoteParent);
		if (array != null) {
			ApplicationResource app = (ApplicationResource) array[0];
			String path = (String) array[1];
			CloudFoundryServer server = app.getServer();
			String appName = app.getCloudApplication().getName();
			int instance = app.getInstanceId();
			try {
				String content = server.getBehaviour().getFile(appName, instance, path.concat(remoteFile).substring(1),
						monitor);
				if (content != null) {
					if (!localFile.exists()) {
						localFile.getParentFile().mkdirs();
					}
					ByteArrayInputStream inStream = new ByteArrayInputStream(content.getBytes());
					OutputStream outStream = new BufferedOutputStream(new FileOutputStream(localFile));
					int byteCount = 0;
					byte[] buffer = new byte[4096];
					int bytesRead = -1;
					while ((bytesRead = inStream.read(buffer)) != -1) {
						outStream.write(buffer, 0, bytesRead);
						byteCount += bytesRead;
					}
					outStream.flush();
					inStream.close();
					outStream.close();
				}
			}
			catch (Exception e) {
				CloudFoundryRsePlugin.logError("An error occurred while opening file: " + remoteFile
						+ " for application: " + appName, e);
			}
		}

	}

	@Override
	public String getDescription() {
		return "The Cloud File Service provides services for the Applications and Files subsystem";
	}

	public IHostFile getFile(String remoteParent, String name, IProgressMonitor monitor) throws SystemMessageException {
		remoteParent = remoteParent.concat("/");
		AccountResource account = parseAccount(remoteParent.concat(name));
		if (account != null) {
			return account;
		}
		ApplicationResource app = parseApp(remoteParent.concat(name));
		if (app != null) {
			return app;
		}
		Object[] array = parseNestedFiles(remoteParent);
		if (array != null) {
			app = (ApplicationResource) array[0];
			String path = (String) array[1];
			List<FileResource> files = app.getChildren(path, monitor);
			if (files != null) {
				for (FileResource file : files) {
					if (name.equals(file.getName())) {
						return file;
					}
				}
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return "Cloud File Service";
	}

	public IHostFile[] getRoots(IProgressMonitor monitor) throws SystemMessageException {
		List<AccountResource> list = new ArrayList<AccountResource>();
		IServer[] allServers = ServerCore.getServers();
		for (int i = 0; i < allServers.length; i++) {
			IServer candidate = allServers[i];
			if (CloudFoundryRsePlugin.doesServerBelongToHost(candidate, host)) {
				CloudFoundryServer server = (CloudFoundryServer) candidate.loadAdapter(CloudFoundryServer.class,
						monitor);
				AccountResource resource = new AccountResource(server);
				list.add(resource);
			}
		}
		accounts = list;
		return accounts.toArray(new AccountResource[accounts.size()]);
	}

	public IHostFile getUserHome() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isCaseSensitive() {
		// TODO Auto-generated method stub
		return false;
	}

	public void move(String srcParent, String srcName, String tgtParent, String tgtName, IProgressMonitor monitor)
			throws SystemMessageException {
		// TODO Auto-generated method stub

	}

	public void rename(String remoteParent, String oldName, String newName, IHostFile oldFile, IProgressMonitor monitor)
			throws SystemMessageException {
		// TODO Auto-generated method stub

	}

	public void rename(String remoteParent, String oldName, String newName, IProgressMonitor monitor)
			throws SystemMessageException {
		// TODO Auto-generated method stub

	}

	public void setLastModified(String parent, String name, long timestamp, IProgressMonitor monitor)
			throws SystemMessageException {
		// TODO Auto-generated method stub

	}

	public void setReadOnly(String parent, String name, boolean readOnly, IProgressMonitor monitor)
			throws SystemMessageException {
		// TODO Auto-generated method stub

	}

	public void upload(File localFile, String remoteParent, String remoteFile, boolean isBinary, String srcEncoding,
			String hostEncoding, IProgressMonitor monitor) throws SystemMessageException {
		// TODO Auto-generated method stub

	}

	public void upload(InputStream stream, String remoteParent, String remoteFile, boolean isBinary,
			String hostEncoding, IProgressMonitor monitor) throws SystemMessageException {
		// TODO Auto-generated method stub

	}

	private AccountResource parseAccount(String path) {
		int index = path.lastIndexOf("@");
		if (index >= 0 && (index + 1) <= path.length()) {
			String accountName = path.substring(0, index);
			String accountUrl = path.substring(index + 1);
			for (AccountResource account : accounts) {
				CloudFoundryServer server = account.getServer();
				if (accountUrl.equals(server.getUrl()) && accountName.equals(server.getServer().getName())) {
					return account;
				}
			}
		}
		return null;
	}

	private ApplicationResource parseApp(String path) {
		int index = path.indexOf("/");
		if (index >= 0 && (index + 1) <= path.length()) {
			String appUrl = path.substring(0, index);
			String instance = path.substring(index + 1);
			for (ApplicationResource app : applications) {
				CloudApplication cloudApp = app.getCloudApplication();
				String cloudAppId = ((Integer) app.getInstanceId()).toString();
				if (validApp(appUrl, cloudApp, cloudAppId, instance)) {
					return app;
				}
			}
		}
		return null;
	}

	protected boolean validApp(String appUrl, CloudApplication cloudApp, String cloudAppId, String instance) {
		List<String> uris = cloudApp.getUris();
		boolean matchingUri = false;
		if (appUrl != null && appUrl.length() > 0) {
			// an appURL will be one of two things: 1. an actual URL or 2. app
			// name for standalone apps with no actual URL
			matchingUri = (uris != null && !uris.isEmpty() && appUrl.equals(uris.get(0)))
					|| (appUrl.equals(cloudApp.getName()));
		}
		return matchingUri && instance.equals(cloudAppId);
	}

	private Object[] parseNestedFiles(String path) {
		int index = path.indexOf("/");
		if (index >= 0 && (index + 1) <= path.length()) {
			String appUrl = path.substring(0, index);
			String leftover = path.substring(index + 1);
			index = leftover.indexOf("/");
			if (index >= 0) {
				String instance = leftover.substring(0, index);
				String filePath = leftover.substring(index);
				for (ApplicationResource app : applications) {
					CloudApplication cloudApp = app.getCloudApplication();
					String cloudAppId = ((Integer) app.getInstanceId()).toString();
					if (validApp(appUrl, cloudApp, cloudAppId, instance)) {
						return new Object[] { app, filePath };
					}
				}
			}
		}
		return null;
	}

	@Override
	protected IHostFile[] internalFetch(String parentPath, String fileFilter, int fileType, IProgressMonitor monitor)
			throws SystemMessageException {
		AccountResource account = parseAccount(parentPath);
		if (account != null) {
			applications = account.fetchChildren(monitor);
			return applications.toArray(new ApplicationResource[applications.size()]);
		}
		ApplicationResource app = parseApp(parentPath);
		if (app != null) {
			List<FileResource> files = app.fetchChildren("/", monitor);
			return files.toArray(new FileResource[files.size()]);
		}
		Object[] array = parseNestedFiles(parentPath);
		if (array != null) {
			app = (ApplicationResource) array[0];
			String path = (String) array[1];
			List<FileResource> files = app.fetchChildren(path, monitor);
			return files.toArray(new FileResource[files.size()]);
		}
		return null;
	}

}
