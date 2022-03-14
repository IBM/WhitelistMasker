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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.api.json.JSONArray;
import com.api.json.JSONObject;

/**
 * Make the whitelist words from component parts (lists of words, etc)
 *
 */
public class MakeWhitelistWords implements Serializable {

	private static final long serialVersionUID = -2628246300032922461L;

	/**
	 * Uses the list of words, each of which is split on whitespace regex characters
	 * to add the parts to the supplied JSON object, capturing their provenance
	 * (type) and optionally cleaned
	 * 
	 * @param words
	 *                  list of words to be considered
	 * @param obj
	 *                  JSON object to capture word parts
	 * @param type
	 *                  the provenance from which the words originated
	 * @param cleanword
	 *                  whether to cleanse the word before adding to the JSON object
	 * @return the number of words added to the JSON object (which may be less than
	 *         the number of words submitted due to collisions with existing words
	 *         in the JSON object.
	 */
	static public int addWordPartsToJSONObject(List<String> words, JSONObject obj, String type, boolean cleanword) {
		int added = 0;
		String[] wordPieces = null;
		String[] wordParts = null;
		for (String word : words) {
			if (word.contains("_")) {
				// skip complex words like drinking_age
				continue;
			}
			wordPieces = word.toLowerCase().split("\\s");
			for (String piece : wordPieces) {
				if (piece.length() == 0) {
					continue;
				}
				wordParts = MaskerUtils.cleanWord(piece);
				if (cleanword) {
					word = wordParts[1];
				} else {
					word = word.toLowerCase();
				}
				if (word.length() > 0 && (MaskerUtils.checkWordFilter(word) == false || cleanword == false)) {
					// preserve provenance
					if (obj.get(word) == null) {
						obj.put(word, type);
						added++;
					}
				}
			}
		}
		return added;
	}

	/**
	 * Write list of input words to the supplied JSON object, providing word
	 * cleaning, and recording the provenance of the words using the type
	 * 
	 * @param words
	 *                  words to be added
	 * @param obj
	 *                  object to capture the words being added
	 * @param type
	 *                  the provenance from where the words originated
	 * @param cleanword
	 *                  whether to clean the word or leave it as-is
	 * @return the number of words added to the JSON object (which may be less than
	 *         the number of words submitted due to collisions with existing words
	 *         in the JSON object.
	 */
	static public int addWordsToJSONObject(List<String> words, JSONObject obj, String type, boolean cleanword) {
		int added = 0;
		String[] wordParts = null;
		for (String word : words) {
			if (word.contains("_")) {
				// skip complex words like drinking_age
				continue;
			}
			wordParts = MaskerUtils.cleanWord(word.toLowerCase());
			if (cleanword) {
				word = wordParts[1];
			} else {
				word = word.toLowerCase();
			}
			if (word.length() > 0 && (MaskerUtils.checkWordFilter(word) == false || cleanword == false)) {
				// preserve provenance
				if (obj.get(word) == null) {
					obj.put(word, type);
					added++;
				}
			}
		}
		return added;
	}

