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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Utility class for durations in time.
 * 
 */
public class MaskerDuration implements Serializable, Comparable<MaskerDuration> {

	// --------
	// statics
	// --------
	static final public String m_strClassName = MaskerUtils.getNameFromClass(MaskerDuration.class);

	private static final long serialVersionUID = 2151322379607630650L;

	/**
	 * An undefined version of this object. An undefined MaskerDuration has an
	 * undefined name.
	 * 
	 * @see MaskerUtils#isUndefined(String)
	 */
	static public MaskerDuration UNDEFINED_MaskerDuration = new MaskerDuration();

	/**
	 * Calculate the number of elapsed days between the start and end of this
	 * duration. If no end date has been set, then the duration is 0L. If no start
	 * date has been set, then the duration is the number of days since the epoch
	 * (midnight 1/1/70 GMT) until the end date. If the start date is greater than
	 * the end date, then 0L is returned.
	 * 
	 * @param dateStart
	 *                  the start date of the duration
	 * @param dateEnd
	 *                  the end date of the duration
	 * @return the number of elapsed days between the start and end of this
	 *         duration.
	 */
	static public long elapsedDays(Date dateStart, Date dateEnd) {
		long lElapsedTime = elapsedHours(dateStart, dateEnd);
		lElapsedTime /= 24L;
		return lElapsedTime;
	}

	/**
	 * Calculate the number of elapsed hours between the start and end of this
	 * duration. If no end date has been set, then the duration is 0L. If no start
	 * date has been set, then the duration is the number of hours since the epoch
	 * (midnight 1/1/70 GMT) until the end date. If the start date is greater than
	 * the end date, then 0L is returned.
	 * 
	 * @param dateStart
	 *                  the start date of the duration
	 * @param dateEnd
	 *                  the end date of the duration
	 * @return the number of elapsed hours between the start and end of this
	 *         duration.
	 */
	static public long elapsedHours(Date dateStart, Date dateEnd) {
		long lElapsedTime = elapsedMinutes(dateStart, dateEnd);
		lElapsedTime /= 60L;
		return lElapsedTime;
	}

	/**
	 * Calculate the number of elapsed minutes between the start and end of this
	 * duration. If no end date has been set, then the duration is 0L. If no start
	 * date has been set, then the duration is the number of minutes since the epoch
	 * (midnight 1/1/70 GMT) until the end date. If the start date is greater than
	 * the end date, then 0L is returned.
	 * 
	 * @param dateStart
	 *                  the start date of the duration
	 * @param dateEnd
	 *                  the end date of the duration
	 * @return the number of elapsed minutes between the start and end of this
	 *         duration.
	 */
	static public long elapsedMinutes(Date dateStart, Date dateEnd) {
		long lElapsedTime = elapsedSeconds(dateStart, dateEnd);
		lElapsedTime /= 60L;
		return lElapsedTime;
	}

	/**
	 * Calculate the number of elapsed seconds between the start and end of this
	 * duration. If no end date has been set, then the duration is 0L. If no start
	 * date has been set, then the duration is the number of seconds since the epoch
	 * (midnight 1/1/70 GMT) until the end date. If the start date is greater than
	 * the end date, then 0L is returned.
	 * 
	 * @param dateStart
	 *                  the start date of the duration
	 * @param dateEnd
	 *                  the end date of the duration
	 * @return the number of elapsed seconds between the start and end of this
	 *         duration.
	 */
	static public long elapsedSeconds(Date dateStart, Date dateEnd) {
		long lElapsedTime = elapsedTime(dateStart, dateEnd);
		lElapsedTime /= 1000L;
		return lElapsedTime;
	}

