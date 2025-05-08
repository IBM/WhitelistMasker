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

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Special Masker Date object extends java.util.Date with special constructors
 * taking Masker specific date formatted strings. The toString implementation
 * provides a formatted date suitable for sorting in the format
 * "yyyy/MM/dd-HH:mm:ss.SSS(Z)"
 */
public class MaskerDate extends Date {

	/**
	 * Format for parsing the create date string: 2004/08/04-07:23:15.987(-0400)
	 */
	static public String CREATE_DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss.SSS(Z)";

	/**
	 * Format for parsing the create date string: 2004/08/04-07:23:15.987 which is
	 * assumed to be 0000 (Greenwich Mean Time).
	 */
	static public String CREATE_DATE_FORMAT_0 = "yyyy/MM/dd-HH:mm:ss.SSS";

	/**
	 * Format for parsing the create date string: 2004/08/04-07:23:15 which is
	 * assumed to be 0000 (Greenwich Mean Time) with 0 milliseconds.
	 */
	static public String CREATE_DATE_FORMAT_1 = "yyyy/MM/dd-HH:mm:ss";

	/**
	 * Format for parsing the create date string: 2004/08/04 07:23:15.987123 which
	 * is assumed to be 0000 (Greenwich Mean Time).
	 */
	static public String CREATE_DATE_FORMAT_10 = "yyyy/MM/dd HH:mm:ss.SSSSSS";

	/**
	 * Format for parsing the create date string: Monday, 22 Sep 1959 08:12:34 which
	 * is assumed to be UTC (GMT)
	 */
	static public String CREATE_DATE_FORMAT_11 = "E, dd MMM yyyy HH:mm:ss";

	/**
	 * Format for parsing the create date string: January 05 2018 00:44:45 which is
	 * assumed to be UTC (GMT)
	 */
	static public String CREATE_DATE_FORMAT_12 = "MMMM dd yyyy HH:mm:ss";
	/**
	 * Format for parsing the create date string: 2004/08/04-07:23 which is assumed
	 * to be 0000 (Greenwich Mean Time) with 0 seconds and milliseconds.
	 */
	static public String CREATE_DATE_FORMAT_2 = "yyyy/MM/dd-HH:mm";

	/**
	 * Format for parsing the create date string: 2004/08/04-07 which is assumed to
	 * be 0000 (Greenwich Mean Time) with 0 minutes, seconds and milliseconds.
	 */
	static public String CREATE_DATE_FORMAT_3 = "yyyy/MM/dd-HH";

	/**
	 * Format for parsing the create date string: 2004/08/04 which is assumed to be
	 * 0000 (Greenwich Mean Time) at midnight.
	 */
	static public String CREATE_DATE_FORMAT_4 = "yyyy/MM/dd";

	/**
	 * Format for parsing the create date string: 2004/08/04-07:23:15(-0400) which
	 * is assumed to be -0400 (Eastern Daylight Time) with 0 milliseconds.
	 */
	static public String CREATE_DATE_FORMAT_5 = "yyyy/MM/dd-HH:mm:ss(Z)";

	/**
	 * Format for parsing the create date string: 2004/08/04-07:23(-0400) which is
	 * assumed to be -0400 (Eastern Daylight Time) with 0 seconds and milliseconds.
	 */
	static public String CREATE_DATE_FORMAT_6 = "yyyy/MM/dd-HH:mm(Z)";

	/**
	 * Format for parsing the create date string: 2004/08/04-07(-0400) which is
	 * assumed to be -0400 (Eastern Daylight Time) with 0 minutes, seconds and
	 * milliseconds.
	 */
	static public String CREATE_DATE_FORMAT_7 = "yyyy/MM/dd-HH(Z)";

	/**
	 * Format for parsing the create date string: 2004/08/04(-0400) which is assumed
	 * to be -0400 (Eastern Daylight Time) at midnight.
	 */
	static public String CREATE_DATE_FORMAT_8 = "yyyy/MM/dd(Z)";

	/**
	 * Format for parsing the create date string: 2004/08/04T07Z which is assumed to
	 * be 07:00:00.000 Zulu (UTC) Time.
	 */
	static public String CREATE_DATE_FORMAT_9 = "yyyy/MM/ddTHH";

	// --------
	// statics
	// --------
	static public String m_strClassName = MaskerUtils.getNameFromClass(MaskerDate.class);

	static private final long serialVersionUID = -1235455818943814022L;

	/**
	 * Number of milliseconds past the 1/1/70 midnight epoch used to define an
	 * undefined date
	 */
	static final public long UNDEFINED_MaskerDate_Milliseconds = 1L;

	/**
	 * An undefined version of this object. The undefined version is the epoch date
	 * + 1 created by new MaskerDate(1).
	 */
	static final public MaskerDate UNDEFINED_MaskerDate = new MaskerDate(UNDEFINED_MaskerDate_Milliseconds);

	/**
	 * Intended to be equal to "1970-01-01 00:00:00.000001". That should be the
	 * result obtained from the calculation used here. In any case, that calculation
	 * should reflect the ways in which the elements involved are related. Note:
	 * This form of string for a timestamp can inserted via JDBC into a DB2 field of
	 * type Timestamp, but it requires a value that extends to six decimal places
	 * for seconds. Values shorter than that may be padded, but that may lead to
	 * warnings, if not errors, when loading data (and may complicate other things,
	 * as well).
	 */
	static public final String UNDEFINED_Masker_DB2_Date_String = new MaskerDate(UNDEFINED_MaskerDate)
			.toStringDBTimestamp();

	/**
	 * Converts a String in xs:dateTime format to MaskerDate format
	 * 
	 * @param strDateTime
	 *                    String in xs:dateTime format
	 * @return String in MaskerDate format
	 * @see #CREATE_DATE_FORMAT_0
	 * @see #CREATE_DATE_FORMAT_1
	 * @see #CREATE_DATE_FORMAT_2
	 * @see #CREATE_DATE_FORMAT_3
	 * @see #CREATE_DATE_FORMAT_4
	 * @see #CREATE_DATE_FORMAT_5
	 * @see #CREATE_DATE_FORMAT_6
	 * @see #CREATE_DATE_FORMAT_7
	 * @see #CREATE_DATE_FORMAT_8
	 * @see #CREATE_DATE_FORMAT_9
	 * @see #CREATE_DATE_FORMAT_10
	 * @see #CREATE_DATE_FORMAT_11
	 * @see #CREATE_DATE_FORMAT_12
	 */
	static public String convertDateTime(String strDateTime) {
		if (MaskerUtils.isUndefined(strDateTime)) {
			return UNDEFINED_MaskerDate.toString();
		}
		// try simple transforms
		// 2006-08-04T07:15:23 to 2006/08/04T07:15:23
		strDateTime = strDateTime.replace('-', '/');
		// 2006/08/04-07:15:23
		strDateTime = strDateTime.replace('T', '-');
		if (strDateTime.length() == 11) {
			strDateTime = strDateTime.substring(0, 10);
		}
		if (strDateTime.toUpperCase().endsWith("Z") == false) {
			strDateTime = strDateTime + "(+0000)";
		} else {
			strDateTime = strDateTime.substring(0, strDateTime.length() - 1) + "(+0000)";
		}
		return strDateTime;
	}

