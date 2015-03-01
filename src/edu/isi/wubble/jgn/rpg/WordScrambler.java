package edu.isi.wubble.jgn.rpg;

import static java.lang.Math.random;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import edu.isi.wubble.jgn.db.DatabaseManager;

public class WordScrambler {
	protected String _consonants = "bcdfghjklmnpqrstvwxyz";
	protected String _vowels = "aeiou";
	
	protected String _initialVowels = "aeiou";
	protected String _middelVowels = "aeiou";
	protected String _finalVowels = "aeiouy";

	protected ArrayList<String> _initialVowelPair = new ArrayList<String>();
	protected ArrayList<String> _middleVowelPair = new ArrayList<String>();
	protected ArrayList<String> _finalVowelPair = new ArrayList<String>();
	
	protected String _initialConsonant = "bcdfghjklmnprstvwz";
	protected String _middleConsonant = "bcdfghjklmnprstvw";
	protected String _finalConsonant = "bdgklmnprstw";
	
	protected ArrayList<String> _initialConsonantPair = new ArrayList<String>();
	protected ArrayList<String> _middleConsonantPair = new ArrayList<String>();
	protected ArrayList<String> _finalConsonantPair = new ArrayList<String>();
	
	protected HashMap<String,String> _wordMap = new HashMap<String,String>();

	public WordScrambler() {
		initVowels();
		initConsonants();
		
		loadWords();
	}
	
	protected void initVowels() {
		_initialVowelPair.add("ea");
		_initialVowelPair.add("ey");
		_initialVowelPair.add("ei");
		_initialVowelPair.add("eo");
		_initialVowelPair.add("eu");
		_initialVowelPair.add("au");
		_initialVowelPair.add("ai");
		_initialVowelPair.add("ay");
		_initialVowelPair.add("oi");
		_initialVowelPair.add("ou");
		_initialVowelPair.add("oa");
		_initialVowelPair.add("oo");
		_initialVowelPair.add("ye");
		_initialVowelPair.add("ya");
		_initialVowelPair.add("yi");
		_initialVowelPair.add("yu");
		_initialVowelPair.add("yo");
		
		_middleVowelPair.add("ea");
		_middleVowelPair.add("ei");
		_middleVowelPair.add("eo");
		_middleVowelPair.add("eu");
		_middleVowelPair.add("ee");
		_middleVowelPair.add("au");
		_middleVowelPair.add("ai");
		_middleVowelPair.add("ay");
		_middleVowelPair.add("oi");
		_middleVowelPair.add("oe");
		_middleVowelPair.add("ou");
		_middleVowelPair.add("oa");
		_middleVowelPair.add("oo");
		_middleVowelPair.add("io");
		_middleVowelPair.add("ie");
		
		_finalVowelPair.add("oo");
		_finalVowelPair.add("oe");
		_finalVowelPair.add("ue");
		_finalVowelPair.add("ou");
		_finalVowelPair.add("ie");		
		_finalVowelPair.add("io");		
		_finalVowelPair.add("oy");		
		_finalVowelPair.add("ey");			
	}
	