	/**
	 * Calculate the number of elapsed milliseconds between the start and end of
	 * this duration. If no end date has been set, then the duration is 0L. If no
	 * start date has been set, then the duration is the number of milliseconds
	 * since the epoch (midnight 1/1/70 GMT) until the end date. If the start date
	 * is greater than the end date, then the negative of the elapsed time from the
	 * end date to the start date is returned.
	 * 
	 * @param dateStart
	 *                  the start date of the duration
	 * @param dateEnd
	 *                  the end date of the duration
	 * @return the number of elapsed milliseconds between the start and end of this
	 *         duration.
	 */
	static public long elapsedTime(Date dateStart, Date dateEnd) {
		dateStart = MaskerUtils.undefinedForNull(dateStart);
		dateEnd = MaskerUtils.undefinedForNull(dateEnd);

		if (MaskerDate.isUndefined(dateEnd) == true) {
			return 0L;
		}
		if (MaskerDate.isUndefined(dateStart) == true) {
			return dateEnd.getTime();
		}
		if (dateStart.compareTo(dateEnd) > 0) {
			return 0L - elapsedTime(dateEnd, dateStart);
		}
		return dateEnd.getTime() - dateStart.getTime();
	}

	static public String formattedElapsedTime(Date dateStart, Date dateEnd) {
		long lElapsedMS = elapsedTime(dateStart, dateEnd);
		return formattedElapsedTime(lElapsedMS);
	}

	static public String formattedElapsedTime(double dElapsedSeconds) {
		long lElapsedMS = (long) (dElapsedSeconds * 1000L);
		return formattedElapsedTime(lElapsedMS);
	}

	static public String formattedElapsedTime(long lElapsedMS) {
		String strSign = MaskerConstants.EMPTY_String;
		if (lElapsedMS < 0L) {
			strSign = "-";
			lElapsedMS = -lElapsedMS;
		}
		long lElapsedMilli = lElapsedMS % 1000;
		lElapsedMS /= 1000;
		StringBuffer sb = new StringBuffer();
		long lDays = lElapsedMS / 86400;
		lElapsedMS = lElapsedMS % 86400;
		long lHours = lElapsedMS / 3600;
		lElapsedMS = lElapsedMS % 3600;
		long lMinutes = lElapsedMS / 60;
		lElapsedMS = lElapsedMS % 60;
		if (MaskerUtils.isEmpty(strSign) == false) {
			sb.append("-");
		}
		sb.append(lDays);
		sb.append("-");
		sb.append(MaskerUtils.padLeftZero((int) lHours, 2));
		sb.append(":");
		sb.append(MaskerUtils.padLeftZero((int) lMinutes, 2));
		sb.append(":");
		sb.append(MaskerUtils.padLeftZero((int) lElapsedMS, 2));
		sb.append(".");
		sb.append(MaskerUtils.padLeftZero((int) lElapsedMilli, 3));
		return sb.toString();
	}

	public static void main(String[] args) {
		try {
			String strBirthday = MaskerUtils.prompt("Enter your birthday in the form: YYYY/MM/DD-hh:mm:ss.SSS(ZZZ):");
			if (strBirthday == null || strBirthday.length() == 0) {
				strBirthday = "1957/08/04-07:15:00.000(EDT)";
			}
			MaskerDuration dur = new MaskerDuration("Birthday", new MaskerDate(strBirthday), new MaskerDate());
			String strTimeZone = MaskerUtils.prompt("Enter the output timezone (e.g., -0500 for EST or +0000 for GMT):");
			System.out.println(dur.toString(strTimeZone));
			MaskerDuration negdur = new MaskerDuration("Reverse", dur.getEndDate(), dur.getStartDate());
			System.out.println(negdur.toString(strTimeZone));
		} catch (Exception aer) {
			aer.printStackTrace();
		}
		System.out.println("Goodbye");
	}