	/*
	 * @see com.whitelistmasker.masker.MaskerDuration#elapsedTime(Date, Date)
	 */
	static public long elapsedTime(MaskerDate dateStart, MaskerDate dateEnd) {
		return MaskerDuration.elapsedTime(dateStart, dateEnd);
	}

	/**
	 * @return a String of hexadecimal characters representing the current time in
	 *         milliseconds. This can be used as a name for temporary assets.
	 */
	static public String getHexTime() {
		return Long.toHexString(new MaskerDate().getTime());
	}

	/**
	 * Return a list of timezone values. Each timezone value is followed by the list
	 * of timezones to which it relates.
	 * 
	 * @return an array of timezone values followed by a space followed by the list
	 *         of timezones to which the value applies.
	 */
	static public String[] getShortTimeZoneList() {
		return getShortTimeZoneList(null);
	}

	/**
	 * Return a list of timezone values associated with the supplied prefix. Each
	 * timezone value is followed by the list of timezones to which it relates.
	 * 
	 * @param strPrefix
	 *                  the prefix to be used as a filter on TimeZones
	 * @return an array of timezone values followed by a space followed by the list
	 *         of timezones to which the value applies.
	 */
	static public String[] getShortTimeZoneList(String strPrefix) {
		String[] strTimeZones = getTimeZoneList(strPrefix);
		// map all timezones to a list of descriptors following their numeric
		// value
		String strTZValue = null;
		HashMap<String, ArrayList<String>> mapTZValues = new HashMap<String, ArrayList<String>>();
		ArrayList<String> listDescriptions = new ArrayList<String>();
		for (int i = 0; i < strTimeZones.length; i++) {
			strTZValue = strTimeZones[i].substring(0, 5);
			listDescriptions = mapTZValues.get(strTZValue);
			if (listDescriptions == null) {
				listDescriptions = new ArrayList<String>();
				listDescriptions.add(strTimeZones[i].substring(6));
				mapTZValues.put(strTZValue, listDescriptions);
			} else {
				listDescriptions.add(strTimeZones[i].substring(6));
			}
		}
		Vector<String> vectKeys = new Vector<String>(mapTZValues.keySet());
		Collections.sort(vectKeys);
		String[] output = new String[vectKeys.size()];
		for (int i = 0; i < output.length; i++) {
			output[i] = vectKeys.elementAt(i);
			output[i] = output[i] + " " + MaskerUtils.arrayListToListString(mapTZValues.get(vectKeys.elementAt(i)));
		}
		return output;
	}

	/**
	 * @return a sorted array of Strings containing the TimeZone identifiers,
	 *         followed by the timezone offset from GMT enclosed in parentheses. For
	 *         example, there is an entry "America/New_York (-0500)" as well as "EST
	 *         (-0500)".
	 */
	static public String[] getTimeZoneList() {
		return getTimeZoneList(null);
	}

	/**
	 * @param strPrefix
	 *                  the prefix to be compared against the timezone ID's as a
	 *                  filter. If the timezone id starts with the supplied prefix
	 *                  it will be included in the returned list. If the prefix is
	 *                  null or empty then all ID's will be returned.
	 * @return a sorted array of Strings containing the timezone offset from GMT
	 *         followed by a space then the TimeZone ID. For example, there is an
	 *         entry "-0500 America/New_York" as well as "-0500 EST" as well as
	 *         "+0000 GMT" and "+0100 Europe/Paris".
	 */
	static public String[] getTimeZoneList(String strPrefix) {
		String[] timezones = TimeZone.getAvailableIDs();
		ArrayList<String> outputList = new ArrayList<String>();
		for (int i = 0; i < timezones.length; i++) {
			TimeZone tz = TimeZone.getTimeZone(timezones[i]);
			if (strPrefix != null && strPrefix.length() != 0) {
				if (timezones[i].startsWith(strPrefix) == false) {
					continue;
				}
			}
			outputList.add(MaskerUtils.convertMillisecondsToTimeZone(tz.getRawOffset()) + " " + timezones[i]);
		}
		String[] outputStrings = new String[outputList.size()];
		outputStrings = (String[]) outputList.toArray(outputStrings);
		Arrays.sort(outputStrings);
		return outputStrings;
	}

	/**
	 * Tests for equality to the UNDEFINED_MaskerDate which is one millisecond after
	 * the epoch date (midnight 1/1/1970 GMT). This is used to identify and/or
	 * initialize an MaskerDate value that has yet to receive a valid value.
	 * Typically used for comparisons to glean if the value has been set properly.
	 * 
	 * @param date
	 *             the date to be tested to see if it is undefined
	 * @return whether (true) or not (false) the supplied date is undefined (meaning
	 *         it is null or its {@link #getTime()} returns a value of
	 *         {@value #UNDEFINED_MaskerDate_Milliseconds}
	 * @see #isUndefined(MaskerDate)
	 */
	static public boolean isUndefined(MaskerDate date) {
		if (date == null) {
			return true;
		}
		/**
		 * Note: this should be == but due to an error some dates meant to be undefined
		 * ended up with 0 milliseconds.
		 */
		return (date.getTime() <= UNDEFINED_MaskerDate_Milliseconds);
	}

	/**
	 * Tests for equality to the UNDEFINED_MaskerDate which is one millisecond after
	 * the epoch date (midnight 1/1/1970 GMT). This is used to identify and/or
	 * initialize an MaskerDate value that has yet to receive a valid value.
	 * Typically used for comparisons to glean if the value has been set properly.
	 * 
	 * @param date
	 *             the date to be tested to see if it is undefined
	 * @return whether (true) or not (false) the supplied date is undefined (meaning
	 *         it is null or its {@link #getTime()} returns a value of
	 *         {@value #UNDEFINED_MaskerDate_Milliseconds}
	 * @see #isUndefined(MaskerDate)
	 */
	static public boolean isUndefined(Date date) {
		if (date == null) {
			return true;
		}
		/**
		 * Note: this should be == but due to an error some dates meant to be undefined
		 * ended up with 0 milliseconds.
		 */
		return (date.getTime() <= UNDEFINED_MaskerDate_Milliseconds);
	}

