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

package com.whitelistmasker.masker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import com.api.json.JSON;
import com.api.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Common utility methods used by other classes
 *
 */
public class MaskerUtils implements Serializable {

	private static final long serialVersionUID = 7829338567692523456L;

	static public final int iDayMilliseconds = 86400000;

	static public final int iHourMilliseconds = 3600000;

	static public final int iMinuteMilliseconds = 60000;

	static final public Charset UTF8_CHARSET = Charset.forName("UTF-8");

	static boolean s_debug = false; // true;

	static Gson s_gson = new GsonBuilder().create();

	/**
	 * Utility method to create a list of the content of an ArrayList by converting
	 * each object to its toString() representation and appending it within a list
	 * using the supplied delimiter. If a String is encountered in the ArrayList, it
	 * will be quoted (e.g., surrounded by double quote marks). The list itself is
	 * enclosed by an opening ('{') and closing ('}') brace.
	 * 
	 * Note: there is no escaping of the characters in a String object, so if it
	 * contains a delimiter or a closing brace, you may get unexpected results.
	 * 
	 * @param list
	 *             the array list of objects to be converted to a list string.
	 * @return the list comprising an opening brace, then each object in the
	 *         arraylist converted to a string, followed by the closing brace, with
	 *         the caveat that string objects encountered in the arraylist are
	 *         enclosed in a pair of double quotes.
	 * @see #listStringToArrayList
	 */
	static public String arrayListToListString(ArrayList<?> list) {
		// WNM3: add support for Int[], Double[], Float[], Long[], String[]
		// WNM3: escape the Strings?
		if (list == null || list.size() == 0) {
			return "{}"; // empty string
		}
		String strDelimiter = ",";
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		Object item = null;
		for (int i = 0; i < list.size(); i++) {
			if (i != 0) {
				sb.append(strDelimiter);
			}
			item = list.get(i);
			if (item instanceof Integer) {
				sb.append(item.toString());
			} else if (item instanceof Long) {
				sb.append(item.toString());
			} else if (item instanceof Double) {
				sb.append(item.toString());
			} else if (item instanceof Float) {
				sb.append(item.toString());
			} else if (item instanceof String) {
				sb.append("\"");
				sb.append(item.toString());
				sb.append("\"");
			} else {
				// WNM3: not sure what to do here... hex of serialized?
				sb.append("\"");
				sb.append(item.toString());
				sb.append("\"");
			}
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Set up filter if the supplied word contains an @ like in an email address, or
	 * starts or ends with a number, or contains http
	 * 
	 * @param word
	 *             value to be tested
	 * @return true if the word should be filtered
	 */
	static public boolean checkWordFilter(String word) {
		if (word == null) {
			return true;
		}
		word = word.trim();
		if (word.length() == 0) {
			return true;
		}
		// check for email pattern
		if (word.contains("@")) {
			return true;
		}
		// check for http pattern
		if (word.toLowerCase().contains("http")) {
			return true;
		}
		// check for non-words that begin with a number
		char wordChar = word.charAt(0);
		if (0x0030 <= wordChar && wordChar <= 0x0039) {
			return true;
		}

		// check for non-words that end with a number
		wordChar = word.charAt(word.length() - 1);
		if (0x0030 <= wordChar && wordChar <= 0x0039) {
			return true;
		}
		return false;
	}

	/**
	 * Cleans trailing characters from the supplied URL based on how URL's might be
	 * referenced within dialogs (e.g., removes trailing comma, double quote,
	 * question mark, or period, as well as newline, and carriage return.
	 * 
	 * @param url
	 *            the URL to be cleansed
	 * @return the cleansed URL
	 */
	static public String cleanURL(String url) {
		// Truncate at newlines ("\r", "\n")
		int index = url.indexOf("\r");
		while (index >= 0) {
			url = url.substring(0, index);
			index = url.indexOf("\r");
		}
		index = url.indexOf("\n");
		while (index >= 0) {
			url = url.substring(0, index);
			index = url.indexOf("\n");
		}
		index = url.indexOf("\u00A0");
		if (index >= 0) {
			url = url.substring(0, index);
		}
		// strip any trailing period, comma, double quote
		while (url.endsWith(".") || url.endsWith(",") || url.endsWith("\"") || url.endsWith("!") || url.endsWith("'")
				|| url.endsWith("?") || url.endsWith(":") || url.endsWith("]") || url.endsWith(")") || url.endsWith("`")
				|| url.endsWith("\\") || url.endsWith("/") || url.endsWith("\r") || url.endsWith("\n") || url.endsWith("\t")
				|| url.endsWith("\u00A0") || url.endsWith("\u2028") || url.endsWith("\u2029") || url.endsWith("\u2019")
				|| url.endsWith("\u201A")) {
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}

	/**
	 * Strip off non-word characters from the beginning and end of the supplied
	 * word.
	 * 
	 * @param word
	 *             the word to be cleansed
	 * @return array of cleansed word parts: [0] prefix removed from word, [1]
	 *         cleansed word, [2] suffix removed from word
	 */
	static public String[] cleanWord(String word) {
		String[] result = new String[3];
		result[0] = "";
		result[1] = "";
		result[2] = "";
		if (word == null) {
			return result;
		}
		word = trimSpaces(word);
		if (word.length() == 0) {
			result[1] = word;
			return result;
		}
		// clean front
		int index = 0;
		int len = word.length();
		char wordchar = word.charAt(index);
		StringBuffer sb = new StringBuffer();
		// numbers, letters, some quotes, but not @ (to block emails)
		while (wordchar < 0x0030 || (wordchar > 0x0039 && wordchar < 0x0061 && wordchar != 0x0040)
				|| (wordchar > 0x007a && wordchar <= 0x007f) || wordchar == '\u2003' || wordchar == '\u2013'
				|| wordchar == '\u2018' || wordchar == '\u2019' || wordchar == '\u201C' || wordchar == '\u201D'
				|| wordchar == '\u2022' || wordchar == '\u2026' || wordchar == '\u2028' || wordchar == '\u202A'
				|| wordchar == '\u202C' || wordchar == '\u202F') {
			sb.append(wordchar);
			index++;
			if (index == len) {
				break;
			}
			wordchar = word.charAt(index);
		}
		result[0] = sb.toString();
		if (index == len) {
			return result;
		}
		word = word.substring(index);
		len = word.length();
		index = len - 1;
		sb.setLength(0); // clear the accumulator
		wordchar = word.charAt(index);
		while (wordchar < 0x0030 || (wordchar > 0x0039 && wordchar < 0x0061 && wordchar != 0x0040)
				|| (wordchar > 0x007a && wordchar <= 0x007f) || wordchar == '\u2003' || wordchar == '\u2013'
				|| wordchar == '\u2018' || wordchar == '\u2019' || wordchar == '\u201C' || wordchar == '\u201D'
				|| wordchar == '\u2022' || wordchar == '\u2026' || wordchar == '\u2028' || wordchar == '\u202A'
				|| wordchar == '\u202C' || wordchar == '\u202F') {

			sb.append(wordchar);
			index--;
			if (index == 0) {
				break;
			}
			wordchar = word.charAt(index);
		}
		result[2] = sb.reverse().toString();
		word = word.substring(0, index + 1);
		result[1] = word;
		return result;
	}

	/**
	 * Close a buffered reader opened using {@link #openTextFile(String)}
	 * 
	 * @param br
	 */
	static public void closeTextFile(BufferedReader br) {
		if (br != null) {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Close a buffered writer flushing its content first. Nothing happens if a null
	 * is passed.
	 * 
	 * @param bw
	 *           the buffered writer to be flushed and closed.
	 */
	static public void closeTextFile(BufferedWriter bw) {
		if (bw != null) {
			try {
				bw.flush();
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Convert a number of milliseconds to a formatted String containing a sign,
	 * followed by the hours and minutes as in "-0500" or "+0100" which are used for
	 * TimeZones.
	 * 
	 * @param iMillisecs
	 *                   the number of milliseconds from Greenwich Mean Time.
	 * @return a string of the form +/-hhmm as in "-0500" or "+0100"
	 */
	static public String convertMillisecondsToTimeZone(int iMillisecs) {
		StringBuffer sb = new StringBuffer();
		if (iMillisecs < 0) {
			sb.append("-");
			iMillisecs *= -1;
		} else {
			sb.append("+");
		}
		int iHours = iMillisecs / iHourMilliseconds;
		if (iHours < 10) {
			sb.append("0");
		}
		sb.append(iHours);
		iMillisecs -= iHours * iHourMilliseconds;
		int iMinutes = iMillisecs / iMinuteMilliseconds;
		if (iMinutes < 10) {
			sb.append("0");
		}
		sb.append(iMinutes);
		return sb.toString();
	}

	/**
	 * Converts a timezone of the format +/-hhmm to milliseconds
	 * 
	 * @param strTimeZone
	 *                    timezone offset from Greenwich Mean Time (GMT) for example
	 *                    "-0500" is Eastern Standard Time, "-0400" is Eastern
	 *                    Daylight Time, "+0000" is Greenwich Mean Time, and "+0100"
	 *                    is the offset for Europe/Paris.
	 * 
	 * @return milliseconds from Greenwich Mean Time
	 */
	static public int convertTimeZoneToMilliseconds(String strTimeZone) {
		int iMillisecs = 0;
		if (strTimeZone == null || strTimeZone.length() != 5) {
			return iMillisecs;
		}
		// convert timezone (+/-hhmm)
		String strSign = strTimeZone.substring(0, 1);
		String strHours = strTimeZone.substring(1, 3);
		String strMinutes = strTimeZone.substring(3, 5);
		try {
			int iHours = new Integer(strHours).intValue();
			int iMinutes = new Integer(strMinutes).intValue();
			iMillisecs = iMinutes * iMinuteMilliseconds;
			iMillisecs = iMillisecs + (iHours * iHourMilliseconds);
			if (strSign.startsWith("-") == true) {
				iMillisecs *= -1;
			}
		} catch (NumberFormatException nfe) {
			iMillisecs = 0;
		}
		return iMillisecs;
	}

	/**
	 * Transform a fully qualified Class' name into just the name of the class
	 * without the leading package. For example,
	 * "com.whitelistmasker.masker.MaskerDate" would return just "MaskerDate"
	 * 
	 * @param inClass
	 * @return name of the class without leading qualification
	 */
	static public String getNameFromClass(Class<?> inClass) {
		return inClass.getName().lastIndexOf(".") == -1 ? inClass.getName()
				: inClass.getName().substring(inClass.getName().lastIndexOf(".") + 1);
	}

	/**
	 * Determine if the String is empty (equals "").
	 * 
	 * @param strInput
	 *                 the string to be evaluated.
	 * @return true if the strInput compares to {@link MaskerConstants#EMPTY_String}
	 *         (""). Returns false if strInput is null or not empty.
	 */
	static public boolean isEmpty(String strInput) {
		if (strInput == null) {
			return false;
		}
		return strInput.compareTo(MaskerConstants.EMPTY_String) == 0;
	}

	static public boolean isUndefined(String test) {
		if (test == null || test.length() == 0 || "?".equals(test)) {
			return true;
		}
		return false;
	}

	/**
	 * Tests whether the supplied url is valid
	 * 
	 * @param url
	 *            the URL to be tested
	 * @return true if the URL references http or https protocol
	 */
	static public boolean isValidURL(String url) {
		boolean result = false;
		// must be http:// at minimum, could be https://
		if (url.length() < 7) {
			return result;
		}
		String secure = url.substring(4, 5);
		if (secure.equalsIgnoreCase("s")) {
			if (url.length() < 8) {
				return result;
			}
			if (url.length() >= 12 && url.substring(5, 12).equals("://http")) {
				return result;
			}
			result = url.substring(5, 8).equals("://");
		} else {
			if (secure.equals(":") == false) {
				return result;
			}
			// now check for //
			if (url.length() >= 11 && url.substring(4, 11).equals("://http")) {
				return result;
			}
			result = url.substring(5, 7).equals("//");
		}
		return result;
	}

	/**
	 * Construct and return a sorted list of files in a directory identified by the
	 * dir that have extensions matching the ext
	 * 
	 * @param dir
	 *            the path to the directory containing files to be returned in the
	 *            list
	 * @param ext
	 *            the file extension (without the leading period) used to filter
	 *            files in the dir
	 * @return sorted list of files in a directory identified by the dir that have
	 *         extensions matching the ext
	 * @throws IOException
	 *                     if there is difficulty accessing the files in the
	 *                     supplied dir
	 */
	static public List<Path> listSourceFiles(Path dir, String ext) throws IOException {
		List<Path> result = new ArrayList<Path>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{" + ext + "}")) {
			for (Path entry : stream) {
				result.add(entry);
			}
		} catch (DirectoryIteratorException ex) {
			// I/O error encountered during the iteration, the cause is an
			// IOException
			throw ex.getCause();
		}
		result.sort(null);
		return result;
	}

	/**
	 * Transform a list of fields contained in a String bounded with opening ('{')
	 * and closing ('}') braces, and delimited with one of the delimiters (comma,
	 * space, tab, pipe). Fields containing strings are expected to be enclosed in
	 * double quotes ('"').
	 * 
	 * @param strList
	 *                the list of fields enclosed in braces.
	 * @return an ArrayList of the fields parsed from the supplied list string.
	 *         Note: if the passed strList is null or empty, an empty ArrayList is
	 *         returned (e.g., its size() is 0).
	 * @see #arrayListToListString
	 */
	static public ArrayList<Object> listStringToArrayList(String strList) {
		ArrayList<Object> list = new ArrayList<Object>();
		if (strList == null || strList.length() == 0) {
			return list;
		}
		// expects a string enclosed in open/close braces
		// strip off the braces and parse the fields in the list.
		String strRecord = strList;
		while (strRecord.startsWith("{") == true) {
			strRecord = strRecord.substring(1);
		}
		while (strRecord.endsWith("}") == true) {
			strRecord = strRecord.substring(0, strRecord.length() - 1);
		}
		Object[] objList = parseFields(strRecord);
		for (int i = 0; i < objList.length; i++) {
			list.add(objList[i]);
		}
		return list;
	}

	/**
	 * Load the specified JSON file from the fully qualified file name or throw the
	 * appropriate exception.
	 * 
	 * @param jsonFQFileName
	 *                       name of the JSON file to be loaded
	 * @return the JSONObject contained in the file, or an empty JSONObject if no
	 *         object exists
	 * @throws Exception
	 *                   If the file can no be located, or if there is a problem
	 *                   reading the file
	 */
	static public JSONObject loadJSONFile(String jsonFQFileName) throws Exception {
		JSONObject retObj = new JSONObject();
		BufferedReader br = null;
		try {
			br = openTextFile(jsonFQFileName);
			if (br != null) {
				retObj = JSONObject.parse(br);
			}
		} catch (IOException ioe) {
			throw new IOException("Can not parse \"" + jsonFQFileName + "\"", ioe);
		} catch (Exception e) {
			throw new IOException("Can not load file \"" + jsonFQFileName + "\"", e);
		} finally {
			closeTextFile(br);
		}
		return retObj;
	}

	/**
	 * Reads the lines of a text file into a list of strings and returns that list.
	 * If no lines are present (e.g., empty file) then an empty list is returned.
	 * 
	 * @param fqFilename
	 *                   fully qualified filename
	 * @return list of strings read from the file
	 * @throws Exception
	 *                   if the file can not be read.
	 */
	static public List<String> loadTextFile(String fqFilename) throws Exception {
		List<String> result = new ArrayList<String>();
		BufferedReader br = openTextFile(fqFilename);
		String line = br.readLine();
		while (line != null) {
			result.add(line);
			line = br.readLine();
		}
		MaskerUtils.closeTextFile(br);
		return result;
	}

	/**
	 * @param fqFilename
	 *                   fully qualified name of the text file to be opened
	 * @return open buffered reader to allow individual lines of a text file to be
	 *         read
	 * @throws Exception
	 * @see #closeTextFile(BufferedReader) to close the reader returned by this
	 *      function
	 */
	static public BufferedReader openTextFile(String fqFilename) throws Exception {
		BufferedReader input = null;
		File inputFile = new File(fqFilename);
		if (inputFile.exists() == false) {
			throw new Exception(inputFile.getCanonicalPath() + " does not exist.");
		}
		if (inputFile.isFile() == false) {
			throw new IOException(
					"Input is not a file: " + inputFile.getCanonicalPath() + File.separator + inputFile.getName());
		}
		if (inputFile.canRead() == false) {
			throw new IOException(
					"Can not read file " + inputFile.getCanonicalPath() + File.separator + inputFile.getName());
		}
		input = new BufferedReader(new FileReader(inputFile));
		return input;
	}

	/**
	 * Helper method to create strings of the form "000nn".
	 * 
	 * @param iIn
	 *               integer value to be right justified with leading characters in
	 *               the returned String.
	 * @param iWidth
	 *               integer value of the width of the returned String.
	 * @param cPad
	 *               character value to be used to pad the left portion of the
	 *               returned String to make it as wide as the specified iWidth
	 *               parameter. For example, calling toLeftPaddedString(iNum,4,'0')
	 *               would result in "0045" if iNum == 45, or "0004" if iNum == 4.
	 * 
	 * @return String containing the right justified value, padded to the specified
	 *         with the specified pad character.
	 */
	static public String padLeft(int iIn, int iWidth, char cPad) {
		String strTemp = String.valueOf(iIn);
		return padLeft(strTemp, iWidth, cPad);
	}

	/**
	 * Creates a new String padded on its left side with the supplied pad character
	 * guaranteed to be the supplied length. If the supplied length is less than or
	 * equal to the length of the supplied string, the supplied string is returned.
	 * If the supplied string is null, a new string is returned filled with the
	 * supplied pad character that is as long as the supplied length.
	 * 
	 * @param strInput
	 * @param iMax
	 * @param cPadChar
	 * @return formatted string with padding
	 */
	static public String padLeft(String strInput, int iMax, char cPadChar) {
		if (strInput == null) {
			char[] padChars = new char[iMax];
			Arrays.fill(padChars, cPadChar);
			return new String(padChars);
		}
		int iLength = strInput.length();
		if (iLength < iMax) {
			char[] padChars = new char[iMax - iLength];
			Arrays.fill(padChars, cPadChar);
			return new String(padChars) + strInput;
		}
		// else already bigger so leave it alone
		return strInput;
	}

	/**
	 * Creates a new String padded on its left side with zeros ('0') that is
	 * guaranteed to be the supplied length. If the supplied length is less than or
	 * equal to the length of the supplied string, the supplied string is returned.
	 * If the supplied string is null, a new string is returned filled with zeros as
	 * long as the supplied length.
	 * 
	 * @param iValue
	 *               the value to be right justified and left padded with zeros in
	 *               the returned string.
	 * @param iMax
	 *               the desired maximum length of the String to be returned.
	 * @return a string with the value right justified with leading zeros to fill
	 *         out to the desired maximum length specified. If iMax is less than the
	 *         number of digits in the value, the returned string will be large
	 *         enough to represent the entire value with no padding applied.
	 */
	static public String padLeftZero(int iValue, int iMax) {
		return padLeft(String.valueOf(iValue), iMax, '0');
	}

	/**
	 * Creates a new String padded on its left side with zeros ('0') that is
	 * guaranteed to be the supplied length. If the supplied length is less than or
	 * equal to the length of the supplied string, the supplied string is returned.
	 * If the supplied string is null, a new string is returned filled with zeros as
	 * long as the supplied length.
	 * 
	 * @param strInput
	 *                 the input string to be right justified and left padded with
	 *                 zeros in the returned string.
	 * @param iMax
	 *                 the desired maximum length of the String to be returned.
	 * @return a string with the input string right justified with leading zeros to
	 *         fill out to the desired maximum length specified. If iMax is less
	 *         than the length of the input string, the returned string will be
	 *         input string.
	 */
	static public String padLeftZero(String strInput, int iMax) {
		return padLeft(strInput, iMax, '0');
	}

	/**
	 * Helper method to create strings of the form "nn000".
	 * 
	 * @param iIn
	 *               integer value to be right justified with leading characters in
	 *               the returned String.
	 * @param iWidth
	 *               integer value of the width of the returned String.
	 * @param cPad
	 *               character value to be used to pad the right portion of the
	 *               returned String to make it as wide as the specified iWidth
	 *               parameter. For example, calling toRightPaddedString(iNum,4,'0')
	 *               would result in "4500" if iNum == 45, or "4000" if iNum == 4.
	 * 
	 * @return String containing the right justified value, padded to the specified
	 *         with the specified pad character.
	 */
	static public String padRight(int iIn, int iWidth, char cPad) {
		String strTemp = String.valueOf(iIn);
		return padRight(strTemp, iWidth, cPad);
	}

	/**
	 * Creates a new String padded on its right side with the supplied pad character
	 * guaranteed to be the supplied length. If the supplied length is less than or
	 * equal to the length of the supplied string, the supplied string is returned.
	 * If the supplied string is null, a new string is returned filled with the
	 * supplied pad character that is as long as the supplied length.
	 * 
	 * @param strInput
	 * @param iMax
	 * @param cPadChar
	 * @return formatted string with padding
	 */
	static public String padRight(String strInput, int iMax, char cPadChar) {
		if (strInput == null) {
			char[] padChars = new char[iMax];
			Arrays.fill(padChars, cPadChar);
			return new String(padChars);
		}
		int iLength = strInput.length();
		if (iLength < iMax) {
			char[] padChars = new char[iMax - iLength];
			Arrays.fill(padChars, cPadChar);
			return strInput + new String(padChars);
		}
		// else already bigger so leave it alone
		return strInput;
	}

	/**
	 * Creates a new String padded on its right side with zeros ('0') that is
	 * guaranteed to be the supplied length. If the supplied length is less than or
	 * equal to the length of the supplied string, the supplied string is returned.
	 * If the supplied string is null, a new string is returned filled with zeros as
	 * long as the supplied length.
	 * 
	 * @param iValue
	 *               the value to be right justified and right padded with zeros in
	 *               the returned string.
	 * @param iMax
	 *               the desired maximum length of the String to be returned.
	 * @return a string with the value right justified with leading zeros to fill
	 *         out to the desired maximum length specified. If iMax is less than the
	 *         number of digits in the value, the returned string will be large
	 *         enough to represent the entire value with no padding applied.
	 */
	static public String padRightZero(int iValue, int iMax) {
		return padRight(String.valueOf(iValue), iMax, '0');
	}

	/**
	 * Creates a new String padded on its right side with zeros ('0') that is
	 * guaranteed to be the supplied length. If the supplied length is less than or
	 * equal to the length of the supplied string, the supplied string is returned.
	 * If the supplied string is null, a new string is returned filled with zeros as
	 * long as the supplied length.
	 * 
	 * @param strInput
	 *                 the input string to be right justified and right padded with
	 *                 zeros in the returned string.
	 * @param iMax
	 *                 the desired maximum length of the String to be returned.
	 * @return a string with the input string right justified with leading zeros to
	 *         fill out to the desired maximum length specified. If iMax is less
	 *         than the length of the input string, the returned string will be
	 *         input string.
	 */
	static public String padRightZero(String strInput, int iMax) {
		return padRight(strInput, iMax, '0');
	}

	/**
	 * Method to parse the passed String record to extract data values defined by
	 * fields delimited by space (0x20), comma (0x2C), pipe (0x7C) or tab (0x09).
	 * The fields are examined to determine if they contain data able to be
	 * transformed into int, double, or Strings, in that order. A list is
	 * represented by content enclosed in open/closed braces ('{' and '}') and is
	 * preserved as such in a String. Lists may include embedded quotes and
	 * delimiters.
	 * 
	 * @param strRecord
	 *                  A String containing a record to be parsed.
	 * 
	 * @return An Object[] containing each of the data fields parsed from the
	 *         record. If the input string is null or empty, an empty Object[] is
	 *         returned (e.g., new Object[0]).
	 */
	static public Object[] parseFields(String strRecord) {
		if (strRecord == null || strRecord.length() == 0) {
			return new Object[0];
		}
		ArrayList<Object> retArray = new java.util.ArrayList<Object>();
		// parse the input record for its fields
		byte[] recbytes = strRecord.getBytes();
		int iLength = recbytes.length;
		boolean bInList = false;
		boolean bInQuoted = false;
		boolean bInField = false;
		byte bLastByte = 0x00;
		boolean bNeedDelim = false;
		int iFieldStart = 0;
		int iFieldCount = 0;
		for (int i = 0; i < iLength; i++) {
			switch ((int) (recbytes[i])) {
			// space, tab, and comma are delimiters if not in quoted field
			case 0x20: // space
			case 0x7C: // pipe '|'
			case 0x09: // tab
			{
				if (bInField == false) {
					// skip merrily along
					iFieldStart++;
					bNeedDelim = false;
					break; // skip this
				} else if ((bInQuoted == false) && (bInList == false)) {
					// found field delimiter, process this field
					// get our data
					String strField = new String(recbytes, iFieldStart, iFieldCount);
					retArray.add(processField(strField));
					bInQuoted = false;
					bInField = false;
					bNeedDelim = false;
					iFieldCount = 0;
					break;
				}
				// we're in a quoted field, so count this byte
				iFieldCount++;
				break;
			}
			case 0x2C: // comma
			{
				if (bInField == false) {
					if (bLastByte == 0x2C) {
						// consecutive commas, treat as an empty (undefined)
						// field
						retArray.add(processField(null));
						bInQuoted = false;
						bInField = false;
						bNeedDelim = false;
						iFieldCount = 0;
					} else {
						// skip merrily along
						iFieldStart++;
						bNeedDelim = false;
					}
					break; // skip this
				} else if ((bInQuoted == false) && (bInList == false)) {
					// found field delimiter, process this field
					// get our data
					String strField = new String(recbytes, iFieldStart, iFieldCount);
					retArray.add(processField(strField));
					bInQuoted = false;
					bInField = false;
					bNeedDelim = false;
					iFieldCount = 0;
					break;
				}
				// we're in a quoted field, so count this byte
				iFieldCount++;
				break;
			}
			// start or end quoted field
			case 0x22: // double quote "
			{
				if (bInField == false) {
					// start new field on next char
					iFieldStart = i + 1;
					iFieldCount = 0;
					bInField = true;
					bInQuoted = true;
					break;
				} else if (bInQuoted == true) {
					// found end of quoted string == end of field
					// get our data
					String strField = new String(recbytes, iFieldStart, iFieldCount);
					// retArray.add(processField(strField));
					retArray.add(strField); // save as a String regardless
					bInQuoted = false;
					bInField = false;
					bNeedDelim = true;
					iFieldCount = 0;
					break;
				} // else just count this as a normal field character
				iFieldCount++;
				break;
			}
			// start or end quoted field
			case 0x7B: { // open brace '{'
				if (bInField == false) {
					// start new field on next char
					iFieldStart = i + 1;
					iFieldCount = 0;
					bInField = true;
					bInList = true;
					break;
				} // else just count this as a normal field character }
				iFieldCount++;
				break;
			}
			case 0x7D: { // close brace '}'
				if (bInList == true) {
					// found end of list == end of field
					// get our data
					String strField = new String(recbytes, iFieldStart, iFieldCount);
					// add back list delimiters (will be treated as String in
					// DataTable)
					strField = "{" + strField + "}";
					retArray.add(processField(strField));
					bInList = false;
					bInField = false;
					bNeedDelim = true;
					iFieldCount = 0;
					break;
				} // else just count this as a normal field character }
				iFieldCount++;
				break;
			}
			default: { // field char
				if (bInField == false) {
					if (bNeedDelim == false) {
						// start new field on this char
						iFieldStart = i;
						iFieldCount = 1;
						bInField = true;
						bInQuoted = false;
						break;
					} // otherwise, skip this char
					break;
				}
				// already in field, count this byte
				iFieldCount++;
				break;
			}
			}
			bLastByte = recbytes[i];
		}
		// process remainder if any
		if ((bInField == true) || (bLastByte == 0x2C)) {
			String strField = new String(recbytes, iFieldStart, iFieldCount);
			retArray.add(processField(strField));
		}
		return retArray.toArray();
	}

	/**
	 * Retrieve the object from the passed String by interpreting the content of the
	 * string to guess if it contains an Integer, Double, or String. The guess is
	 * based on first checking for a decimal polong ('.') and if it is present,
	 * attempting to create a Double, otherwise, attempting to create an Integer. If
	 * the attempted creating fails, the content is retained as a String.
	 * 
	 * @param strField
	 *                 the String containing the potential Integer or Double value.
	 * @return an Integer, Double or String object. If the input string is null or
	 *         empty, null is returned.
	 */
	static public Object processField(String strField) {
		if (strField == null || strField.length() == 0) {
			return null;
		}
		// check to see if decimal point is present
		Object objField = null;
		try {
			if (strField.indexOf('.') == -1) {
				Integer iField = new Integer(strField);
				objField = iField;
			} else {
				Double dField = new Double(strField);
				objField = dField;
			}
		} catch (Exception e) {
			// assume String
			if (strField == null || strField.length() == 0) {
				strField = MaskerConstants.UNDEFINED_String;
			}
			objField = strField;
		}
		return objField;
	}

	/**
	 * Print the supplied prompt (if not null) and return the trimmed response
	 * 
	 * @param strPrompt
	 * @return the trimmed response to the prompt (may be the empty String ("") if
	 *         nothing entered)
	 */
	static public String prompt(String strPrompt) {
		return prompt(strPrompt, true);
	}

	/**
	 * Print the supplied prompt (if not null) and return the trimmed response
	 * according to the supplied trim control
	 * 
	 * @param strPrompt
	 * @param bTrim
	 * @return the trimmed response (if so commanded) to the prompt (may be the
	 *         empty String ("") if nothing entered)
	 */
	static public String prompt(String strPrompt, boolean bTrim) {
		String strReply = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			if ((strPrompt != null) && (strPrompt.length() != 0)) {
				System.out.println(strPrompt);
			}
			strReply = in.readLine();
			if (bTrim && strReply != null) {
				strReply = strReply.trim();
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return strReply;
	}

	/**
	 * Save the specified JSONObject in serialized form to the specified file or
	 * throw the appropriate exception.
	 * 
	 * @param jsonFileName
	 *                     fully qualified name of the JSON file to be saved
	 * @param jsonData
	 *                     the JSONObject to be saved to a file.
	 * @return the jsonData that was saved
	 * @throws Exception
	 *                   {@link IOException}) if there is a problem writing the file
	 */
	static public JSONObject saveJSONFile(String jsonFileName, JSONObject jsonData) throws Exception {
		if (jsonData == null) {
			throw new InvalidObjectException("jsonData is null");
		}
		if (jsonFileName == null || jsonFileName.trim().length() == 0) {
			throw new InvalidTargetObjectTypeException("Output filename is null or empty.");
		}
		BufferedWriter br = null;
		try {
			File outputFile = new File(jsonFileName);
			// write the JSON file
			br = new BufferedWriter(new FileWriter(outputFile));
			br.write(jsonData.serialize(true));
		} catch (IOException e) {
			throw new IOException("Can not write file \"" + jsonFileName + "\"", e);
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				// error trying to close writer ...
			}
		}

		return jsonData;
	}

	/**
	 * Save the specified JSONObject in serialized form to the specified file or
	 * throw the appropriate exception.
	 * 
	 * @param textFileName
	 *                     fully qualified name of the JSON file to be saved
	 * @param content
	 *                     the content to be saved to a file.
	 * @throws Exception
	 *                   {@link IOException}) if there is a problem writing the file
	 */
	static public void saveTextFile(String textFileName, String content) throws Exception {
		if (content == null) {
			throw new InvalidObjectException("content is null");
		}
		if (textFileName == null || textFileName.trim().length() == 0) {
			throw new InvalidTargetObjectTypeException("Output filename is null or empty.");
		}
		BufferedWriter br = null;
		try {
			File outputFile = new File(textFileName);
			br = new BufferedWriter(new FileWriter(outputFile));
			br.write(content);
		} catch (IOException e) {
			throw new IOException("Can not write file \"" + textFileName + "\"", e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// error trying to close writer ...
			}
		}

		return;
	}

	static public JSONObject sendRequest(String method, JSONObject service, JSONObject params) throws Exception {
		JsonObject serviceObj = s_gson.fromJson(service.toString(), JsonObject.class);
		JsonObject parameters = s_gson.fromJson(params.toString(), JsonObject.class);
		return (JSONObject) JSON.parse(sendRequest(method, serviceObj, parameters).toString());
	}

	static public ObjectNode sendRequest(String method, ObjectNode serviceObj, ObjectNode params) throws Exception {
		JsonObject parameters = s_gson.fromJson(params.toString(), JsonObject.class);
		return sendRequest(method, serviceObj, parameters);
	}

	static public JsonObject sendRequest(String method, JsonObject serviceObj, JsonObject params) throws Exception {
		if (method == null) {
			throw new Exception("The method is null.");
		}
		if (serviceObj == null) {
			throw new Exception("The serviceObj is null");
		}
		if (params == null) {
			throw new Exception("The params object is null");
		}
		ObjectNode service = (ObjectNode) new ObjectMapper().readTree(serviceObj.toString());
		ObjectNode response = sendRequest(method, service, params);
		return s_gson.fromJson(response.toString(), JsonObject.class);
	}

	static public ObjectNode sendRequest(String method, ObjectNode serviceObj, JsonObject params) throws Exception {
		if (method == null) {
			throw new Exception("The method is null.");
		}
		if (serviceObj == null) {
			throw new Exception("The serviceObj is null");
		}
		if (params == null) {
			throw new Exception("The params object is null");
		}
		String protocol = getStringFromObject(serviceObj, "protocol").trim();
		String domain = getStringFromObject(serviceObj, "domain").trim();
		// allow number or string of number
		JsonNode portnumberElt = serviceObj.get("portnumber");
		if (portnumberElt == null) {
			throw new Exception("The portnumber in the service is missing.");
		}
		String portnumber = "";
		if (portnumberElt.isValueNode()) {
			portnumber = portnumberElt.asText().trim();
		} else {
			throw new Exception("The portnumber is not a String of a number, nor a Number.");
		}
		String endpoint = getStringFromObject(serviceObj, "endpoint", "").trim();
		String username = getStringFromObject(serviceObj, "username").trim();
		String password = getStringFromObject(serviceObj, "password").trim();
		String apitimeout = getStringFromObject(serviceObj, "apitimeout").trim();
		return sendRequest(method, protocol, domain, portnumber, endpoint, username, password, apitimeout, params);
	}

	static public String getStringFromObject(ObjectNode obj, String key) throws Exception {
		JsonNode test = obj.get(key);
		if (test == null) {
			throw new Exception("The " + key + " is missing.");
		}
		if (test.isTextual()) {
			return test.asText();
		}
		throw new Exception("The value for " + key + " is not a String.");
	}

	static public String getStringFromObject(ObjectNode obj, String key, String defaultValue) throws Exception {
		try {
			return getStringFromObject(obj, key);
		} catch (Exception e) {
			/**
			 * TODO: define exceptions as format templates to avoid error due to rewording
			 */
			if (e.getLocalizedMessage().endsWith("is missing.")) {
				return defaultValue;
			} else {
				throw e;
			}
		}
	}

	static public ObjectNode sendRequest(String method, String protocol, String domain, String portnumber,
			String endpoint, String username, String password, String apitimeout, JsonObject params) throws Exception {
		if (method == null) {
			throw new Exception("The method is null.");
		}
		method = method.trim();
		if (method.length() == 0) {
			throw new Exception("The method is empty.");
		}
		if (params == null) {
			throw new Exception("The params object is null");
		}
		int timeout = 10000;
		try {
			timeout = new Integer(apitimeout);
			if (timeout < 0) {
				throw new Exception("The apitimeout is less than zero milliseconds.");
			}
		} catch (NumberFormatException nfe) {
			throw new Exception("The apitimeout is not a positive integer of milliseconds.");
		}
		ObjectNode responseObj = null;
		String url = protocol + "://" + domain;
		if (portnumber != null && portnumber.length() > 0) {
			url += ":" + portnumber;
		}
		boolean needsQuestionMark = (endpoint.indexOf("?") == -1);
		// Note: assume endpoint entered with appropriate encoding
		url += endpoint;
		if ("GET".equals(method)) {
			// move parameters to URL query string
			String key = null;
			JsonElement value;
			boolean first = true;
			StringBuffer sb = new StringBuffer();
			if (params.size() > 0) {
				for (Iterator<String> it = params.keySet().iterator(); it.hasNext();) {
					if (first) {
						if (needsQuestionMark) {
							sb.append("?");
						} else {
							// assume appending additional parameters
							sb.append("&");
						}
						first = false;
					} else {
						sb.append("&");
					}
					key = it.next();
					sb.append(key);
					sb.append("=");
					value = (JsonElement) params.get(key);
					sb.append(s_gson.toJson(value));
				}
				url += URLEncoder.encode(sb.toString(), "UTF-8");
			}
		}
		if (s_debug) {
			System.out.println("URL: " + url);
		}
		URL obj = new URL(url);
		HttpURLConnection.setFollowRedirects(true);
		HttpURLConnection serviceConnection = (HttpURLConnection) obj.openConnection();
		serviceConnection.setRequestMethod(method);
		if (username != null && password != null) {
			// set up authentication header
			String auth = username + ":" + password;
			// Note: do not use the Base64.getUrlEncoder for authentication
			byte[] authEncBytes = Base64.getEncoder().encode(auth.getBytes());
			String authStringEnc = new String(authEncBytes);
			authStringEnc = "Basic " + authStringEnc;
			serviceConnection.setRequestProperty("Authorization", authStringEnc);
		}
		serviceConnection.setRequestProperty("Content-Type", "application/json");
		serviceConnection.setDoOutput(true);

		OutputStream os = serviceConnection.getOutputStream();
		serviceConnection.setConnectTimeout(timeout);
		if ("GET".equals(method)) {
			os.write(new byte[0]);
		} else {
			os.write(params.toString().getBytes());
		}
		os.flush();
		os.close();
		int responseCode = serviceConnection.getResponseCode();
		String responseMsg = serviceConnection.getResponseMessage();
		if (s_debug) {
			System.out.println("Returned code " + responseCode + " " + responseMsg);
		}
		if (responseCode >= 200 && responseCode < 299) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(serviceConnection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			ObjectMapper mapper = new ObjectMapper();
			// convert request to JsonNode
			JsonNode responseNode = mapper.readTree(response.toString());
			if (responseNode.isObject()) {
				responseObj = (ObjectNode) responseNode;
			}
		} else {
			responseObj = JsonNodeFactory.instance.objectNode();
			responseObj.put("errorCode", responseCode);
			responseObj.put("errorMsg", responseMsg);
		}
		return responseObj;
	};

	static public String trimSpaces(String word) {
		while (word.startsWith(" ")) {
			word = word.substring(1);
		}
		while (word.endsWith(" ")) {
			word = word.substring(0, word.length() - 1);
		}
		return word;
	}

	/**
	 * Converts the input Date to {@link MaskerConstants#UNDEFINED_Date} iff the
	 * input Date is null.
	 * 
	 * @param date
	 *             Date to be tested against null and converted.
	 * @return {@link MaskerConstants#UNDEFINED_Date} if the input Date was null,
	 *         otherwise, the input Date is echoed back.
	 */
	static public Date undefinedForNull(Date date) {
		if (date == null) {
			return MaskerConstants.UNDEFINED_Date;
		}
		return date;
	}

	/**
	 * Converts the input String to {@link MaskerConstants#UNDEFINED_String} iff the
	 * input String is null or empty after being trimmed.
	 * 
	 * @param strValue
	 *                 String to be tested against null or an empty string after
	 *                 being trimmed, and converted to the
	 *                 {@link MaskerConstants#UNDEFINED_String}.
	 * @return {@link MaskerConstants#UNDEFINED_String} if the input String was
	 *         null, otherwise, the input String is echoed back.
	 */
	static public String undefinedForNull(String strValue) {
		if (strValue == null) {
			return MaskerConstants.UNDEFINED_String;
		} else if (strValue.trim().length() == 0) {
			return MaskerConstants.UNDEFINED_String;
		}
		return strValue;
	}

	/**
	 * 
	 */
	public MaskerUtils() {
		// TODO Auto-generated constructor stub
	}

}
