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

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

/**
 * Mask Web Services Application
 * 
 */
public class MaskServicesApplication extends Application {

	private Set<Class<?>> classes = new HashSet<Class<?>>();
	private Set<Object> singletons = new HashSet<Object>();

	/**
	 * Constructor
	 */
	public MaskServicesApplication() {
		singletons.add(new MaskWebServices());
	}

	/**
	 * Setter to add this class to the set of classes
	 * 
	 * @return updated set of classes
	 */
	public Set<Class<?>> getClasses() {
		classes.add(MaskWebServices.class);
		return classes;
	}

	/**
	 * Getter for singletons
	 * 
	 * @return set of singletons
	 * 
	 */
	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}

}