	protected void initConsonants() {
		_initialConsonantPair.add("tr");
		_initialConsonantPair.add("th");
		_initialConsonantPair.add("wh");
		_initialConsonantPair.add("qu");
		_initialConsonantPair.add("sw");
		_initialConsonantPair.add("st");
		_initialConsonantPair.add("sm");
		_initialConsonantPair.add("sc");
		_initialConsonantPair.add("sl");
		_initialConsonantPair.add("sp");
		_initialConsonantPair.add("sn");
		_initialConsonantPair.add("sh");
		_initialConsonantPair.add("sk");
		_initialConsonantPair.add("br");
		_initialConsonantPair.add("bl");
		_initialConsonantPair.add("gl");
		_initialConsonantPair.add("gr");
		_initialConsonantPair.add("cl");
		_initialConsonantPair.add("ch");
		_initialConsonantPair.add("cr");
		_initialConsonantPair.add("dr");
		_initialConsonantPair.add("pr");
		_initialConsonantPair.add("pl");
		_initialConsonantPair.add("ph");
		_initialConsonantPair.add("kl");
		_initialConsonantPair.add("fr");
		_initialConsonantPair.add("fl");
		
		_middleConsonantPair.add("th");
		_middleConsonantPair.add("tr");
		_middleConsonantPair.add("tt");
		_middleConsonantPair.add("ww");
		_middleConsonantPair.add("sw");
		_middleConsonantPair.add("st");
		_middleConsonantPair.add("sm");
		_middleConsonantPair.add("sc");
		_middleConsonantPair.add("sl");
		_middleConsonantPair.add("ss");
		_middleConsonantPair.add("sp");
		_middleConsonantPair.add("sn");
		_middleConsonantPair.add("sh");
		_middleConsonantPair.add("sk");
		_middleConsonantPair.add("br");
		_middleConsonantPair.add("bb");
		_middleConsonantPair.add("bl");
		_middleConsonantPair.add("bs");
		_middleConsonantPair.add("rt");
		_middleConsonantPair.add("rs");
		_middleConsonantPair.add("rl");
		_middleConsonantPair.add("rg");
		_middleConsonantPair.add("rd");
		_middleConsonantPair.add("rk");
		_middleConsonantPair.add("rr");
		_middleConsonantPair.add("rn");
		_middleConsonantPair.add("gh");
		_middleConsonantPair.add("gl");
		_middleConsonantPair.add("gr");
		_middleConsonantPair.add("gg");
		_middleConsonantPair.add("gs");
		_middleConsonantPair.add("cl");
		_middleConsonantPair.add("ck");
		_middleConsonantPair.add("ch");
		_middleConsonantPair.add("cc");
		_middleConsonantPair.add("cr");
		_middleConsonantPair.add("ld");
		_middleConsonantPair.add("ll");
		_middleConsonantPair.add("lt");
		_middleConsonantPair.add("dd");
		_middleConsonantPair.add("ds");
		_middleConsonantPair.add("dr");
		_middleConsonantPair.add("nd");
		_middleConsonantPair.add("ng");
		_middleConsonantPair.add("ns");
		_middleConsonantPair.add("nn");
		_middleConsonantPair.add("nt");
		_middleConsonantPair.add("pr");
		_middleConsonantPair.add("pp");
		_middleConsonantPair.add("pl");
		_middleConsonantPair.add("ph");
		_middleConsonantPair.add("ps");
		_middleConsonantPair.add("ks");
		_middleConsonantPair.add("kl");
		_middleConsonantPair.add("mp");
		_middleConsonantPair.add("mm");
		_middleConsonantPair.add("ms");
		_middleConsonantPair.add("mr");
		_middleConsonantPair.add("ff");
		_middleConsonantPair.add("fr");
		_middleConsonantPair.add("fl");
		
		_finalConsonantPair.add("th");
		_finalConsonantPair.add("st");
		_finalConsonantPair.add("ss");
		_finalConsonantPair.add("sh");
		_finalConsonantPair.add("sk");
		_finalConsonantPair.add("bs");
		_finalConsonantPair.add("rt");
		_finalConsonantPair.add("rs");
		_finalConsonantPair.add("ry");
		_finalConsonantPair.add("rd");
		_finalConsonantPair.add("rn");
		_finalConsonantPair.add("gh");
		_finalConsonantPair.add("gs");
		_finalConsonantPair.add("ck");
		_finalConsonantPair.add("ch");
		_finalConsonantPair.add("ld");
		_finalConsonantPair.add("ll");
		_finalConsonantPair.add("lt");
		_finalConsonantPair.add("ds");
		_finalConsonantPair.add("nd");
		_finalConsonantPair.add("ng");
		_finalConsonantPair.add("ns");
		_finalConsonantPair.add("nt");
		_finalConsonantPair.add("ph");
		_finalConsonantPair.add("ps");
		_finalConsonantPair.add("ks");
		_finalConsonantPair.add("mp");
		_finalConsonantPair.add("ms");
	}
	
	
	protected void loadWords() {
		try {
			Statement s = DatabaseManager.inst().createStatement();
			String sql = "SELECT english_word, scrambled_word FROM word_scrambler";
			ResultSet rs = s.executeQuery(sql);
			while (rs.next()) {
				_wordMap.put(rs.getString(1),rs.getString(2));
			}
			rs.close();
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	protected void saveWord(String english, String scrambled) {
		try {
			Statement s = DatabaseManager.inst().createStatement();
			String sql = "INSERT INTO word_scrambler (english_word, scrambled_word)" +
					" VALUES ('" + english + "','" + scrambled + "')";
			s.execute(sql);
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	protected String makeCVWord(String word) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < word.length(); ++i) {
			char c = word.charAt(i);
			if (_vowels.indexOf(c) != -1) 
				buf.append('v');
			else 
				buf.append('c');
		}
		return buf.toString();
	}
	
	protected String compactCVWord(String cvWord) {
		if (cvWord.length() <= 1) 
			return cvWord;
		
		if (cvWord.charAt(0) == cvWord.charAt(1)) {
			String shorter = cvWord.substring(0,2);
			String rest = cvWord.substring(2);
			if (!"".equals(rest)) {
				return shorter + "|" + compactCVWord(rest);
			} else {
				return shorter;
			}
		} else {
			String shorter = cvWord.substring(0,1);
			String rest = cvWord.substring(1);
			if (!"".equals(rest)) {
				return shorter + "|" + compactCVWord(rest);
			} else {
				return shorter;
			}
		}
	}
	
	protected String beginningAndEndings(String compactCVWord) {
		StringBuffer buf = new StringBuffer();
		StringTokenizer str = new StringTokenizer(compactCVWord, "|");
		buf.append("b" + str.nextToken() + "|");
		while (str.hasMoreTokens()) {
			String curr = str.nextToken();
			if (str.hasMoreTokens()) {
				buf.append(curr + "|");
			} else {
				buf.append("e" + curr);
			}
		}
		return buf.toString();
	}
	
	protected String randomElement(String s) {
		int randValue = (int) (random()*(float)s.length());
		return s.substring(randValue,randValue+1);
	}
	
	protected String randomElement(ArrayList<String> set) {
		int randValue = (int) (random()*(float)set.size());
		return set.get(randValue);
	}

	protected String translateWordHelper(String word) {
		String cvWord = makeCVWord(word);
		String compact = compactCVWord(cvWord);
		String augment = beginningAndEndings(compact);
		
		StringBuffer newWord = new StringBuffer();
		StringTokenizer str = new StringTokenizer(augment, "|");
		while (str.hasMoreTokens()) {
			String tok = str.nextToken();
			if ("ebv".equals(tok)) {
				newWord.append(randomElement(_vowels));
			} else if ("bv".equals(tok)) {
				newWord.append(randomElement(_vowels));
			} else if ("bvv".equals(tok)) {
				newWord.append(randomElement(_initialVowelPair));
			} else if ("bc".equals(tok)) {
				newWord.append(randomElement(_initialConsonant));
			} else if ("bcc".equals(tok)) {
				newWord.append(randomElement(_initialConsonantPair));
			} else if ("v".equals(tok)) {
				newWord.append(randomElement(_vowels));
			} else if ("vv".equals(tok)) {
				newWord.append(randomElement(_middleVowelPair));
			} else if ("c".equals(tok)) {
				newWord.append(randomElement(_middleConsonant));
			} else if ("cc".equals(tok)) {
				newWord.append(randomElement(_middleConsonantPair));
			} else if ("ev".equals(tok)) {
				newWord.append(randomElement(_finalVowels));
			} else if ("evv".equals(tok)) {
				newWord.append(randomElement(_finalVowelPair));
			} else if ("ec".equals(tok)) {
				newWord.append(randomElement(_finalConsonant));
			} else if ("ecc".equals(tok)) {
				newWord.append(randomElement(_finalConsonantPair));
			} else {
				System.out.println("unknown: " + tok);
			}
		}
		return newWord.toString();
	}
	
	/**
	 * assume that the word will be lowercase as it comes
	 * in since this tree is case sensitive...
	 * @param word
	 * @return
	 */
	public String translateWord(String word) {
		String scrambled = _wordMap.get(word);
		if (scrambled != null) {
			return scrambled;
		}
		
		scrambled = translateWordHelper(word);
		saveWord(word,scrambled);
		_wordMap.put(word, scrambled);
		
		return scrambled;
	}
	
	public static void main(String[] args) {
		String[] words = new String[] {
				"go", "to", "the", "yellow", "cube",
				"put", "down", "the", "cube"
		};
		
		WordScrambler ws = new WordScrambler();
		for (int i = 0; i < words.length; ++i) {
			System.out.println(words[i] + " " + ws.translateWord(words[i]));
		}
	}
}