	static public void main(String[] args) {
		System.out.println("Undefined Date is: " + UNDEFINED_MaskerDate.toStringDBTimestamp());
		MaskerDate now = new MaskerDate();
		System.out.println("Today's Date is: " + now.toStringDateTime() + " (" + now.getTime() + ")");
		while (true) {
			String strDate = MaskerUtils.prompt("Enter date to be converted (or Q to quit): ");
			if (MaskerUtils.isUndefined(strDate)) {
				strDate = new MaskerDate().toString();
			}
			if ("Q".equalsIgnoreCase(strDate.substring(0, 1))) {
				return;
			}
			MaskerDate date = new MaskerDate();
			try {
				// did we get a timestamp?
				try {
					long time = Long.valueOf(strDate);
					date = new MaskerDate(time);
				} catch (Exception e) {
					date = new MaskerDate(strDate);
				}
				System.out.println("Timestamp: " + date.toStringDBTimestamp());
				System.out.println("xs:datetime: " + date.toStringDateTime());
				System.out.println("Default: " + date.toString());
				System.out.println("EpochMS: " + date.getTime());
				System.out.println(date.toString(MaskerDate.CREATE_DATE_FORMAT_11, "GMT"));
				System.out.println(date.toString(MaskerDate.CREATE_DATE_FORMAT_12, "GMT"));

			} catch (Exception ber) {
				ber.printStackTrace();
			}
		}
	}

