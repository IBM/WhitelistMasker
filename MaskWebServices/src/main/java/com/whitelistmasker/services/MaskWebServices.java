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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.whitelistmasker.masker.MaskerUtils;
import com.whitelistmasker.services.Patch.PATCH;

@Path("/v1/")

/**
 * Mask Web Services
 *
 */
public class MaskWebServices implements Serializable {

	static public final String ACTIONS = "actions";
	static public final String ADMIN_ROLE = "MaskAdmin";
	/**
	 * Requests that change masking policy and so require the ADMIN_ROLE. This is
	 * checked here as well as by the security-constraint in web.xml so a URL
	 * variant the constraint does not match is still refused.
	 */
	static public final Set<String> ADMIN_REQUESTS = new HashSet<String>(Arrays.asList("masker/updateMasks"));
	static final boolean debug = true; // false for no _debug messages
	static public final String DELETE = "delete";
	static public final String GET = "get";
	static public final String JSON_REQUEST = "jsonRequest";
	static public final String PATCH = "patch";
	static public final String POST = "post";
	static public final String PUT = "put";
	static public final String TOPIC = "topic";
	static public final String TYPE = "type";

	@Inject
	public ServicesManager _servicesManager;

	@Context
	SecurityContext _securityContext;

	/**
	 * Refuse requests that require the ADMIN_ROLE when the caller lacks it
	 * 
	 * @param requestType
	 *                    the topic/type of the request
	 * @return an error response if the caller is not authorized, otherwise null
	 */
	Response checkAuthorized(String requestType) {
		if (ADMIN_REQUESTS.contains(requestType)
				&& (_securityContext == null || _securityContext.isUserInRole(ADMIN_ROLE) == false)) {
			return MaskServiceUtil.getErrorResponse("\"" + requestType + "\" requires the " + ADMIN_ROLE + " role",
					MaskResponseCodes.Mask_NOT_AUTHORIZED);
		}
		return null;
	}

	/**
	 * Perform deletion request
	 * 
	 * @param headers
	 *                    HTTP headers from request
	 * @param uriInfo
	 *                    URI information from request
	 * @param topic
	 *                    request topic
	 * @param type
	 *                    request type
	 * @param jsonRequest
	 *                    request body
	 * @return response
	 */
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{topic}/{type}")
	public Response doDeleteV1(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
			@PathParam(TYPE) String type, InputStream jsonRequest) {
		JSONObject request = null;
		try {
			request = JSONObject.parse(jsonRequest);
		} catch (IOException e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_INVALID_JSON_GET_REQUEST);
		}

