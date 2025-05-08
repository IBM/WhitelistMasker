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

import java.util.ArrayList;
import java.util.List;
import com.api.json.JSONObject;

/**
 * Utility service to check for the existence of a list of parameters in a
 * passed JSONObject
 *
 */

public class ParameterUtils {

	/**
	 * Return the value of the key in the object
	 * 
	 * @param obj
	 *            object
	 * @param key
	 *            key
	 * @return value of key in the object
	 * @throws Exception
	 */
	static public Object getObj(JSONObject obj, String key) throws Exception {
		if (obj == null || key == null) {
			throw new Exception("null parameter passed.");
		}
		return obj.get(key);
	}

	/**
	 * Get the parameter data for the supplied class, method, and named parameters
	 * from the supplied json object
	 * 
	 * @param json
	 *                   object containing parameter data
	 * @param className
	 *                   name of the class
	 * @param methodName
	 *                   name of the method
	 * @param params
	 *                   names of the parameters to be retrieved
	 * @return list of parameters
	 * @throws Exception
	 */
	static public List<Object> getParameterData(JSONObject json, String className, String methodName, String... params)
			throws Exception {
		List<Object> retList = new ArrayList<Object>();
		if (params == null) {
			throw new Exception(className + ":" + methodName + ": null parameter passed");
		}
		for (String param : params) {
			String[] paramTypes = param.split(":");
			if (paramTypes.length == 1) {
				// just treat as a constant String
				retList.add(paramTypes[0]);
				continue;
			}
			if (paramTypes.length != 2) {
				throw new Exception(className + ":" + methodName
						+ ": Malformed parameter (missing class name or too many colon delimiters?)");
			}
			String[] parts = paramTypes[0].split("/");
			String pastPart = "";
			Object ret = json;
			for (String part : parts) {
				// first time below passes since original object is a JSONObject
				if (ret instanceof JSONObject == false) {
					throw new Exception(
							className + ":" + methodName + ": No JSONObject for " + pastPart + " path \"" + param + "\"");
				}
				ret = getObj(json, part);
				if (ret == null) {
					throw new Exception(className + ":" + methodName + ": No entry for path \"" + param + "\" in " + json);
				}
				// ret might not be a JSONObject but if this is the last part of the
				// path that is okay
				pastPart = part;
			}
			try {
				@SuppressWarnings("unused")
				Class<?> cls = Class.forName(paramTypes[1]);
				if (ret.getClass().getName().compareTo(paramTypes[1]) != 0) {
					throw new Exception(className + ":" + methodName + ": Actual class \"" + ret.getClass().getName()
							+ "\" differs from expected \"" + paramTypes[1] + "\"");
				}
				retList.add(ret);

			} catch (ClassNotFoundException e) {
				throw new Exception(className + ":" + methodName + ": Can not load \"" + paramTypes[1] + "\"", e);
			}
		}
		return retList;
	}

	/**
	 * Test case for building list of parameters
	 * 
	 * @param args
	 *             not used
	 */
	public static void main(String[] args) {
		String testJSON = "{\"key\":\"value\",\"complex\":{\"key\":\"value\"}}";
		try {
			JSONObject test1 = JSONObject.parse(testJSON);
			System.out.println("Testing against:\n" + test1.serialize(true));
			validate(test1, "key:java.lang.String", "complex/key:java.lang.String");
			try {
				validate(test1, "badKey:java.lang.String");
				System.out.println("Error: didn't get expected exception for badKey.");
			} catch (Exception e) {
			}
			try {
				validate(test1, "key/badValue:java.lang.String");
				System.out.println("Error: didn't get expected exception for key/badValue.");
			} catch (Exception e) {
				if (e instanceof Exception) {
					System.out.println("All okay");
				} else {
					System.out
							.println("Error: didn't get expected exception for key/badValue. Got " + e.getClass().getName());
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Validates the named parameter is part of the json object
	 * 
	 * @param json
	 *              object
	 * @param param
	 *              parameter name
	 * @throws Exception
	 */
	static public void validate(JSONObject json, String param) throws Exception {
		validate(json, new String[] { param });
	}

	/**
	 * Validates the named parameters are part of the json object
	 * 
	 * @param json
	 *              object
	 * @param params
	 *              array of parameter names
	 * @throws Exception
	 */
	static public void validate(JSONObject json, String... params) throws Exception {
		if (params == null) {
			throw new Exception("null parameter passed");
		}
		for (String param : params) {
			String[] paramTypes = param.split(":");
			if (paramTypes.length != 2) {
				throw new Exception("Malformed parameter (missing class name or too many colon delimiters?)");
			}
			String[] parts = paramTypes[0].split("/");
			Object ret = new Object();
			String pastPart = "";
			for (String part : parts) {
				// first time below passes since original object is a JSONObject
				if (ret instanceof JSONObject == false) {
					throw new Exception("No JSONObject for " + pastPart + " path \"" + param + "\"");
				}
				ret = getObj(json, part);
				if (ret == null) {
					throw new Exception("No entry for path \"" + param + "\"");
				}
				// ret might not be a JSONObject but if this is the last part of the
				// path that is okay
				pastPart = part;
			}
			try {
				@SuppressWarnings("unused")
				Class<?> cls = Class.forName(paramTypes[1]);
				if (ret.getClass().getName().compareTo(paramTypes[1]) != 0) {
					throw new Exception("Actual class \"" + ret.getClass().getName() + "\" differs from expected \""
							+ paramTypes[1] + "\"");
				}
			} catch (ClassNotFoundException e) {
				throw new Exception("Can not load \"" + paramTypes[1] + "\"", e);
			}
		}
	}

	/**
	 * Validates the named parameter 1 and 2 are part of the json object
	 * 
	 * @param json
	 *               object
	 * @param param1
	 *               parameter name 1
	 * @param param2
	 *               parameter name 2
	 * @throws Exception
	 */
	static public void validate(JSONObject json, String param1, String param2) throws Exception {
		validate(json, new String[] { param1, param2 });
	}

	/**
	 * Validates the named parameter 1, 2 and 3 are part of the json object
	 * 
	 * @param json
	 *               object
	 * @param param1
	 *               parameter name 1
	 * @param param2
	 *               parameter name 2
	 * @param param3
	 *               parameter name 3
	 * @throws Exception
	 */
	static public void validate(JSONObject json, String param1, String param2, String param3) throws Exception {
		validate(json, new String[] { param1, param2, param3 });
	}

	/**
	 * Validates the named parameter 1, 2, 3 and 4 are part of the json object
	 * 
	 * @param json
	 *               object
	 * @param param1
	 *               parameter name 1
	 * @param param2
	 *               parameter name 2
	 * @param param3
	 *               parameter name 3
	 * @param param4
	 *               parameter name 4
	 * @throws Exception
	 */
	static public void validate(JSONObject json, String param1, String param2, String param3, String param4)
			throws Exception {
		validate(json, new String[] { param1, param2, param3, param4 });
	}

	/**
	 * Validates the named parameter 1, 2, 3, 4 and 5 are part of the json object
	 * 
	 * @param json
	 *               object
	 * @param param1
	 *               parameter name 1
	 * @param param2
	 *               parameter name 2
	 * @param param3
	 *               parameter name 3
	 * @param param4
	 *               parameter name 4
	 * @param param5
	 *               parameter name 5
	 * @throws Exception
	 */
	static public void validate(JSONObject json, String param1, String param2, String param3, String param4,
			String param5) throws Exception {
		validate(json, new String[] { param1, param2, param3, param4, param5 });
	}

}
