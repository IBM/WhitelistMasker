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
import java.io.Serializable;
import java.util.Date;

/**
 * Common constant values used by other methods
 *
 */
public class MaskerConstants implements Serializable {

	private static final long serialVersionUID = -1702516740588125459L;

	/**
	 * Equal to "", this is used to initialize values, typically used for optionally
	 * present values (e.g., descriptions, notes, long names).
	 */
	static public final String EMPTY_String = "";

	static public final String Masker_DIR_PROPERTIES = "properties" + File.separator;

	static public final String schemaFileName = "Masker_RESTServicesSchema.json";

	/**
	 * Equal to one millisecond after the epoch date (midnight 1/1/1970
	 * 00:00:00.001), this is used to identify and/or initialize a Date value that
	 * has yet to receive a valid value. Typically used for comparisons to glean if
	 * the value has been set properly.
	 */
	static public final Date UNDEFINED_Date = new Date(1);

	/**
	 * Equal to a question mark, this is used to identify and/or initialize a String
	 * that has yet to receive a valid value. Typically used for comparisons to
	 * glean if the value has been set properly.
	 */
	static public final String UNDEFINED_String = "?";

	/**
	 * Constructor
	 */
	public MaskerConstants() {
	}

}
