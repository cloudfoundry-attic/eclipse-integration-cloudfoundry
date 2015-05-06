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
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.jrebel.CloudRebelAppHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IModule;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CloudRebelUIHandler extends CloudRebelAppHandler {

	private static CloudRebelAppHandler handler;

	public CloudRebelUIHandler() {
	}

	public void register() {
		if (handler == null && CloudRebelAppHandler.isJRebelIDEInstalled()) {
			handler = this;
			ServerEventHandler.getDefault().addServerListener(handler);
		}
	}

	@Override
	protected void handleRebelProject(CloudServerEvent event, IModule module, IProgressMonitor monitor)
			throws CoreException {

		IProject project = module.getProject();
		// Only replace rebel xml file if a manual Remoting
		// update is performed on Spring boot applications
		if (event.getType() == CloudServerEvent.EVENT_JREBEL_REMOTING_UPDATE
				&& CloudFoundryProjectUtil.isSpringBootJarProject(project)) {
			updateRebelXML(project, monitor);
		}

		super.handleRebelProject(event, module, monitor);
	}

	protected void updateRebelXML(IProject project, IProgressMonitor monitor) {

		// rebel.xml is overwritten for Spring Boot Jar apps to skip the /lib
		// folder which
		// is only generated in the Spring Boot Jar but has no workspace
		// equivalent
		try {

			IFile file = project.getFile("/src/main/resources/rebel.xml"); //$NON-NLS-1$
			if (file.isAccessible()) {
				String path = file.getRawLocation() != null ? file.getRawLocation().toString() : null;

				if (path != null) {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

					DocumentBuilder db = factory.newDocumentBuilder();
					Document doc = db.parse(new File(path));

					Element binElement = null;
					NodeList nodeList = doc.getElementsByTagName("*"); //$NON-NLS-1$
					if (nodeList != null) {

						String libFolderName = "lib/**"; //$NON-NLS-1$

						String javaBuildpackFolderName = ".java-buildpack/**"; //$NON-NLS-1$

						for (int i = 0; i < nodeList.getLength(); i++) {
							Node node = nodeList.item(i);
							if ((node instanceof Element) && node.getNodeName().equals("dir")) { //$NON-NLS-1$
								Element el = (Element) node;
								String att = el.getAttribute("name"); //$NON-NLS-1$
								if (att != null && att.endsWith("/bin")) { //$NON-NLS-1$
									binElement = el;
									break;
								}
							}
						}

						if (binElement != null) {
							NodeList binChildren = binElement.getChildNodes();
							Element existingExcludeLib = null;
							Element existingExcludeJavabuildpack = null;
							if (binChildren != null) {

								for (int i = 0; i < binChildren.getLength(); i++) {
									Node node = binChildren.item(i);
									if (node instanceof Element && node.getNodeName().equals("exclude")) { //$NON-NLS-1$
										Element excludeElement = (Element) node;
										Attr attr = excludeElement.getAttributeNode("name"); //$NON-NLS-1$
										if (attr != null && attr.getNodeValue() != null) {
											if (attr.getNodeValue().equals(libFolderName)) { //$NON-NLS-1$
												existingExcludeLib = excludeElement;
											}
											else if (attr.getNodeValue().equals(javaBuildpackFolderName)) {
												existingExcludeJavabuildpack = excludeElement;
											}
										}
									}
								}
							}

							Element updatedExcludeLib = null;
							if (existingExcludeLib == null) {
								updatedExcludeLib = doc.createElement("exclude"); //$NON-NLS-1$
								updatedExcludeLib.setAttribute("name", libFolderName); //$NON-NLS-1$ 
								binElement.appendChild(updatedExcludeLib);
							}
							Element updatedExcludeJavabuildpack = null;
							if (existingExcludeJavabuildpack == null) {
								updatedExcludeJavabuildpack = doc.createElement("exclude"); //$NON-NLS-1$
								updatedExcludeJavabuildpack.setAttribute("name", javaBuildpackFolderName); //$NON-NLS-1$ 
								binElement.appendChild(updatedExcludeJavabuildpack);
							}
							if (updatedExcludeLib != null || updatedExcludeJavabuildpack != null) {

								final boolean[] proceed = { false };

								final List<String> toexclude = new ArrayList<String>();
								if (updatedExcludeLib != null) {
									toexclude.add(libFolderName);
								}
								if (updatedExcludeJavabuildpack != null) {
									toexclude.add(javaBuildpackFolderName);
								}
								Display.getDefault().syncExec(new Runnable() {

									public void run() {
										Shell shell = CloudUiUtil.getShell();

										proceed[0] = shell != null
												&& !shell.isDisposed()
												&& MessageDialog.openQuestion(
														shell,
														Messages.CloudRebelUIHandler_TEXT_REPLACE_REBEL_XML_TITLE,
														NLS.bind(
																Messages.CloudRebelUIHandler_TEXT_REPLACE_REBEL_XML_BODY,
																toexclude));
									}
								});

								if (proceed[0]) {
									// If replacing the exist rebel.xml file, be
									// sure to switch off automatic rebel.xml
									// generation
									project.setPersistentProperty(new QualifiedName(
											"org.zeroturnaround.eclipse.jrebel",//$NON-NLS-1$
											"autoGenerateRebelXml"), "false"); //$NON-NLS-1$ //$NON-NLS-2$

									Transformer transformer = TransformerFactory.newInstance().newTransformer();
									transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
									DOMSource source = new DOMSource(doc);
									StreamResult console = new StreamResult(new File(path));
									transformer.transform(source, console);
									project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
								}
							}
						}
					}
				}
			}
		}
		catch (ParserConfigurationException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (SAXException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (IOException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (TransformerException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
	}
}
