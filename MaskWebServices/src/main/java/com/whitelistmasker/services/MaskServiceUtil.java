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

package com.whitelistmasker.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.whitelistmasker.masker.MaskerUtils;

/**
 * Mask Web Service Utilities
 *
 */
public class MaskServiceUtil implements Serializable {

	// HTTP Authentication
	public static final String AUTHORIZATION = "Authorization";
	public static final String AUTHORIZATION_ADMIN_USER = "maskadmin";

	public static final String AUTHORIZATION_CREDENTIALS = "bWFza2FkbWluOm1hc2tpc2Z1bg==";
	//
	public static final byte[] AUTHORIZATION_CREDENTIALS_DECODED = "maskadmin:maskisfun".getBytes();
	public static final String AUTHORIZATION_PREFIX = "Basic ";
	public static final String AUTHORIZATION_LOGIN = AUTHORIZATION_PREFIX + AUTHORIZATION_CREDENTIALS;
	public static final String AUTHORIZATION_SESSION_PREFIX = AUTHORIZATION_ADMIN_USER + ":";
	public static final int AUTHORIZATION_SESSION_ID_OFFSET = (AUTHORIZATION_SESSION_PREFIX).length();
	static boolean debug = false; // true; // to turn on
	// HTTP header extensions
	public static final String HTTP_HEADER_SESSION_ID = "X-Session-ID";
	// user id request header (including cookie) attributes
	public static final String HTTP_HEADER_USER_ID = "X-User-ID";

	static public String HTTP_RESPONSE = "Response";

	static public String HTTP_RETURN_CODE = "ReturnCode";
	private static final long serialVersionUID = -3804636662225945449L;
	public static final String SESSION_COOKIE_APPL_NAME = "Mask";
	public static final String SESSION_COOKIE_DOMAIN = null;
	public static final int SESSION_COOKIE_MAX_AGE = 86400; // 24 hours
	public static final String SESSION_COOKIE_NAME = "session";
	public static final String SESSION_COOKIE_PATH = null;
	public static final boolean SESSION_COOKIE_SECURE = false;
	public static final int SESSION_COOKIE_VERSION = 1;
	public static final String USER_COOKIE_APPL_NAME = "MaskUser";
	public static final String USER_COOKIE_DOMAIN = null;
	public static final int USER_COOKIE_MAX_AGE = 86400; // 24 hours
	public static final String USER_COOKIE_NAME = "user";
	public static final String USER_COOKIE_PATH = null;
	public static final boolean USER_COOKIE_SECURE = false;
	public static final int USER_COOKIE_VERSION = 1;

	/**
	 * Creates a response to contain an error message by the passed
	 * ildResponseCodes object, and the passed errorMessage (if one exists (e.g., is
	 * not null)).
	 * 
	 * @param errorMessage
	 *                         description of the error
	 * @param maskResponseCode
	 *                         error response code
	 * @return response
	 */
	public static Response getErrorResponse(String errorMessage, MaskResponseCodes maskResponseCode) {
		if (maskResponseCode == null) {
			maskResponseCode = MaskResponseCodes.Mask_UNEXPECTED_ERROR;
		}
		JSONObject errorContentsObj = new JSONObject();
		errorContentsObj.put("code", maskResponseCode.label());
		errorContentsObj.put("description", maskResponseCode.description());
		errorContentsObj.put("code", maskResponseCode.code());
		errorContentsObj.put("src", "Mask");
		errorContentsObj.put("type", maskResponseCode.label());
		if (!MaskerUtils.isUndefined(errorMessage)) {
			errorContentsObj.put("detail", errorMessage);
		}
		JSONObject errorObj = new JSONObject();
		errorObj.put("error", errorContentsObj);
		JSONObject ildErrorObj = errorObj;
		Response resp = Response.status(maskResponseCode.respCode()).header("Access-Control-Allow-Credentials", "true")
				.header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
				.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
				.header("Access-Control-Allow-Origin", "*").header("Access_Control_Max_Age", 43200)
				.entity(ildErrorObj.toString()).type(MediaType.APPLICATION_JSON).build();
		return resp;

	}

