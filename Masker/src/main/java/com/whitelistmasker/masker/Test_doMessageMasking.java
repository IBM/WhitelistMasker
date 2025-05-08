/**
 * (c) Copyright 2020-2023 IBM Corporation
 * 1 New Orchard Road, 
 * Armonk, New York, 10504-1722
 * United States
 * +1 914 499 1900
 * Nathaniel Mills wnm3@us.ibm.com
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
 * Utility to test the doMessageMasking requests
 * 
 */
public class Test_doMessageMasking implements Serializable {

   private static final long serialVersionUID = 6442272576397186034L;

   /**
    * Constructor
    */
   public Test_doMessageMasking() {
   }

   /**
    * Test rig for sending text from a file to be masked by the MaskWebService
    * 
    * @param args
    *           unused
    */
   public static void main(String[] args) {
      Test_doMessageMasking pgm = new Test_doMessageMasking();
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
                     System.out
                        .println("Full Response:\n" + response.serialize(true));
                  }
                  break;
               }
               default: {
                  System.err.println(
                     "The action you entered is not equal to 1. Try again.");
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
    * Creates a request comprising the text from a specified file and a choice
    * to mask numbers, and a template to mask US phone numbers and sends the
    * request to the MaskWebService for processing.
    * 
    * @return the response object returned from the MaskWebService
    */
   public JSONObject getMaskedContent() {
      JSONObject response = new JSONObject();
      String tenantID = "companyA";
      String unmaskedFile = "UnmaskedMessages.json";
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
            "Enter the filename of the conversation to be masked ("
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
         try {
            JSONObject unmaskedObj = MaskerUtils.loadJSONFile(MaskerConstants.Masker_DIR_PROPERTIES+tenantID+File.separator + filename);
            JSONArray templates = new JSONArray();
            JSONObject usPhoneTemplate = new JSONObject();
            usPhoneTemplate.put("template",
               "\\+?1? *\\(?\\d{3}\\)? *[\\-\\.]?(\\d{3}) *[\\-\\.]? *\\d{4}");
            usPhoneTemplate.put("mask", "~Phone");
            templates.add(usPhoneTemplate);
            JSONArray messages = (JSONArray)unmaskedObj.get("messages");
            unmaskedFile = filename;
            JSONObject request = new JSONObject();
            request.put("tenantID", tenantID);
            request.put("maskNumbers", maskNumbers);
            request.put("messages", messages);
            request.put("templates", templates);
            response = Masker.maskMessageContent(request);
            break;
         } catch (Exception e) {
         	e.printStackTrace();
         }
      }
      return response;
   }
}