		// based on the URI we received, parse out the reqTopic and reqType
		try {
			URI uri = uriInfo.getAbsolutePath();
			String path = uri.getPath();
			String requestType = topic;
			if (MaskerUtils.isUndefined(type) == false) {
				requestType += "/" + type;
			}
			Response notAuthorized = checkAuthorized(requestType);
			if (notAuthorized != null) {
				return notAuthorized;
			}
			JSONObject serviceLogic = (JSONObject) ServicesManager.deleteRequests.get(requestType);
			if (serviceLogic == null) {
				return MaskServiceUtil.getErrorResponse(
						"No DELETE service registered for \"" + requestType + "\" for path \"" + path + "\"",
						MaskResponseCodes.Mask_CLASS_NOT_FOUND);
			}
			// get what is to be returned
			JSONObject actionResponses = new JSONObject();
			// execute the actions
			JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
			for (Iterator<?> it = actions.iterator(); it.hasNext();) {
				JSONObject action = (JSONObject) it.next();
				ServicesManager.performAction(DELETE, request, action, actionResponses);
			}
			return MaskServiceUtil.getResponse(actionResponses);
		} catch (Exception e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_UNEXPECTED_ERROR);
		}
	}

	/**
	 * GET request
	 * 
	 * @param headers
	 *                    HTTP headers from request
	 * @param uriInfo
	 *                    URI information from request
	 * @param topic
	 *                    request topic
	 * @param jsonRequest
	 *                    request body
	 * @return response
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{topic}/{jsonRequest}")
	public Response doGetV1(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
			@PathParam(JSON_REQUEST) String jsonRequest) {
		JSONObject request = new JSONObject();
		if (jsonRequest.startsWith("{") && jsonRequest.endsWith("}")) {
			try {
				request = JSONObject.parse(jsonRequest);
			} catch (IOException e) {
				return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_INVALID_JSON_GET_REQUEST);
			}
		}

		// based on the URI we received, parse out the reqTopic and reqType
		try {
			URI uri = uriInfo.getAbsolutePath();
			String path = uri.getPath();
			JSONObject serviceLogic = (JSONObject) ServicesManager.getRequests.get(topic);
			if (serviceLogic == null) {
				return MaskServiceUtil.getErrorResponse(
						"No GET service registered for \"" + topic + "\" for path \"" + path + "\"",
						MaskResponseCodes.Mask_CLASS_NOT_FOUND);
			}
			// get what is to be returned
			JSONObject actionResponses = new JSONObject();
			// execute the actions
			JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
			for (Iterator<?> it = actions.iterator(); it.hasNext();) {
				JSONObject action = (JSONObject) it.next();
				ServicesManager.performAction(GET, request, action, actionResponses);
			}
			return MaskServiceUtil.getResponse(actionResponses);
		} catch (Exception e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_UNEXPECTED_ERROR);
		}
	}

	/**
	 * GET request
	 * 
	 * @param headers
	 *                    HTTP headers from request
	 * @param uriInfo
	 *                    URI information from request
	 * @param topic
	 *                    request topic
	 * @param type
	 *                    request type
	 * @param jsonRequest
	 *                    request body
	 * @return response
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{topic}/{type}/{jsonRequest}")
	public Response doGetV1(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
			@PathParam(TYPE) String type, @PathParam(JSON_REQUEST) String jsonRequest) {
		JSONObject request = null;
		try {
			request = JSONObject.parse(jsonRequest);
		} catch (IOException e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_INVALID_JSON_GET_REQUEST);
		}

		// based on the URI we received, parse out the reqTopic and reqType
		try {
			URI uri = uriInfo.getAbsolutePath();
			String path = uri.getPath();
			Response notAuthorized = checkAuthorized(topic + "/" + type);
			if (notAuthorized != null) {
				return notAuthorized;
			}
			JSONObject serviceLogic = (JSONObject) ServicesManager.getRequests.get(topic + "/" + type);
			if (serviceLogic == null) {
				return MaskServiceUtil.getErrorResponse(
						"No GET service registered for \"" + topic + "/" + type + "\" for path \"" + path + "\"",
						MaskResponseCodes.Mask_CLASS_NOT_FOUND);
			}
			// get what is to be returned
			JSONObject actionResponses = new JSONObject();
			// execute the actions
			JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
			for (Iterator<?> it = actions.iterator(); it.hasNext();) {
				JSONObject action = (JSONObject) it.next();
				ServicesManager.performAction(GET, request, action, actionResponses);
			}
			return MaskServiceUtil.getResponse(actionResponses);
		} catch (Exception e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_UNEXPECTED_ERROR);
		}
	}

	/**
	 * GET request
	 * 
	 * @param headers
	 *                HTTP headers from request
	 * @param uriInfo
	 *                URI information from request
	 * @param topic
	 *                request topic
	 * @return response
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{topic}")
	public Response doGetV1NoParams(@Context HttpHeaders headers, @Context UriInfo uriInfo,
			@PathParam(TOPIC) String topic) {
		JSONObject request = new JSONObject();

		// based on the URI we received, parse out the reqTopic and reqType
		try {
			URI uri = uriInfo.getAbsolutePath();
			String path = uri.getPath();
			JSONObject serviceLogic = (JSONObject) ServicesManager.getRequests.get(topic);
			if (serviceLogic == null) {
				return MaskServiceUtil.getErrorResponse(
						"No GET service registered for \"" + topic + "\" for path \"" + path + "\"",
						MaskResponseCodes.Mask_CLASS_NOT_FOUND);
			}
			// get what is to be returned
			JSONObject actionResponses = new JSONObject();
			// execute the actions
			JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
			for (Iterator<?> it = actions.iterator(); it.hasNext();) {
				JSONObject action = (JSONObject) it.next();
				ServicesManager.performAction(GET, request, action, actionResponses);
			}
			return MaskServiceUtil.getResponse(actionResponses);
		} catch (Exception e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_UNEXPECTED_ERROR);
		}
	}

	/**
	 * PATCH request
	 * 
	 * @param headers
	 *                    HTTP headers from request
	 * @param uriInfo
	 *                    URI information from request
	 * @param topic
	 *                    request topic
	 * @param type
	 *                    request type
	 * @param jsonRequest
	 *                    request body
	 * @return response
	 */
	@PATCH
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{topic}/{type}")
	public Response doPatchV1(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
			@PathParam(TYPE) String type, InputStream jsonRequest) {
		JSONObject request = null;
		try {
			request = JSONObject.parse(jsonRequest);
		} catch (IOException e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_INVALID_JSON_GET_REQUEST);
		}

		// based on the URL we received, parse out the reqTopic and reqType
		try {
			URI uri = uriInfo.getAbsolutePath();
			String path = uri.getPath();
			String requestType = topic;
			if (MaskerUtils.isUndefined(type) == false) {
				requestType += "/" + type;
			}
			Response notAuthorized = checkAuthorized(requestType);
			if (notAuthorized != null) {
				return notAuthorized;
			}
			JSONObject serviceLogic = (JSONObject) ServicesManager.putRequests.get(requestType);
			if (serviceLogic == null) {
				return MaskServiceUtil.getErrorResponse(
						"No PATCH service registered for \"" + requestType + "\" for path \"" + path + "\"",
						MaskResponseCodes.Mask_CLASS_NOT_FOUND);
			}
			// get what is to be returned
			JSONObject actionResponses = new JSONObject();
			// execute the actions
			JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
			for (Iterator<?> it = actions.iterator(); it.hasNext();) {
				JSONObject action = (JSONObject) it.next();
				ServicesManager.performAction(PATCH, request, action, actionResponses);
			}
			return MaskServiceUtil.getResponse(actionResponses);
		} catch (Exception e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_UNEXPECTED_ERROR);
		}
	}

	/**
	 * POST request
	 * 
	 * @param headers
	 *                    HTTP headers from request
	 * @param uriInfo
	 *                    URI information from request
	 * @param topic
	 *                    request topic
	 * @param type
	 *                    request type
	 * @param jsonRequest
	 *                    request body
	 * @return response
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{topic}/{type}")
	public Response doPostV1(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
			@PathParam(TYPE) String type, InputStream jsonRequest) {
		JSONObject request = null;
		try {
			request = JSONObject.parse(jsonRequest);
		} catch (IOException e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_INVALID_JSON_GET_REQUEST);
		}

		// based on the URI we received, parse out the reqTopic and reqType
		try {
			URI uri = uriInfo.getAbsolutePath();
			String path = uri.getPath();
			String requestType = topic;
			if (MaskerUtils.isUndefined(type) == false) {
				requestType += "/" + type;
			}
			Response notAuthorized = checkAuthorized(requestType);
			if (notAuthorized != null) {
				return notAuthorized;
			}
			JSONObject serviceLogic = (JSONObject) ServicesManager.postRequests.get(requestType);
			if (serviceLogic == null) {
				return MaskServiceUtil.getErrorResponse(
						"No POST service registered for \"" + requestType + "\" for path \"" + path + "\"",
						MaskResponseCodes.Mask_CLASS_NOT_FOUND);
			}
			// get what is to be returned
			JSONObject actionResponses = new JSONObject();
			// execute the actions
			JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
			for (Iterator<?> it = actions.iterator(); it.hasNext();) {
				JSONObject action = (JSONObject) it.next();
				ServicesManager.performAction(POST, request, action, actionResponses);
			}
			return MaskServiceUtil.getResponse(actionResponses);
		} catch (Exception e) {
			if (debug) {
				e.printStackTrace(System.err);
			}
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_UNEXPECTED_ERROR);
		}
	}

	/**
	 * PUT request
	 * 
	 * @param headers
	 *                    HTTP headers from request
	 * @param uriInfo
	 *                    URI information from request
	 * @param topic
	 *                    request topic
	 * @param type
	 *                    request type
	 * @param jsonRequest
	 *                    request body
	 * @return response
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{topic}/{type}")
	public Response doPutV1(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
			@PathParam(TYPE) String type, InputStream jsonRequest) {
		JSONObject request = null;
		try {
			request = JSONObject.parse(jsonRequest);
		} catch (IOException e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_INVALID_JSON_GET_REQUEST);
		}

		// based on the URI we received, parse out the reqTopic and reqType
		try {
			URI uri = uriInfo.getAbsolutePath();
			String path = uri.getPath();
			String requestType = topic;
			if (MaskerUtils.isUndefined(type) == false) {
				requestType += "/" + type;
			}
			Response notAuthorized = checkAuthorized(requestType);
			if (notAuthorized != null) {
				return notAuthorized;
			}
			JSONObject serviceLogic = (JSONObject) ServicesManager.putRequests.get(requestType);
			if (serviceLogic == null) {
				return MaskServiceUtil.getErrorResponse(
						"No PUT service registered for \"" + requestType + "\" for path \"" + path + "\"",
						MaskResponseCodes.Mask_CLASS_NOT_FOUND);
			}
			// get what is to be returned
			JSONObject actionResponses = new JSONObject();
			// execute the actions
			JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
			for (Iterator<?> it = actions.iterator(); it.hasNext();) {
				JSONObject action = (JSONObject) it.next();
				ServicesManager.performAction(PUT, request, action, actionResponses);
			}
			return MaskServiceUtil.getResponse(actionResponses);
		} catch (Exception e) {
			return MaskServiceUtil.getErrorResponse(e, MaskResponseCodes.Mask_UNEXPECTED_ERROR);
		}
	}
}