	/**
	 * General purpose Date generator that parses multiple formats, supplying
	 * default values if they are unspecified.
	 * 
	 * @param strDate
	 *                String containing the date information in one of the Masker
	 *                date formats
	 * @return date object initialized to the date specified in the passed string.
	 * @throws Exception
	 *                   if the passed string doesn't match one of the Masker date
	 *                   formats
	 * @throws Exception
	 * @see #CREATE_DATE_FORMAT
	 * @see #CREATE_DATE_FORMAT_0
	 * @see #CREATE_DATE_FORMAT_1
	 * @see #CREATE_DATE_FORMAT_2
	 * @see #CREATE_DATE_FORMAT_3
	 * @see #CREATE_DATE_FORMAT_4
	 * @see #CREATE_DATE_FORMAT_5
	 * @see #CREATE_DATE_FORMAT_6
	 * @see #CREATE_DATE_FORMAT_7
	 * @see #CREATE_DATE_FORMAT_8
	 * @see #CREATE_DATE_FORMAT_9
	 * @see #CREATE_DATE_FORMAT_10
	 * @see #CREATE_DATE_FORMAT_11
	 * @see #CREATE_DATE_FORMAT_12
	 */
	static synchronized public Date makeDate(String strDate) throws Exception {
		if (strDate == null || strDate.length() == 0) {
			return new Date();
		}
		// check for odd formats first
		String[] parts = strDate.split(" ");
		if (parts.length == 4) {
			// rebuild strDate as an xs:DateTime format yyyy:MM:ddTHH:mm:ss.000
			String newDate = parts[2].trim() + "/";
			// format is Month DD yyyy HH:mm:ss (#12)
			switch (parts[0].trim().toLowerCase()) {
			case "january": {
				newDate += "01/";
				break;
			}
			case "february": {
				newDate += "02/";
				break;
			}
			case "march": {
				newDate += "03/";
				break;
			}
			case "april": {
				newDate += "04/";
				break;
			}
			case "may": {
				newDate += "05/";
				break;
			}
			case "june": {
				newDate += "06/";
				break;
			}
			case "july": {
				newDate += "07/";
				break;
			}
			case "august": {
				newDate += "08/";
				break;
			}
			case "september": {
				newDate += "09/";
				break;
			}
			case "october": {
				newDate += "10/";
				break;
			}
			case "november": {
				newDate += "11/";
				break;
			}
			case "december": {
				newDate += "12/";
				break;
			}
			default: {
				throw new Exception("Unrecognized month \"" + parts[2] + "\" in input " + strDate);
			}
			} // end switch on month
			try {
				int day = Integer.valueOf(parts[1].trim());
				newDate += MaskerUtils.padLeftZero(day, 2);
				newDate += "T";
			} catch (NumberFormatException nfe) {
				throw new Exception("Unrecognized day of month \"" + parts[1] + "\" in input " + strDate);
			}
			if (parts[3].trim().length() != 8) {
				throw new Exception("Unrecognized time \"" + parts[3] + "\" in input " + strDate);
			}
			newDate += parts[3].trim();
			strDate = newDate;
		} else if (parts.length == 5) {
			// Mon, 11 Jun 2019 00:43:45
			String newDate = parts[3] + "/";
			switch (parts[2].trim().toLowerCase()) {
			case "jan": {
				newDate += "01/";
				break;
			}
			case "feb": {
				newDate += "02/";
				break;
			}
			case "mar": {
				newDate += "03/";
				break;
			}
			case "apr": {
				newDate += "04/";
				break;
			}
			case "may": {
				newDate += "05/";
				break;
			}
			case "jun": {
				newDate += "06/";
				break;
			}
			case "jul": {
				newDate += "07/";
				break;
			}
			case "aug": {
				newDate += "08/";
				break;
			}
			case "sep": {
				newDate += "09/";
				break;
			}
			case "oct": {
				newDate += "10/";
				break;
			}
			case "nov": {
				newDate += "11/";
				break;
			}
			case "dec": {
				newDate += "12/";
				break;
			}
			default: {
				throw new Exception("Unrecognized month \"" + parts[3] + "\" in input " + strDate);
			}
			} // end switch on month
			try {
				int day = Integer.valueOf(parts[1].trim());
				newDate += MaskerUtils.padLeftZero(day, 2);
				newDate += "T";
			} catch (NumberFormatException nfe) {
				throw new Exception("Unrecognized day of month \"" + parts[1] + "\" in input " + strDate);
			}
			if (parts[4].trim().length() != 8) {
				throw new Exception("Unrecognized time \"" + parts[4] + "\" in input " + strDate);
			}
			newDate += parts[4].trim();
			strDate = newDate;
		}

		// check to see if the date has a database Timestamp format
		int iSpaceOffset = strDate.indexOf(" ");
		if (iSpaceOffset == 10) {
			// make it look like xs:dateTime format
			strDate = strDate.replace(' ', 'T');
		}
		// check to see if the date has an xs:dateTime format
		int iTOffset = strDate.indexOf("T");
		if (iTOffset == 10) {
			strDate = convertDateTime(strDate);
		}
		int iHOffset = strDate.indexOf("-");
		if (iHOffset > 0 && iHOffset <= 9) {
			strDate = strDate.replace('-', '/');
		}
		if (strDate.length() == 10) {
			strDate = strDate + "-00:00:00.000(+0000)";
		}
		int iZOffset = strDate.toUpperCase().indexOf("Z");
		if (iZOffset > 0) {
			strDate = strDate.substring(0, iZOffset) + "(+0000)";
		}
		// try to determine the entered format
		int iTZOffset = strDate.indexOf("(");
		// if -1 use #0 only yyyy/MM/dd-HH:mm:ss.SSS
		int iMSOffset = strDate.indexOf(".");
		// if -1 use #1 only yyyy/MM/dd-HH:mm:ss
		// Note: below added to fix up dates with more than 3 decimal precision
		if (iMSOffset > 0) {
			if (iTZOffset > 0) {
				// have ss.SSSS( or more
				if (iTZOffset - iMSOffset > 4) {
					String strMS = strDate.substring(iMSOffset + 1, iTZOffset);
					int iMSLen = strMS.length();
					if (iMSLen > 3) {
						strMS = strMS.substring(0, 3) + "." + strMS.substring(3);
					}
					int imsecs = Math.round(Float.valueOf(strMS));
					strMS = MaskerUtils.padRight(imsecs, 3, '0');
					// rebuild the date string with the 3 digits
					String strTZ = strDate.substring(iTZOffset);
					strDate = strDate.substring(0, iMSOffset + 1) + strMS + strTZ;
					iTZOffset = strDate.indexOf("(");
				} else { // 3 or less decimals + timezone
					// rebuild the date string with the 3 digits
					String strTZ = strDate.substring(iTZOffset);
					String strMS = strDate.substring(iMSOffset + 1, iTZOffset);
					strMS = MaskerUtils.padRight(strMS, 3, '0');
					strDate = strDate.substring(0, iMSOffset + 1) + strMS + strTZ;
				}
			} else { // no timezone
				if (strDate.length() - iMSOffset > 3) {
					// have ss.SSSS or more
					String strMS = strDate.substring(iMSOffset + 1);
					strMS = strMS.substring(0, 3) + "." + strMS.substring(3);
					int imsecs = Math.round(Float.valueOf(strMS));
					strMS = MaskerUtils.padRight(imsecs, 3, '0');
					// rebuild the date string with the 3 digits
					strDate = strDate.substring(0, iMSOffset) + strMS;
				} else { // have 3 or less decimals
					String strMS = strDate.substring(iMSOffset + 1);
					strMS = MaskerUtils.padRight(strMS, 3, '0');
					strDate = strDate.substring(0, iMSOffset + 1) + strMS;
				}
			}
		}
		int iMinOffset = strDate.indexOf(":"); // if -1 use #3 only
		// yyyy/MM/dd-HH
		int iSecOffset = strDate.lastIndexOf(":");
		// if -1 use #3 only yyyy/MM/dd-HH
		int iTimeOffset = strDate.indexOf("-"); // if -1 use #4 only yyyy/MM/dd
		if ((iTimeOffset > iTZOffset) && (iTZOffset != -1)) {
			iTimeOffset = -1; // handle case where TimeZone is -xxxx format
		}

		if (strDate.length() >= 10) {
			String[] dateParts = strDate.substring(0, 10).split("/");
			if (dateParts.length >= 3) {
				for (int i = 0; i < 3; i++) {
					try {
						int test = Integer.parseInt(dateParts[i]);
						switch (i) {
						case 0: { // yyyy
							if (test < 1900 || test > 2500) {
								throw new Exception("Invalid year. Must be between 1900 and 2500 inclusive. Received: \""
										+ dateParts[i] + "\"");
							}
							break;
						}
						case 1: { // MM
							if (test < 1 || test > 12) {
								throw new Exception(
										"Invalid month. Must be between 1 and 12 inclusive. Received: \"" + dateParts[i] + "\"");
							}
							break;
						}
						case 2: { // dd
							if (test < 1 || test > 31) {
								throw new Exception(
										"Invalid day. Must be between 1 and 31 (depending on the month and leap year) inclusive. Received: \""
												+ dateParts[i] + "\"");
							}
							break;
						}
						}
					} catch (Exception e) {
						throw new Exception("Date contains \"" + dateParts[i] + "\"", e);
					}
				}
			}
		}
		if (strDate.length() > 11) {
			String time = strDate.substring(11);
			if (time.length() > 8) {
				time = time.substring(0, 8);
			}
			String timeParts[] = time.split(":");
			if (timeParts.length >= 3) {
				for (int i = 0; i < 3; i++) {
					try {
						int test = Integer.parseInt(timeParts[i]);
						switch (i) {
						case 0: { // hh
							if (test < 0 || test > 23) {
								throw new Exception(
										"Invalid hour. Must be between 1 and 23 inclusive. Received: \"" + timeParts[i] + "\"");
							}
							break;
						}
						case 1: { // mm
							if (test < 0 || test > 59) {
								throw new Exception("Invalid minutes. Must be between 0 and 59 inclusive. Received: \""
										+ timeParts[i] + "\"");
							}
							break;
						}
						case 2: { // ss
							if (test < 0 || test > 59) {
								throw new Exception(
										"Invalid day. Must be between 0 and 59 inclusive. Received: \"" + timeParts[i] + "\"");
							}
							break;
						}
						}
					} catch (Exception e) {
						throw new Exception("Date contains \"" + timeParts[i] + "\"", e);
					}
				}
			}
		}
		try {
			if (iTimeOffset == -1) {
				SimpleDateFormat sdFormat = null;
				if (iTZOffset == -1) {
					sdFormat = new SimpleDateFormat(CREATE_DATE_FORMAT_4, new DateFormatSymbols());
					sdFormat.setTimeZone(TimeZone.getDefault());
					// TimeZone.getTimeZone("+0000"));
				} else {
					sdFormat = new SimpleDateFormat(CREATE_DATE_FORMAT_8, new DateFormatSymbols());
				}
				return sdFormat.parse(strDate);
			} else if (iSecOffset == -1) {
				SimpleDateFormat sdFormat = null;
				if (iTZOffset == -1) {
					sdFormat = new SimpleDateFormat(CREATE_DATE_FORMAT_3, new DateFormatSymbols());
					sdFormat.setTimeZone(TimeZone.getDefault());
					// TimeZone.getTimeZone("+0000"));
				} else {
					sdFormat = new SimpleDateFormat(CREATE_DATE_FORMAT_7, new DateFormatSymbols());
				}
				return sdFormat.parse(strDate);
			} else if (iSecOffset == iMinOffset) { // only on ":" so
				// yyyy/MM/dd-HH:mm use #2
				SimpleDateFormat sdFormat = null;
				if (iTZOffset == -1) {
					sdFormat = new SimpleDateFormat(CREATE_DATE_FORMAT_2, new DateFormatSymbols());
					sdFormat.setTimeZone(TimeZone.getDefault());
					// TimeZone.getTimeZone("+0000"));
				} else {
					sdFormat = new SimpleDateFormat(CREATE_DATE_FORMAT_6, new DateFormatSymbols());
				}
				return sdFormat.parse(strDate);
			} else if (iMSOffset == -1) {
				SimpleDateFormat sdFormat = null;
				if (iTZOffset == -1) {
					sdFormat = new SimpleDateFormat(CREATE_DATE_FORMAT_1, new DateFormatSymbols());
					sdFormat.setTimeZone(TimeZone.getDefault());
					// TimeZone.getTimeZone("+0000"));
				} else {
					sdFormat = new SimpleDateFormat(CREATE_DATE_FORMAT_5, new DateFormatSymbols());
				}
				return sdFormat.parse(strDate);
			} else if (iTZOffset == -1) {
				SimpleDateFormat sdFormat = new SimpleDateFormat(CREATE_DATE_FORMAT_0, new DateFormatSymbols());
				sdFormat.setTimeZone(TimeZone.getDefault());
				// TimeZone.getTimeZone("+0000"));
				return sdFormat.parse(strDate);
			}
			// else assumes full string is present
			return new SimpleDateFormat(CREATE_DATE_FORMAT, new DateFormatSymbols()).parse(strDate);
		} catch (ParseException e) {
			throw new Exception(
					"Date \"" + strDate + "\" does not parse according to the format \"" + CREATE_DATE_FORMAT + "\"");
		}
	}