	/**
	 * Utility to construct the whitelist-words.json file from component parts.
	 * Components are found in the properties directory and output is written to the
	 * properties directory.
	 * 
	 * @param args
	 *             not used
	 */
	public static void main(String[] args) {
		System.out.println("Make Whitelist Words");
		String tenantID = "companyA";
		if (args.length >= 1) {
			tenantID = args[0];
		} else {
			String test = MaskerUtils.prompt("Enter tenant id or q to quit (" + tenantID + "):");
			if (test.length() == 0) {
				test = tenantID;
			}
			if ("q".equalsIgnoreCase(test)) {
				System.out.println("Goodbye");
				System.exit(0);
			}
			tenantID = test;
		}
		System.out.println("Generating whitelist");
		JSONObject _potentialWhitelistWords = new JSONObject();
		JSONObject _names = new JSONObject();
		JSONObject _geolocations = new JSONObject();
		JSONObject _geolocationsRemoved = new JSONObject();
		Set<String> _geolocationsReviewed = new HashSet<String>();
		JSONObject _namesRemoved = new JSONObject();
		Set<String> _namesReviewed = new HashSet<String>();
		JSONObject _profanities = new JSONObject();
		JSONObject _profanitiesRemoved = new JSONObject();
		Set<String> _profanitiesReviewed = new HashSet<String>();
		JSONObject _emojis = new JSONObject();

		/**
		 * Note, words containing an underscored are ignored so an underscore can also
		 * serve as a comment
		 */
		try {
			System.out.println("Populate white-list words from sources.");
			List<String> words = new ArrayList<String>();
			System.out.println("Loading umich-words.txt");
			words = MaskerUtils
					.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "umich-words.txt");
			System.out.println(
					"Added " + addWordsToJSONObject(words, _potentialWhitelistWords, "umich_word", true) + " umich_words.");

			System.out.println("Loading workspace-words.json");
			JSONObject workspaceWords = MaskerUtils.loadJSONFile(
					MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "workspace-words.json");
			JSONObject whitelist = (JSONObject) workspaceWords.get("whitelist");
			List<String> whitelist_words = new ArrayList<String>(whitelist.keySet());
			System.out
					.println("Added " + addWordsToJSONObject(whitelist_words, _potentialWhitelistWords, "workspace", true)
							+ " workspace words.");

			System.out.println("Loading website-words.json (from websites)");
			JSONObject websiteWords = MaskerUtils
					.loadJSONFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "website-words.json");
			words = new ArrayList<String>(websiteWords.keySet());
			System.out.println(
					"Added " + addWordsToJSONObject(words, _potentialWhitelistWords, "website", true) + " website words.");

			// have the potentialWhitelistWords with provenance at this point
			System.out.println("Initial whitelist size is " + _potentialWhitelistWords.size());