	/**
	 * Create an MaskerDuration from the passed list string.
	 * 
	 * @param listString
	 *                   the list of String fields needed to create an
	 *                   MaskerDuration (name, start date, end date);
	 * @return a newly created MaskerDuration object filled with the content of the
	 *         supplied listString. If the listString is null or empty, or does not
	 *         contain at least the name field, an undefined MaskerDuration is
	 *         returned.
	 * @throws Exception
	 *                   if listString is null or empty, or if the name in the list
	 *                   is null, empty or undefined.
	 * @see #isUndefined()
	 */
	static public MaskerDuration newInstanceFromListString(String listString) throws Exception {
		if (listString == null || listString.length() == 0) {
			throw new Exception("String listString is null or empty.");
		}
		MaskerDuration duration = MaskerDuration.UNDEFINED_MaskerDuration;
		ArrayList<Object> list = new ArrayList<Object>();
		list = MaskerUtils.listStringToArrayList(listString);
		// process what we got from the listString
		MaskerDate dateEnd = MaskerDate.UNDEFINED_MaskerDate;
		MaskerDate dateStart = MaskerDate.UNDEFINED_MaskerDate;
		for (int i = 0; i < list.size(); i++) {
			switch (i) {
			case 0: {
				// Name
				if (list.get(i) instanceof String) {
					duration.setName((String) list.get(i));
				}
				break;
			}
			case 1: {
				// Start Date
				if (list.get(i) instanceof String) {
					try {
						dateStart = new MaskerDate((String) list.get(i));
					} catch (Exception e) {
						// use the undefined date
					}
					duration.setStartDate(dateStart);
				}
				break;
			}
			case 2: {
				// End Date
				if (list.get(i) instanceof String) {
					try {
						dateEnd = new MaskerDate((String) list.get(i));
					} catch (Exception e) {
						// use the undefined date
					}
					duration.setEndDate(dateEnd);
				}
				break;
			}
			}
		} // end for loop
		return duration;
	}

	/**
	 * Converts the inputMaskerDuration to an undefinedMaskerDuration if the input
	 * MaskerDuration is null.
	 * 
	 * @param duration
	 *                 MaskerDuration to be tested against null and converted.
	 * @return An undefinedMaskerDuration if the inputMaskerDuration was null,
	 *         otherwise, the inputMaskerDuration is echoed back.
	 */
	static public MaskerDuration undefinedForNull(MaskerDuration duration) {
		if (duration == null) {
			return UNDEFINED_MaskerDuration;
		}
		return duration;
	}

	MaskerDate m_dateEnd = MaskerDate.UNDEFINED_MaskerDate;

	MaskerDate m_dateStart = MaskerDate.UNDEFINED_MaskerDate;

	String m_strName = MaskerConstants.UNDEFINED_String;

	/**
	 * Construct an undefinedMaskerDuration.
	 */
	public MaskerDuration() {
		initDuration(null, null, null);
	}

	/**
	 * Construct a well formedMaskerDuration
	 * 
	 * @param strName
	 *                name of this duration
	 */
	public MaskerDuration(String strName) {
		initDuration(strName, null, null);
	}

	/**
	 * Construct a well formedMaskerDuration
	 * 
	 * @param strName
	 *                  name of this duration
	 * @param dateStart
	 *                  start date of this duration
	 * @param dateEnd
	 *                  end date of this duration
	 */
	public MaskerDuration(String strName, MaskerDate dateStart, MaskerDate dateEnd) {
		initDuration(strName, dateStart, dateEnd);
	}

	/*
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(MaskerDuration o1) {
		int iRC = 0;
		if (o1 == null || ((o1 instanceof MaskerDuration) == false)) {
			return -1;
		}
		iRC = getStartDate().compareTo(((MaskerDuration) o1).getStartDate());
		if (iRC != 0) {
			return iRC;
		}
		iRC = getEndDate().compareTo(((MaskerDuration) o1).getEndDate());
		return iRC;
	}

	/**
	 * Calculate the number of elapsed days between the start and end of this
	 * duration. If no end date has been set, then the duration is 0L. If no start
	 * date has been set, then the duration is the number of days since the epoch
	 * (midnight 1/1/70 GMT) until the end date. If the start date is greater than
	 * the end date, then 0L is returned.
	 * 
	 * @return the number of elapsed days between the start and end of this
	 *         duration.
	 */
	public long elapsedDays() {
		long lElapsedTime = elapsedHours();
		lElapsedTime /= 24L;
		return lElapsedTime;
	}

	/**
	 * Calculate the number of elapsed hours between the start and end of this
	 * duration. If no end date has been set, then the duration is 0L. If no start
	 * date has been set, then the duration is the number of hours since the epoch
	 * (midnight 1/1/70 GMT) until the end date. If the start date is greater than
	 * the end date, then 0L is returned.
	 * 
	 * @return the number of elapsed hours between the start and end of this
	 *         duration.
	 */
	public long elapsedHours() {
		long lElapsedTime = elapsedMinutes();
		lElapsedTime /= 60L;
		return lElapsedTime;
	}