	/**
	 * Create a formatted string showing the date in the Masker date format for 0000
	 * (Greenwich Mean Time).
	 * 
	 * @param date
	 *                    the date to be transformed into the string.
	 * @param strFormat
	 *                    the date format to be returned. If the format is
	 *                    illdefined, the standard Masker Date format is used
	 *                    (CREATE_DATE_FORMAT).
	 * @param strTimeZone
	 *                    the format of the timezone using +/-hhmm
	 * @return a formatted string showing the local date in the Masker date format
	 * @see #CREATE_DATE_FORMAT
	 */
	static synchronized public String makeFormattedDate(Date date, String strFormat, String strTimeZone) {
		int iTZMillisecs = MaskerUtils.convertTimeZoneToMilliseconds(strTimeZone);
		// TimeZone tz = TimeZone.getTimeZone(strTimeZone);
		SimpleDateFormat dateformat;
		try {
			dateformat = new SimpleDateFormat(strFormat, new DateFormatSymbols());
		} catch (Exception e) {
			dateformat = new SimpleDateFormat(CREATE_DATE_FORMAT, new DateFormatSymbols());
		}
		dateformat.setTimeZone(new SimpleTimeZone(iTZMillisecs, strTimeZone));
		return dateformat.format(date);
	}

	/**
	 * Create a formatted string showing the local date in the Masker date format
	 * 
	 * @param date
	 *             the date to be transformed into the string.
	 * @return a formatted string showing the local date in the Masker date format
	 * @see #CREATE_DATE_FORMAT
	 */
	static synchronized public String makeFormattedLocalDate(Date date) {
		return new SimpleDateFormat(CREATE_DATE_FORMAT, new DateFormatSymbols()).format(date);
	}

	static public long MAX_TIME() {
		return 253402214399999L;
	}

	/**
	 * @return the largest date that matches the Masker Date Format for any
	 *         timezone. The value is the date "9999/12/30-23:59:59.999(+0000)" --
	 *         the extra day allows for any timezone to be added and still have a 4
	 *         digit year.
	 */
	static public MaskerDate MAX_VALUE() {
		return new MaskerDate(MAX_TIME()); // equals
		// "9999/12/30-23:59:59.999(+0000)"
	}

	/**
	 * @return the earliest date that matches the Masker Date Format for any
	 *         timezone. The value is the date of the epoch
	 *         "1970/01/01-00:00:00.000(+0000)"
	 */
	static public MaskerDate MIN_VALUE() {
		return new MaskerDate(0L);
	}

	/**
	 * Find the timezone string by parsing the timezone list entry formed by the
	 * getTimeZoneList routine.
	 * 
	 * @param strTimeZoneListEntry
	 *                             the timezone list entry from the getTimeZoneList
	 *                             method comprising the timezone offset from GMT
	 *                             followed by a space and then the TimeZone ID. For
	 *                             example, "+0100 Europe/Paris" or "-0500 EST" or
	 *                             "+0000 GMT" would return "+0100", "-0500", or
	 *                             "+0000" respectively.
	 * @return the timezone in the form +/-hhmm (e.g., +0000 is GMT, -0500 is
	 *         Eastern Standard Time, +0100 is Europe/Paris. If the passed timezone
	 *         list entry is null, empty, or does not comply with the expected
	 *         format, the GMT timezone "+0000" is returned.
	 */
	static public String parseTimeZoneFromListEntry(String strTimeZoneListEntry) {
		String strTimeZone = "+0000"; // default to GMT
		if (strTimeZoneListEntry == null || strTimeZoneListEntry.length() == 0) {
			return strTimeZone; // invalid so use GMT
		}
		int iLength = strTimeZoneListEntry.length();
		if (iLength >= 5) {
			strTimeZone = strTimeZoneListEntry.substring(0, 5);
			try {
				int iTime = Integer.valueOf(strTimeZone).intValue();
				if (iTime < -1300 || iTime > 1300) {
					strTimeZone = "+0000"; // invalid so use GMT
				}
			} catch (NumberFormatException nfe) {
				strTimeZone = "+0000"; // invalid so use GMT
			}
		}
		return strTimeZone;
	}

	/**
	 * Converts the input MaskerDate to an undefined MaskerDate if the input
	 * MaskerDate is null.
	 * 
	 * @param date
	 *             MaskerDate to be tested against null and converted.
	 * @return An undefined MaskerDate if the input MaskerDate was null, otherwise,
	 *         the input MaskerDate is echoed back.
	 */
	static public MaskerDate undefinedForNull(MaskerDate date) {
		if (date == null) {
			return UNDEFINED_MaskerDate;
		}
		return date;
	}

	/**
	 * Constructor for MaskerDate using the current date/time.
	 */
	public MaskerDate() {
		super();
	}

	/**
	 * Constructor initializing the time to the date supplied.
	 * 
	 * @param date
	 *             the date for this MaskerDate.
	 */
	public MaskerDate(Date date) {
		super();
		// if (ILDDate.isUndefined(date)) {
		if (date == null) {
			date = MaskerDate.UNDEFINED_MaskerDate;
		}
		setTime(date.getTime());
	}

	/**
	 * Constructor initializing the time to the value supplied.
	 * 
	 * @param dDate
	 *              the number of milliseconds since the epoch (midnight, 1/1/70
	 *              0000).
	 */
	public MaskerDate(double dDate) {
		super((long) dDate);
	}

