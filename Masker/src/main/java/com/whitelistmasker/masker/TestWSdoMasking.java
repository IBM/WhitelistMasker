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
import java.util.List;
import java.util.Properties;
import com.api.json.JSONArray;
import com.api.json.JSONObject;

/**
 * Utility to test the WebService requests
 * 
 */
public class TestWSdoMasking implements Serializable {

	private static final long serialVersionUID = 6442272576397186034L;

	/**
	 * Constructor
	 */
	public TestWSdoMasking() {
	}

	/**
	 * Test rig for sending text from a file to be masked by the MaskWebService
	 * 
	 * @param args
	 *             unused
	 */
	public static void main(String[] args) {
		TestWSdoMasking pgm = new TestWSdoMasking();
		boolean quit = false;
		while (!quit) {
			int actionVal = 1;
			try {
				switch (actionVal) {
				case 1: {
					JSONObject response = pgm.getMaskedContent();
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
	 * Creates a request comprising the text from a specified file and a choice to
	 * mask numbers, and a template to mask US phone numbers and sends the request
	 * to the MaskWebService for processing.
	 * 
	 * @return the response object returned from the MaskWebService
	 */
   public JSONObject getMaskedContent() {
      JSONObject response = new JSONObject();
      String tenantID = "companyA";
      String servicename = "localhost";
      String unmaskedFile = "Unmasked.txt";
      String maskerWebServicesFile = "MaskWebService.properties";
      Boolean maskNumbers = Boolean.TRUE;
      while (true) {
         String tmp = MaskerUtils.prompt("Enter the tenant ID or q to exit ("+tenantID+"):");
         if (tmp.length() == 0) {
            tmp = tenantID;
         }
         if ("q".equalsIgnoreCase(tmp)) {
            break;
         }
         tenantID = tmp;
         
         String propertiesFilename = MaskerUtils.prompt(
            "Enter the filename of the web service properties file ("
               + maskerWebServicesFile + ") or q  to quit:");
         if (propertiesFilename.length() == 0) {
            propertiesFilename = maskerWebServicesFile;
         }
         if ("q".equalsIgnoreCase(propertiesFilename)) {
            break;
         }
         Properties propFile = new Properties();
         FileInputStream fis = null;
         try {
            fis = new FileInputStream(MaskerConstants.Masker_DIR_PROPERTIES+tenantID+File.separator + propertiesFilename);
            propFile.load(fis);
         } catch (IOException ioe) {
            System.out.println("Can not load " + MaskerConstants.Masker_DIR_PROPERTIES+tenantID+File.separator + propertiesFilename + " Error: "
               + ioe.getLocalizedMessage());
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

         String filename = MaskerUtils.prompt(
            "Enter the filename of the text to be masked ("
               + unmaskedFile + ") or q to quit:");
         if ("q".equalsIgnoreCase(filename)) {
            break;
         }
         if (filename.length() == 0) {
            filename = unmaskedFile;
         }

         File testFile = new File(MaskerConstants.Masker_DIR_PROPERTIES+tenantID+File.separator + filename);
         if (testFile.exists() == false) {
            System.err.println(
               "Filename \"" + MaskerConstants.Masker_DIR_PROPERTIES+tenantID+File.separator + filename + "\" does not exist. Try again.");
            continue;
         }

         if (testFile.isDirectory()) {
            System.err.println(
               "Filename \"" + MaskerConstants.Masker_DIR_PROPERTIES+tenantID+File.separator + filename + "\" is a directory. Try again.");
            continue;
         }
         String test = MaskerUtils
            .prompt("Should numbers be masked, or q to quit(Y)");
         if ("q".equals(test)) {
            break;
         }
         if (test.length() == 0) {
            test = "Y";
         }
         if ("Y".equalsIgnoreCase(test)) {
            maskNumbers = Boolean.TRUE;
         } else {
            maskNumbers = Boolean.FALSE;
         }
         String hostname = MaskerUtils.prompt(
            "Enter the hostname where the service is running or q to quit ("
               + servicename + ")");
         if ("q".equalsIgnoreCase(hostname)) {
            break;
         }
         if (hostname.length() == 0) {
            hostname = servicename;
         }
         servicename = hostname;

         try {
            List<String> unmaskedLines = MaskerUtils.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES+tenantID+File.separator + filename);
            JSONArray templates = new JSONArray();
            JSONObject usPhoneTemplate = new JSONObject();
            usPhoneTemplate.put("template",
               "\\+?1? *\\(?\\d{3}\\)? *[\\-\\.]?(\\d{3}) *[\\-\\.]? *\\d{4}");
            usPhoneTemplate.put("mask", "~Phone");
            templates.add(usPhoneTemplate);
            JSONArray unmasked = new JSONArray();
            for (String line : unmaskedLines) {
               unmasked.add(line);
            }
            unmaskedFile = filename;
            JSONObject service = new JSONObject();
            service.put("protocol", propFile.getProperty("protocol", "http"));
            service.put("domain",
               propFile.getProperty("hostname", "localhost"));
            service.put("portnumber", propFile.getProperty("port", "9080"));
            service.put("endpoint",
               "/" + propFile.getProperty("servletname", "MaskWebServices")
                  + "/" + propFile.getProperty("version", "v1")
                  + "/masker/doMasking");
            service.put("username", propFile.getProperty("username", "mask"));
            service.put("password",
               propFile.getProperty("password", "password"));
            service.put("apitimeout",
               propFile.getProperty("apitimeout", "100000")); // 100 seconds
            JSONObject request = new JSONObject();
            request.put("tenantID", tenantID);
            request.put("maskNumbers", maskNumbers);
            request.put("unmasked", unmasked);
            request.put("templates", templates);
            JSONObject body = new JSONObject();
            body.put("request", request);
            System.out.println("Sending Request:\n" + body.serialize(true));
            response = MaskerUtils.sendRequest("POST", service, body);
            break;
         } catch (Exception e) {
            System.err.println("Error calling service: "
               + e.getLocalizedMessage() + ". Try again.");
            System.err.println(
               "Note: you can check the server is running by pasting this in a browser:\n"
                  + "http://" + hostname
                  + ":9080/MaskWebServices/v1/HelloMasker");
         }
      }
      return response;
   }
}
