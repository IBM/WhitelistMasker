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

import java.io.File;
import java.io.Serializable;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.api.json.JSONArray;
import com.api.json.JSONObject;

/**
 * There is a facility to run through a specified input directory of json dialog
 * files and mask their content, writing the masked files to the specified
 * output directory.
 * 
 * More importantly, there is a public static method (maskContent) used by the
 * MaskWebServices to enable masking content via REST request.
 */
public class Masker implements Serializable {

	private static final long serialVersionUID = -4315882565512778401L;

	/**
	 * Class to manage associations of reference counts to words being masked
	 *
	 */
	class Tuple {

		Integer _count;
		String _word;

		/**
		 * Constructor
		 * 
		 * @param word
		 *              word to be counted
		 * @param count
		 *              count of references to the word
		 */
		Tuple(String word, Integer count) {
			_word = word;
			_count = count;
		}

		/**
		 * Getter for the reference count of the word
		 * 
		 * @return count of references to the word
		 */
		Integer getCount() {
			return _count;
		}

		/**
		 * Getter for the word being counted
		 * 
		 * @return word being counted
		 */
		String getWord() {
			return _word;
		}

		/**
		 * @return the string representation of the word and its reference count as a
		 *         csv pair
		 */
		@Override
		public String toString() {
			return "\"" + _word + "\", " + _count;
		}
	}

	static String _domainPrefixesFile = "." + File.separator + "DomainPrefixes.txt";
	static List<String> _domainPrefixList = new ArrayList<String>();
	static String _domainSuffixesFile = "." + File.separator + "DomainSuffixes.txt";
	static List<String> _domainSuffixList = new ArrayList<String>();
	static JSONObject _geolocations = new JSONObject();
	static String _geolocationsFileName = "." + File.separator + "geolocations.json";
	static String _initializing = "Initializing";
	static boolean _isInitialized = false;
	static String _maskBad = "$bad$";
	static Map<String, Integer> _maskedWords = new HashMap<String, Integer>();
	static String _maskGeo = "~geo~";
	static String _maskMisc = "~misc~";
	static String _maskName = "~name~";
	static String _maskNum = "~num~";
	static Boolean _maskNumbers = Boolean.TRUE;
	static String _maskPrefix = "~";
	static List<String> _masks = new ArrayList<String>();
	static String _maskTemplatesFile = "." + File.separator + "maskTemplates.json";
	static String _maskURL = "~url~";
	static int _minDialogs = 5;
	static JSONObject _names = new JSONObject();
	static String _namesFileName = "." + File.separator + "names.json";
	static List<Pattern> _patterns = new ArrayList<Pattern>();
	static JSONObject _profanities = new JSONObject();
	static String _profanitiesFileName = "." + File.separator + "profanities.json";
	static String _queryStringContainsFile = "." + File.separator + "QueryStringContains.txt";
	static List<String> _queryStringContainsList = new ArrayList<String>();
	static JSONObject _whitelist = new JSONObject();
	static String _whitelistFileName = "." + File.separator + "whitelist-words.json";
	static final int INDEX_BACKSLASH = 0x4000;
	static final int INDEX_COLON = 0x0080;
	static final int INDEX_COMMA = 0x0400;
	static final int INDEX_CR = 0x0002;
	static final int INDEX_EM_DASH = 0x8000;
	static final int INDEX_GT = 0x0200;
	static final int INDEX_HYPHEN = 0x0020;
	static final int INDEX_LPAREN = 0x0040;
	static final int INDEX_NL = 0x0001;
	static final int INDEX_PERIOD = 0x0010;
	static final int INDEX_PLUS = 0x0800;
	static final int INDEX_RPAREN = 0x2000;
	static final int INDEX_SEMICOLON = 0x1000;
	static final int INDEX_SLASH = 0x0008;
	static final int INDEX_TAB = 0x0004;
	static final int INDEX_UNDERSCORE = 0x0100;

	/**
	 * Static initializer for loading the whitelist and associated mask resources
	 */
	static {
		init();
	}

