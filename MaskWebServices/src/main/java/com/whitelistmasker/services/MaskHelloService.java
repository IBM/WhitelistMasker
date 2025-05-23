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

package com.whitelistmasker.services;

import com.api.json.JSONObject;

/**
 * MaskHelloService provides an echo-back Hello service to test execution of the
 * Masker Web Services
 */
public class MaskHelloService {

	/**
	 * Return Hello from MaskWebServices for any request received.
	 * 
	 * @return JSON Object containing a "response" key with value "Hello from
	 *         MaskWebServices"
	 */
	public static JSONObject doHello() {
		JSONObject response = new JSONObject();
		response.put("response", "Hello from MaskWebServices");
		return response;
	}

}