	/**
	 * Creates a response to contain an error identified by the passed
	 * ildResponseCodes object, and the exception (if one exists (e.g., is not
	 * null)).
	 * 
	 * @param exception
	 *                         describing error condition
	 * @param maskResponseCode
	 *                         response code
	 * @return response
	 */
	public static Response getErrorResponse(Throwable exception, MaskResponseCodes maskResponseCode) {
		if (maskResponseCode == null) {
			maskResponseCode = MaskResponseCodes.Mask_UNEXPECTED_ERROR;
		}
		JSONObject errorContentsObj = new JSONObject();
		errorContentsObj.put("code", maskResponseCode.label());
		errorContentsObj.put("description", maskResponseCode.description());
		errorContentsObj.put("code", maskResponseCode.code());
		errorContentsObj.put("src", "Mask");
		errorContentsObj.put("type", maskResponseCode.label());
		if (exception != null) {
			errorContentsObj.put("detail", exception.getLocalizedMessage());
		}

		// expand
		List<String> errorStack = new ArrayList<String>();
		if (exception != null) {
			Stack<Throwable> chainStack = new Stack<Throwable>();
			Throwable tTrigger = new Throwable(exception.toString());
			tTrigger.setStackTrace(exception.getStackTrace());
			errorStack.add("Caused by: " + tTrigger.getMessage());
			// add the stack trace content
			StackTraceElement[] stElements = tTrigger.getStackTrace();
			for (StackTraceElement stElt : stElements) {
				errorStack.add("  " + stElt.toString());
			}
			chainStack.push(tTrigger);
			// process the underlying chained exceptions
			Throwable tCause = exception.getCause();
			while (tCause != null) {
				Throwable tNew = new Throwable(tCause.getMessage());
				tNew.setStackTrace(tCause.getStackTrace());
				chainStack.push(tNew);
				errorStack.add("Caused by: " + tCause.getMessage());
				// add the stack trace content
				stElements = tCause.getStackTrace();
				for (StackTraceElement stElt : stElements) {
					errorStack.add("  " + stElt.toString());
				}
				tCause = tCause.getCause();
			}
			// unwind the stack
			// will at least have our child exception
			Throwable tChild = (Throwable) chainStack.pop();
			try {
				Throwable tParent = (Throwable) chainStack.pop();
				while (tParent != null) {
					tParent.initCause(tChild);
					tChild = tParent;
					tParent = (Throwable) chainStack.pop();
				}
			} catch (EmptyStackException e) {
				// expect to iterate until stack is empty
				// leaving the tChild pointing at the revised
			}
		}
		JSONArray errorStackArray = new JSONArray();
		for (String errorStackMsg : errorStack) {
			errorStackArray.add(errorStackMsg);
		}
		errorContentsObj.put("stackTrace", errorStackArray);
		JSONObject errorObj = new JSONObject();
		errorObj.put("error", errorContentsObj);
		JSONObject ildErrorObj = errorObj;
		if (debug) {
			try {
				System.out.println(ildErrorObj.serialize(true));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Response resp = Response.status(maskResponseCode.respCode()).header("Access-Control-Allow-Credentials", "true")
				.header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
				.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
				.header("Access-Control-Allow-Origin", "*").header("Access_Control_Max_Age", 43200)
				.entity(ildErrorObj.toString()).type(MediaType.APPLICATION_JSON).build();
		return resp;

	}

	/**
	 * Create a response to the REST Request
	 * 
	 * @param jsonMessage
	 *                    message object to be returned in the response
	 * @return response
	 */
	public static Response getResponse(JSONObject jsonMessage) {
		JSONObject respObj = jsonMessage;
		Response resp = Response.status(MaskResponseCodes.Mask_OKAY.respCode())
				.header("Access-Control-Allow-Credentials", "true")
				.header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
				.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
				.header("Access-Control-Allow-Origin", "*").header("Access_Control_Max_Age", 43200)
				.entity(respObj.toString()).type(MediaType.APPLICATION_JSON).build();
		return resp;
	}

	/**
	 * Helper function to read either the inputStream or errorStream depending on
	 * the response code.
	 * 
	 * @param con
	 *            http connection
	 * @return String - the Response data
	 * @throws IOException
	 */
	static String readResponse(HttpURLConnection con) throws IOException {
		int responseCode = con.getResponseCode();
		BufferedReader in = null;
		StringBuffer response = new StringBuffer();

		if (responseCode < 200 || responseCode > 299) {
			in = new BufferedReader(new InputStreamReader(con.getErrorStream())); // error
		} else {
			in = new BufferedReader(new InputStreamReader(con.getInputStream())); // success
		}

		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		return response.toString();
	}

	/**
	 * Sends an REST Delete request to the supplied URL with the serialized json
	 * supplied
	 * 
	 * @param url
	 *             destination URL
	 * @param body
	 *             body object to be sent
	 * @return response
	 * @throws Exception
	 *                   if unable to serialize the supplied json body object
	 */
	static public String sendRESTDelete(URL url, JSONObject body) throws Exception {
		try {
			return sendRESTDelete(url, body.serialize(), "Mozilla/5.0");
		} catch (Exception e) {
			throw new IOException("Can't serialize json object", e);
		}
	}

	/**
	 * Sends an REST Delete request to the supplied URL with the supplied user agent
	 * and deleteContent
	 * 
	 * @param url
	 *                      destination URL for delete request
	 * @param deleteContent
	 *                      payload of delete request
	 * @param userAgent
	 *                      optional userAgent for header (defaults to Mozilla/5.0
	 *                      if undefined)
	 * @return response to REST request
	 * @throws Exception
	 *                   if unable to read the deleteContent supplied or to connect
	 *                   to the destination URL
	 */
	static public String sendRESTDelete(URL url, String deleteContent, String userAgent) throws Exception {
		HttpURLConnection con;
		try {
			con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("DELETE");
			if (MaskerUtils.isUndefined(userAgent)) {
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
			} else {
				con.setRequestProperty("User-Agent", userAgent);
			}
			con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setInstanceFollowRedirects(true);

			// Send delete request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(deleteContent);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			String response = readResponse(con); // read the data from the
																// connection input/error stream
			if (responseCode < 200 || responseCode > 299) {
				throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + "," + HTTP_RESPONSE + "=" + response);
			}
			return (response);
		} catch (Exception e1) {
			throw new IOException("Can not open connection to " + url.toExternalForm(), e1);
		}

	}

	/**
	 * Sends an REST Get request to the specified URL
	 * 
	 * @param url
	 *            destination URL
	 * @return response to REST request
	 * @throws Exception
	 *                   if an error occurs during sending
	 */
	static public String sendRESTGet(URL url) throws Exception {
		return sendRESTGet(url, "Mozilla/5.0");
	}

	/**
	 * Sends an REST Get request to the specified URL
	 * 
	 * @param url
	 *                  destination URL
	 * @param userAgent
	 *                  optional user agent (defaults to Moziaal/5.0 if undefined)
	 * @return response to REST request
	 * @throws Exception
	 *                   if not possible to connect or send to the destination URL
	 */
	static public String sendRESTGet(URL url, String userAgent) throws Exception {
		HttpURLConnection con;
		try {
			con = (HttpURLConnection) url.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			// add request header
			if (MaskerUtils.isUndefined(userAgent)) {
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
			} else {
				con.setRequestProperty("User-Agent", userAgent);
			}
			con.setInstanceFollowRedirects(true);

			String response = "";
			try {
				int responseCode = con.getResponseCode();
				response = readResponse(con); // read the data from the connection
														// input/error stream
				if (responseCode < 200 || responseCode > 299) {
					throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + "," + HTTP_RESPONSE + "=" + response);
				}
			} catch (Exception e) {
				throw new Exception(e);
			}
			return (response);

		} catch (Exception e) {
			throw new IOException("Can not open connection to " + url.toExternalForm(), e);
		}
	}