			System.out.println("Now populate cities from sources.");
			System.out.println("Loading cities.txt");
			List<String> cities = MaskerUtils
					.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "cities.txt");
			System.out.println("Added " + addWordPartsToJSONObject(cities, _geolocations, "city", false) + " cities.");

			System.out.println("Now populate states from sources.");
			System.out.println("Loading states.txt");
			List<String> states = MaskerUtils
					.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "states.txt");
			System.out.println("Added " + addWordPartsToJSONObject(states, _geolocations, "state", false) + " states.");

			System.out.println("Now populate countries from sources.");
			System.out.println("Loading countries.txt");
			List<String> countries = MaskerUtils
					.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "countries.txt");
			System.out
					.println("Added " + addWordPartsToJSONObject(countries, _geolocations, "country", true) + " countries.");

			System.out.println("Now populate names from sources.");
			System.out.println("Loading first_names.all.txt");
			List<String> firstnames = MaskerUtils
					.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "first_names.all.txt");
			System.out.println("Added " + addWordsToJSONObject(firstnames, _names, "first_name", false) + " first names.");

			System.out.println("Loading last_names.all.txt");
			List<String> lastnames = MaskerUtils
					.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "last_names.all.txt");
			System.out.println("Added " + addWordsToJSONObject(lastnames, _names, "last_name", false) + " last names.");

			System.out.println("Loading dialogNames.json");
			JSONObject dialogNames = MaskerUtils
					.loadJSONFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "dialog_names.json");
			List<String> dialognames = new ArrayList<String>(dialogNames.keySet());
			System.out
					.println("Added " + addWordsToJSONObject(dialognames, _names, "dialog_name", false) + " dialog names.");

			System.out.println("Populate profanities from sources.");
			System.out.println("Loading profanity_words.txt");
			List<String> profanitywords = MaskerUtils
					.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "profanity_words.txt");
			System.out.println(
					"Added " + addWordsToJSONObject(profanitywords, _profanities, "profanity", false) + " profanities.");

			System.out.println("Now remove names from the _referenceWords.");
			String name = null;
			for (Iterator<String> it = _names.keySet().iterator(); it.hasNext();) {
				name = it.next();
				if (_potentialWhitelistWords.remove(name) != null) {
					_namesRemoved.put(name, _names.get(name));
				}
				_namesReviewed.add(name);

			}

			System.out.println("Now remove geolocations from the _referenceWords.");
			String geoloc = null;
			for (Iterator<String> it = _geolocations.keySet().iterator(); it.hasNext();) {
				geoloc = it.next();
				if (_potentialWhitelistWords.remove(geoloc) != null) {
					_geolocationsRemoved.put(geoloc, _geolocations.get(geoloc));
				}
				_geolocationsReviewed.add(geoloc);

			}

			System.out.println("Now remove profanities from the _referenceWords.");
			String profanityWord = null;
			for (Iterator<String> it = _profanities.keySet().iterator(); it.hasNext();) {
				profanityWord = it.next();
				if (_potentialWhitelistWords.remove(profanityWord) != null) {
					_profanitiesRemoved.put(profanityWord, _profanities.get(profanityWord));
				}
				_profanitiesReviewed.add(profanityWord);

			}

			System.out.println("Of the " + _namesReviewed.size() + " names we removed " + _namesRemoved.size()
					+ " from the whitelist.");
			System.out.println("Of the " + _geolocationsReviewed.size() + " geolocations we removed "
					+ _geolocationsRemoved.size() + " from the whitelist.");
			System.out.println("Of the " + _profanitiesReviewed.size() + " profanities we removed "
					+ _profanitiesRemoved.size() + " from the whitelist.");

			System.out.println("After cleanup the whitelist size is " + _potentialWhitelistWords.size());

			System.out.println("Augmenting whitelist with override-words.txt overrides.");
			List<String> overrideWords = MaskerUtils
					.loadTextFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "override-words.txt");
			Collections.sort(overrideWords);
			for (String overrideWord : overrideWords) {
				if (overrideWord.contains("_")) {
					continue;
				}
				if (_potentialWhitelistWords.get(overrideWord) == null) {
					_potentialWhitelistWords.put(overrideWord.toLowerCase(), "override");
				}
			}

			System.out.println("After adding the override words the whitelist size is " + _potentialWhitelistWords.size());
			
			try {
   			System.out.println("Loading emoji_overrides.json");
   			_emojis = MaskerUtils.loadJSONFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "emoji_overrides.json");
   			JSONArray emoji_overrides = (JSONArray)_emojis.get("emoji_overrides");
   			JSONObject emoji_obj = null;
   			String emoji_value = "";
   			String emoji_label = "";
   			for (Object obj : emoji_overrides) {
   			   emoji_obj = (JSONObject) obj;
   			   for (Iterator<String>it = emoji_obj.keySet().iterator();it.hasNext();) {
   			      emoji_label = it.next();
   			      emoji_value = (String)emoji_obj.get(emoji_label);
   			      _potentialWhitelistWords.put(emoji_value, emoji_label.toLowerCase());
   			   }
   			}
			} catch (Exception e) {
			   // ignore if not found, just show the error
			   System.out.println("Could not process the emoji_overrides.json file. Error: "+e.getLocalizedMessage());
			}

			MaskerUtils.saveJSONFile(
					MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "whitelist-words.json",
					_potentialWhitelistWords);
			System.out.println("Saved whitelist-words.json");
			MaskerUtils.saveJSONFile(MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "names.json",
					_names);
			System.out.println("Saved " + _names.size() + " entries to names.json");
			MaskerUtils.saveJSONFile(
					MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "profanities.json", _profanities);
			System.out.println("Saved " + _profanities.size() + " entries to profanities.json");
			MaskerUtils.saveJSONFile(
					MaskerConstants.Masker_DIR_PROPERTIES + tenantID + File.separator + "geolocations.json", _geolocations);
			System.out.println("Saved " + _geolocations.size() + " entries to geolocations.json");

			System.out.println("\nPotential names allowed as override words:");
			int count = 0;
			for (String override : overrideWords) {
				if (_namesReviewed.contains(override.toLowerCase())) {
					count++;
					if (count % 15 == 0) {
						System.out.println();
					}
					System.out.print(override + ", ");
				}
			}
			System.out.println();

			System.out.println("\nPotential geolocations allowed as override words:");
			count = 0;
			for (String override : overrideWords) {
				if (_geolocationsReviewed.contains(override.toLowerCase())) {
					count++;
					if (count % 15 == 0) {
						System.out.println();
					}
					System.out.print(override + ", ");
				}
			}
			System.out.println();

			System.out.println("\nPotential profanities allowed as override words:");
			count = 0;
			for (String override : overrideWords) {
				if (_profanitiesReviewed.contains(override.toLowerCase())) {
					count++;
					if (count % 15 == 0) {
						System.out.println();
					}
					System.out.print(override + ", ");
				}
			}
			System.out.println();

			System.out.println("\nFinal number of whitelist words: " + _potentialWhitelistWords.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("\nGoodbye");
	}

	/**
	 * Constructor
	 */
	public MakeWhitelistWords() {
	}
}