	/**
	 * Create an MaskerDate object from the supplied date information.
	 * 
	 * @param iYear
	 *                      2, 3 or 4 digit year. If less than 4 digits, 2000 is
	 *                      added to the supplied value.
	 * @param iMonth
	 *                      number of the month where 1=January, 12=December.
	 * @param iDay
	 *                      number of the day of the month where the first day is 1.
	 * @param iHours
	 *                      hours within the day from 0 through 23.
	 * @param iMinutes
	 *                      minutes within the hour from 0 through 59.
	 * @param iSeconds
	 *                      seconds within the minute from 0 through 59.
	 * @param iMilliseconds
	 *                      milliseconds within the second from 0 through 999.
	 * @param strTimeZone
	 *                      the timezone (e.g., "+0000" is Greenwich Mean Time,
	 *                      "-0400" is Eastern Daylight Time). If no timezone is
	 *                      supplied then "+0000" is used as a default.
	 * @throws Exception
	 *                   if the values for Day is invalid for the Month, or if an
	 *                   invalid Timezone is supplied (Exception), or if one of the
	 *                   values is not within their allowed range
	 *                   (ExceptionInvalidParam).
	 */
	public MaskerDate(int iYear, int iMonth, int iDay, int iHours, int iMinutes, int iSeconds, int iMilliseconds,
			String strTimeZone) throws Exception {
		super();
		StringBuffer sb = new StringBuffer();
		// Year
		if (iYear < 100) {
			iYear += 2000;
		}
		sb.append(iYear);

		sb.append("/");

		// Month
		if (iMonth < 1 || iMonth > 12) {
			throw new Exception("Month is not between 1 and 12, inclusive.");
		}
		if (iMonth < 10) {
			sb.append("0");
		}
		sb.append(iMonth);

		sb.append("/");

		// Day
		if (iDay < 1 || iDay > 31) {
			// Parse error is thrown if day doesn't match month, leap year, etc.
			throw new Exception("Day is not between 1 and 31, inclusive.");
		}
		if (iDay < 10) {
			sb.append("0");
		}
		sb.append(iDay);

		sb.append("-");

		// Hour
		if (iHours < 0 || iHours > 23) {
			throw new Exception("Hours is not between 0 and 23, inclusive.");
		}
		if (iHours < 10) {
			sb.append("0");
		}
		sb.append(iHours);

		sb.append(":");

		// Minute
		if (iMinutes < 0 || iMinutes > 59) {
			throw new Exception("Minutes is not between 0 and 59, inclusive.");
		}
		if (iMinutes < 10) {
			sb.append("0");
		}
		sb.append(iMinutes);

		sb.append(":");

		// Second
		if (iSeconds < 0 || iSeconds > 59) {
			throw new Exception("Seconds is not between 0 and 59, inclusive.");
		}
		if (iSeconds < 10) {
			sb.append("0");
		}
		sb.append(iSeconds);

		sb.append(".");

		// Milliseconds
		if (iMilliseconds < 0 || iMilliseconds > 999) {
			throw new Exception("Milliseconds is not between 0 and 999, inclusive.");
		}
		if (iMilliseconds < 10) {
			sb.append("00");
		} else if (iMilliseconds < 100) {
			sb.append("0");
		}
		sb.append(iMilliseconds);

		sb.append("(");
		if (strTimeZone == null || strTimeZone.length() == 0) {
			strTimeZone = "+0000";
		}
		sb.append(strTimeZone);
		sb.append(")");

		setTime(makeDate(sb.toString()).getTime());
	}

	/**
	 * Constructor initializing the time to the value supplied.
	 * 
	 * @param lDate
	 *              the number of milliseconds since the epoch (midnight, 1/1/70
	 *              0000).
	 */
	public MaskerDate(long lDate) {
		super(lDate);
	}

	/**
	 * Create an MaskerDate object from the Masker formatted date string
	 * 
	 * @param strDate
	 *                the Masker formatted date string to be transformed into the
	 *                MaskerDate object.
	 * @see #CREATE_DATE_FORMAT
	 * @see #CREATE_DATE_FORMAT_0
	 * @see #CREATE_DATE_FORMAT_1
	 * @see #CREATE_DATE_FORMAT_2
	 * @see #CREATE_DATE_FORMAT_3
	 * @see #CREATE_DATE_FORMAT_4
	 * @see #CREATE_DATE_FORMAT_5
	 * @see #CREATE_DATE_FORMAT_6
	 * @see #CREATE_DATE_FORMAT_7
	 * @see #CREATE_DATE_FORMAT_8
	 * @see #CREATE_DATE_FORMAT_12
	 * @throws Exception
	 *                   {@link Exception} if the passed date does not comply with
	 *                   the appropriate supported formats. {@link Exception} if the
	 *                   date or time components are not correct.
	 */
	public MaskerDate(String strDate) throws Exception {
		super();
		setTime(makeDate(strDate).getTime());
	}

	@Override
	public MaskerDate clone() {
		return new MaskerDate((Date) this);
	}

	/**
	 * Compares this date with the supplied date and returns -1 if this date is less
	 * than the supplied date, 0 if equal, or 1 if greater than the supplied date.
	 * 
	 * @param date
	 *                the date to compare against this date
	 * @return -1 if this date is less than the supplied date, 0 if equal, or 1 if
	 *         greater than the supplied date.
	 */
	public int compareTo(MaskerDate date) {
		return super.compareTo(date);
	}

	// /*
	// * @see java.lang.Comparable#compareTo(java.lang.Object)
	// */
	// public int compareTo(Object object) {
	// return super.compareTo((Date) object);
	// }

	/*
	 * @see java.util.Date#compareTo(java.util.Date)
	 */
	public int compareTo(Date date) {
		return super.compareTo(date);
	}

	/**
	 * Calculate the number of elapsed milliseconds between the supplied date and
	 * this date. If no end date has been set, then the duration is 0L. If no start
	 * date has been set, then the duration is the number of milliseconds since the
	 * epoch (midnight 1/1/70 0000) until the end date. If the start date is greater
	 * than the end date, then 0L is returned.
	 * 
	 * @param dateFrom
	 *                 the date from which the elapsed time is calculated (e.g.,
	 *                 until this date).
	 * @return the number of elapsed milliseconds between the start and end of this
	 *         duration.
	 */
	public long elapsedTime(MaskerDate dateFrom) {
		return MaskerDuration.elapsedTime(dateFrom, this);
	}