	/**
	 * Sends an REST Get request to the specified URL appending the encoded json
	 * object as a query string using Context-Type "application/json"
	 * 
	 * @param urlString
	 *                  destination URL
	 * @param queryJSON
	 *                  the body of the GET request sent as a query string
	 * @return response to REST request
	 * @throws Exception
	 *                   if an error occurs serializing the queryJSON, or connecting
	 *                   or sending to the destination URL
	 */
	static public String sendRESTGetJSON(String urlString, JSONObject queryJSON) throws Exception {

		String queryString = "";
		if (queryJSON != null) {
			try {
				queryString = queryJSON.serialize();
			} catch (IOException e) {
				throw new Exception(e);
			}
		}
		if (queryString.length() > 0) {
			if (urlString.endsWith("/") == false) {
				urlString = urlString + "/";
			}
			try {
				urlString = urlString + URLEncoder.encode(queryString, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new Exception(e);
			}
		}
		URL url;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			throw new Exception("\"" + urlString + "\" creates a malformed URL", e);
		}
		return sendRESTGetJSON(url, "Mozilla/5.0");
	}

	/**
	 * Sends an REST Get request to the specified URL using Context-Type
	 * "application/json"
	 * 
	 * @param url
	 *            destination URL
	 * @return response to REST request
	 * @throws Exception
	 *                   if connecting or sending to the destination URL
	 */
	static public String sendRESTGetJSON(URL url) throws Exception {
		return sendRESTGetJSON(url, "Mozilla/5.0");
	}

