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
import java.io.FilenameFilter;
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

	/**
	 * Class to manage associations of reference counts to words being masked
	 *
	 */
	public class Tuple {

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
		public Tuple(String word, Integer count) {
			_word = word;
			_count = count;
		}

		/**
		 * Getter for the reference count of the word
		 * 
		 * @return count of references to the word
		 */
		public Integer getCount() {
			return _count;
		}

		/**
		 * Getter for the word being counted
		 * 
		 * @return word being counted
		 */
		public String getWord() {
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

	public static boolean _maskCommonPatterns = false;
	public static String _domainPrefixesFile = "DomainPrefixes.txt";
	public static String _domainSuffixesFile = "DomainSuffixes.txt";
	public static String _geolocationsFileName = "geolocations.json";
	public static String _initializing = "Initializing";
	public static boolean _isInitialized = false;
	public static final Map<String, List<String>> _mapDomainPrefixLists = new HashMap<String, List<String>>();
	public static final Map<String, List<String>> _mapDomainSuffixLists = new HashMap<String, List<String>>();
	public static final Map<String, JSONObject> _mapGeoLocationsObjs = new HashMap<String, JSONObject>();
	public static final Map<String, Boolean> _mapMaskNumbers = new HashMap<String, Boolean>();
	public static final Map<String, List<String>> _mapMasksList = new HashMap<String, List<String>>();
	public static final Map<String, JSONObject> _mapNameObjs = new HashMap<String, JSONObject>();
	public static final Map<String, List<Pattern>> _mapPatternsList = new HashMap<String, List<Pattern>>();
	public static final Map<String, JSONObject> _mapProfanityObjs = new HashMap<String, JSONObject>();
	public static final Map<String, List<String>> _mapQueryStringLists = new HashMap<String, List<String>>();
	public static final Map<String, JSONObject> _mapWhitelistObjs = new HashMap<String, JSONObject>();
	public static String _maskBad = "~bad~";
	public static Map<String, Integer> _maskedWords = new HashMap<String, Integer>();
	public static String _maskGeo = "~geo~";
	public static String _maskMisc = "~misc~";
	public static String _maskName = "~name~";
	public static String _maskNum = "~num~";
	public static String _maskPrefix = "~";
	public static String _maskTemplatesFile = "maskTemplates.json";
	public static String _maskURL = "~url~";
	public static int _minDialogs = 5;
	public static String _namesFileName = "names.json";
	public static String _profanitiesFileName = "profanities.json";
	public static String _queryStringContainsFile = "QueryStringContains.txt";
	public static final Set<String> _setTenantIDs = new HashSet<String>();
	public static String _tenantID = "companyA";
	public static String _whitelistFileName = "whitelist-words.json";
	public static final long INDEX_BACKSLASH = 0x00000001L;
	public static final long INDEX_COLON = 0x00000002L;
	public static final long INDEX_COMMA = 0x00000004L;
	public static final long INDEX_CR = 0x00000008L;
	public static final long INDEX_EM_DASH = 0x00000010L;
	public static final long INDEX_GT = 0x00000020L;
	public static final long INDEX_HYPHEN = 0x00000040L;
	public static final long INDEX_LPAREN = 0x00000080L;
	public static final long INDEX_NL = 0x00000100L;
	public static final long INDEX_PERIOD = 0x00000200L;
	public static final long INDEX_PLUS = 0x00000400L;
	public static final long INDEX_RPAREN = 0x00000800L;
	public static final long INDEX_SEMICOLON = 0x00001000L;
	public static final long INDEX_SLASH = 0x00002000L;
	public static final long INDEX_TAB = 0x00004000L;
	public static final long INDEX_TILDE = 0x00008000L;
	public static final long INDEX_UNDERSCORE = 0x00010000L;
	private static final long serialVersionUID = -4315882565512778401L;

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
	static public boolean acceptableURLReference(String message, List<String> queryStringContainsList,
			List<String> domainPrefixList, List<String> domainSuffixList) {
		String url = message.toLowerCase();
		String domain = null;
		int portIndex = -1;
		int queryStringIndex = -1;
		String queryString = null;
		String[] urlParts = url.split("http[s]?://");
		if (urlParts.length > 1) {
			if (urlParts.length > 2) {
				/**
				 * This message has a URL referencing another URL so we'll mask it until we can
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
				for (String qsFilter : queryStringContainsList) {
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
			for (String suffix : domainSuffixList) {
				if (domain.endsWith(suffix)) {
					return false;
				}
			}
			// check prefixes
			for (String prefix : domainPrefixList) {
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
	 * @return true if initialization succeeded
	 */
	static boolean init() {
		synchronized (_initializing) {
			// get the tenant IDs from the properties directory
			File propsDir = new File("." + File.separator + MaskerConstants.Masker_DIR_PROPERTIES);
			if (propsDir.exists() == false) {
				System.out.println("Can not find " + propsDir.getAbsolutePath());
				System.out.println("Aborting.");
				return false;
			}
			String[] tenantIDs = propsDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File current, String name) {
					return new File(current, name).isDirectory();
				}
			});
			for (String tenantID : tenantIDs) {
				Boolean _maskNumbers = Boolean.TRUE;
				JSONObject _whitelist = new JSONObject();
				JSONObject _names = new JSONObject();
				JSONObject _geolocations = new JSONObject();
				JSONObject _profanities = new JSONObject();
				List<String> _queryStringContainsList = new ArrayList<String>();
				List<String> _domainPrefixList = new ArrayList<String>();
				List<String> _domainSuffixList = new ArrayList<String>();
				List<Pattern> _patternsList = new ArrayList<Pattern>();
				List<String> _masksList = new ArrayList<String>();

				String filePrefix = "." + File.separator + MaskerConstants.Masker_DIR_PROPERTIES + File.separator + tenantID
						+ File.separator;
				try {
					_whitelist = (JSONObject) MaskerUtils.loadJSONFile(filePrefix + _whitelistFileName);
					if (_whitelist == null) {
						System.out.println("Can not find the whitelist key in the file " + filePrefix + _whitelistFileName);
						return false;
					}
				} catch (Exception e) {
					System.out
							.println("Error loading file " + filePrefix + _whitelistFileName + ": " + e.getLocalizedMessage());
					e.printStackTrace();
					return false;
				}
				try {
					_names = (JSONObject) MaskerUtils.loadJSONFile(filePrefix + _namesFileName);
				} catch (Exception e) {
					System.out.println("Error loading file " + filePrefix + _namesFileName + ": " + e.getLocalizedMessage());
					e.printStackTrace();
					return false;
				}
				try {
					_geolocations = (JSONObject) MaskerUtils.loadJSONFile(filePrefix + _geolocationsFileName);
				} catch (Exception e) {
					System.out.println(
							"Error loading file " + filePrefix + _geolocationsFileName + ": " + e.getLocalizedMessage());
					e.printStackTrace();
					return false;
				}
				try {
					_profanities = (JSONObject) MaskerUtils.loadJSONFile(filePrefix + _profanitiesFileName);
				} catch (Exception e) {
					System.out.println(
							"Error loading file " + filePrefix + _profanitiesFileName + ": " + e.getLocalizedMessage());
					e.printStackTrace();
					return false;
				}
				try {
					List<String> domainPrefixList = MaskerUtils.loadTextFile(filePrefix + _domainPrefixesFile);
					for (String domainPrefix : domainPrefixList) {
						if (domainPrefix.startsWith("_")) {
							continue;
						}
						_domainPrefixList.add(domainPrefix.toLowerCase());
					}
				} catch (Exception e) {
					System.out.println(
							"Error loading file " + filePrefix + _domainPrefixesFile + ": " + e.getLocalizedMessage());
					return false;
				}
				try {
					List<String> domainSuffixList = MaskerUtils.loadTextFile(filePrefix + _domainSuffixesFile);
					for (String domainSuffix : domainSuffixList) {
						if (domainSuffix.startsWith("_")) {
							continue;
						}
						_domainSuffixList.add(domainSuffix.toLowerCase());
					}
				} catch (Exception e) {
					System.out.println(
							"Error loading file " + filePrefix + _domainSuffixesFile + ": " + e.getLocalizedMessage());
					return false;
				}
				try {
					List<String> queryStringContainsList = MaskerUtils.loadTextFile(filePrefix + _queryStringContainsFile);
					for (String queryStringContains : queryStringContainsList) {
						if (queryStringContains.startsWith("_")) {
							continue;
						}
						_queryStringContainsList.add(queryStringContains.toLowerCase());
					}
				} catch (Exception e) {
					System.out.println(
							"Error loading file " + filePrefix + _queryStringContainsFile + ": " + e.getLocalizedMessage());
					return false;
				}

				try {
					JSONObject maskTemplates = MaskerUtils.loadJSONFile(filePrefix + _maskTemplatesFile);
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
									_patternsList.add(newPattern);
									_masksList.add(addMask);
								} catch (PatternSyntaxException pse) {
									System.out.println("Skipping \"" + addPattern + "\" because it did not compile: "
											+ pse.getLocalizedMessage());
								}
							} else {
								System.out.println("Skipping \"" + addPattern + "\" because its \"mask\" was empty.");
							}
						} else {
							if (addPattern == null) {
								System.out
										.println("Skipping \"" + addMask + "\" because its \"template\" was missing or null.");
							} else {
								System.out.println("Skipping \"" + addPattern + "\" because its \"mask\" was missing or null.");
							}
						}
					}
				} catch (Exception e) {
					System.out
							.println("Error loading file " + filePrefix + _maskTemplatesFile + ": " + e.getLocalizedMessage());
					return false;
				}
				_setTenantIDs.add(tenantID);
				_mapMaskNumbers.put(tenantID, _maskNumbers);
				_mapWhitelistObjs.put(tenantID, _whitelist);
				_mapNameObjs.put(tenantID, _names);
				_mapGeoLocationsObjs.put(tenantID, _geolocations);
				_mapProfanityObjs.put(tenantID, _profanities);
				_mapQueryStringLists.put(tenantID, _queryStringContainsList);
				_mapDomainPrefixLists.put(tenantID, _domainPrefixList);
				_mapDomainSuffixLists.put(tenantID, _domainSuffixList);
				_mapPatternsList.put(tenantID, _patternsList);
				_mapMasksList.put(tenantID, _masksList);

			} // end while processing each tenantID
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

	static void getDifferences(String unmasked, String masked, JSONArray diffs) {
		/**
		 * for each mask, find its equivalent in the original string and save it as an
		 * object
		 */
		int maskOffset = masked.indexOf("~");
		int maskEnd = -1;
		String maskSearch = "";
		String mask = "";
		String betweenMasks = "";
		int nextMaskOffset = -1;
		int betweenMasksEnd = -1;
		String value = "";
		while (maskOffset != -1) {
			unmasked = unmasked.substring(maskOffset);
			maskSearch = masked.substring(maskOffset + 1);
			maskEnd = maskSearch.indexOf("~");
			if (maskEnd != -1) {
				mask = masked.substring(maskOffset, maskOffset + maskEnd + 2);
				betweenMasks = masked.substring(maskOffset + maskEnd + 2);
				masked = masked.substring(maskOffset + maskEnd + 2);
				nextMaskOffset = betweenMasks.indexOf("~");
				if (nextMaskOffset != -1) {
					betweenMasks = betweenMasks.substring(0, nextMaskOffset);
					betweenMasksEnd = unmasked.indexOf(betweenMasks);
					JSONObject part = new JSONObject();
					if (betweenMasksEnd != -1) {
					   value = unmasked.substring(0, betweenMasksEnd).replace("\u223C", "~");
					   part.put(mask, value);
					   diffs.add(part);
					   unmasked = unmasked.substring(betweenMasksEnd);
					} else {
					   // keep unmasked as it was
					   /**
					    * this is due to a buggy regex that results in a 
					    * dual tilde situation. Hopefully the additional 
					    * processing added will avoid this situation.
					    * 
					    * the order of the regex in the templates array 
					    * is important to ensure larger regex patterns are
					    * masked before smaller ones that might overlap.
					    */
					   System.err.println("Could not find a match for \""+betweenMasks+
					      "\" in the unmasked content: \""+unmasked+"\" so we "+
					      "could not find the difference to report correctly.\n"+
					      "The masked segment following the mask \""+mask+"\" is: \""+
					      masked+"\"");
					}
					maskOffset = masked.indexOf("~");
					continue;
				} // else no further mask so remainder is masked value
				JSONObject part = new JSONObject();
				if (betweenMasks.length() == 0) {
					part.put(mask, unmasked.replace("\u223C", "~"));
				} else {
					betweenMasksEnd = unmasked.indexOf(betweenMasks);
					if (betweenMasksEnd == -1) {
						part.put(mask, unmasked.replace("\u223C", "~"));
					} else {
						part.put(mask, unmasked.substring(0, betweenMasksEnd).replace("\u223C", "~"));
					}
				}
				diffs.add(part);
				break;
			} // else not a mask
			break;
		}
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
					+ "\nand content not in the " + _tenantID + " whitelist will be masked."//
					+ "\nIf the dialog contains a reference to a URL" //
					+ "\nwith a domain not ending with a suffix in the domain suffixes list" //
					+ "\nnor starting with a prefix in the domain prefixes list" //
					+ "\nnor containing a string in the query string contains list" //
					+ "\nthe URL will not be masked. If the masked dialog file" //
					+ "\nhas at least the minimum number of dialogs per day," //
					+ "\nthe dialog content will be saved to the output directory " + pgm._outputPath); //
			System.out.println("It is "+_mapMaskNumbers.get(_tenantID)+" that numbers will be masked.");
			if (MaskerUtils.prompt("Press q to quit or press Enter to continue	").length() == 0) {
				try {

					JSONObject _whitelist = _mapWhitelistObjs.get(_tenantID);
					JSONObject _names = _mapNameObjs.get(_tenantID);
					JSONObject _geolocations = _mapGeoLocationsObjs.get(_tenantID);
					JSONObject _profanities = _mapProfanityObjs.get(_tenantID);
					List<Pattern> _patterns = _mapPatternsList.get(_tenantID);
					List<String> _masks = _mapMasksList.get(_tenantID);
					List<String> _queryStringContainsList = _mapQueryStringLists.get(_tenantID);
					List<String> _domainPrefixList = _mapDomainPrefixLists.get(_tenantID);
					List<String> _domainSuffixList = _mapDomainSuffixLists.get(_tenantID);
					Boolean _maskNumbers = _mapMaskNumbers.get(_tenantID);

					List<Path> files = MaskerUtils
							.listSourceFiles(FileSystems.getDefault().getPath(pgm._inputPath.toString()), pgm._ext);
					Collections.sort(files);
					for (Path file : files) {
						pgm.doWork(file, _whitelist, _names, _geolocations, _profanities, _queryStringContainsList,
								_domainPrefixList, _domainSuffixList, _patterns, _masks, _maskNumbers);
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
	 * Splits the input String into an array of Strings, preserving situations with
	 * consecutive patterns (returned as empty strings in the array)
	 * 
	 * @param input
	 *                The input string to be split.
	 * @param pattern
	 *                the pattern used to perform the splitting
	 * @return array of strings that were terminated by the patterns
	 */
	static public String[] splitOnPattern(String input, String pattern) {
		ArrayList<String> lines = new ArrayList<String>();
		if (input != null) {
			int nlIndex = input.indexOf(pattern);
			if (nlIndex != -1) {
				while (nlIndex > -1) {
					lines.add(input.substring(0, nlIndex));
					nlIndex += pattern.length();
					input = input.substring(nlIndex);
					nlIndex = input.indexOf(pattern);
				}
				if (input.length() == 0) {
					lines.add(""); // ending case
				}
			} else {
				lines.add(input);
			}
		}
		return lines.toArray(new String[0]);
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
		counts.put("maskedName", 0L);
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
		response.put("request", request);
		response.put("masked", masked);
		response.put("errors", errors);
		String tenantID = (String) request.get("tenantID");
		if (tenantID == null) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID is missing.");
			errors.add(error);
			return response;
		}
		if (_setTenantIDs.contains(tenantID) == false) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID \"" + tenantID + "\" is not a known tenantID.");
			errors.add(error);
			return response;
		}
		String line = "";
		String maskedLine = "";

		JSONObject _whitelist = _mapWhitelistObjs.get(tenantID);
		if (_whitelist == null) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID \"" + tenantID + "\" has no whitelist.");
			errors.add(error);
			return response;
		}
		JSONObject _names = _mapNameObjs.get(tenantID);
		if (_names == null) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID \"" + tenantID + "\" has no names.");
			errors.add(error);
			return response;
		}
		JSONObject _geolocations = _mapGeoLocationsObjs.get(tenantID);
		if (_geolocations == null) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID \"" + tenantID + "\" has no geolocations.");
			errors.add(error);
			return response;
		}
		JSONObject _profanities = _mapProfanityObjs.get(tenantID);
		if (_profanities == null) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID \"" + tenantID + "\" has no profanities.");
			errors.add(error);
			return response;
		}
		List<Pattern> _patterns = _mapPatternsList.get(tenantID);
		if (_patterns == null) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID \"" + tenantID + "\" has no patterns.");
			errors.add(error);
			return response;
		}
		List<String> _masks = _mapMasksList.get(tenantID);
		if (_masks == null) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID \"" + tenantID + "\" has no masks.");
			errors.add(error);
			return response;
		}

		List<String> _queryStringContainsList = _mapQueryStringLists.get(tenantID);
		if (_queryStringContainsList == null) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID \"" + tenantID + "\" has no QueryStringContains.");
			errors.add(error);
			return response;
		}

		List<String> _domainPrefixList = _mapDomainPrefixLists.get(tenantID);
		if (_domainPrefixList == null) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID \"" + tenantID + "\" has no DomainPrefixList.");
			errors.add(error);
			return response;
		}

		List<String> _domainSuffixList = _mapDomainSuffixLists.get(tenantID);
		if (_domainSuffixList == null) {
			JSONObject error = new JSONObject();
			error.put("error", "tenantID \"" + tenantID + "\" has no DomainSuffixList.");
			errors.add(error);
			return response;
		}

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

		// append the global masks to transactional masks
		synchronized (_initializing) {
			patterns.addAll(_patterns);
			masks.addAll(_masks);
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

				// finally do standard masking
				String[] mixedCaseWords = splitWordsOnChar(line, ' ');
			     // need to treat existing masks as words
		      List<String> withMasks = new ArrayList<String>();
		      boolean hasPrefix = false;
		      String saveWord = "";
		      for (String testWord : mixedCaseWords) {
		         if (testWord.contains("\u223C")) {
		            String[] mixedCaseTildeWords = splitWordsOnChar(testWord, '\u223C');
		            for (String splitWord: mixedCaseTildeWords) {
		               if (splitWord.length() == 0) {
		                  if (!hasPrefix) {
		                     hasPrefix = true;
		                  } else {
	                        saveWord = saveWord + "\u223C";
	                        withMasks.add(saveWord);
	                        hasPrefix = false;
		                  }
		                  continue;
		               }
		               if (hasPrefix == true) {
		                  saveWord = "\u223C"+splitWord;
		                  continue;
		               }
		               withMasks.add(splitWord);
		            }
		         } else {
		            withMasks.add(testWord);
		         }
		      }
		      mixedCaseWords = withMasks.toArray(new String[0]);

				StringBuffer sb = new StringBuffer();
				String lastWordMasked = "";
				lastWordMasked = processWords(mixedCaseWords, ' ', sb, lastWordMasked, counts, maskNumbers, _whitelist,
						_names, _geolocations, _profanities, _queryStringContainsList, _domainPrefixList, _domainSuffixList,
						patterns, masks);
				maskedLine = MaskerUtils.trimSpaces(sb.toString());
			}
			masked.add(maskedLine);
		}
		return response;
	}

	/**
	 * Perform whitelist masking of the supplied message, updating masked word
	 * counts where applicable.
	 * 
	 * @param msg
	 *                                the message to be masked
	 * @param counts
	 *                                the JSON object whose masked word counts will
	 *                                be updated
	 * @param msgCount
	 *                                the volley index in the conversation
	 *                                (zero-based)
	 * @param whitelist
	 *                                whitelist for the current tenantID
	 * @param names
	 *                                names for the current tenantID
	 * @param geolocations
	 *                                geolocations for the current tenantID
	 * @param profanities
	 *                                profanities for the current tenantID
	 * @param queryStringContainsList
	 *                                queryStringContainsList for the current
	 *                                tenantID
	 * @param domainPrefixList
	 *                                domainPrefixList for the current tenantID
	 * @param domainSuffixList
	 *                                domainSuffixList for the current tenantID
	 * @param patterns
	 *                                the patterns for the current tenantID
	 * @param masks
	 *                                the masks for the current tenantID
	 * @param maskNumbers
	 *                                whether numbers should be masked
	 * @return masked version of the message (not the counts are updated in the
	 *         passed counts object as well)
	 * @throws Exception
	 */
	static public String maskMessage(String msg, JSONObject counts, int msgCount, JSONObject whitelist, JSONObject names,
			JSONObject geolocations, JSONObject profanities, List<String> queryStringContainsList,
			List<String> domainPrefixList, List<String> domainSuffixList, List<Pattern> patterns, List<String> masks,
			Boolean maskNumbers) throws Exception {

		Pattern pattern = null;
		Matcher matcher = null;
		synchronized (_initializing) {
			// next apply global templates
			for (int i = 0; i < patterns.size(); i++) {
				pattern = patterns.get(i);
				matcher = pattern.matcher(msg);
				if (matcher.find()) {
					msg = matcher.replaceAll(_maskPrefix + masks.get(i) + _maskPrefix);
				}
			}
		}

		if (_maskCommonPatterns) {
			/**
			 * Do global fix ups here for patterns like: Welcome X. Your chat or Customer X
			 * has left the chat
			 */
			int testIndex = -1;
			// Welcome X. Your chat
			if (msg.startsWith("Welcome ")) {
				testIndex = msg.indexOf(". Your chat");
				if (testIndex > 8) {
					updateMasked(msg.substring(8, testIndex),maskNumbers);
					msg = "Welcome " + _maskName + msg.substring(testIndex);
					counts.put("maskedName", ((Long) counts.get("maskedName")) + 1L);
				}
			}
			// Customer X has left the chat
			if (msg.startsWith("Customer ")) {
				testIndex = msg.indexOf(" has left the chat");
				if (testIndex > 9) {
					updateMasked(msg.substring(9, testIndex),maskNumbers);
					msg = "Customer " + _maskName + msg.substring(testIndex);
					counts.put("maskedName", ((Long) counts.get("maskedName")) + 1L);
				}
			}
			// for AskHR, first message is always Hello XXX using client's first
			// name(s)
			if (msgCount == 0 && msg.startsWith("Hello ")) {
				testIndex = msg.indexOf("\n");
				if (testIndex > 6) {
					updateMasked(msg.substring(6),maskNumbers);
					msg = "Hello " + _maskName + "\n";
					counts.put("maskedName", ((Long) counts.get("maskedName")) + 1L);
				}
			}
		}

		// need to preserve newlines so only split on space
		String[] mixedCaseWords = splitWordsOnChar(msg, ' ');
		StringBuffer sb = new StringBuffer();
		String lastWordMasked = "";
		lastWordMasked = processWords(mixedCaseWords, ' ', sb, lastWordMasked, counts, maskNumbers, whitelist, names,
				geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList, patterns, masks);
		return MaskerUtils.trimSpaces(sb.toString());
	}

	static public JSONObject maskMessageContent(JSONObject request) throws Exception {
		JSONObject newRequest = new JSONObject();
		newRequest.put("tenantID", request.get("tenandID"));
		newRequest.put("maskNumbers", request.get("maskNumbers"));
		newRequest.put("templates", request.get("templates"));
		JSONArray unmasked = new JSONArray();
		JSONArray messages = (JSONArray) request.get("messages");
		JSONObject jObj = null;
		String unmaskedStr = "";
		String maskedStr = "";
		for (Object obj : messages) {
			jObj = (JSONObject) obj;
			unmaskedStr = (String) jObj.get("utterance");
			unmaskedStr = unmaskedStr.replace("~", "\u223C");
			jObj.put("utterance", unmaskedStr);
			unmasked.add(unmaskedStr);
		}
		newRequest.put("unmasked", unmasked);
		newRequest.put("tenantID", (String) request.get("tenantID"));
		JSONObject newResponse = Masker.maskContent(newRequest);
		JSONObject response = new JSONObject();
		response.put("request", request);
		response.put("errors", newResponse.get("errors"));
		JSONArray masked = (JSONArray) newResponse.get("masked");
		JSONArray diffs = new JSONArray();
		response.put("diffs", diffs);
		if (masked.size() == messages.size()) {
			int i = 0;
			for (Object obj : messages) {
				jObj = (JSONObject) obj;
				unmaskedStr = (String) jObj.get("utterance");
				maskedStr = (String) masked.get(i);
				getDifferences(unmaskedStr, maskedStr, diffs);
				jObj.put("utterance", ((String) masked.get(i)).replace("\u223C", "~"));
				i++;
			}
		} else {
			throw new Exception(
					"Masked content size " + masked.size() + " does not equal message size " + messages.size());
		}
		response.put("maskedMessages", messages);
		return response;
	}

	/**
	 * Mask the supplied volley including the speaker and message
	 * 
	 * @param volley
	 *                                object containing the speaker (e.g., agent,
	 *                                bot, or client(\d)?), datetime, and message
	 * @param counts
	 *                                the object storing counts of masked words
	 * @param volleyCount
	 *                                which volley index in the conversation
	 *                                (zero-based)
	 * @param whitelist
	 *                                whitelist for the current tenantID
	 * @param names
	 *                                names for the current tenantID
	 * @param geolocations
	 *                                geolocations for the current tenantID
	 * @param profanities
	 *                                profanities for the current tenantID
	 * @param queryStringContainsList
	 *                                queryStringContainsList for the current
	 *                                tenantID
	 * @param domainPrefixList
	 *                                domainPrefixList for the current tenantID
	 * @param domainSuffixList
	 *                                domainSuffixList for the current tenantID
	 * @param patterns
	 *                                the patterns for the current tenantID
	 * @param masks
	 *                                the masks for the current tenantID
	 * @param maskNumbers
	 *                                whether numbers should be masked
	 * @return the masked version of the supplied volley
	 * @throws Exception
	 */
	public JSONObject maskVolley(JSONObject volley, JSONObject counts, int volleyCount, JSONObject whitelist,
			JSONObject names, JSONObject geolocations, JSONObject profanities, List<String> queryStringContainsList,
			List<String> domainPrefixList, List<String> domainSuffixList, List<Pattern> patterns, List<String> masks,
			Boolean maskNumbers) throws Exception {
		JSONObject result = new JSONObject();
		// set up volley issuer
		String agentID = (String)volley.get("agent");
		if (agentID != null) {
			result.put("agent",agentID); // preserve the agentID
		} else if (volley.get("bot") != null) {
			result.put("bot", _maskName);
			// counts.put("maskedName", ((Long) counts.get("maskedName")) + 1L);
		} else if (volley.get("system") != null) {
         result.put("system", _maskName);
         // counts.put("maskedName", ((Long) counts.get("maskedName")) + 1L);
      } else {
			result.put("client", _maskName);
			// counts.put("maskedName", ((Long) counts.get("maskedName")) + 1L);
		}
		String date = (String) volley.get("datetime");
		result.put("datetime", date);
		String msg = (String) volley.get("message");
		msg = maskMessage(msg, counts, volleyCount, whitelist, names, geolocations, profanities, queryStringContainsList,
				domainPrefixList, domainSuffixList, patterns, masks, maskNumbers);
		result.put("message", msg);
		Long turn = (Long) volley.get("turn");
		if (turn != null) {
			result.put("turn", turn);
		}
		String skill = (String) volley.get("skill");
		if (skill != null) {
			result.put("skill", skill);
		}
		return result;
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
	 *                                words to be masked
	 * @param splitChar
	 *                                character used to split a word into parts
	 * @param sb
	 *                                the string buffer to receive the masked
	 *                                content from the input words
	 * @param lastWordMasked
	 *                                the last type of mask applied
	 * @param counts
	 *                                the counts of standard masks that were applied
	 * @param whitelist
	 *                                whitelist for the current tenantID
	 * @param names
	 *                                names for the current tenantID
	 * @param geolocations
	 *                                geolocations for the current tenantID
	 * @param profanities
	 *                                profanities for the current tenantID
	 * @param queryStringContainsList
	 *                                queryStringContainsList for the current
	 *                                tenantID
	 * @param domainPrefixList
	 *                                domainPrefixList for the current tenantID
	 * @param domainSuffixList
	 *                                domainSuffixList for the current tenantID
	 * @param patterns
	 *                                the patterns for the current tenantID
	 * @param masks
	 *                                the masks for the current tenantID
	 * @param maskNumbers
	 *                                whether numbers should be masked
	 * @return the last type of mask applied to the text
	 * @throws Exception
	 */
	static public String processWords(String[] mixedCaseWords, Character splitChar, StringBuffer sb,
			String lastWordMasked, JSONObject counts, boolean maskNumbers, JSONObject whitelist, JSONObject names,
			JSONObject geolocations, JSONObject profanities, List<String> queryStringContainsList,
			List<String> domainPrefixList, List<String> domainSuffixList, List<Pattern> patterns, List<String> masks)
			throws Exception {
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
			   // this should avoid getting double tilde's like ~~misc~~ for ~acct~~mask~
				counts.put("words", ((Long) counts.get("words")) + 1L);
				sb.append(word);
				// sb.append(splitChar);
				lastWordMasked = "";
				continue;
			}
			if ("~".equals(wordParts[0]) && "~".equals(wordParts[2])) {
            // checkWord is already masked
            counts.put("words", ((Long) counts.get("words")) + 1L);
            sb.append(word);
            // sb.append(splitChar);
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
				lastWordMasked = processWords(urlPrefixWords, splitChar, sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				// now handle the URL part
				if (acceptableURLReference(url, queryStringContainsList, domainPrefixList, domainSuffixList)) {
					counts.put("words", ((Long) counts.get("words")) + 1L);
					sb.append(url); // was mixedCaseWord
					lastWordMasked = "";
					sb.append(wordParts[2]);
					sb.append(splitChar);
					continue;
				}
				counts.put("words", ((Long) counts.get("words")) + 1L);
				// just treat as a single word URL needing to be masked
				if (whitelist.get(url) == null) {
					updateMasked(url,maskNumbers);
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
						/**
						 * Back out last splitchar since we are reusing the same mask for a pattern like
						 * namesplitcharname to result in "~name~" and not "~name~ " (assuming space is
						 * the splitchar)
						 */
						sb.setLength(Math.max(sb.length() - 1, 0));
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
			if (acceptableURLReference(wordParts[1], queryStringContainsList, domainPrefixList, domainSuffixList)) {
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
				if (whitelist.get(wordParts[1]) == null && masks.contains(wordParts[1]) == false) {
					updateMasked(wordParts[1],maskNumbers);
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
						/**
						 * Back out last splitchar since we are reusing the same mask for a pattern like
						 * namesplitcharname to result in "~name~" and not "~name~ " (assuming space is
						 * the splitchar)
						 */
						sb.setLength(Math.max(sb.length() - 1, 0));
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
			long processed = 0L;
			// does this need to deal with newlines, carriage returns, tabs, or
			// slashes
			if (processed == 0L && wordParts[1].contains("\n")) {
				String[] mixedCaseNLWords = splitWordsOnChar(mixedCaseCleansedWord, '\n');
				lastWordMasked = processWords(mixedCaseNLWords, '\n', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_NL;
			}
			if (processed == 0L && wordParts[1].contains("\r")) {
				String[] mixedCaseCRWords = splitWordsOnChar(mixedCaseCleansedWord, '\r');
				lastWordMasked = processWords(mixedCaseCRWords, '\r', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_CR;
			}
			if (processed == 0L && wordParts[1].contains("\t")) {
				String[] mixedCaseTabWords = splitWordsOnChar(mixedCaseCleansedWord, '\t');
				lastWordMasked = processWords(mixedCaseTabWords, '\t', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_TAB;
			}
			if (processed == 0L && wordParts[1].contains("/")) {
				String[] mixedCaseSlashWords = splitWordsOnChar(mixedCaseCleansedWord, '/');
				lastWordMasked = processWords(mixedCaseSlashWords, '/', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_SLASH;
			}
			if (processed == 0L && wordParts[1].contains(".")) {
				String[] mixedCasePeriodWords = splitWordsOnChar(mixedCaseCleansedWord, '.');
				lastWordMasked = processWords(mixedCasePeriodWords, '.', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_PERIOD;
			}
			if (processed == 0L && wordParts[1].contains("-")) {
				String[] mixedCaseHyphenWords = splitWordsOnChar(mixedCaseCleansedWord, '-');
				lastWordMasked = processWords(mixedCaseHyphenWords, '-', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_HYPHEN;
			}
			if (processed == 0L && wordParts[1].contains("(")) {
				String[] mixedCaseLParenWords = splitWordsOnChar(mixedCaseCleansedWord, '(');
				lastWordMasked = processWords(mixedCaseLParenWords, '(', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_LPAREN;
			}
			if (processed == 0L && wordParts[1].contains(":")) {
				String[] mixedCaseColonWords = splitWordsOnChar(mixedCaseCleansedWord, ':');
				lastWordMasked = processWords(mixedCaseColonWords, ':', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_COLON;
			}
			if (processed == 0L && wordParts[1].contains("_")) {
				String[] mixedCaseUnderscoreWords = splitWordsOnChar(mixedCaseCleansedWord, '_');
				lastWordMasked = processWords(mixedCaseUnderscoreWords, '_', sb, lastWordMasked, counts, maskNumbers,
						whitelist, names, geolocations, profanities, queryStringContainsList, domainPrefixList,
						domainSuffixList, patterns, masks);
				processed |= INDEX_UNDERSCORE;
			}
			if (processed == 0L && wordParts[1].contains(">")) {
				String[] mixedCaseGTWords = splitWordsOnChar(mixedCaseCleansedWord, '>');
				lastWordMasked = processWords(mixedCaseGTWords, '>', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_GT;
			}
			if (processed == 0L && wordParts[1].contains(",")) {
				String[] mixedCaseCommaWords = splitWordsOnChar(mixedCaseCleansedWord, ',');
				lastWordMasked = processWords(mixedCaseCommaWords, ',', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_COMMA;
			}
			if (processed == 0L && wordParts[1].contains("+")) {
				String[] mixedCasePlusWords = splitWordsOnChar(mixedCaseCleansedWord, '+');
				lastWordMasked = processWords(mixedCasePlusWords, '+', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_PLUS;
			}
			if (processed == 0L && wordParts[1].contains(";")) {
				String[] mixedCaseSemiColonWords = splitWordsOnChar(mixedCaseCleansedWord, ';');
				lastWordMasked = processWords(mixedCaseSemiColonWords, ';', sb, lastWordMasked, counts, maskNumbers,
						whitelist, names, geolocations, profanities, queryStringContainsList, domainPrefixList,
						domainSuffixList, patterns, masks);
				processed |= INDEX_SEMICOLON;
			}
			if (processed == 0L && wordParts[1].contains(")")) {
				String[] mixedCaseRParenWords = splitWordsOnChar(mixedCaseCleansedWord, ')');
				lastWordMasked = processWords(mixedCaseRParenWords, ')', sb, lastWordMasked, counts, maskNumbers, whitelist,
						names, geolocations, profanities, queryStringContainsList, domainPrefixList, domainSuffixList,
						patterns, masks);
				processed |= INDEX_RPAREN;
			}
			if (processed == 0L && wordParts[1].contains("\\")) {
				String[] mixedCaseBackslashWords = splitWordsOnChar(mixedCaseCleansedWord, '\\');
				lastWordMasked = processWords(mixedCaseBackslashWords, '\\', sb, lastWordMasked, counts, maskNumbers,
						whitelist, names, geolocations, profanities, queryStringContainsList, domainPrefixList,
						domainSuffixList, patterns, masks);
				processed |= INDEX_BACKSLASH;
			}
			if (processed == 0L && wordParts[1].contains("\u2014")) {
				String[] mixedCaseEMDashWords = splitWordsOnChar(mixedCaseCleansedWord, '\u2014');
				lastWordMasked = processWords(mixedCaseEMDashWords, '\u2014', sb, lastWordMasked, counts, maskNumbers,
						whitelist, names, geolocations, profanities, queryStringContainsList, domainPrefixList,
						domainSuffixList, patterns, masks);
				processed |= INDEX_EM_DASH;
			}
//			// important this is last
//         if (processed == 0L && wordParts[1].contains("\u223C")) {
//            String[] mixedCaseTildeWords = splitWordsOnChar(mixedCaseCleansedWord, '~');
//            lastWordMasked = processWords(mixedCaseTildeWords, '\u223C', sb, lastWordMasked, counts, maskNumbers,
//                  whitelist, names, geolocations, profanities, queryStringContainsList, domainPrefixList,
//                  domainSuffixList, patterns, masks);
//            processed |= INDEX_TILDE;
//         }
			if (processed == 0L) {
				// process as a normal word
				counts.put("words", ((Long) counts.get("words")) + 1L);
				String testWord = wordParts[1];
				if (whitelist.get(testWord) == null && masks.contains(testWord) == false) {
					updateMasked(testWord,maskNumbers);
					// determine the type of mask to apply
					if (names.get(testWord) != null) {
						counts.put("maskedName", ((Long) counts.get("maskedName")) + 1L);
						if (lastWordMasked.equals(_maskName) == false) {
							sb.append(_maskName);
						} else {
							/**
							 * Back out last splitchar since we are reusing the same mask for a pattern like
							 * namesplitcharname to result in "~name~" and not "~name~ " (assuming space is
							 * the splitchar)
							 */
							sb.setLength(Math.max(sb.length() - 1, 0));
						}
						lastWordMasked = _maskName;
					} else if (geolocations.get(testWord) != null) {
						counts.put("maskedGeo", ((Long) counts.get("maskedGeo")) + 1L);
						if (lastWordMasked.equals(_maskGeo) == false) {
							sb.append(_maskGeo);
						} else {
							/**
							 * Back out last splitchar since we are reusing the same mask for a pattern like
							 * namesplitcharname to result in "~name~" and not "~name~ " (assuming space is
							 * the splitchar)
							 */
							sb.setLength(Math.max(sb.length() - 1, 0));
						}
						lastWordMasked = _maskGeo;
					} else if (profanities.get(testWord) != null) {
						counts.put("maskedBad", ((Long) counts.get("maskedBad")) + 1L);
						if (lastWordMasked.equals(_maskBad) == false) {
							sb.append(_maskBad);
						} else {
							/**
							 * Back out last splitchar since we are reusing the same mask for a pattern like
							 * namesplitcharname to result in "~name~" and not "~name~ " (assuming space is
							 * the splitchar)
							 */
							sb.setLength(Math.max(sb.length() - 1, 0));
						}
						lastWordMasked = _maskBad;
					} else {
						// is this all numbers?
						if (isNumbers(testWord)) {
							if (maskNumbers) {
								counts.put("maskedNum", ((Long) counts.get("maskedNum")) + 1L);
								if (lastWordMasked.equals(_maskNum) == false) {
									sb.append(_maskNum);
								} else {
									/**
									 * Back out last splitchar since we are reusing the same mask for a pattern like
									 * namesplitcharname to result in "~name~" and not "~name~ " (assuming space is
									 * the splitchar)
									 */
									sb.setLength(Math.max(sb.length() - 1, 0));
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
							} else {
								/**
								 * Back out last splitchar since we are reusing the same mask for a pattern like
								 * namesplitcharname to result in "~name~" and not "~name~ " (assuming space is
								 * the splitchar)
								 */
								sb.setLength(Math.max(sb.length() - 1, 0));
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
			if ((wordParts[2].endsWith("\n")) || (wordParts[2].trim().length() > 0)) {
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
	 * @param maskNumbers
	 *               true if words comprising all numbers should be masked
	 */
	public static void updateMasked(String masked,Boolean maskNumbers) {
	   if (!maskNumbers && isNumbers(masked)) {
	      return;
	   }
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

			String tenantID = (String) request.get("tenantID");
			if (tenantID == null) {
				JSONObject error = new JSONObject();
				error.put("error", "tenantID is null");
				errors.add(error);
				return response;
			} else if (_setTenantIDs.contains(tenantID) == false) {
				JSONObject error = new JSONObject();
				error.put("error", "tenantID \"" + tenantID + "\" is a knonwn tenantID.");
				errors.add(error);
				return response;
			}
			List<Pattern> _patterns = _mapPatternsList.get(tenantID);
			if (_patterns == null) {
				JSONObject error = new JSONObject();
				error.put("error", "tenantID \"" + tenantID + "\" has no patterns.");
				errors.add(error);
				return response;
			}
			List<String> _masks = _mapMasksList.get(tenantID);
			if (_masks == null) {
				JSONObject error = new JSONObject();
				error.put("error", "tenantID \"" + tenantID + "\" has no masks.");
				errors.add(error);
				return response;
			}

			Set<String> deletePatterns = new HashSet<String>();
			String delPattern = "";
			String delMask = "";
			JSONObject jObj = null;
			for (Object obj : removals) {
				delPattern = (String) obj;
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
				Pattern pattern = it.next();
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

	public String _ext = "json";

	public NumberFormat _formatter = NumberFormat.getInstance(Locale.US);

	public Path _inputPath = null;

	public String _outputPath = "." + File.separator + "Masked";

	public MaskerDate _startDate = new MaskerDate();

	public Long _totalDialogs = 0L;

	public Long _totalMasked = 0L;

	public Long _totalWords = 0L;

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
	 *                                path to the JSON-based dialog file to be
	 *                                reviewed.
	 * @param whitelist
	 *                                whitelist for the current tenantID
	 * @param names
	 *                                names for the current tenantID
	 * @param geolocations
	 *                                geolocations for the current tenantID
	 * @param profanities
	 *                                profanities for the current tenantID
	 * @param queryStringContainsList
	 *                                queryStringContainsList for the current
	 *                                tenantID
	 * @param domainPrefixList
	 *                                domainPrefixList for the current tenantID
	 * @param domainSuffixList
	 *                                domainSuffixList for the current tenantID
	 * @param patterns
	 *                                the patterns for the current tenantID
	 * @param masks
	 *                                the masks for the current tenantID
	 * @param maskNumbers
	 *                                whether numbers should be masked
	 */
	public void doWork(Path file, JSONObject whitelist, JSONObject names, JSONObject geolocations,
			JSONObject profanities, List<String> queryStringContainsList, List<String> domainPrefixList,
			List<String> domainSuffixList, List<Pattern> patterns, List<String> masks, Boolean maskNumbers) {
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

			maskDialogContent(dialogsObj, shortFileName, whitelist, names, geolocations, profanities,
					queryStringContainsList, domainPrefixList, domainSuffixList, patterns, masks, maskNumbers);
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
	public boolean getParams(String[] args) {
		String inputPath = "." + File.separator + "Dialogs";
		String outputPath = "." + File.separator + "Masked";
		_tenantID = "companyA";
		String tmp = "";
		if (args.length < 1) {
			tmp = MaskerUtils.prompt("Enter the  tenant ID or q to  exit(" + _tenantID + "):");
			if (tmp.length() == 0) {
				tmp = _tenantID;
			}
			if ("q".equalsIgnoreCase(tmp)) {
				return false;
			}
			_tenantID = tmp;
		} else {
			_tenantID = args[0];
		}
		_setTenantIDs.add(_tenantID);
		String filePrefix = "." + File.separator + MaskerConstants.Masker_DIR_PROPERTIES + _tenantID + File.separator;

		try {
			if (args.length < 2) {
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
				inputPath = args[1].trim();
			}
			if (inputPath.endsWith(File.separator) == false) {
				inputPath += File.separator;
			}
			_inputPath = FileSystems.getDefault().getPath(inputPath);
		} catch (InvalidPathException ipe) {
			System.out.println(args[1] + " is not a valid directory to form a path.");
			return false;
		}
		if (args == null || args.length < 3) {
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
			outputPath = args[2].trim();
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

		if (args == null || args.length < 4) {
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
			_whitelistFileName = args[3].trim();
		}
		try {
			System.out.println("Loading " + filePrefix + _whitelistFileName + " -- this could take a few seconds.\n");
			JSONObject _whitelist = (JSONObject) MaskerUtils.loadJSONFile(filePrefix + _whitelistFileName);
			if (_whitelist == null) {
				System.out.println("Can not find the whitelist key in the file " + filePrefix + _whitelistFileName);
				return false;
			}
			_mapWhitelistObjs.put(_tenantID, _whitelist);
		} catch (Exception e) {
			System.out.println("Error loading file " + filePrefix + _whitelistFileName + ": " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}

		if (args == null || args.length < 5) {
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
			_namesFileName = args[4].trim();
		}
		try {
			JSONObject _names = (JSONObject) MaskerUtils.loadJSONFile(filePrefix + _namesFileName);
			_mapNameObjs.put(_tenantID, _names);
		} catch (Exception e) {
			System.out.println("Error loading file " + filePrefix + _namesFileName + ": " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}

		if (args == null || args.length < 6) {
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
			_geolocationsFileName = args[5].trim();
		}
		try {
			JSONObject _geolocations = (JSONObject) MaskerUtils.loadJSONFile(filePrefix + _geolocationsFileName);
			_mapGeoLocationsObjs.put(_tenantID, _geolocations);
		} catch (Exception e) {
			System.out
					.println("Error loading file " + filePrefix + _geolocationsFileName + ": " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}

		if (args == null || args.length < 7) {
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
			_profanitiesFileName = args[6].trim();
		}
		try {
			JSONObject _profanities = (JSONObject) MaskerUtils.loadJSONFile(filePrefix + _profanitiesFileName);
			_mapProfanityObjs.put(_tenantID, _profanities);
		} catch (Exception e) {
			System.out.println("Error loading file " + filePrefix + _profanitiesFileName + ": " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}

		if (args == null || args.length < 8) {
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
			_domainPrefixesFile = args[7].trim();
		}

		try {
			List<String> _domainPrefixList = new ArrayList<String>();
			List<String> domainPrefixList = MaskerUtils.loadTextFile(filePrefix + _domainPrefixesFile);
			for (String domainPrefix : domainPrefixList) {
				if (domainPrefix.startsWith("_")) {
					continue;
				}
				_domainPrefixList.add(domainPrefix.toLowerCase());
			}
			_mapDomainPrefixLists.put(_tenantID, _domainPrefixList);
		} catch (Exception e) {
			System.out.println("Error loading file " + filePrefix + _domainPrefixesFile + ": " + e.getLocalizedMessage());
			return false;
		}

		if (args == null || args.length < 9) {
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
			_domainSuffixesFile = args[8].trim();
		}

		try {
			List<String> _domainSuffixList = new ArrayList<String>();
			List<String> domainSuffixList = MaskerUtils.loadTextFile(filePrefix + _domainSuffixesFile);
			for (String domainSuffix : domainSuffixList) {
				if (domainSuffix.startsWith("_")) {
					continue;
				}
				_domainSuffixList.add(domainSuffix.toLowerCase());
			}
			_mapDomainSuffixLists.put(_tenantID, _domainSuffixList);
		} catch (Exception e) {
			System.out.println("Error loading file " + _domainSuffixesFile + ": " + e.getLocalizedMessage());
			return false;
		}

		if (args == null || args.length < 10) {
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
			_queryStringContainsFile = args[9].trim();
		}

		try {
			List<String> _queryStringContainsList = new ArrayList<String>();
			List<String> queryStringContainsList = MaskerUtils.loadTextFile(filePrefix + _queryStringContainsFile);
			for (String queryStringContains : queryStringContainsList) {
				if (queryStringContains.startsWith("_")) {
					continue;
				}
				_queryStringContainsList.add(queryStringContains.toLowerCase());
			}
			_mapQueryStringLists.put(_tenantID, _queryStringContainsList);
		} catch (Exception e) {
			System.out
					.println("Error loading file " + filePrefix + _queryStringContainsFile + ": " + e.getLocalizedMessage());
			return false;
		}
		if (args == null || args.length < 11) {
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
				_minDialogs = new Integer(args[10]);
				if (_minDialogs < 1) {
					System.out.println("Minimum dialogs per day must be a positive integer.");
					return false;
				}
			} catch (NumberFormatException nfe) {
				System.out.println("Minimum dialogs per day must be a positive integer.");
				return false;
			}
		}
		Boolean _maskNumbers = Boolean.TRUE;
		if (args == null || args.length < 12) {
			tmp = MaskerUtils.prompt("Numbers should be masked, or q to exit (" + _maskNumbers + ")");
			if (tmp == null || tmp.length() == 0) {
				tmp = _maskNumbers.toString();
			}
			if ("q".equalsIgnoreCase(tmp)) {
				return false;
			}
			_maskNumbers = new Boolean(tmp);
			_mapMaskNumbers.put(_tenantID, _maskNumbers);
		} else {
			_maskNumbers = new Boolean(args[11]);
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
	 *                                object containing a set of dialogs between
	 *                                clients and support agents.
	 * @param fileName
	 *                                the name of the file from which the dialogsObj
	 *                                was read
	 * @param whitelist
	 *                                whitelist for the current tenantID
	 * @param names
	 *                                names for the current tenantID
	 * @param geolocations
	 *                                geolocations for the current tenantID
	 * @param profanities
	 *                                profanities for the current tenantID
	 * @param queryStringContainsList
	 *                                queryStringContainsList for the current
	 *                                tenantID
	 * @param domainPrefixList
	 *                                domainPrefixList for the current tenantID
	 * @param domainSuffixList
	 *                                domainSuffixList for the current tenantID
	 * @param patterns
	 *                                the patterns for the current tenantID
	 * @param masks
	 *                                the masks for the current tenantID
	 * @param maskNumbers
	 *                                whether numbers should be masked
	 * @throws Exception
	 */
	protected void maskDialogContent(JSONObject dialogsObj, String fileName, JSONObject whitelist, JSONObject names,
			JSONObject geolocations, JSONObject profanities, List<String> queryStringContainsList,
			List<String> domainPrefixList, List<String> domainSuffixList, List<Pattern> patterns, List<String> masks,
			Boolean maskNumbers) throws Exception {
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
		fileCounts.put("maskedName", 0L);
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
			counts.put("maskedName", 0L);
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
					volleyObj.put("datetime", maskedVolleyDate.toStringDateTime());

				} catch (Exception e) {
					e.printStackTrace();
				}

				maskedVolley = maskVolley(volleyObj, counts, volleyCount, whitelist, names, geolocations, profanities,
						queryStringContainsList, domainPrefixList, domainSuffixList, patterns, masks, maskNumbers);
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
			Long maskedNam = (Long) counts.get("maskedName");
			Long maskedNum = (Long) counts.get("maskedNum");
			Long maskedURL = (Long) counts.get("maskedURL");
			Long masked = maskedBad + maskedGeo + maskedMisc + maskedNum + maskedURL;
			Double pctMasked = (100.0d * masked) / (1.0d * count);
			dialogHeader.put("words", count);
			dialogHeader.put("maskedBad", maskedBad);
			dialogHeader.put("maskedGeo", maskedGeo);
			dialogHeader.put("maskedMisc", maskedMisc);
			dialogHeader.put("maskedName", maskedNam);
			dialogHeader.put("maskedNum", maskedNum);
			dialogHeader.put("maskedURL", maskedURL);
			dialogHeader.put("pctMasked", (_formatter.format(pctMasked)) + "%");
			fileCounts.put("words", ((Long) fileCounts.get("words")) + count);
			fileCounts.put("maskedBad", ((Long) fileCounts.get("maskedBad")) + maskedBad);
			fileCounts.put("maskedGeo", ((Long) fileCounts.get("maskedGeo")) + maskedGeo);
			fileCounts.put("maskedMisc", ((Long) fileCounts.get("maskedMisc")) + maskedMisc);
			fileCounts.put("maskedName", ((Long) fileCounts.get("maskedName")) + maskedNam);
			fileCounts.put("maskedNum", ((Long) fileCounts.get("maskedNum")) + maskedNum);
			fileCounts.put("maskedURL", ((Long) fileCounts.get("maskedURL")) + maskedURL);
			maskedDialogObject.put("dialogHeader", dialogHeader);
			maskedDialogVolleys.add(maskedDialogObject);
		} // end for each dialog
		Long wordCount = (Long) fileCounts.get("words");
		Long maskedBad = (Long) fileCounts.get("maskedBad");
		Long maskedGeo = (Long) fileCounts.get("maskedGeo");
		Long maskedMisc = (Long) fileCounts.get("maskedMisc");
		Long maskedNam = (Long) fileCounts.get("maskedName");
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

}
