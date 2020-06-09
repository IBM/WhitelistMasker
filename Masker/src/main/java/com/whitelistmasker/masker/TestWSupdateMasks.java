/**
 * (c) Copyright 2020 IBM Corporation
 * 1 New Orchard Road, 
 * Armonk, New York, 10504-1722
 * United States
 * +1 914 499 1900
 * support: Nathaniel Mills wnm3@us.ibm.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.whitelistmasker.masker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import com.api.json.JSONArray;
import com.api.json.JSONObject;

/**
 * Utility to test the MaskWebService requests to manipulate the standard set of
 * templates used for masking
 * 
 */
public class TestWSupdateMasks implements Serializable {

	private static final long serialVersionUID = 2758586728942808124L;

	/**
	 * Constructor
	 */
	public TestWSupdateMasks() {
	}

	/**
	 * Test rig to create a request to delete and update mask templates used by the
	 * MaskWebService and to display the results
	 * 
	 * @param args
	 *             not used
	 */
	public static void main(String[] args) {
		TestWSupdateMasks pgm = new TestWSupdateMasks();
		boolean quit = false;
		while (!quit) {
			int actionVal = 1;
			try {
				switch (actionVal) {
				case 1: {
					JSONObject response = pgm.testUpdateMasks();
					if (response.size() == 0) {
						quit = true;
					} else {
						System.out.println("Full Response:\n" + response.serialize(true));
					}
					break;
				}
				default: {
					System.err.println("The action you entered is not equal to 1. Try again.");
					break;
				}
				}
			} catch (NumberFormatException | IOException nfe) {
				nfe.printStackTrace();
			}
		}
		System.out.println("Goodbye");
	}

	/**
	 * Test rig to create a request to update the mask templates used by the
	 * MaskWebService by removing the email template and adding a US Phone number
	 * mask. The mask provided is missing a tilde to test that the service added
	 * appropriate delimiters to the mask
	 * 
	 * @return the response to the request for updating mask templates
	 */
	public JSONObject testUpdateMasks() {
		JSONObject response = new JSONObject();
      String tenantID = "companyA";
		String maskerWebServicesFile = "MaskWebService.properties";
		while (true) {
         String tmp = MaskerUtils.prompt("Enter the tenant ID or q to exit ("+tenantID+"):");
         if (tmp.length() == 0) {
            tmp = tenantID;
         }
         if ("q".equalsIgnoreCase(tmp)) {
            break;
         }
         tenantID = tmp;

         String propertiesFilename = MaskerUtils
					.prompt("Enter the fully qualified filename of the web service properties file (" + maskerWebServicesFile
							+ ") or q  to quit:");
			if (propertiesFilename.length() == 0) {
				propertiesFilename = maskerWebServicesFile;
			}
			if ("q".equalsIgnoreCase(propertiesFilename)) {
				break;
			}
			Properties propFile = new Properties();
			FileInputStream fis = null;
			try {
				fis = new FileInputStream("." + File.separator + MaskerConstants.Masker_DIR_PROPERTIES+tenantID+ File.separator + propertiesFilename);
				propFile.load(fis);
			} catch (IOException ioe) {
				System.out.println("Can not load " + "." + File.separator + MaskerConstants.Masker_DIR_PROPERTIES+tenantID+ File.separator + propertiesFilename + " Error: " + ioe.getLocalizedMessage());
				continue;
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			try {
				JSONArray updates = new JSONArray();
				JSONObject usPhoneTemplate = new JSONObject();
				usPhoneTemplate.put("template", "\\+?1? *\\(?\\d{3}\\)? *[\\-\\.]?(\\d{3}) *[\\-\\.]? *\\d{4}");
				usPhoneTemplate.put("mask", "~Phone");
				updates.add(usPhoneTemplate);
				JSONArray removals = new JSONArray();
				removals
						.add("([a-zA-Z])+(\\w)*((\\.(\\w)+)?)+@([a-zA-Z])+(\\w)*((\\.([a-zA-Z])+(\\w)*)+)?(\\.)[a-zA-Z]{2,}");

				JSONObject service = new JSONObject();
				service.put("protocol", propFile.getProperty("protocol", "http"));
				service.put("domain", propFile.getProperty("hostname", "localhost"));
				service.put("portnumber", propFile.getProperty("port", "9080"));
				service.put("endpoint", "/" + propFile.getProperty("servletname", "MaskWebServices") + "/"
						+ propFile.getProperty("version", "v1") + "/masker/updateMasks");
				service.put("username", propFile.getProperty("username", "mask"));
				service.put("password", propFile.getProperty("password", "password"));
				service.put("apitimeout", propFile.getProperty("apitimeout", "100000")); // 100 seconds
				JSONObject request = new JSONObject();
				request.put("tenantID", tenantID);
				request.put("updates", updates);
				request.put("removals", removals);
				JSONObject body = new JSONObject();
				body.put("request", request);
				System.out.println("Sending Request:\n" + body.serialize(true));
				response = MaskerUtils.sendRequest("POST", service, body);
				break;
			} catch (Exception e) {
				System.err.println("Error calling service: " + e.getLocalizedMessage() + ". Try again.");
				System.err.println("Note: you can check the server is running by pasting this in a browser:\n" + "http://"
						+ propFile.getProperty("hostname", "localhost") + ":" + propFile.getProperty("port", "9080") + "/"
						+ propFile.getProperty("servletname", "MaskWebServices") + "/" + propFile.getProperty("version", "v1")
						+ "/HelloMasker");
			}
		}
		return response;
	}
}
