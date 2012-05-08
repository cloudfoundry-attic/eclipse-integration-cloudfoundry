/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.cloudfoundry.caldecott.TunnelException;
import org.cloudfoundry.caldecott.client.TunnelHelper;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.springframework.core.io.ClassPathResource;

public class CaldecottTunnerHelper {

	private CaldecottTunnerHelper() {
		// util class
	}

	public static void deployTunnelApp(CloudFoundryClient client, ClassLoader classLoader) {
		ClassPathResource cpr = new ClassPathResource("caldecott_helper.zip", classLoader);
		try {
			File temp = copyCaldecottZipFile(cpr);
			client.createApplication(TunnelHelper.getTunnelAppName(), "sinatra", 64,
					Arrays.asList(new String[] { TunnelHelper.getRandomUrl(client, TunnelHelper.getTunnelAppName()) }),
					Arrays.asList(new String[] {}), false);
			client.uploadApplication(TunnelHelper.getTunnelAppName(), temp);
			client.updateApplicationEnv(TunnelHelper.getTunnelAppName(),
					Collections.singletonMap("CALDECOTT_AUTH", UUID.randomUUID().toString()));
			client.startApplication(TunnelHelper.getTunnelAppName());
			temp.delete();
		}
		catch (IOException e) {
			throw new TunnelException("Unable to deploy the Caldecott server application", e);
		}
	}

	private static File copyCaldecottZipFile(ClassPathResource cpr) throws IOException {
		File temp = File.createTempFile("caldecott", "zip");
		InputStream in = cpr.getInputStream();
		OutputStream out = new FileOutputStream(temp);
		int read = 0;
		byte[] bytes = new byte[1024];
		while ((read = in.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}
		in.close();
		out.flush();
		out.close();
		return temp;
	}

}