	/**
	 * Sends an REST Get request to the specified URL using Context-Type
	 * "application/json"
	 * 
	 * @param url
	 *                  destination URL
	 * @param userAgent
	 *                  optional user agent (defaults to "Mozilla/5.0" if undefined)
	 * @return response to REST request
	 * @throws Exception
	 *                   if connecting or sending to the destination URL
	 */
	static public String sendRESTGetJSON(URL url, String userAgent) throws Exception {

		HttpURLConnection con;
		try {
			con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("GET");

			// add request header
			if (MaskerUtils.isUndefined(userAgent)) {
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
			} else {
				con.setRequestProperty("User-Agent", userAgent);
			}
			con.setRequestProperty("Content_Type", "application/json; charset=utf-8");
			con.setInstanceFollowRedirects(true);

			String response = "";
			try {
				int responseCode = con.getResponseCode();
				response = readResponse(con); // read the data from the connection
														// input/error stream
				if (responseCode < 200 || responseCode > 299) {
					throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + "," + HTTP_RESPONSE + "=" + response); // error
				}
			} catch (Exception e) {
				throw new Exception(e);
			}
			return (response);

		} catch (Exception e) {
			throw new IOException("Can not open connection to " + url.toExternalForm(), e);
		}
	}

	/**
	 * Sends an REST Patch request to the supplied URL with the serialized json
	 * supplied
	 * 
	 * @param url
	 *             destination URL
	 * @param json
	 *             body of the request to be sent
	 * @return response to REST request
	 * @throws Exception
	 *                   when serializing the json, connecting or sending to the
	 *                   destination URL
	 */
	static public String sendRESTPatch(URL url, JSONObject json) throws Exception {

		HttpURLConnection con;
		try {
			con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("PATCH");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Content_Type", "application/json; charset=utf-8");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setInstanceFollowRedirects(true);

			// Send patch request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(json.toString());
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			String response = readResponse(con); // read the data from the
																// connection input/error stream
			if (responseCode < 200 || responseCode > 299) {
				throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + "," + HTTP_RESPONSE + "=" + response);
			}
			return (response);
		} catch (Exception e1) {
			throw new IOException("Can not open connection to " + url.toExternalForm(), e1);
		}

	}

	/**
	 * Sends an REST Patch request to the supplied URL with the supplied
	 * patchContent
	 *
	 * @param url
	 *                     destination URL
	 * @param patchContent
	 *                     content to be send in the request
	 * @return response to REST request
	 * @throws Exception
	 *                   if an error occurs connecting or sending to the
	 *                   destionation URL
	 */
	static public String sendRESTPatch(URL url, String patchContent) throws Exception {
		return sendRESTPatch(url, patchContent, "Mozilla/5.0");
	}

	/**
	 * Sends an REST Patch request to the supplied URL with the supplied user agent
	 * and patchContent
	 * 
	 * @param url
	 *                     destination URL
	 * @param patchContent
	 *                     patch content for the body of the request
	 * @param userAgent
	 *                     optional user agent (default is Mozilla/5.0 if undefined)
	 * @return response
	 * @throws Exception
	 *                   if connecting or sending to the destination URL
	 */
	static public String sendRESTPatch(URL url, String patchContent, String userAgent) throws Exception {

		HttpURLConnection con;
		try {
			con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("PATCH");
			if (MaskerUtils.isUndefined(userAgent)) {
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
			} else {
				con.setRequestProperty("User-Agent", userAgent);
			}
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setInstanceFollowRedirects(true);

			// Send patch request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(patchContent);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			String response = readResponse(con); // read the data from the
																// connection input/error stream
			if (responseCode < 200 || responseCode > 299) {
				throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + "," + HTTP_RESPONSE + "=" + response);
			}
			return (response);
		} catch (Exception e1) {
			throw new IOException("Can not open connection to " + url.toExternalForm(), e1);
		}

	}

	/**
	 * Sends an REST Post request to the supplied URL with the serialized json
	 * supplied
	 * 
	 * @param url
	 *             destination URL
	 * @param json
	 *             the body of the POST request sent as a query string
	 * @return response to REST request
	 * @throws Exception
	 *                   if an error occurs serializing the json, or connecting or
	 *                   sending to the destination URL
	 */
	static public String sendRESTPost(URL url, JSONObject json) throws Exception {

		HttpURLConnection con;
		try {
			con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setInstanceFollowRedirects(true);

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(json.toString());
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			String response = readResponse(con); // read the data from the
																// connection input/error stream
			if (responseCode < 200 || responseCode > 299) {
				throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + "," + HTTP_RESPONSE + "=" + response);
			}
			return (response);
		} catch (Exception e1) {
			throw new IOException("Can not open connection to " + url.toExternalForm(), e1);
		}
	}

	/**
	 * Sends an REST Post request to the supplied URL with the supplied postContent
	 * 
	 * @param url
	 *                    destination URL
	 * @param postContent
	 *                    the body of the POST request sent as a query string
	 * @return response to REST request
	 * @throws Exception
	 *                   if an error occurs serializing the postContent, or
	 *                   connecting or sending to the destination URL
	 */
	static public String sendRESTPost(URL url, String postContent) throws Exception {
		return sendRESTPost(url, postContent, "Mozilla/5.0");
	}

	/**
	 * Sends an REST Post request ot the supplied URL with the supplied user agent
	 * and postContent
	 * 
	 * @param url
	 *                    destination URL
	 * @param postContent
	 *                    the body of the POST request sent as a query string
	 * @param userAgent
	 *                    optional user agent (default is Mozilla/5.0 if undefined)
	 * @return response to REST request
	 * @throws Exception
	 *                   if an error occurs serializing the postContent, or
	 *                   connecting or sending to the destination URL
	 */
	static public String sendRESTPost(URL url, String postContent, String userAgent) throws Exception {

		HttpURLConnection con;
		try {
			con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("POST");
			if (MaskerUtils.isUndefined(userAgent)) {
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
			} else {
				con.setRequestProperty("User-Agent", userAgent);
			}
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setInstanceFollowRedirects(true);

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(postContent);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			String response = readResponse(con); // read the data from the
																// connection input/error stream
			if (responseCode < 200 || responseCode > 299) {
				throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + "," + HTTP_RESPONSE + "=" + response);
			}
			return (response);
		} catch (Exception e1) {
			throw new IOException("Can not open connection to " + url.toExternalForm(), e1);
		}

	}

	/**
	 * Sends an REST Put request to the supplied URL with the serialized json
	 * supplied
	 * 
	 * @param url
	 *             destination URL
	 * @param json
	 *             the body of the POST request sent as a query string
	 * @return response to REST request
	 * @throws Exception
	 *                   if an error occurs serializing the json, or connecting or
	 *                   sending to the destination URL
	 */
	static public String sendRESTPut(URL url, JSONObject json) throws Exception {

		HttpURLConnection con;
		try {
			con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("PUT");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setInstanceFollowRedirects(true);
			// Send put request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(json.toString());
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			String response = readResponse(con); // read the data from the
																// connection input/error stream
			if (responseCode < 200 || responseCode > 299) {
				throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + "," + HTTP_RESPONSE + "=" + response);
			}
			return (response);
		} catch (Exception e1) {
			throw new IOException("Can not open connection to " + url.toExternalForm(), e1);
		}

	}

	/**
	 * Sends an REST Put request to the supplied URL with the supplied putContent
	 * 
	 * @param url
	 *                   destination URL
	 * @param putContent
	 *                   the body of the PUT request sent as a query string
	 * @return response to REST request
	 * @throws Exception
	 *                   if an error occurs serializing the putContent, or
	 *                   connecting or sending to the destination URL
	 */
	static public String sendRESTPut(URL url, String putContent) throws Exception {
		return sendRESTPut(url, putContent, "Mozilla/5.0");
	}

	/**
	 * Sends an REST Put request ot the supplied URL with the supplied user agent
	 * and putContent
	 * 
	 * @param url
	 *                   destination URL
	 * @param putContent
	 *                   the body of the PUT request sent as a query string
	 * @param userAgent
	 *                   optional user agent (default is Mozilla/5.0 if undefined)
	 * @return response to REST request
	 * @throws Exception
	 *                   if an error occurs serializing the putContent, or
	 *                   connecting or sending to the destination URL
	 */
	static public String sendRESTPut(URL url, String putContent, String userAgent) throws Exception {

		HttpURLConnection con;
		try {
			con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("PUT");
			if (MaskerUtils.isUndefined(userAgent)) {
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
			} else {
				con.setRequestProperty("User-Agent", userAgent);
			}
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setInstanceFollowRedirects(true);
			// Send put request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(putContent);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			String response = readResponse(con); // read the data from the
																// connection input/error stream
			if (responseCode < 200 || responseCode > 299) {

				throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + "," + HTTP_RESPONSE + "=" + response);
			}
			return (response);
		} catch (Exception e1) {
			throw new Exception("Can not open connection to " + url.toExternalForm(), e1);
		}

	}

}