	/**
	 * Checks whether there is a URL is in the message and whether its domain ends
	 * with an undesirable domain suffix
	 * 
	 * @param message
	 *                input to be checked for an unacceptable URL reference
	 * @return true if this has an acceptable URL reference, otherwise false if the
	 *         message has a URL with an unacceptable URL reference
	 */
	static public boolean acceptableURLReference(String message) {
		String url = message.toLowerCase();
		String domain = null;
		int portIndex = -1;
		int queryStringIndex = -1;
		String queryString = null;
		String[] urlParts = url.split("http[s]?://");
		if (urlParts.length > 1) {
			if (urlParts.length > 2) {
				/**
				 * This message has a URL referncing another URL so we'll mask it until we can
				 * better preserve the 2nd reference in the query string to test for the
				 * queryStringContains.
				 * 
				 * TODO: need to determine whether http:// or https:// was in the referenced URL
				 * in this message to reconstruct it from the parts (e.g., by appending parts
				 * [2] and greater
				 */
				return false;
			}
			domain = urlParts[1];
			portIndex = domain.indexOf(":");
			queryStringIndex = domain.indexOf("/");
			if (queryStringIndex != -1 && queryStringIndex < domain.length() - 1) {
				queryString = domain.substring(queryStringIndex + 1);
				for (String qsFilter : _queryStringContainsList) {
					if (queryString.contains(qsFilter)) {
						return false;
					}
				}
			}
			if (portIndex != -1 && queryStringIndex != -1) {
				// take lower one
				if (portIndex < queryStringIndex) {
					domain = domain.substring(0, portIndex);
				} else {
					domain = domain.substring(0, queryStringIndex);
				}
			} else if (portIndex != -1) {
				domain = domain.substring(0, portIndex);
			} else if (queryStringIndex != -1) {
				domain = domain.substring(0, queryStringIndex);
			} // else domain is correct
			for (String suffix : _domainSuffixList) {
				if (domain.endsWith(suffix)) {
					return false;
				}
			}
			// check prefixes
			for (String prefix : _domainPrefixList) {
				if (domain.startsWith(prefix)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Initialize the various input files used for masking
	 * 
	 * @return true if initialization suceeded
	 */
	static boolean init() {
		synchronized (_initializing) {
			try {
				_whitelist = (JSONObject) MaskerUtils
						.loadJSONFile(MaskerConstants.Masker_DIR_PROPERTIES + _whitelistFileName);
				if (_whitelist == null) {
					System.out.println("Can not find the whitelist key in the file " + MaskerConstants.Masker_DIR_PROPERTIES
							+ _whitelistFileName);
					return false;
				}
			} catch (Exception e) {
				System.out.println("Error loading file " + MaskerConstants.Masker_DIR_PROPERTIES + _whitelistFileName + ": "
						+ e.getLocalizedMessage());
				e.printStackTrace();
				return false;
			}
			try {
				_names = (JSONObject) MaskerUtils.loadJSONFile(MaskerConstants.Masker_DIR_PROPERTIES + _namesFileName);
			} catch (Exception e) {
				System.out.println("Error loading file " + MaskerConstants.Masker_DIR_PROPERTIES + _namesFileName + ": "
						+ e.getLocalizedMessage());
				e.printStackTrace();
				return false;
			}
			try {
				_geolocations = (JSONObject) MaskerUtils
						.loadJSONFile(MaskerConstants.Masker_DIR_PROPERTIES + _geolocationsFileName);
			} catch (Exception e) {
				System.out.println("Error loading file " + MaskerConstants.Masker_DIR_PROPERTIES + _geolocationsFileName
						+ ": " + e.getLocalizedMessage());
				e.printStackTrace();
				return false;
			}
			try {
				_profanities = (JSONObject) MaskerUtils
						.loadJSONFile(MaskerConstants.Masker_DIR_PROPERTIES + _profanitiesFileName);
			} catch (Exception e) {
				System.out.println("Error loading file " + MaskerConstants.Masker_DIR_PROPERTIES + _profanitiesFileName
						+ ": " + e.getLocalizedMessage());
				e.printStackTrace();
				return false;
			}
			try {
				List<String> domainPrefixList = MaskerUtils
						.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + _domainPrefixesFile);
				for (String domainPrefix : domainPrefixList) {
					if (domainPrefix.startsWith("_")) {
						continue;
					}
					_domainPrefixList.add(domainPrefix.toLowerCase());
				}
			} catch (Exception e) {
				System.out.println("Error loading file " + MaskerConstants.Masker_DIR_PROPERTIES + _domainPrefixesFile
						+ ": " + e.getLocalizedMessage());
				return false;
			}
			try {
				List<String> domainSuffixList = MaskerUtils
						.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + _domainSuffixesFile);
				for (String domainSuffix : domainSuffixList) {
					if (domainSuffix.startsWith("_")) {
						continue;
					}
					_domainSuffixList.add(domainSuffix.toLowerCase());
				}
			} catch (Exception e) {
				System.out.println("Error loading file " + MaskerConstants.Masker_DIR_PROPERTIES + _domainSuffixesFile
						+ ": " + e.getLocalizedMessage());
				return false;
			}
			try {
				List<String> queryStringContainsList = MaskerUtils
						.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + _queryStringContainsFile);
				for (String queryStringContains : queryStringContainsList) {
					if (queryStringContains.startsWith("_")) {
						continue;
					}
					_queryStringContainsList.add(queryStringContains.toLowerCase());
				}
			} catch (Exception e) {
				System.out.println("Error loading file " + MaskerConstants.Masker_DIR_PROPERTIES + _queryStringContainsFile
						+ ": " + e.getLocalizedMessage());
				return false;
			}

			try {
				JSONObject maskTemplates = MaskerUtils
						.loadJSONFile(MaskerConstants.Masker_DIR_PROPERTIES + _maskTemplatesFile);
				Object test = maskTemplates.get("maskNumbers");
				if (test != null && test instanceof Boolean) {
					_maskNumbers = (Boolean) test;
				}
				JSONArray templates = (JSONArray) maskTemplates.get("templates");
				if (templates == null) {
					templates = new JSONArray();
				}
				String addPattern = "";
				String addMask = "";
				JSONObject jObj;
				for (Object obj : templates) {
					jObj = (JSONObject) obj;
					addPattern = (String) jObj.get("template");
					addMask = (String) jObj.get("mask");
					if (addPattern != null && addMask != null) {
						addPattern = addPattern.trim();
						// ensure masks are lowercase to work with masking check
						addMask = addMask.toLowerCase().trim();
						// ensure there is no wrapper
						if (addMask.startsWith(_maskPrefix) == true) {
							addMask = addMask.substring(1);
						}
						if (addMask.endsWith(_maskPrefix) == true) {
							addMask = addMask.substring(0, addMask.length() - 1);
						}
						if (addMask.length() > 0) {
							try {
								Pattern newPattern = Pattern.compile(addPattern);
								_patterns.add(newPattern);
								_masks.add(addMask);
							} catch (PatternSyntaxException pse) {
								System.out.println("Skipping \"" + addPattern + "\" because it did not compile: "
										+ pse.getLocalizedMessage());
							}
						} else {
							System.out.println("Skipping \"" + addPattern + "\" because its \"mask\" was empty.");
						}
					} else {
						if (addPattern == null) {
							System.out.println("Skipping \"" + addMask + "\" because its \"template\" was missing or null.");
						} else {
							System.out.println("Skipping \"" + addPattern + "\" because its \"mask\" was missing or null.");
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Error loading file " + MaskerConstants.Masker_DIR_PROPERTIES + _maskTemplatesFile + ": "
						+ e.getLocalizedMessage());
				return false;
			}
			System.out.println("System initialized properly.");
			_isInitialized = true;
		}
		return true;
	}

	/**
	 * Tests whether the testWord comprises all number values
	 * 
	 * @param testWord
	 *                 word to be tested
	 * @return true if the supplied word comprises all number values between ASCII 0
	 *         and 9
	 */
	static boolean isNumbers(String testWord) {
		if (testWord == null || testWord.length() == 0) {
			return false;
		}
		char testChar = ' ';
		for (int i = 0; i < testWord.length(); i++) {
			testChar = testWord.charAt(i);
			if (testChar < 0x0030 || testChar > 0x0039) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Main entry point to run the filtering of JSON-based dialog files in the
	 * specified input directory having a specified file extension to the output
	 * directory iff the dialog number of dialogs is at least the minimum number
	 * specified, after masking their content.
	 * 
	 * @param args
	 *             the input directory, the output directory, the
	 *             whitelist-words.json file, the names.json file, the
	 *             geolocations.json file, the profanities.json file, the
	 *             DomainPrefixes.txt file, the DomainSuffixes.txt file, the
	 *             QueryStringContains.txt file, the minimum number of dialogs
	 *             value, and a flag whether numbers should be masked. If these are
	 *             not specified, the program will prompt for them and provide an
	 *             opportunity to quit before execution of the filtering begins.
	 */
	public static void main(String[] args) {
		Masker pgm = new Masker();
		if (pgm.getParams(args)) {
			System.out.println("\nFiles ending with ." + pgm._ext + " will be read from " + pgm._inputPath //
					+ "\nand content not in the whitelist will be masked."//
					+ "\nIf the dialog contains a reference to a URL" //
					+ "\nwith a domain not ending with a suffix in the domain suffixes list" //
					+ "\nnor starting with a prefix in the domain prefixes list" //
					+ "\nnor containing a string in the query string contains list" //
					+ "\nthe URL will not be masked. If the masked dialog file" //
					+ "\nhas at least the minimum number of dialogs per day," //
					+ "\nthe dialog content will be saved to the output directory " + pgm._outputPath); //
			if (MaskerUtils.prompt("Press q to quit or press Enter to continue...").length() == 0) {
				try {
					List<Path> files = MaskerUtils
							.listSourceFiles(FileSystems.getDefault().getPath(pgm._inputPath.toString()), pgm._ext);
					Collections.sort(files);
					for (Path file : files) {
						pgm.doWork(file);
					}
					if (pgm._totalWords != 0L) {
						Double pct = (100.0d * pgm._totalMasked) / pgm._totalWords;
						System.out.println("For " + pgm._totalDialogs + " total dialogs there were " + pgm._totalMasked
								+ " masked words of " + pgm._totalWords + " total words (" + pgm._formatter.format(pct) + "%)");
					}
				} catch (Exception e) {
					System.out.println("Can not reference files with extension " + pgm._ext + " in directory "
							+ pgm._inputPath + " reason: " + e.getLocalizedMessage());
				}
				// save blacklist words
				List<Tuple> blacklist = new ArrayList<Tuple>();
				String word = "";
				for (Iterator<String> it = Masker._maskedWords.keySet().iterator(); it.hasNext();) {
					word = it.next();
					blacklist.add(pgm.new Tuple(word, (Integer) Masker._maskedWords.get(word)));
				}
				Collections.sort(blacklist, new Comparator<Tuple>() {

					@Override
					public int compare(Tuple o1, Tuple o2) {
						// reverse sort largest first
						return o2.getCount() - o1.getCount();
					}

				});
				StringBuffer sb = new StringBuffer();
				for (Tuple elt : blacklist) {
					sb.append(elt.toString());
					sb.append("\n");
				}
				try {
					System.out.println("Writing blacklist to " + pgm._outputPath + "blacklist.txt");
					MaskerUtils.saveTextFile(pgm._outputPath + "blacklist.txt", sb.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.out.println();
		}
		System.out.println("Goodbye");
	}

	/**
	 * Receives a JSON request object containing an array of templates, each
	 * providing a regex template and a mask to be applied replacing the text
	 * matching the regex pattern. An optional maskNumbers boolean value can be
	 * provided to control whether numbers are to be masked. A response JSON object
	 * is returned containing an array of masked text lines, and any errors
	 * encountered while attempting to perform the masking.
	 * 
	 * @param request
	 *                (see above)
	 * @return response (see above)
	 * @throws Exception
	 *                   if a supplied regex pattern in a template fails to compile
	 *                   or an invalid mask is provided.
	 */
	static public JSONObject maskContent(JSONObject request) throws Exception {
		boolean maskNumbers = true; // default
		JSONObject counts = new JSONObject();
		counts.put("maskedBad", 0L);
		counts.put("maskedGeo", 0L);
		counts.put("maskedMisc", 0L);
		counts.put("maskedNam", 0L);
		counts.put("maskedNum", 0L);
		counts.put("maskedURL", 0L);

		counts.put("words", 0L);
		counts.put("masked", 0L);

		JSONObject response = new JSONObject();
		if (!_isInitialized) {
			if (!Masker.init()) {
				throw new Exception("Can not initialize masking environment.");
			}
		}
		Object test = request.get("maskNumbers");
		if (test != null && test instanceof Boolean) {
			maskNumbers = (Boolean) test;
		}
		JSONArray templates = (JSONArray) request.get("templates");
		JSONArray unmasked = (JSONArray) request.get("unmasked");
		JSONArray masked = new JSONArray();
		JSONArray errors = new JSONArray();
		response.put("masked", masked);
		response.put("errors", errors);
		String line = "";
		String maskedLine = "";

		List<Pattern> patterns = new ArrayList<Pattern>();
		List<String> masks = new ArrayList<String>();
		if (templates != null) {
			for (Object obj : templates) {
				if (obj == null) {
					continue;
				}
				JSONObject template = (JSONObject) obj;
				String pattern = (String) template.get("template");
				String mask = (String) template.get("mask");
				if (pattern != null && mask != null) {
					mask = mask.trim();
					// ensure masks are lowercase to work with masking check
					mask = mask.toLowerCase().trim();
					// ensure there is no wrapper
					if (mask.startsWith(_maskPrefix) == true) {
						mask = mask.substring(1);
					}
					if (mask.endsWith(_maskPrefix) == true) {
						mask = mask.substring(0, mask.length() - 1);
					}
					if (mask.length() > 0) {
						try {
							Pattern patternComp = Pattern.compile(pattern);
							patterns.add(patternComp);
							masks.add(mask);
						} catch (PatternSyntaxException pse) {
							JSONObject error = new JSONObject();
							error.put("template", pattern);
							error.put("mask", mask);
							error.put("error", pse.getLocalizedMessage());
							errors.add(error);
						}
					} else {
						JSONObject error = new JSONObject();
						error.put("template", pattern);
						error.put("mask", mask);
						error.put("error", "\"mask\" was empty.");
						errors.add(error);
					}
				} else {
					if (pattern == null) {
						JSONObject error = new JSONObject();
						error.put("template", null);
						error.put("mask", mask);
						error.put("error", "\"template\" was missing or null.");
						errors.add(error);
					} else {
						JSONObject error = new JSONObject();
						error.put("template", pattern);
						error.put("mask", null);
						error.put("error", "\"mask\" was missing or null.");
						errors.add(error);
					}
				}
			}
		}
		Pattern pattern = null;
		Matcher matcher = null;
		for (Object obj : unmasked) {
			if (obj == null) {
				maskedLine = "";
			} else {
				line = obj.toString();

				// first apply request templates
				for (int i = 0; i < patterns.size(); i++) {
					pattern = patterns.get(i);
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						line = matcher.replaceAll(_maskPrefix + masks.get(i) + _maskPrefix);
					}
				}

				synchronized (_initializing) {
					// next apply global templates
					for (int i = 0; i < _patterns.size(); i++) {
						pattern = _patterns.get(i);
						matcher = pattern.matcher(line);
						if (matcher.find()) {
							line = matcher.replaceAll(_maskPrefix + _masks.get(i) + _maskPrefix);
						}
					}
				}

				// finally do standard masking
				String[] mixedCaseWords = splitWordsOnChar(line, ' ');
				StringBuffer sb = new StringBuffer();
				String lastWordMasked = "";
				lastWordMasked = processWords(mixedCaseWords, ' ', sb, lastWordMasked, counts, maskNumbers);
				maskedLine = MaskerUtils.trimSpaces(sb.toString());
			}
			masked.add(maskedLine);
		}
		return response;
	}

	/**
	 * Takes an array of mixed case words, a split character used to further break
	 * these words into parts, a string buffer that gets updated with masked
	 * content, the lastWordMasked describing what was last masked (to enable
	 * avoiding repeated masks of the same type), the object recording counts of
	 * different semantic masks that were applied, and a flag whether numbers should
	 * be masked.
	 * 
	 * @param mixedCaseWords
	 *                       words to be masked
	 * @param splitChar
	 *                       character used to split a word into parts
	 * @param sb
	 *                       the string buffer to receive the masked content from
	 *                       the input words
	 * @param lastWordMasked
	 *                       the last type of mask applied
	 * @param counts
	 *                       the counts of standard masks that were applied
	 * @param maskNumbers
	 *                       whether words containing all numbers should be masked
	 * @return the last type of mask applied to the text
	 * @throws Exception
	 */
	static public String processWords(String[] mixedCaseWords, Character splitChar, StringBuffer sb,
			String lastWordMasked, JSONObject counts, boolean maskNumbers) throws Exception {
		String checkWord = "";
		String cleanedWord = "";
		int cleanedWordOffset = -1;
		String mixedCaseCleansedWord = null;
		for (String word : mixedCaseWords) {
			mixedCaseCleansedWord = word;
			checkWord = word.toLowerCase();
			if (checkWord.length() == 0) {
				sb.append(splitChar);
				continue;
			}
			String[] wordParts = MaskerUtils.cleanWord(checkWord);
			if (wordParts[1].length() == 0) {
				// checkWord may only have non-word characters
				counts.put("words", ((Long) counts.get("words")) + 1L);
				sb.append(word);
				sb.append(splitChar);
				lastWordMasked = "";
				continue;
			}
			// otherwise, there is content to be checked for masking
			if (wordParts[0].length() > 0) {
				// append cleansed non-word characters
				sb.append(wordParts[0]);
				mixedCaseCleansedWord = word.substring(wordParts[0].length(), word.length() - wordParts[2].length());
			} else {
				try {
					mixedCaseCleansedWord = word.substring(0, word.length() - wordParts[2].length());
				} catch (StringIndexOutOfBoundsException sioob) {
					System.out.println(" original \"" + word + "\"" + " length=" + word.length()
							+ " mixedCaseCleansedWord \"" + mixedCaseCleansedWord + "\"" + "length="
							+ mixedCaseCleansedWord.length() + " cleckWord \"" + checkWord + "\"" + " cleaned \"" + cleanedWord
							+ "\" cleanedWordOffset=" + cleanedWordOffset);
				}
			}

			// special case where a word has a URL like meeting:https://zoom.us
			int urlIndex = wordParts[1].indexOf("http");
			if (urlIndex > 0) { // if 0 then subsequent logic handles it
				String url = wordParts[1].substring(urlIndex);
				// handle processing anything before the URL first into the string
				// buffer
				wordParts[1] = wordParts[1].substring(0, urlIndex);
				String[] urlPrefixWords = new String[] { wordParts[1] };
				lastWordMasked = processWords(urlPrefixWords, splitChar, sb, lastWordMasked, counts, maskNumbers);
				// now handle the URL part
				if (acceptableURLReference(url)) {
					counts.put("words", ((Long) counts.get("words")) + 1L);
					sb.append(mixedCaseCleansedWord);
					lastWordMasked = "";
					sb.append(wordParts[2]);
					sb.append(splitChar);
					continue;
				}
				counts.put("words", ((Long) counts.get("words")) + 1L);
				// just treat as a single word URL needing to be masked
				if (_whitelist.get(url) == null) {
					updateMasked(url);
					counts.put("maskedURL", ((Long) counts.get("maskedURL")) + 1L);
					// word should be masked unless last word was masked
					if (lastWordMasked.equals(_maskURL) == false) {
						sb.append(_maskURL);
						lastWordMasked = _maskURL;
					} else if (wordParts[0].length() > 0) {
						// need to add mask after non-word characters
						sb.append(_maskURL);
						lastWordMasked = _maskURL;
					} else {
						sb.append(wordParts[2]);
						if (wordParts[2].length() > 0) {
							lastWordMasked = "";
						}
						continue;
					}
				} else {
					sb.append(mixedCaseCleansedWord.substring(urlIndex));
					lastWordMasked = "";
				}
				sb.append(wordParts[2]);
				continue;
			}

			// is this referencing an acceptable URL
			if (acceptableURLReference(wordParts[1])) {
				counts.put("words", ((Long) counts.get("words")) + 1L);
				sb.append(mixedCaseCleansedWord);
				lastWordMasked = "";
				sb.append(wordParts[2]);
				sb.append(splitChar);
				continue;
			}

			// not an acceptable URL so if it starts with http or file_http mask it
			if (wordParts[1].startsWith("http") || wordParts[1].startsWith("file_http")) {
				counts.put("words", ((Long) counts.get("words")) + 1L);
				// just treat as a single word URL needing to be masked
				if (_whitelist.get(wordParts[1]) == null && _masks.contains(wordParts[1]) == false) {
					updateMasked(wordParts[1]);
					counts.put("maskedURL", ((Long) counts.get("maskedURL")) + 1L);
					// word should be masked unless last word was masked
					if (lastWordMasked.equals(_maskURL) == false) {
						sb.append(_maskURL);
						lastWordMasked = _maskURL;
					} else if (wordParts[0].length() > 0) {
						// need to add mask after non-word characters
						sb.append(_maskURL);
						lastWordMasked = _maskURL;
					} else {
						sb.append(wordParts[2]);
						if (wordParts[2].length() > 0) {
							lastWordMasked = "";
						}
						continue;
					}
				} else {
					sb.append(mixedCaseCleansedWord);
					lastWordMasked = "";
				}
				sb.append(wordParts[2]);
				if (wordParts[2].trim().length() > 0) {
					lastWordMasked = "";
				}
				continue;
			}
			int processed = 0;
			// does this need to deal with newlines, carriage returns, tabs, or
			// slashes
			if (processed == 0 && wordParts[1].contains("\n")) {
				String[] mixedCaseNLWords = splitWordsOnChar(mixedCaseCleansedWord, '\n');
				lastWordMasked = processWords(mixedCaseNLWords, '\n', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_NL;
			}
			if (processed == 0 && wordParts[1].contains("\r")) {
				String[] mixedCaseCRWords = splitWordsOnChar(mixedCaseCleansedWord, '\r');
				lastWordMasked = processWords(mixedCaseCRWords, '\r', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_CR;
			}
			if (processed == 0 && wordParts[1].contains("\t")) {
				String[] mixedCaseTabWords = splitWordsOnChar(mixedCaseCleansedWord, '\t');
				lastWordMasked = processWords(mixedCaseTabWords, '\t', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_TAB;
			}
			if (processed == 0 && wordParts[1].contains("/")) {
				String[] mixedCaseSlashWords = splitWordsOnChar(mixedCaseCleansedWord, '/');
				lastWordMasked = processWords(mixedCaseSlashWords, '/', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_SLASH;
			}
			if (processed == 0 && wordParts[1].contains(".")) {
				String[] mixedCasePeriodWords = splitWordsOnChar(mixedCaseCleansedWord, '.');
				lastWordMasked = processWords(mixedCasePeriodWords, '.', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_PERIOD;
			}
			if (processed == 0 && wordParts[1].contains("-")) {
				String[] mixedCaseHyphenWords = splitWordsOnChar(mixedCaseCleansedWord, '-');
				lastWordMasked = processWords(mixedCaseHyphenWords, '-', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_HYPHEN;
			}
			if (processed == 0 && wordParts[1].contains("(")) {
				String[] mixedCaseLParenWords = splitWordsOnChar(mixedCaseCleansedWord, '(');
				lastWordMasked = processWords(mixedCaseLParenWords, '(', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_LPAREN;
			}
			if (processed == 0 && wordParts[1].contains(":")) {
				String[] mixedCaseColonWords = splitWordsOnChar(mixedCaseCleansedWord, ':');
				lastWordMasked = processWords(mixedCaseColonWords, ':', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_COLON;
			}
			if (processed == 0 && wordParts[1].contains("_")) {
				String[] mixedCaseUnderscoreWords = splitWordsOnChar(mixedCaseCleansedWord, '_');
				lastWordMasked = processWords(mixedCaseUnderscoreWords, '_', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_UNDERSCORE;
			}
			if (processed == 0 && wordParts[1].contains(">")) {
				String[] mixedCaseGTWords = splitWordsOnChar(mixedCaseCleansedWord, '>');
				lastWordMasked = processWords(mixedCaseGTWords, '>', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_GT;
			}
			if (processed == 0 && wordParts[1].contains(",")) {
				String[] mixedCaseCommaWords = splitWordsOnChar(mixedCaseCleansedWord, ',');
				lastWordMasked = processWords(mixedCaseCommaWords, ',', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_COMMA;
			}
			if (processed == 0 && wordParts[1].contains("+")) {
				String[] mixedCasePlusWords = splitWordsOnChar(mixedCaseCleansedWord, '+');
				lastWordMasked = processWords(mixedCasePlusWords, '+', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_PLUS;
			}
			if (processed == 0 && wordParts[1].contains(";")) {
				String[] mixedCaseSemiColonWords = splitWordsOnChar(mixedCaseCleansedWord, ';');
				lastWordMasked = processWords(mixedCaseSemiColonWords, ';', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_SEMICOLON;
			}
			if (processed == 0 && wordParts[1].contains(")")) {
				String[] mixedCaseRParenWords = splitWordsOnChar(mixedCaseCleansedWord, ')');
				lastWordMasked = processWords(mixedCaseRParenWords, ')', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_RPAREN;
			}
			if (processed == 0 && wordParts[1].contains("\\")) {
				String[] mixedCaseBackslashWords = splitWordsOnChar(mixedCaseCleansedWord, '\\');
				lastWordMasked = processWords(mixedCaseBackslashWords, '\\', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_BACKSLASH;
			}
			if (processed == 0 && wordParts[1].contains("\u2014")) {
				String[] mixedCaseBackslashWords = splitWordsOnChar(mixedCaseCleansedWord, '\u2014');
				lastWordMasked = processWords(mixedCaseBackslashWords, '\u2014', sb, lastWordMasked, counts, maskNumbers);
				processed |= INDEX_EM_DASH;
			}
			if (processed == 0) {
				// process as a normal word
				counts.put("words", ((Long) counts.get("words")) + 1L);
				String testWord = wordParts[1];
				if (_whitelist.get(testWord) == null && _masks.contains(testWord) == false) {
					updateMasked(testWord);
					// determine the type of mask to apply
					if (_names.get(testWord) != null) {
						counts.put("maskedNam", ((Long) counts.get("maskedNam")) + 1L);
						if (lastWordMasked.equals(_maskName) == false) {
							sb.append(_maskName);
						}
						lastWordMasked = _maskName;
					} else if (_geolocations.get(testWord) != null) {
						counts.put("maskedGeo", ((Long) counts.get("maskedGeo")) + 1L);
						if (lastWordMasked.equals(_maskGeo) == false) {
							sb.append(_maskGeo);
						}
						lastWordMasked = _maskGeo;
					} else if (_profanities.get(testWord) != null) {
						counts.put("maskedBad", ((Long) counts.get("maskedBad")) + 1L);
						if (lastWordMasked.equals(_maskBad) == false) {
							sb.append(_maskBad);
						}
						lastWordMasked = _maskBad;
					} else {
						// is this all numbers?
						if (isNumbers(testWord)) {
							if (maskNumbers) {
								counts.put("maskedNum", ((Long) counts.get("maskedNum")) + 1L);
								if (lastWordMasked.equals(_maskNum) == false) {
									sb.append(_maskNum);
								}
								lastWordMasked = _maskNum;
							} else {
								// allow this word
								sb.append(mixedCaseCleansedWord);
								lastWordMasked = "";
							}
						} else {
							counts.put("maskedMisc", ((Long) counts.get("maskedMisc")) + 1L);
							if (lastWordMasked.equals(_maskMisc) == false) {
								sb.append(_maskMisc);
							}
							lastWordMasked = _maskMisc;
						}
					}
				} else {
					sb.append(mixedCaseCleansedWord);
					lastWordMasked = "";
				}
			}
			sb.append(wordParts[2]);
			if (wordParts[2].trim().length() > 0) {
				lastWordMasked = "";
			}
		}
		return lastWordMasked;
	}

	/**
	 * Split the incoming word using the provided splitChar
	 * 
	 * @param word
	 *                  string to be split
	 * @param splitChar
	 *                  character used for splitting the word
	 * @return array of strings comprising word fragments before and after where the
	 *         splitChar occurred in the word. Each occurrence of the splitChar
	 *         results in an empty string being added to the string array. So "this
	 *         is split" being split on a space would return
	 *         ["this","","","is","","split"]
	 */
	static public String[] splitWordsOnChar(String word, Character splitChar) {
		if (word.length() == 0) {
			return new String[] { "" };
		}
		List<String> splitWords = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		int index = 0;
		for (int i = 0; i < word.length(); i++) {
			Character wordChar = word.charAt(i);
			if (wordChar.equals(splitChar)) {
				if (index == 0) {
					splitWords.add("");
				} else {
					splitWords.add(sb.toString());
					splitWords.add("");
					index = 0;
					sb.setLength(index);
				}
			} else {
				sb.append(wordChar);
				index++;
			}
		}
		// handle string following a newline (or if no newline)
		if (index > 0) {
			splitWords.add(sb.toString());
		}
		return splitWords.toArray(new String[0]);
	}

	/**
	 * Cuts the supplied masked string on the first space encountered and increments
	 * a counter in the _maskedWords map for that word
	 * 
	 * @param masked
	 *               masked word to be counted
	 */
	static void updateMasked(String masked) {
		if (masked != null && masked.trim().length() > 0) {
			masked = masked.toLowerCase();
			int index = masked.indexOf(" ");
			if (index >= 0) {
				masked = masked.substring(0, index);
			}
			Integer maskedCount = 0;
			maskedCount = _maskedWords.get(masked);
			if (maskedCount == null) {
				maskedCount = 0;
			}
			maskedCount++;
			_maskedWords.put(masked, maskedCount);
		}
	}

	/**
	 * Receives a request with arrays of updates and removals to be applied to the
	 * patterns and masks. These arrays contain objects with a template and a mask.
	 * If the mask doesn't have the proper wrapper, it is added.
	 * 
	 * Removals are performed first, and reported in a removed array. Updates are
	 * 
	 * @param request
	 *                the request specifying udpates and removals of templates for
	 *                masking based on regex patterns and the mask to be used to
	 *                replace text matching the pattern. Note: the mask will be
	 *                surrounded by tilde's if not already provided.
	 * @return the response describing deletions and additions to the templates
	 * @throws Exception
	 *                   if a pattern will not compile correctly or if an invalid
	 *                   mask is provided
	 */
	static public JSONObject updateMaskTemplates(JSONObject request) throws Exception {
		if (!_isInitialized) {
			if (!Masker.init()) {
				throw new Exception("Can not initialize masking environment.");
			}
		}
		synchronized (_initializing) {
			JSONObject response = new JSONObject();
			JSONArray updates = (JSONArray) request.get("updates");
			JSONArray removals = (JSONArray) request.get("removals");
			JSONArray updated = new JSONArray();
			JSONArray removed = new JSONArray();
			JSONArray errors = new JSONArray();

			response.put("updated", updated);
			response.put("removed", removed);
			response.put("errors", errors);

			Set<String> deletePatterns = new HashSet<String>();
			String delPattern = "";
			String delMask = "";
			JSONObject jObj = null;
			for (Object obj : removals) {
				jObj = (JSONObject) obj;
				delPattern = (String) jObj.get("template");
				if (delPattern != null) {
					deletePatterns.add(delPattern);
				}
			}
			for (Object obj : updates) {
				jObj = (JSONObject) obj;
				delPattern = (String) jObj.get("template");
				if (delPattern != null) {
					// ensure it compiles before removing it
					try {
						Pattern.compile(delPattern);
						deletePatterns.add(delPattern);
					} catch (PatternSyntaxException pse) {
						; // will be reported later
					}
				}
			}
			// try removals first
			int i = 0;
			for (Iterator<Pattern> it = _patterns.iterator(); it.hasNext();) {
				Pattern pattern = _patterns.get(i);
				String patternStr = pattern.pattern();
				if (deletePatterns.contains(patternStr)) {
					it.remove();
					delMask = _masks.remove(i);
					jObj = new JSONObject();
					jObj.put("template", patternStr);
					jObj.put("mask", delMask);
					removed.add(jObj);
				}
				i++;
			}

			String addPattern = "";
			String addMask = "";
			for (Object obj : updates) {
				jObj = (JSONObject) obj;
				addPattern = (String) jObj.get("template");
				addMask = (String) jObj.get("mask");
				if (addPattern != null && addMask != null) {
					addPattern = addPattern.trim();
					addMask = addMask.trim();
					// ensure masks are lowercase to work with masking check
					addMask = addMask.toLowerCase().trim();
					// ensure there is no wrapper
					if (addMask.startsWith(_maskPrefix) == true) {
						addMask = addMask.substring(1);
					}
					if (addMask.endsWith(_maskPrefix) == true) {
						addMask = addMask.substring(0, addMask.length() - 1);
					}
					if (addMask.length() > 0) {
						try {
							Pattern newPattern = Pattern.compile(addPattern);
							_patterns.add(newPattern);
							_masks.add(addMask);
							updated.add(jObj);
						} catch (PatternSyntaxException pse) {
							JSONObject error = new JSONObject();
							error.put("template", addPattern);
							error.put("mask", addMask);
							error.put("error", pse.getLocalizedMessage());
							errors.add(error);
						}
					} else {
						JSONObject error = new JSONObject();
						error.put("template", addPattern);
						error.put("mask", addMask);
						error.put("error", "\"mask\" was empty.");
						errors.add(error);
					}
				} else {
					if (addPattern == null) {
						JSONObject error = new JSONObject();
						error.put("template", null);
						error.put("mask", addMask);
						error.put("error", "\"template\" was missing or null.");
						errors.add(error);
					} else {
						JSONObject error = new JSONObject();
						error.put("template", addPattern);
						error.put("mask", null);
						error.put("error", "\"mask\" was missing or null.");
						errors.add(error);
					}
				}
			}
			return response;
		}
	}

	String _ext = "json";

	NumberFormat _formatter = NumberFormat.getInstance(Locale.US);

	Path _inputPath = null;

	String _outputPath = "." + File.separator + "Masked";

	MaskerDate _startDate = new MaskerDate();

	Long _totalDialogs = 0L;

	Long _totalMasked = 0L;

	Long _totalWords = 0L;

	/**
	 * Constructor
	 */
	public Masker() {
		_formatter.setMaximumFractionDigits(2);
		_formatter.setMinimumFractionDigits(2);
	}

	/**
	 * Given the provided fully qualified path to a JSON-based dialog file, perform
	 * the masking and filtering based on volley counts to determine which (if any)
	 * dialogs should be moved to the resulting JSON-based dialog file in the output
	 * directory.
	 * 
	 * @param file
	 *             path to the JSON-based dialog file to be reviewed.
	 */
	void doWork(Path file) {
		JSONObject dialogsObj;
		try {
			System.out.println("Processing: " + file);
			dialogsObj = MaskerUtils.loadJSONFile(file.toString());
			String shortFileName = file.toString();
			shortFileName = shortFileName.substring(shortFileName.lastIndexOf(File.separator) + 1);
			// get the date from the shortFileName
			try {
				int nameLen = shortFileName.length();
				String fileDate = shortFileName.substring(nameLen - 10 - 1 - (_ext.length()));
				fileDate = fileDate.substring(0, 10);
				fileDate.replaceAll("\\/", "-");
				try {
					_startDate = new MaskerDate(fileDate + "T12:00:00.000Z");
				} catch (Exception e) {
					_startDate = new MaskerDate(new MaskerDate().toString().substring(0, 10) + "T12:00:00.000Z");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			maskDialogContent(dialogsObj, shortFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load the parameters needed for execution from the passed arguments, prompting
	 * for any missing arguments, and provide a way to quit the program.
	 * 
	 * @param args
	 *             the input path, output path, and filename of the domains used for
	 *             filtering dialogs.
	 * @return true if we have all the parameters needed for execution, or false if
	 *         the user has opted to cancel execution.
	 */
	boolean getParams(String[] args) {
		String inputPath = "." + File.separator + "Dialogs";
		String outputPath = "." + File.separator + "Masked";
		String tmp = "";
		try {
			if (args == null || args.length < 1) {
				tmp = MaskerUtils.prompt(
						"Enter the fully qualified path to directory containing JSON files to be reviewed, or q to exit ("
								+ inputPath + "):");
				if (tmp == null || tmp.length() == 0) {
					tmp = inputPath;
				}
				if (tmp.toLowerCase().equals("q")) {
					return false;
				}
				inputPath = tmp;
			} else {
				inputPath = args[0].trim();
			}
			if (inputPath.endsWith(File.separator) == false) {
				inputPath += File.separator;
			}
			_inputPath = FileSystems.getDefault().getPath(inputPath);
		} catch (InvalidPathException ipe) {
			System.out.println(args[0] + " is not a valid directory to form a path.");
			return false;
		}
		if (args == null || args.length < 2) {
			tmp = MaskerUtils
					.prompt("Enter the fully qualified path to the output directory, or q to exit (" + outputPath + "):");
			if (tmp == null || tmp.length() == 0) {
				tmp = outputPath;
			}
			if (tmp.toLowerCase().equals("q")) {
				return false;
			}
			outputPath = tmp;
		} else {
			outputPath = args[1].trim();
		}
		if (outputPath.endsWith(File.separator) == false) {
			outputPath += File.separator;
		}
		File testOutput = new File(outputPath);
		if (testOutput.exists() == false) {
			System.out.println("The output directory \"" + outputPath + "\" must exist.");
			return false;
		}
		if (testOutput.isDirectory() == false) {
			System.out.println("The output directory \"" + outputPath + "\" must be a directory.");
			return false;
		}
		_outputPath = outputPath;

		if (args == null || args.length < 3) {
			tmp = MaskerUtils.prompt("Enter the fully qualified filename of the whitelist json file, or q to exit ("
					+ _whitelistFileName + ")");
			if (tmp == null || tmp.length() == 0) {
				tmp = _whitelistFileName;
			}
			if (tmp.toLowerCase().equals("q")) {
				return false;
			}
			_whitelistFileName = tmp;
		} else {
			_whitelistFileName = args[2].trim();
		}
		try {
			System.out.println("Loading " + _whitelistFileName + " -- this could take a few seconds.\n");
			_whitelist = (JSONObject) MaskerUtils.loadJSONFile(_whitelistFileName);
			if (_whitelist == null) {
				System.out.println("Can not find the whitelist key in the file " + _whitelistFileName);
				return false;
			}
		} catch (Exception e) {
			System.out.println("Error loading file " + _whitelistFileName + ": " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}

		if (args == null || args.length < 4) {
			tmp = MaskerUtils.prompt(
					"Enter the fully qualified filename of the names json file, or q to exit (" + _namesFileName + ")");
			if (tmp == null || tmp.length() == 0) {
				tmp = _namesFileName;
			}
			if (tmp.toLowerCase().equals("q")) {
				return false;
			}
			_namesFileName = tmp;
		} else {
			_namesFileName = args[3].trim();
		}
		try {
			_names = (JSONObject) MaskerUtils.loadJSONFile(_namesFileName);
		} catch (Exception e) {
			System.out.println("Error loading file " + _namesFileName + ": " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}

		if (args == null || args.length < 5) {
			tmp = MaskerUtils.prompt("Enter the fully qualified filename of the geolocations json file, or q to exit ("
					+ _geolocationsFileName + ")");
			if (tmp == null || tmp.length() == 0) {
				tmp = _geolocationsFileName;
			}
			if (tmp.toLowerCase().equals("q")) {
				return false;
			}
			_geolocationsFileName = tmp;
		} else {
			_geolocationsFileName = args[4].trim();
		}
		try {
			_geolocations = (JSONObject) MaskerUtils.loadJSONFile(_geolocationsFileName);
		} catch (Exception e) {
			System.out.println("Error loading file " + _geolocationsFileName + ": " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}

		if (args == null || args.length < 6) {
			tmp = MaskerUtils.prompt("Enter the fully qualified filename of the profanities json file, or q to exit ("
					+ _profanitiesFileName + ")");
			if (tmp == null || tmp.length() == 0) {
				tmp = _profanitiesFileName;
			}
			if (tmp.toLowerCase().equals("q")) {
				return false;
			}
			_profanitiesFileName = tmp;
		} else {
			_profanitiesFileName = args[5].trim();
		}
		try {
			_profanities = (JSONObject) MaskerUtils.loadJSONFile(_profanitiesFileName);
		} catch (Exception e) {
			System.out.println("Error loading file " + _profanitiesFileName + ": " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}

		if (args == null || args.length < 7) {
			tmp = MaskerUtils.prompt("Enter the fully qualified filename of the domain prefixes filters, or q to exit ("
					+ _domainPrefixesFile + ")");
			if (tmp == null || tmp.length() == 0) {
				tmp = _domainPrefixesFile;
			}
			if (tmp.toLowerCase().equals("q")) {
				return false;
			}
			_domainPrefixesFile = tmp;
		} else {
			_domainPrefixesFile = args[6].trim();
		}

		try {
			List<String> domainPrefixList = MaskerUtils.loadTextFile(_domainPrefixesFile);
			for (String domainPrefix : domainPrefixList) {
				if (domainPrefix.startsWith("_")) {
					continue;
				}
				_domainPrefixList.add(domainPrefix.toLowerCase());
			}
		} catch (Exception e) {
			System.out.println("Error loading file " + _domainPrefixesFile + ": " + e.getLocalizedMessage());
			return false;
		}

		if (args == null || args.length < 8) {
			tmp = MaskerUtils.prompt("Enter the fully qualified filename of the domain suffix filters, or q to exit ("
					+ _domainSuffixesFile + ")");
			if (tmp == null || tmp.length() == 0) {
				tmp = _domainSuffixesFile;
			}
			if (tmp.toLowerCase().equals("q")) {
				return false;
			}
			_domainSuffixesFile = tmp;
		} else {
			_domainSuffixesFile = args[7].trim();
		}

		try {
			List<String> domainSuffixList = MaskerUtils.loadTextFile(_domainSuffixesFile);
			for (String domainSuffix : domainSuffixList) {
				if (domainSuffix.startsWith("_")) {
					continue;
				}
				_domainSuffixList.add(domainSuffix.toLowerCase());
			}
		} catch (Exception e) {
			System.out.println("Error loading file " + _domainSuffixesFile + ": " + e.getLocalizedMessage());
			return false;
		}

		if (args == null || args.length < 9) {
			tmp = MaskerUtils
					.prompt("Enter the fully qualified filename of the query string contains filters, or q to exit ("
							+ _queryStringContainsFile + ")");
			if (tmp == null || tmp.length() == 0) {
				tmp = _queryStringContainsFile;
			}
			if (tmp.toLowerCase().equals("q")) {
				return false;
			}
			_queryStringContainsFile = tmp;
		} else {
			_queryStringContainsFile = args[8].trim();
		}

		try {
			List<String> queryStringContainsList = MaskerUtils.loadTextFile(_queryStringContainsFile);
			for (String queryStringContains : queryStringContainsList) {
				if (queryStringContains.startsWith("_")) {
					continue;
				}
				_queryStringContainsList.add(queryStringContains.toLowerCase());
			}
		} catch (Exception e) {
			System.out.println("Error loading file " + _queryStringContainsFile + ": " + e.getLocalizedMessage());
			return false;
		}

		if (args == null || args.length < 10) {
			tmp = MaskerUtils.prompt("Enter the minimum dialogs per day, or q to exit (" + _minDialogs + ")");
			if (tmp == null || tmp.length() == 0) {
				tmp = new Integer(_minDialogs).toString();
			}
			if ("q".equalsIgnoreCase(tmp)) {
				return false;
			}
			try {
				_minDialogs = new Integer(tmp);
				if (_minDialogs < 1) {
					System.out.println("Minimum dialogs per day must be a positive integer.");
					return false;
				}
			} catch (NumberFormatException nfe) {
				System.out.println("Minimum dialogs per day must be a positive integer.");
				return false;
			}
		} else {
			try {
				_minDialogs = new Integer(args[9]);
				if (_minDialogs < 1) {
					System.out.println("Minimum dialogs per day must be a positive integer.");
					return false;
				}
			} catch (NumberFormatException nfe) {
				System.out.println("Minimum dialogs per day must be a positive integer.");
				return false;
			}
		}
		if (args == null || args.length < 11) {
			tmp = MaskerUtils.prompt("Numbers should be masked, or q to exit (" + _maskNumbers + ")");
			if (tmp == null || tmp.length() == 0) {
				tmp = _maskNumbers.toString();
			}
			if ("q".equalsIgnoreCase(tmp)) {
				return false;
			}
			_maskNumbers = new Boolean(tmp);
		} else {
			_maskNumbers = new Boolean(args[10]);
		}
		_isInitialized = true;
		return true;
	}

	/**
	 * Create a new daily dialog object and populate its masked dialog content based
	 * on the allowed words in the identified whitelist and only allow URL's that do
	 * not contain domains identified in the domain prefix, suffix or query string
	 * filter list.
	 * 
	 * @param dialogsObj
	 *                   object containing a set of dialogs between clients and
	 *                   support agents.
	 * @param fileName
	 *                   the name of the file from which the dialogsObj was read
	 * @throws Exception
	 */
	void maskDialogContent(JSONObject dialogsObj, String fileName) throws Exception {
		if (dialogsObj == null) {
			return;
		}
		JSONObject maskedDialogObj = new JSONObject();
		JSONArray maskedDialogVolleys = new JSONArray();
		maskedDialogObj.put("dialogs", maskedDialogVolleys);
		JSONArray originalDialogs = (JSONArray) dialogsObj.get("dialogs");
		if (originalDialogs == null || originalDialogs.size() == 0) {
			// nothing to mask so no point in saving this dialog
			return;
		}
		JSONObject dialogsHeader = (JSONObject) dialogsObj.get("header");
		maskedDialogObj.put("header", dialogsHeader);

		JSONObject dialog = null;
		JSONObject maskedVolley = null;
		Long timeOffset = new Long(0L);
		MaskerDate lastVolleyDate = null;
		JSONObject fileCounts = new JSONObject();
		fileCounts.put("words", 0L); // file word count
		fileCounts.put("maskedBad", 0L);
		fileCounts.put("maskedGeo", 0L);
		fileCounts.put("maskedMisc", 0L);
		fileCounts.put("maskedNam", 0L);
		fileCounts.put("maskedNum", 0L);
		fileCounts.put("maskedURL", 0L);
		for (Object dialogObject : originalDialogs) {
			/**
			 * Ensure 3 seconds between dialogs within the day. Note that timeOffset is
			 * incremented for each volley by its duration from the prior volley
			 */
			timeOffset = 0L;
			MaskerDate maskedDialogStartDate = new MaskerDate(_startDate.getTime() + timeOffset);
			dialog = (JSONObject) dialogObject;
			JSONObject dialogContent = (JSONObject) dialog.get("dialogContent");
			if (dialogContent == null) {
				continue;
			}
			JSONObject dialogHeader = (JSONObject) dialog.get("dialogHeader");
			if (dialogHeader == null) {
				System.out.println("Missing \"dialogHeader\" key");
				return;
			}
			MaskerDate conversationDateTime = new MaskerDate();
			try {
				conversationDateTime = new MaskerDate((String) dialogHeader.get("conversationDateTime"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			lastVolleyDate = conversationDateTime;
			dialogHeader.put("conversationDateTime",
					maskedDialogStartDate.toString(MaskerDate.CREATE_DATE_FORMAT_12, "GMT"));
			// remove reference to emails before saving to the new dialog
			dialogHeader.remove("agentEmails");
			dialogHeader.remove("clientEmail");
			String sessionID = (String) dialogHeader.get("sessionID");
			if (sessionID == null) {
				System.out.println("Missing \"sessionID\" key in dialogHeader");
				return;
			}
			JSONArray dialogVolleysArray = (JSONArray) dialogContent.get("dialog");
			JSONObject counts = new JSONObject();
			counts.put("maskedBad", 0L);
			counts.put("maskedGeo", 0L);
			counts.put("maskedMisc", 0L);
			counts.put("maskedNam", 0L);
			counts.put("maskedNum", 0L);
			counts.put("maskedURL", 0L);

			counts.put("words", 0L); // word count
			counts.put("masked", 0L); // masked count
			MaskerDate maskedVolleyDate = null;
			MaskerDate volleyDate = null;
			int volleyCount = 0;
			JSONArray maskedDialogVolleysArray = new JSONArray();
			for (Object volleyObject : dialogVolleysArray) {
				JSONObject volleyObj = (JSONObject) volleyObject;
				// change the datetime of the volley
				try {
					volleyDate = new MaskerDate((String) volleyObj.get("datetime"));
					long volleyOffset = MaskerDuration.elapsedTime(lastVolleyDate, volleyDate);
					lastVolleyDate = volleyDate;
					timeOffset += volleyOffset;
					maskedVolleyDate = new MaskerDate(_startDate.getTime() + timeOffset);
					volleyObj.put("datetime", maskedVolleyDate.toString(MaskerDate.CREATE_DATE_FORMAT_12, "GMT"));

				} catch (Exception e) {
					e.printStackTrace();
				}

				maskedVolley = maskVolley(volleyObj, counts, volleyCount);
				maskedDialogVolleysArray.add(maskedVolley);
				volleyCount++;
			}
			JSONObject maskedDialogObject = new JSONObject();
			JSONObject maskedDialogContent = new JSONObject();
			maskedDialogContent.put("dialog", maskedDialogVolleysArray);
			maskedDialogObject.put("dialogContent", maskedDialogContent);
			Long count = (Long) counts.get("words");
			Long maskedBad = (Long) counts.get("maskedBad");
			Long maskedGeo = (Long) counts.get("maskedGeo");
			Long maskedMisc = (Long) counts.get("maskedMisc");
			Long maskedNam = (Long) counts.get("maskedNam");
			Long maskedNum = (Long) counts.get("maskedNum");
			Long maskedURL = (Long) counts.get("maskedURL");
			Long masked = maskedBad + maskedGeo + maskedMisc + maskedNum + maskedURL;
			Double pctMasked = (100.0d * masked) / (1.0d * count);
			dialogHeader.put("words", count);
			dialogHeader.put("maskedBad", maskedBad);
			dialogHeader.put("maskedGeo", maskedGeo);
			dialogHeader.put("maskedMisc", maskedMisc);
			dialogHeader.put("maskedNam", maskedNam);
			dialogHeader.put("maskedNum", maskedNum);
			dialogHeader.put("maskedURL", maskedURL);
			dialogHeader.put("pctMasked", (_formatter.format(pctMasked)) + "%");
			fileCounts.put("words", ((Long) fileCounts.get("words")) + count);
			fileCounts.put("maskedBad", ((Long) fileCounts.get("maskedBad")) + maskedBad);
			fileCounts.put("maskedGeo", ((Long) fileCounts.get("maskedGeo")) + maskedGeo);
			fileCounts.put("maskedMisc", ((Long) fileCounts.get("maskedMisc")) + maskedMisc);
			fileCounts.put("maskedNam", ((Long) fileCounts.get("maskedNam")) + maskedNam);
			fileCounts.put("maskedNum", ((Long) fileCounts.get("maskedNum")) + maskedNum);
			fileCounts.put("maskedURL", ((Long) fileCounts.get("maskedURL")) + maskedURL);
			maskedDialogObject.put("dialogHeader", dialogHeader);
			maskedDialogVolleys.add(maskedDialogObject);
		} // end for each dialog
		Long wordCount = (Long) fileCounts.get("words");
		Long maskedBad = (Long) fileCounts.get("maskedBad");
		Long maskedGeo = (Long) fileCounts.get("maskedGeo");
		Long maskedMisc = (Long) fileCounts.get("maskedMisc");
		Long maskedNam = (Long) fileCounts.get("maskedNam");
		Long maskedNum = (Long) fileCounts.get("maskedNum");
		Long maskedURL = (Long) fileCounts.get("maskedURL");
		Long maskedCount = maskedBad + maskedGeo + maskedMisc + maskedNam + maskedNum + maskedURL;
		Double filePctMasked = (100.0d * maskedCount) / (1.0d * wordCount);
		_totalWords += wordCount;
		_totalMasked += maskedCount;
		_totalDialogs += maskedDialogVolleys.size();
		dialogsHeader.put("fileWords", wordCount);
		dialogsHeader.put("fileMasked", maskedCount);
		dialogsHeader.put("fileMaskedBad", maskedBad);
		dialogsHeader.put("fileMaskedGeo", maskedGeo);
		dialogsHeader.put("fileMaskedMisc", maskedMisc);
		dialogsHeader.put("fileMaskedNam", maskedNam);
		dialogsHeader.put("fileMaskedNum", maskedNum);
		dialogsHeader.put("fileMaskedURL", maskedURL);
		dialogsHeader.put("filePctMasked", (_formatter.format(filePctMasked)) + "%");
		if (maskedDialogVolleys.size() > 0) {
			String outputFileName = _outputPath + fileName;
			try {
				if (maskedDialogVolleys.size() >= _minDialogs) {
					MaskerUtils.saveJSONFile(outputFileName, maskedDialogObj);
					System.out
							.println("Wrote " + outputFileName + " with " + maskedDialogVolleys.size() + " dialogs. Masked "
									+ maskedCount + " of " + wordCount + " words (" + _formatter.format(filePctMasked) + "%)");
				} else {
					System.out.println("Not enough dialogs. Need at least " + _minDialogs + " but found only "
							+ maskedDialogVolleys.size() + ".");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Perform whitelist masking of the supplied message, updating masked word
	 * counts where applicable.
	 * 
	 * @param msg
	 *                 the message to be masked
	 * @param counts
	 *                 the JSON object whose masked word counts will be updated
	 * @param msgCount
	 *                 the volley index in the conversation (zero-based)
	 * @return masked version of the message (not the counts are updated in the
	 *         passed counts object as well)
	 * @throws Exception
	 */
	String maskMessage(String msg, JSONObject counts, int msgCount) throws Exception {

		Pattern pattern = null;
		Matcher matcher = null;
		synchronized (_initializing) {
			// next apply global templates
			for (int i = 0; i < _patterns.size(); i++) {
				pattern = _patterns.get(i);
				matcher = pattern.matcher(msg);
				if (matcher.find()) {
					msg = matcher.replaceAll(_maskPrefix + _masks.get(i) + _maskPrefix);
				}
			}
		}

		// need to preserve newlines so only split on space
		String[] mixedCaseWords = splitWordsOnChar(msg, ' ');
		StringBuffer sb = new StringBuffer();
		String lastWordMasked = "";
		lastWordMasked = processWords(mixedCaseWords, ' ', sb, lastWordMasked, counts, _maskNumbers);
		return MaskerUtils.trimSpaces(sb.toString());
	}

	/**
	 * Mask the supplied volley including the speaker and message
	 * 
	 * @param volley
	 *                    object containing the speaker (e.g., agent, bot, or
	 *                    client(\d)?), datetime, and message
	 * @param counts
	 *                    the object storing counts of masked words
	 * @param volleyCount
	 *                    which volley index in the conversation (zero-based)
	 * @return the masked version of the supplied volley
	 * @throws Exception
	 */
	JSONObject maskVolley(JSONObject volley, JSONObject counts, int volleyCount) throws Exception {
		JSONObject result = new JSONObject();
		// set up volley issuer
		if (volley.get("agent") != null) {
			result.put("agent", _maskName);
			counts.put("maskedNam", ((Long) counts.get("maskedNam")) + 1L);
		} else if (volley.get("bot") != null) {
			result.put("bot", _maskName);
			counts.put("maskedNam", ((Long) counts.get("maskedNam")) + 1L);
		} else {
			result.put("client", _maskName);
			counts.put("maskedNam", ((Long) counts.get("maskedNam")) + 1L);
		}
		String date = (String) volley.get("datetime");
		result.put("datetime", date);
		String msg = (String) volley.get("message");
		msg = maskMessage(msg, counts, volleyCount);
		result.put("message", msg);
		return result;
	}

	/**
	 * Determine if the supplied volley contains a URL that can validate against the
	 * set of domains used for filtering dialogs
	 * 
	 * @param volley
	 *                    the JSON object whose message is tested for URL(s) to be
	 *                    validated
	 * @param volleyCount
	 *                    the number of the volley within the dialog between a
	 *                    client and a support agent
	 * @return true if the volley contains at least one URL that references a domain
	 *         in our list, otherwise false
	 */
	boolean volleyHasURLDomainRef(JSONObject volley, int volleyCount, Long sessionID) {
		boolean result = false;
		int httpOffset = -1;
		int spaceOffset = -1;
		int delimiterOffset = -1;
		String url = "";
		String urlMessage = "";
		String testURL = "";
		String message = (String) volley.get("message");
		if (message == null) {
			System.out.println("Missing \"message\" key in volley " + (volleyCount + 1) + " of sessionID: " + sessionID);
		}
		urlMessage = message;
		httpOffset = message.toLowerCase().indexOf("http");
		String delimiter = " ";
		while (httpOffset >= 0) {
			/**
			 * first check for a surrounding character preceding the URL like a quote
			 */
			if (httpOffset >= 1) {
				delimiter = urlMessage.substring(httpOffset - 1, httpOffset);
				// handle different matching delimiters
				if (delimiter.equals("(")) {
					delimiter = ")";
				}
				if (delimiter.equals("`")) {
					delimiter = "'";
				}
				if (delimiter.equals("\t")) {
					delimiter = " ";
				}
				if (delimiter.equals(":")) {
					delimiter = " ";
				}
				if (delimiter.equals("\u2018")) {
					delimiter = "\u2019";
				}
				if (delimiter.equals("\u201B")) {
					delimiter = "\u2019";
				}
				if (delimiter.equals("\u201C")) {
					delimiter = "\u201D";
				}
				if (delimiter.equals("\u201F")) {
					delimiter = "\u201D";
				}
			} else {
				delimiter = " ";
			}
			urlMessage = urlMessage.substring(httpOffset);
			delimiterOffset = urlMessage.indexOf(delimiter);
			/**
			 * When a user includes a URL in a quoted sentence there may be a space
			 * following the actual URL followed by prose (as in an error message). This
			 * will stop at the delimiter or space, whichever appears first.
			 */
			spaceOffset = urlMessage.indexOf(" ");
			if (spaceOffset >= 0 && spaceOffset < delimiterOffset) {
				delimiterOffset = spaceOffset;
			} else if (delimiterOffset == -1) {
				delimiterOffset = spaceOffset;
			}
			if (delimiterOffset == -1) {
				/**
				 * We have reached the end of the message without finding a delimiter. In cases
				 * where the URL has a newline, comma, carriage return, question mark, or
				 * unicode for newline, remove those trailing characters, and if it is a valid
				 * URL, check to see if the URL's domain is in the list of domains. If so, this
				 * dialog is worth saving. Signal with a true result and stop processing this
				 * volley.
				 */
				urlMessage = MaskerUtils.cleanURL(urlMessage);
				// have last URL
				if (MaskerUtils.isValidURL(urlMessage)) {
					testURL = urlMessage.toLowerCase();
					for (String domain : _domainSuffixList) {
						if (testURL.contains(domain)) {
							result = true;
							break;
						}
					}
				}
				break;
			}
			if (result == true) {
				break;
			}
			url = urlMessage.substring(0, delimiterOffset);
			url = MaskerUtils.cleanURL(url);
			urlMessage = urlMessage.substring(delimiterOffset);
			if (MaskerUtils.isValidURL(url)) {
				testURL = url.toLowerCase();
				for (String domain : _domainSuffixList) {
					if (testURL.contains(domain)) {
						result = true;
						break;
					}
				}
			}
			httpOffset = urlMessage.toLowerCase().indexOf("http");
		} // end while there is a URL to be checked

		return result;
	}

}