	/**
	 * Calculate the number of elapsed milliseconds between the supplied date and
	 * this date. If no end date has been set, then the duration is 0L. If no start
	 * date has been set, then the duration is the number of milliseconds since the
	 * epoch (midnight 1/1/70 0000) until the end date. If the start date is greater
	 * than the end date, then 0L is returned.
	 * 
	 * @param dateFrom
	 *                 the date from which the elapsed time is calculated (e.g.,
	 *                 until this date).
	 * @return the number of elapsed milliseconds between the start and end of this
	 *         duration.
	 */
	public long elapsedTime(Date dateFrom) {
		return MaskerDuration.elapsedTime(dateFrom, this);
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	/**
	 * Get the day of this date for the specified timezone.
	 * 
	 * @param strTimeZone
	 *                    the timezone for which the day applies. If this value is
	 *                    null or empty, "+0000" is used.
	 * @return the day of this date for the specified timezone.
	 */
	public long getDay(String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("+0000"));
		calDate.setTimeInMillis(this.getTime());
		calDate.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		return calDate.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Get the hours of this date for the specified timezone.
	 * 
	 * @param strTimeZone
	 *                    the timezone for which the hours apply. If this value is
	 *                    null or empty, "+0000" is used.
	 * @return the hours of this date for the specified timezone.
	 */
	public long getHours(String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT+0000"));
		calDate.setTimeInMillis(this.getTime());
		calDate.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		return calDate.get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * Get the milliseconds of this date for the specified timezone.
	 * 
	 * @param strTimeZone
	 *                    the timezone for which the milliseconds apply. If this
	 *                    value is null or empty, "+0000" is used.
	 * @return the milliseconds of this date for the specified timezone.
	 */
	public long getMilliseconds(String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT+0000"));
		calDate.setTimeInMillis(this.getTime());
		calDate.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		return calDate.get(Calendar.MILLISECOND);
	}

	/**
	 * Get the minutesr of this date for the specified timezone.
	 * 
	 * @param strTimeZone
	 *                    the timezone for which the minutes apply. If this value is
	 *                    null or empty, "+0000" is used.
	 * @return the month of this date for the specified timezone.
	 */
	public long getMinutes(String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT+0000"));
		calDate.setTimeInMillis(this.getTime());
		calDate.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		return calDate.get(Calendar.MINUTE);
	}

	/**
	 * Get the month of this date for the specified timezone.
	 * 
	 * @param strTimeZone
	 *                    the timezone for which the month applies. If this value is
	 *                    null or empty, "+0000" is used.
	 * @return the month of this date for the specified timezone.
	 */
	public long getMonth(String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT+0000"));
		calDate.setTimeInMillis(this.getTime());
		calDate.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		return calDate.get(Calendar.MONTH) + 1; // make one based
	}

	/**
	 * Get the seconds of this date for the specified timezone.
	 * 
	 * @param strTimeZone
	 *                    the timezone for which the seconds apply. If this value is
	 *                    null or empty, "+0000" is used.
	 * @return the seconds of this date for the specified timezone.
	 */
	public long getSeconds(String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT+0000"));
		calDate.setTimeInMillis(this.getTime());
		calDate.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		return calDate.get(Calendar.SECOND);
	}

	/**
	 * Get the year of this date for the specified timezone.
	 * 
	 * @param strTimeZone
	 *                    the timezone for which the year applies. If this value is
	 *                    null or empty, "+0000" is used.
	 * @return the year of this date for the specified timezone.
	 */
	public long getYear(String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT+0000"));
		calDate.setTimeInMillis(this.getTime());
		calDate.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		return calDate.get(Calendar.YEAR);
	}

	/**
	 * Tests for equality to the UNDEFINED_MaskerDate which is one second after the
	 * epoch date (midnight 1/1/1970 GMT). This is used to identify and/or
	 * initialize an MaskerDate value that has yet to receive a valid value.
	 * Typically used for comparisons to glean if the value has been set properly.
	 * 
	 * @return whether (true) or not (false) this object is undefined
	 * @see #isUndefined(MaskerDate)
	 */
	public boolean isUndefined() {
		return isUndefined(this);
	}

	/**
	 * Set the day of this date to the specified day.
	 * 
	 * @param iDay
	 *                    the day of the month (first day is 1).
	 * @param strTimeZone
	 *                    the timezone for which the day applies. If this value is
	 *                    null or empty, "+0000" is used.
	 */
	public void setDay(int iDay, String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT" + strTimeZone));
		calDate.setTimeInMillis(this.getTime());
		calDate.set(Calendar.DAY_OF_MONTH, iDay);
		setTime(calDate.getTime().getTime());
	}

	/**
	 * Set the hours of this date to the specified hours.
	 * 
	 * @param iHours
	 *                    the hours within the day (0=midnight).
	 * @param strTimeZone
	 *                    the timezone for which the hours apply. If this value is
	 *                    null or empty, "+0000" is used.
	 */
	public void setHours(int iHours, String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT" + strTimeZone));
		calDate.setTimeInMillis(this.getTime());
		calDate.set(Calendar.HOUR_OF_DAY, iHours);
		setTime(calDate.getTime().getTime());
	}

	/**
	 * Set the milliseconds of this date to the specified seconds.
	 * 
	 * @param iMilliseconds
	 *                      the milliseconds within the second [0..999].
	 * @param strTimeZone
	 *                      the timezone for which the milliseconds apply. If this
	 *                      value is null or empty, "+0000" is used.
	 */
	public void setMilliseconds(int iMilliseconds, String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT" + strTimeZone));
		calDate.setTimeInMillis(this.getTime());
		calDate.set(Calendar.MILLISECOND, iMilliseconds);
		setTime(calDate.getTime().getTime());
	}

	/**
	 * Set the minutes of this date to the specified minutes.
	 * 
	 * @param iMinutes
	 *                    the minutes within the hour [0..59].
	 * @param strTimeZone
	 *                    the timezone for which the minutes apply. If this value is
	 *                    null or empty, "+0000" is used.
	 */
	public void setMinutes(int iMinutes, String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT" + strTimeZone));
		calDate.setTimeInMillis(this.getTime());
		calDate.set(Calendar.MINUTE, iMinutes);
		setTime(calDate.getTime().getTime());
	}

	/**
	 * Set the month of this date to the specified month.
	 * 
	 * @param iMonth
	 *                    the one-based counting month number (e.g., 1=January).
	 * @param strTimeZone
	 *                    the timezone for which the month applies. If this value is
	 *                    null or empty, "+0000" is used.
	 */
	public void setMonth(int iMonth, String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT" + strTimeZone));
		calDate.setTimeInMillis(this.getTime());
		calDate.set(Calendar.MONTH, iMonth - 1); // make zero based
		setTime(calDate.getTime().getTime());
	}

	/**
	 * Set the seconds of this date to the specified seconds.
	 * 
	 * @param iSeconds
	 *                    the seconds within the minute [0..59].
	 * @param strTimeZone
	 *                    the timezone for which the seconds apply. If this value is
	 *                    null or empty, "+0000" is used.
	 */
	public void setSeconds(int iSeconds, String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT" + strTimeZone));
		calDate.setTimeInMillis(this.getTime());
		calDate.set(Calendar.SECOND, iSeconds);
		setTime(calDate.getTime().getTime());
	}

	/**
	 * Set the year of this date to the specified year.
	 * 
	 * @param iYear
	 *                    the complete, 4 digit year (yeah, Y10K bug... ;^)
	 * @param strTimeZone
	 *                    the timezone for which the year applies. If this value is
	 *                    null or empty, "+0000" is used.
	 */
	public void setYear(int iYear, String strTimeZone) {
		Calendar calDate = new GregorianCalendar(TimeZone.getTimeZone("GMT" + strTimeZone));
		calDate.setTimeInMillis(this.getTime());
		calDate.set(Calendar.YEAR, iYear);
		setTime(calDate.getTime().getTime());
	}

	public Date toDate() {
		return (Date) this;
	}

	/**
	 * @return the formatted date using "yyyy-MM-ddTHH:mm:ss.SSS(Z)" where yyyy is
	 *         the 4 digit year, MM is the 2 digit month, dd is the 2 digit day, HH
	 *         is the 2 digit hours, mm is the 2 digit minutes, ss is the 2 digit
	 *         seconds, SSS is the 3 digit milliseconds, and Z is GMT (e.g., +0000).
	 *         For example, August 4th, 2004 at 7:15:35.456 AM Greenwich Mean Time
	 *         is "2004-08-04T07:15:35.456Z"
	 * @see #toString(String)
	 */
	public String toString() {
		// return makeFormattedDate(this, CREATE_DATE_FORMAT, "+0000");
		return toStringDateTime();
	}

	/**
	 * Return a String containing formatted date in the specified timezone.
	 * 
	 * @param strTimeZone
	 *                    the ID for a TimeZone, either an abbreviation such as
	 *                    "PST", a full name such as "America/Los_Angeles", or a
	 *                    custom ID such as "0000-8:00". Note that the support of
	 *                    abbreviations is for JDK 1.1.x compatibility only and full
	 *                    names should be used.
	 * @return the formatted date using "yyyy/MM/dd-HH:mm:ss.SSS(Z)" where yyyy is
	 *         the 4 digit year, MM is the 2 digit month, dd is the 2 digit day, HH
	 *         is the 2 digit hours, mm is the 2 digit minutes, ss is the 2 digit
	 *         seconds, SSS is the 3 digit milliseconds, and z is the 3 character
	 *         timezone. For example, August 4th, 2004 at 7:15:35.456 AM Greenwich
	 *         Mean Time is "2004/08/04-07:15:35.456(+0000)"
	 * @see #toString()
	 */
	public String toString(String strTimeZone) {
		return makeFormattedDate(this, CREATE_DATE_FORMAT, strTimeZone);
	}

	/**
	 * Return a String containing formatted date in the specified timezone.
	 * 
	 * @param strFormat
	 *                    the desired date format to be returned. If the format is
	 *                    invalid, the standard Masker Date format is used
	 *                    (CREATE_DATE_FORMAT).
	 * @param strTimeZone
	 *                    the ID for a TimeZone, either an abbreviation such as
	 *                    "PST", a full name such as "America/Los_Angeles", or a
	 *                    custom ID such as "0000-8:00". Note that the support of
	 *                    abbreviations is for JDK 1.1.x compatibility only and full
	 *                    names should be used.
	 * @return the formatted date using "yyyy/MM/dd-HH:mm:ss.SSS(Z)" where yyyy is
	 *         the 4 digit year, MM is the 2 digit month, dd is the 2 digit day, HH
	 *         is the 2 digit hours, mm is the 2 digit minutes, ss is the 2 digit
	 *         seconds, SSS is the 3 digit milliseconds, and z is the 3 character
	 *         timezone. For example, August 4th, 2004 at 7:15:35.456 AM Greenwich
	 *         Mean Time is "2004/08/04-07:15:35.456(0000)"
	 * @see #toString()
	 * @see #CREATE_DATE_FORMAT
	 */
	public String toString(String strFormat, String strTimeZone) {
		if (strFormat == null) {
			strFormat = CREATE_DATE_FORMAT;
		}
		if (strTimeZone == null) {
			strTimeZone = "+0000";
		}
		return makeFormattedDate(this, strFormat, strTimeZone);
	}

	/**
	 * @return the formatted date using "yyyy-MM-dd HH:mm:ss.SSS" where yyyy is the
	 *         4 digit year, MM is the 2 digit month, dd is the 2 digit day, HH is
	 *         the 2 digit hours, mm is the 2 digit minutes, ss is the 2 digit
	 *         seconds, SSS is the 3 digit milliseconds in GMT timezone (e.g.,
	 *         +0000). For example, August 4th, 2004 at 7:15:35.456 AM Greenwich
	 *         Mean Time is "2004-08-04 07:15:35.456"
	 * @see #toString(String)
	 */
	public String toStringCookie() {
		String strRC = makeFormattedDate(this, CREATE_DATE_FORMAT_11, "+0000");
		// simple transform
		strRC = strRC.replace('-', ' ');
		strRC = strRC.replace('/', '-');
		return strRC;
	}

	/**
	 * @return the formatted date using "yyyy-MM-ddTHH:mm:ss.SSSZ" where yyyy is the
	 *         4 digit year, MM is the 2 digit month, dd is the 2 digit day, HH is
	 *         the 2 digit hours, mm is the 2 digit minutes, ss is the 2 digit
	 *         seconds, SSS is the 3 digit milliseconds, and , and Z is GMT (e.g.,
	 *         +0000). For example, August 4th, 2004 at 7:15:35.456 AM Greenwich
	 *         Mean Time is "2004/08/04T07:15:35.456(0000)"
	 * @see #toString(String)
	 */
	public String toStringDateTime() {
		String strRC = makeFormattedDate(this, CREATE_DATE_FORMAT_0, "+0000");
		// simple transform
		strRC = strRC.replace('-', 'T');
		strRC = strRC.replace('/', '-');
		return strRC + "Z";
	}

	/**
	 * @return the formatted date using "yyyy-MM-dd HH:mm:ss.SSS" where yyyy is the
	 *         4 digit year, MM is the 2 digit month, dd is the 2 digit day, HH is
	 *         the 2 digit hours, mm is the 2 digit minutes, ss is the 2 digit
	 *         seconds, SSS is the 3 digit milliseconds in GMT timezone (e.g.,
	 *         +0000). For example, August 4th, 2004 at 7:15:35.456 AM Greenwich
	 *         Mean Time is "2004-08-04 07:15:35.456"
	 * @see #toString(String)
	 */
	public String toStringDBTimestamp() {
		String strRC = makeFormattedDate(this, CREATE_DATE_FORMAT_10, "+0000");
		// simple transform
		strRC = strRC.replace('-', ' ');
		strRC = strRC.replace('/', '-');
		return strRC;
	}
}
