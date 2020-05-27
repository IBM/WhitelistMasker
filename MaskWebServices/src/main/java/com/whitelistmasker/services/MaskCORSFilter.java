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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter to allow cross origin requests
 */
public class MaskCORSFilter implements Filter {

	static boolean _debug = false;
	private final Logger log = LoggerFactory.getLogger(MaskCORSFilter.class);

	/**
	 * Constructor
	 */
	public MaskCORSFilter() {
		if (_debug) {
			log.info("MaskerCORSFilter init");
		}
	}

	/**
	 * Destruction processing
	 */
	@Override
	public void destroy() {
	}

	/**
	 * Filter to add cross origin headers to the response
	 * 
	 * @param req
	 *              servlet request
	 * @param res
	 *              servlet response
	 * @param chain
	 *              filter chain
	 * @throws IOException,
	 *                      ServletException
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
		response.setHeader("Vary", "Origin");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
		response.setHeader("Access-Control-Max-Age", "3600");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With, remember-me");
		chain.doFilter(req, res);
	}

	/**
	 * Initialize using filter configuration
	 * 
	 * @param filterConfig
	 *                     filter configuration
	 */
	@Override
	public void init(FilterConfig filterConfig) {
	}

}
