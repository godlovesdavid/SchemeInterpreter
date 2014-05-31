package frontend;

import intermediate.Procedure;
import intermediate.Symbol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scanner of Scheme source code that makes tokens out of it.
 */
public class Scanner
{
	//parenthesis token.
	public enum Bracket
	{
		OPEN, CLOSE
	}

	//regular expression matcher.
	Matcher matcher;

	/*
	* Regular expressions
	*/
	//basic characters.
	public static String LETTER = "[A-Za-z]";
	public static String DIGIT = "[0-9]";
	public static String UNSIGNED_INT = DIGIT + "+";
	public static String OPEN_BRACKET = "[(\\[]";
	public static String CLOSE_BRACKET = "[)\\]]";
	public static String BRACKET = OPEN_BRACKET + "|" + CLOSE_BRACKET;

	//token types.
	public static String SYMBOL = "[a-zA-Z\\+\\-\\.\\*\\/<=>!?:\\$%_&~^]+";
	public static String SPECIAL_SYMBOL = "[\\(\\)\\[\\]\\{\\};,\\.\"'#\\\\]";
	public static String INTEGER = "[0-9]+(\\.0*)?|\\.0+";
	public static String NUMBER = UNSIGNED_INT + "(\\.(" + UNSIGNED_INT + ")?)?";
	public static String CHARACTER = "#\\\\.";
	public static String STRING = "\"[^\"]+\"";
	public static String BOOLEAN = "#[tTfF]";
	public static String KEYWORD =
		"and|begin|begin0|break-var|case|cond|cycle|define|delay|delay-list-cons|do|else|extend-syntax|for|freeze|if|lambda|let|letrec|let\\*|macro|object-maker|or|quote|repeat|safe-letrec|set!|stream-cons|variable-case|while|wrap";

	//a token.
	public static String TOKEN = "(?<!;[^\n]{0,1000})(" + BRACKET
		+ "|[^ \t\r\n;]+?(?=" + BRACKET + "| |\t|\r|\n|;|$))"; //skip comments, then match bracket, or non-delimiter characters until delimiter

	public Scanner(String sourcecode)
	{
//		System.out.println(unsugar(sourcecode));
		matcher = Pattern.compile(TOKEN).matcher(unsugar(sourcecode));
	}

	/**
	 * Convert ', cadr, etc. to their true forms.
	 * @param sourcecode
	 * @return
	 */
	public static String unsugar(String sourcecode)
	{
		String result = sourcecode;
		Matcher matcher = null;

		//quote
		while ((matcher = Pattern.compile("'").matcher(result)).find())
		{
			String substring = result.substring(matcher.end(), result.length());

			int placetoinsert = giveMatchedParenthesesIdx(substring);

			substring =
				substring.substring(0, placetoinsert) + ")"
					+ substring.substring(placetoinsert, substring.length());

			result =
				result.substring(0, matcher.start())
					+ matcher.group().replaceFirst("'", "(quote " + substring);
		}

		//car / cdr
		while ((matcher = Pattern.compile("c([ad]{2,1000})r").matcher(result))
			.find())
		{
			String substring = result.substring(matcher.end(), result.length());

			int placetoinsert = giveMatchedParenthesesIdx(substring);

			String closeparentheses = "";
			for (int i = 0; i < matcher.group(1).length() - 1; i++)
				closeparentheses += ")";

			substring =
				substring.substring(0, placetoinsert) + closeparentheses
					+ substring.substring(placetoinsert, substring.length());

			result =
				result.substring(0, matcher.start())
					+ matcher.group().replaceFirst("^c(.)", "c$1r").replaceFirst(
						"(.)r$", "(c$1r").replaceAll("([ad])(?!r)", "(c$1r")
					+ substring;
		}

		return result;
	}

	/**
	 * Give the place in a string where the parentheses have all matched up 
	 * (any successive closing ones are wrong then).
	 * @param sourcecode
	 * @return
	 */
	public static int giveMatchedParenthesesIdx(String sourcecode)
	{
		//if next token isnt open parentheses, just return the place of that token's end.
		Matcher matcher = Pattern.compile(TOKEN).matcher(sourcecode);
		if (matcher.find())
		{
			if (!matcher.group().matches(OPEN_BRACKET))
				return matcher.end();

			int unmatched = 1;
			int lastfind = matcher.start();
			while (matcher.find() && unmatched != 0)
				if (matcher.group().matches(BRACKET))
				{
					unmatched +=
						(matcher.group().matches(OPEN_BRACKET) ? 1 : -1);
					lastfind = matcher.start();
				}
			return lastfind;
		}
		return 0;
	}

	/**
	 * Give next token from where the scanner is at, in form of primitive.
	 * @return Java object representing Scheme primitive
	 */
	public Object scanForNextToken()
	{
		//scan next potential token.
		if (!matcher.find())
			return null;

		//get the result.
		String tokenstring = matcher.group();

		//make token.
		if (tokenstring.matches(OPEN_BRACKET))
			return Bracket.OPEN;
		if (tokenstring.matches(CLOSE_BRACKET))
			return Bracket.CLOSE;
		if (tokenstring.matches(INTEGER))
			return (int) (double) Double.valueOf(tokenstring.toString());
		if (tokenstring.matches(NUMBER))
			return Double.parseDouble(tokenstring);
		if (tokenstring.matches(BOOLEAN))
			return tokenstring.equalsIgnoreCase("#t") ? Boolean.TRUE
				: Boolean.FALSE;
		if (tokenstring.matches(CHARACTER))
			return tokenstring.charAt(2);
		if (tokenstring.matches(STRING))
			return tokenstring.substring(1, tokenstring.length() - 1);
		if (Procedure.PROCEDURES.keySet().contains(tokenstring))
			return Procedure.PROCEDURES.get(tokenstring).clone();
		if (tokenstring.matches(SYMBOL))
			return new Symbol(tokenstring);

		return null;
	}
}