	/**
	 * Calculate the number of elapsed minutes between the start and end of this
	 * duration. If no end date has been set, then the duration is 0L. If no start
	 * date has been set, then the duration is the number of minutes since the epoch
	 * (midnight 1/1/70 GMT) until the end date. If the start date is greater than
	 * the end date, then 0L is returned.
	 * 
	 * @return the number of elapsed minutes between the start and end of this
	 *         duration.
	 */
	public long elapsedMinutes() {
		long lElapsedTime = elapsedSeconds();
		lElapsedTime /= 60L;
		return lElapsedTime;
	}

	/**
	 * Calculate the number of elapsed seconds between the start and end of this
	 * duration. If no end date has been set, then the duration is 0L. If no start
	 * date has been set, then the duration is the number of seconds since the epoch
	 * (midnight 1/1/70 GMT) until the end date. If the start date is greater than
	 * the end date, then 0L is returned.
	 * 
	 * @return the number of elapsed seconds between the start and end of this
	 *         duration.
	 */
	public long elapsedSeconds() {
		long lElapsedTime = elapsedTime();
		lElapsedTime /= 1000L;
		return lElapsedTime;
	}

	/**
	 * Calculate the number of elapsed milliseconds between the start and end of
	 * this duration. If no end date has been set, then the duration is 0L. If no
	 * start date has been set, then the duration is the number of milliseconds
	 * since the epoch (midnight 1/1/70 GMT) until the end date. If the start date
	 * is greater than the end date, then 0L is returned.
	 * 
	 * @return the number of elapsed milliseconds between the start and end of this
	 *         duration.
	 */
	public long elapsedTime() {
		if (MaskerDate.isUndefined(m_dateEnd) == true) {
			return 0L;
		}
		if (MaskerDate.isUndefined(m_dateStart) == true) {
			return m_dateEnd.getTime();
		}
		if (m_dateStart.compareTo(m_dateEnd) > 0) {
			return 0L;
		}
		return m_dateEnd.getTime() - m_dateStart.getTime();
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(MaskerDuration o1) {
		return (compareTo(o1) == 0);
	}

	/**
	 * @return the elapsed time of this duration formatted as a String containing an
	 *         optional sign (if the duration is negative) followed by the
	 *         days-hours:minutes:seconds.milliseconds
	 */
	public String formattedElapsedTime() {
		return formattedElapsedTime(m_dateStart, m_dateEnd);
	}

	/**
	 * @return the end date of this duration. Note: this may be set to an
	 *         undefinedMaskerDate. Test for this condition using isUndefined().
	 * @see MaskerDate#UNDEFINED_MaskerDate
	 * @see MaskerDate#isUndefined()
	 */
	public MaskerDate getEndDate() {
		return m_dateEnd;
	}

	/**
	 * @return the name of this duration. Note: this may be set to UNDEFINED_String.
	 *         Test for this condition using isUndefined().
	 * @see MaskerConstants#UNDEFINED_String
	 * @see MaskerUtils#isUndefined(String)
	 */
	public String getName() {
		return m_strName;
	}

	/**
	 * @return the start date of this duration. Note: this may be set to an
	 *         undefinedMaskerDate. Test for this condition using isUndefined().
	 * @see MaskerDate#UNDEFINED_MaskerDate
	 * @see MaskerDate#isUndefined()
	 */
	public MaskerDate getStartDate() {
		return m_dateStart;
	}

	/**
	 * Initialize the content of this duration
	 * 
	 * @param strName
	 *                  name for this duration
	 * @param dateStart
	 *                  start date for this duration
	 * @param dateEnd
	 *                  end date for this duration
	 */
	private void initDuration(String strName, MaskerDate dateStart, MaskerDate dateEnd) {
		m_strName = MaskerUtils.undefinedForNull(strName);
		m_dateStart = MaskerDate.undefinedForNull(dateStart);
		m_dateEnd = MaskerDate.undefinedForNull(dateEnd);
	}

	/**
	 * Determine if thisMaskerDuration is undefined (e.g., if its name equals
	 * UNDEFINED_String).
	 * 
	 * @return true if thisMaskerDuration is determined to be undefined. Otherwise,
	 *         return false.
	 * @see MaskerConstants#UNDEFINED_String
	 */
	public boolean isUndefined() {
		return (getName().compareTo(MaskerConstants.UNDEFINED_String) == 0);
	}

	/**
	 * Set the start and end dates for this duration. If null is passed for either
	 * parameter, it is set to an undefinedMaskerDate.
	 * 
	 * @param dateStart
	 *                  the start date.
	 * @param dateEnd
	 *                  the end date.
	 * @see MaskerDate#UNDEFINED_MaskerDate
	 * @see MaskerDate#isUndefined()
	 */
	public void setDuration(MaskerDate dateStart, MaskerDate dateEnd) {
		setStartDate(dateStart);
		setEndDate(dateEnd);
	}

	/**
	 * Set the end date for this duration. If null is passed, the endDate is set to
	 * an undefinedMaskerDate.
	 * 
	 * @param endDate
	 *                the end date.
	 */
	public void setEndDate(MaskerDate endDate) {
		m_dateEnd = MaskerDate.undefinedForNull(endDate);
	}

	/**
	 * Set the name for this duration.
	 * 
	 * @param strName
	 *                the name.
	 * @throws Exception
	 *                   if strName is null, empty, or undefined.
	 * @see MaskerUtils#isUndefined(String)
	 */
	public void setName(String strName) throws Exception {
		if (MaskerUtils.isUndefined(strName) || strName.length() == 0) {
			throw new Exception("String strName is null, empty, or undefined.");
		}
		m_strName = MaskerUtils.undefinedForNull(strName);
	}

	/**
	 * Set the start date for this duration. If null is passed, the startDate is set
	 * to an undefined MaskerDate.
	 * 
	 * @param startDate
	 *                  the start date.
	 */
	public void setStartDate(MaskerDate startDate) {
		m_dateStart = MaskerDate.undefinedForNull(startDate);
	}

	/**
	 * Generates a list string compatible with the Utils listStringToArrayList
	 * method describing this duration.
	 * 
	 * @return a string list comprising delimited, quoted fields containing the
	 *         name, start date, and end date of this duration, as well as the
	 *         elapsed time formatted as DD-HH:MM:SS.mmm where DD is the number of
	 *         days, HH is the number of hours, MM is the number of minutes, SS is
	 *         the number of seconds, and mmm is the number of milliseconds.
	 * @see MaskerUtils#listStringToArrayList(String)
	 */
	public String toString() {
		return toString("+0000");
	}

	/**
	 * Generates a list string compatible with the Utils listStringToArrayList
	 * method describing this duration using the specified timezone for dates.
	 * 
	 * @param strTimeZone
	 *                    the ID for a TimeZone as a String containing a sign
	 *                    followed by the two digit hour and two digit minute offset
	 *                    from Greenwich Mean Time. For example, for Eastern
	 *                    Standard Time, submit "-0500". For Europe/Paris submit
	 *                    "+0100".
	 * @return a string list comprising delimited, quoted fields containing the
	 *         name, start date, and end date of this duration, as well as the
	 *         elapsed time formatted as DD-HH:MM:SS.mmm where DD is the number of
	 *         days, HH is the number of hours, MM is the number of minutes, SS is
	 *         the number of seconds, and mmm is the number of milliseconds.
	 * @see MaskerUtils#listStringToArrayList(String)
	 */
	public String toString(String strTimeZone) {
		if (strTimeZone == null || strTimeZone.length() == 0) {
			strTimeZone = "+0000";
		}
		ArrayList<String> list = new ArrayList<String>();
		list.add(m_strName);
		list.add(m_dateStart.toString(strTimeZone));
		list.add(m_dateEnd.toString(strTimeZone));
		list.add(formattedElapsedTime(m_dateStart, m_dateEnd));
		return MaskerUtils.arrayListToListString(list);
	}

}
