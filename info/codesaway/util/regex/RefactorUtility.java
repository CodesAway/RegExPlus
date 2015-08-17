package info.codesaway.util.regex;

import static info.codesaway.util.regex.Matcher.noNamedGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Contains utility functions / fields that are used when refactoring the
 * inputted regular expression. These functions / fields are only meant to
 * be called from the {@link Refactor} class.
 */
class RefactorUtility
{
	/** String pattern to match a group name (excluding occurrence). */
	// private static final String groupName = "\\w++";
	private static final String groupName = "(?:\\p{javaJavaIdentifierStart}|\\d)\\p{javaJavaIdentifierPart}*+";

	/**
	 * String pattern to match an optional group name (excluding
	 * occurrence).
	 */
	// private static final String optGroupName = "\\w*+";
	private static final String optGroupName = "(?:(?:\\p{javaJavaIdentifierStart}|\\d)\\p{javaJavaIdentifierPart}*+)?+";

	/**
	 * String pattern to match a group name (including occurrence)
	 * 
	 * <p>(2 groups)</p>
	 * <ol>
	 * <li>Group index wrapped in '[]' or group name (can be empty string)</li>
	 * <li>Group occurrence or group index if first group is empty (wrapped in '[]')</li>
	 * </ol>
	 */
	static final java.util.regex.Pattern fullGroupName = java.util.regex.Pattern
			.compile("(\\[-?\\d++]|" + optGroupName + ")(?:\\[(-?\\d++)])?");

	/**
	 * Main method
	 * 
	 * @param args
	 *            (not used)
	 */
	public static void main(String[] args)
	{
		// String input = "[1]";
		// String input = "[-1]";

		// String input = "groupName[-1]";

		// String input = "[1][1]";

		// Bad data
		// String input = "[1] ";

		// Will think " " is group name (what type of error is thrown?)
		// String input = " [1]";

		// Bad, but will parse (what type of error is thrown?)
		// String input = "[ 1 ]";

		// String input = "[1][[1]";
		// String input = "[1]a[2]";
		// String input = " [1][2]";
		// String input = "[1][2] ";
		String input = "[[1][2]";

		java.util.regex.Matcher matcher = fullGroupName.matcher(input);

		if (matcher.matches())
		{
			System.out.println("1: " + matcher.group(1));
			System.out.println("2: " + matcher.group(2));

			parseGroup(input);
		}
		else
		{
			System.out.println("Doesn't match: " + input);
			parseGroup(input);
		}
	}

	private static String[] parseGroup(String group)
	{
		String groupName;
		String occurrence;

		int bracketIndex = group.indexOf('[');

		if (bracketIndex != -1)
		{
			// Has bracket
			// [1], [-1], [1][1], groupName[occurrence]

			int lastBracketIndex = group.lastIndexOf('[');

			if (bracketIndex == lastBracketIndex)
			{
				// Has only one bracket
				// [1], [-1], groupName[occurrence]

				if (bracketIndex == 0)
				{
					// Starts with bracket
					// [1], [-1]

					if (!group.endsWith("]"))
						throw noNamedGroup(group);

					groupName = group;
					occurrence = null;
				}
				else
				{
					// Has bracket, not a beginning
					// groupName[occurrence]

					if (!group.endsWith("]"))
						throw noNamedGroup(group);

					groupName = group.substring(0, bracketIndex);

					// Get part between brackets
					occurrence = group.substring(bracketIndex + 1, group.length() - 1);
				}
			}
			else
			{
				// Has two brackets
				// [1][1], [1][-1]

				if (bracketIndex != 0)
					throw noNamedGroup(group);

				int closeBracket = group.indexOf(']');

				if (closeBracket != lastBracketIndex - 1)
					throw noNamedGroup(group);

				if (!group.endsWith("]"))
					throw noNamedGroup(group);

				groupName = group.substring(0, lastBracketIndex);

				if (groupName.indexOf('[', 1) != -1)
				{
					// Has multiple opening brackets
					// [[1]
					throw noNamedGroup(group);
				}

				occurrence = group.substring(lastBracketIndex + 1, group.length() - 1);
			}
		}
		else
		{
			// Has no bracket, just a group name
			groupName = group;
			occurrence = null;
		}

		return new String[] { groupName, occurrence };
	}

	// static final java.util.regex.Pattern fullGroupName = java.util.regex.Pattern
	// .compile("(\\[-?\\d++\\]|-?\\d++|" + optGroupName + ")(?:\\[(-?\\d++)])?");

	/**
	 * String pattern to match an "any group"
	 * 
	 * (e.g. groupName, groupName[0])
	 * 
	 * <p>(1 group)</p>
	 */
	private static final String fullGroupName0 = "(\\[-?\\d++]|" +
			groupName + ")(?:\\[0++])?";

	/**
	 * Formats an integer character code (0 through 255) as
	 * <code>\x</code><i>hh</i>.
	 */
	static final String hexCodeFormat = "\\x%1$02x";

	/**
	 * Formats an integer character code as
	 * <code>&#92;u</code><i>hhhh</i>.
	 */
	static final String unicodeFormat = "\\u%1$04x";

	static final Map<String, String> posixClasses = new HashMap<String, String>(
			13);

	static {
		posixClasses.put("alnum", "Alnum");
		posixClasses.put("alpha", "Alpha");
		posixClasses.put("ascii", "ASCII");
		posixClasses.put("blank", "Blank");
		posixClasses.put("cntrl", "Cntrl");
		posixClasses.put("digit", "Digit");
		posixClasses.put("graph", "Graph");
		posixClasses.put("lower", "Lower");
		posixClasses.put("print", "Print");
		posixClasses.put("punct", "Punct");
		posixClasses.put("space", "Space");
		posixClasses.put("upper", "Upper");
		posixClasses.put("xdigit", "XDigit");
	}

	/**
	 * Pattern to match parts of the inputted regular expression that need
	 * to be processed before refactoring.
	 */
	static final java.util.regex.Pattern preRefactor = java.util.regex.Pattern
			.compile(

			/*
			 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:" (also
			 * matches a non-capture group - onFlags/offFlags are omitted)
			 * 
			 * group: onFlags (empty string if none) group + 1: offFlags
			 * (empty string if none; null, if omitted) (2 groups)
			 */
			"\\Q(?\\E(\\w*+)(?:-(\\w*+))?[:\\)]|"

					/*
					 * matches a named capture group
					 * "(?<name>" (form 0)
					 * "(?'name'" (form 1)
					 * "(?P<name>" (form 2)
					 * 
					 * group: everything after first symbol
					 * group + 1: the name
					 * (6 groups)
					 */

					/* form 0 - 2 start */
					+ "\\Q(?\\E(?:"

					/* form 0 */
					+ "<((" + groupName + ")>?)|"

					/* form 1 */
					+ "'((" + optGroupName + ")'?)|"

					/* form 2 */
					+ "P<((" + optGroupName + ")>?)"

					/* form 0 - 2 end */
					+ ")|"

					/*
					 * matches an unnamed capture group "(" - not followed by a "?" (or a '*', used by verbs)
					 * 
					 * group: everything (1 group)
					 */
					+ "(\\((?![?*]))|"

					/*
					 * matches a back reference (by name)
					 * "\g{name}" (form 0)
					 * "\k<name>" (form 1)
					 * "\k'name'" (form 2)
					 * "\k{name}" (form 3)
					 * "(?P=name)" (form 4)
					 * 
					 * group : the name
					 * 
					 * (5 groups)
					 * (<name> can only be an "any group" (e.g. groupName[0]))
					 */

					// NOTE: escaped closing '}' because required for Android (for some reason)

					/* form 0 */
					+ "\\Q\\g{\\E" + fullGroupName0 + "\\}|"

					/* form 1 - 3 start */
					+ "\\\\k(?:"

					/* form 1 */
					+ "<" + fullGroupName0 + ">|"

					/* form 2 */
					+ "'" + fullGroupName0 + "'|"

					/* form 3 */
					+ "\\{" + fullGroupName0 + "\\}"

					/* form 1 - 3 end */
					+ ")|"

					/* form 4 */
					+ "\\Q(?P=\\E" + fullGroupName0 + "\\)|"

					/*
					 * matches an assert condition
					 * "(?(?=", "(?(?!", "(?(?<=), or "(?(?<!"
					 * 
					 * group: the assert part (inside the parentheses)
					 * (1 group)
					 */
					+ "\\Q(?(\\E(\\?<?[!=])|"

					/*
					 * matches a reference condition (by number) matches (conditional pattern)
					 * "(?(n)" or "(?(-n)"
					 * 
					 * group: the number
					 * (1 group)
					 */
					+ "\\Q(?(\\E([-+]?\\d++)\\)|"

					/*
					 * matches a reference condition (by name)
					 * "(?(<name>)" (form 0),
					 * "(?('name')" (form 1), or
					 * "(?(name)" (form 2)
					 * 
					 * group: everything after first symbol (excluding ")")
					 * group + 1: the name
					 * group + 2: the occurrence (if specified)
					 * (9 groups)
					 */

					/* form 0 - 2 start */
					+ "\\Q(?(\\E(?:"

					/* form 0 */
					+ "<(" + fullGroupName + ">?)|"

					/* form 1 */
					+ "'(" + fullGroupName + "'?)|"

					/* form 2 */
					+ "(" + fullGroupName + ")"

					/* form 0 - 2 end */
					+ ")\\)?|"

					/*
					 * matches comment group
					 * "(?#comment) - comment cannot contain ")"
					 * 
					 * group: everything
					 * (1 group)
					 */
					+ "(\\Q(?#\\E[^\\)]*+\\)?)|"

					/*
					 * matches a "branch reset" subpattern "(?|"
					 * 
					 * group: everything
					 * (1 group)
					 */
					+ "(\\Q(?|\\E)|"

					/*
					 * FAIL verb (from PCRE) - always fails
					 * (*FAIL) or (*F) - case sensitive
					 * 
					 * synonym for (?!)
					 */
					+ "\\Q(*\\E(F(?:AIL)?)\\)|"

					/* an open parenthesis - depth + 1 */
					+ "\\(|"

					/* a closed parenthesis - depth - 1 */
					+ "\\)|"

					/* a pike (|) - only react if inside condition */
					+ "\\||"

					/* an open square bracket - starts a character class */
					+ "\\[|"

					/* a closed square bracket - ends a character class */
					+ "\\]|"

					/* an open curly brace */
					+ "\\{|"

					/* a closed curly brace - tracks when needs to be escaped */
					// (escaping done since Android development doesn't allow '}'
					+ "\\}|"

					/* a "#" - starts comments when COMMENTS flag is enabled */
					+ "#|"

					/* matches a \Q..\E block */
					+ "\\\\Q(?s:.*?)(?:\\\\E|$)|"

					/* matches an escaped character */
					+ "\\\\[^Q]|"

					/* matches a line terminator */
					+ "[\n\r\u0085\u2028\u2029]"
			);

	/**
	 * Pattern to match parts of the inputted regular expression that need
	 * to be processed after refactoring.
	 */
	static final java.util.regex.Pattern afterRefactor = java.util.regex.Pattern
			.compile(

			/*
			 * matches:
			 * "\g{##group-mappingName}"
			 * "\g{##branchGroup-mappingName}"
			 * "\g{##test-mappingName}"
			 * "\g{##testF-mappingName}"
			 * 
			 * group: the number - mapped to the position to show the error
			 * group + 1: type ("group", "test", or "testF")
			 * group + 2: mappingName
			 * (3 groups)
			 */
			"\\Q\\g{\\E(\\d++)(group|branchGroup|testF?)-([^}]++)\\}|"

					/*
					 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:"
					 * (also matches a non-capture group - onFlags/offFlags are
					 * omitted)
					 * 
					 * group: onFlags (empty string if none)
					 * group + 1: offFlags (empty string if none; null, if omitted)
					 * (2 groups)
					 */
					+ "\\Q(?\\E(\\w*+)(?:-(\\w*+))?[:\\)]|"

					/*
					 * matches "\x{hhh..}" - a hex code
					 * 
					 * group: the number
					 * (1 group)
					 */
					+ "\\Q\\x{\\E([0-9a-fA-F]++)\\}|"

					/*
					 * matches "\xh" or "\xhh" - a hex code
					 * 
					 * group: the number
					 * (1 group)
					 */
					+ "\\\\x([0-9a-fA-F]{1,2})|"

					/*
					 * matches a unicode character
					 * 
					 * group: the number
					 * (1 group)
					 */
					+ "\\\\u([0-9a-fA-F]{1,4})|"

					/*
					 * matches a POSIX character class
					 * 
					 * group: "^" or "" (whether to negate or not)
					 * group + 1: the class name
					 * (2 group)
					 */
					+ "\\[:(\\^?)([A-Za-z]++):]|"

					/*
					 * matches a control character - \cA through \cZ
					 * 
					 * These are equivalent to \x01 through \x1A (26 decimal).
					 * 
					 * group: the control character's letter
					 * (either upper and lower case are allowed)
					 * (1 group)
					 */
					+ "\\\\c([A-Za-z])|"

					/*
					 * matches an unnamed capture group "(" - not followed by a "?" (or a '*', used by verbs)
					 * 
					 * group: everything (1 group)
					 */
					+ "(\\((?![?*]))|"

					/* an open parenthesis - depth + 1 */
					+ "\\(|"

					/* a closed parenthesis - depth - 1 */
					+ "\\)|"

					/* an open square bracket - starts a character class */
					+ "\\[|"

					/* a closed square bracket - ends a character class */
					+ "\\]|"

					/* a "#" - starts comments when COMMENTS flag is enabled */
					+ "#|"

					/* matches a \Q..\E block */
					+ "\\\\Q(?s:.*?)(?:\\\\E|$)|"

					/* matches an escaped character */
					+ "\\\\[^Q]|"

					/* matches a line terminator */
					+ "[\n\r\u0085\u2028\u2029]"
			);

	// /* matches an escaped character */
	// + "\\\\.");

	/**
	 * A cache of Patterns used during the refactoring.
	 * 
	 * <p>The <code>Integer</code> key is the number of digits in the number
	 * of capture groups for the inputted regular expression.</p>
	 */
	// private static final Map<Integer, java.util.regex.Pattern>
	// refactorPatterns = new HashMap<Integer, java.util.regex.Pattern>();

	/**
	 * Pattern used during refactoring.
	 */
	// private static final java.util.regex.Pattern refactorPattern = createRefactorPattern();
	static final java.util.regex.Pattern refactor = java.util.regex.Pattern
			.compile(

			/*
			 * matches an unnamed subroutine reference
			 * "(?[-+]n)" (form 0)
			 * 
			 * group: number (including [-+] to make it relative)
			 * (1 group)
			 * 
			 * TODO: add format checks
			 */
			"\\Q(?\\E([-+]?\\d++)\\)|"

					/*
					 * matches a named subroutine reference
					 * "(?&group)" (form 0)
					 * "(?P>group)" (form 1)
					 * 
					 * group: group name
					 * group + 1: occurrence
					 * (2 group)
					 * 
					 * TODO: add format checks
					 */
					+ "\\Q(?\\E(?:&|P>)" + fullGroupName + "\\)|"

					/*
					 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:"
					 * (also matches a non-capture group
					 * - onFlags/offFlags are omitted)
					 * 
					 * group: onFlags (empty string if none)
					 * group + 1: offFlags
					 * (empty string if none; null, if omitted)
					 * 
					 * (2 groups)
					 */
					+ "\\Q(?\\E(\\w*+)(?:-(\\w*+))?[:\\)]|"

					/*
					 * matches a named capture group
					 * "(?<name>" (form 0)
					 * "(?'name'" (form 1)
					 * "(?P<name>" (form 2)
					 * 
					 * group: the name (3 groups)
					 */

					/* form 0 - 2 start */
					+ "\\Q(?\\E(?:"

					/* form 0 */
					+ "<("
					+
					groupName
					+
					")>|"

					/* form 1 */
					+
					"'("
					+
					groupName
					+
					")'|"

					/* form 2 */
					+
					"P<("
					+
					groupName
					+
					")>"

					/* form 0 - 2 end */
					+
					")|"

					/*
					 * matches an unnamed capture group "(" - not
					 * followed by a "?" (or a '*', used by verbs)
					 * 
					 * group: everything (1 group)
					 */
					+
					"(\\((?![?*]))|"

					/*
					 * matches a back reference (by number)
					 * "\n" (form 0)
					 * "\gn" (form 1)
					 * "\g{n}" or "\g{-n}" (form 2)
					 * 
					 * group: the number
					 * last group: the next character (if a digit)
					 * (4 groups)
					 */

					/* form 0 - 2 start */
					+
					"(?:"

					/* form 0 */
					+
					"\\\\(\\d++)|"

					/* form 1 */
					+
					"\\\\g(-?\\d++)|"

					/* form 2 */
					+
					"\\Q\\g{\\E(-?\\d++)\\}"

					/* form 0 - 2 end */
					+
					")(\\d?)|"

					/*
					 * matches a back reference (by name)
					 * "\g{name}" (form 0),
					 * "\k<name>" (form 1),
					 * "\k'name'" (form 2),
					 * "\k{name}" (form 3),
					 * "(?P=name)" (form 4)
					 * 
					 * group: everything after the first symbol
					 * group + 1: the name
					 * group + 2: the occurrence (if specified)
					 * last group: the next character (if a digit)
					 * (16 groups)
					 */

					/* form 0 - 4 start */
					+
					"(?:"

					/* form 0 */
					+
					"\\Q\\g{\\E("
					+
					fullGroupName
					+
					"\\}?)|"

					/* form 1 - 3 start */
					+
					"\\\\k(?:"

					/* form 1 */
					+
					"<("
					+
					fullGroupName
					+
					">?)|"

					/* form 2 */
					+
					"'("
					+
					fullGroupName
					+
					"'?)|"

					/* form 3 */
					+
					"\\{("
					+
					fullGroupName
					+
					"\\}?)"

					/* form 1 - 3 end */
					+
					")|"

					/* form 4 */
					+
					"\\Q(?P=\\E("
					+
					fullGroupName
					+
					"\\)?)"

					/* form 0 - 4 end */
					+
					")(\\d?)|"

					/*
					 * matches an assert condition
					 * "(?(?=)", "(?(?!)", "(?(?<=)", or "(?(?<!)"
					 * 
					 * group: the assert part (inside the parentheses)
					 * (1 group)
					 */
					+
					"\\Q(?(\\E(\\?<?[!=])|"

					/*
					 * matches a conditional pattern (by number)
					 * "(?(n)" or "(?(-n)" (form 0)
					 * 
					 * group: the number
					 * (1 group)
					 */
					+
					"\\Q(?(\\E([-+]?\\d++)\\)|"

					/*
					 * matches a named reference condition
					 * "(?(<name>)" (form 0),
					 * "(?('name')" (form 1), or
					 * "(?(name)" (form 2)
					 * 
					 * group: the name
					 * group + 1: the occurrence (if specified)
					 * (6 groups)
					 */

					/* form 0 - 2 start */
					+
					"\\Q(?(\\E(?:"

					/* form 0 */
					+
					"<"
					+
					fullGroupName
					+
					">|"

					/* form 1 */
					+
					"'"
					+
					fullGroupName
					+
					"'|"

					/* form 2 */
					+
					fullGroupName

					/* form 0 - 2 end */
					+
					")\\)|"

					/*
					 * matches a "branch reset" subpattern "(?|"
					 * 
					 * group: everything (1 group)
					 */
					+
					"(\\Q(?|\\E)|"

					// TODO: combine syntax with range
					// allows options

					/*
					 * matches an unbounded numeric range
					 * such as "(?Z[<1.234])"
					 * 
					 * group: "Z" or "NZ"
					 * group + 1: comparison (such as "<")
					 * group + 2: value (such as "1.234")
					 * (3 groups)
					 * 
					 * TODO: add "?" (optional) tags
					 * (then, check of format - in refactor step)
					 */
					+ "\\Q(?\\E(N?Z[id]?(?:\\d++[LU]?)?)\\[([<>]=?)"
					+ "(-?(?:[0-9a-zA-Z]++(?:\\.[0-9a-zA-Z]*)?|\\.[0-9a-zA-Z]++))\\]\\)|"

					/*
					 * matches a numeric range
					 * "(?Z[start..end])" or "(?NZ[start..end])"
					 * 
					 * group: "Z" or "NZ" (optional base and L/U)
					 * //group + 1: "r" for raw mode, or <null>
					 * group + 1:'>' if exclusive start; <null> if inclusive
					 * group + 2: start
					 * group + 3:'<' if exclusive end; <null> if inclusive
					 * group + 4: end
					 * (5 groups)
					 */
					// "\\Q(?\\E(N?Z(r)?(?:\\d++[LU]?)?)\\[(?:(-?\\d++)\\.\\.(-?\\d++)\\])?\\)?|"
					+ "\\Q(?\\E(N?Z[id]?(?:\\d++[LU]?)?)\\[(?:([<>])?"
					// "(-?[0-9a-zA-Z]++)" +
					+ "(-?(?:[0-9a-zA-Z]++(?:\\.[0-9a-zA-Z]*)?|\\.[0-9a-zA-Z]++))"
					+ "\\.\\.([<>])?"
					// + "(-?[0-9a-zA-Z]++)"
					+ "(-?(?:[0-9a-zA-Z]++(?:\\.[0-9a-zA-Z]*)?|\\.[0-9a-zA-Z]++))"
					+ "\\]?)?\\)?|"

					/* an open parenthesis */
					+ "\\(|"

					/* a closed parenthesis */
					+ "\\)|"

					/* a pike (|) */
					+ "\\||"

					/* an open square bracket - starts a character class */
					+ "\\[|"

					/* a closed square bracket - ends a character class */
					+ "\\]|"

					/*
					 * a "#" - starts comments when COMMENTS flag is
					 * enabled
					 */
					+ "#|"

					/* matches a \Q..\E block */
					+ "\\\\Q(?s:.*?)(?:\\\\E|$)|"

					/* matches an escaped character */
					+ "\\\\[^Q]|"

					/* matches a line terminator */
					+ "[\n\r\u0085\u2028\u2029]"
			);

	// /* matches an escaped character */
	// + "\\\\.");

	/**
	 * A cache of Patterns used to match a number with a given digit count.
	 * 
	 * <p>The <code>Integer</code> key is the number of digits, and the
	 * <code>Pattern</code> value is the pattern to match a number with that
	 * many digits. The digits are in the first group, and any trailing
	 * digits are in the second.</p>
	 */
	private static final Map<Integer, java.util.regex.Pattern> digitCountPatterns = new HashMap<Integer, java.util.regex.Pattern>(
			2);

	static final java.util.regex.Pattern perl_octal = java.util.regex.Pattern
			.compile("^([0-3]?[0-7]{1,2})(\\d*+)$");

	/**
	 * Returns the pattern to use when refactoring an inputted regular
	 * expression.
	 * 
	 * @param groupCount
	 *            number of capture groups in the inputted regular
	 *            expression
	 * @return the pattern to use when refactoring
	 */
	// static java.util.regex.Pattern getRefactorPattern(int groupCount)
	// {
	// // number of digits in the largest group index
	// // (e.g. 999 would be 3, and 1000 would be 4)
	// Integer digitCount = digitCount(groupCount);
	//
	// if (!refactorPatterns.containsKey(digitCount)) {
	// java.util.regex.Pattern fixup = createRefactorPattern();
	//
	// refactorPatterns.put(digitCount, fixup);
	// return fixup;
	// }
	//
	// return refactorPatterns.get(digitCount);
	// }

	/**
	 * Returns the pattern to use when refactoring an inputted regular
	 * expression.
	 * 
	 * @return the pattern to use when refactoring
	 */
	// static java.util.regex.Pattern getRefactorPattern()
	// {
	// return refactorPattern;
	// }

	/**
	 * Returns a pattern that matches <code>digitCount</code> digits
	 * followed by zero or more digits. The pattern has two capture groups.
	 * The required digits are in the first group, and any trailing digits
	 * are in the second.
	 * 
	 * @param digitCount
	 *            the number of digits the pattern should match
	 * @return a pattern that matches <code>digitCount</code> digits
	 *         followed by zero or more digits
	 */
	static java.util.regex.Pattern getDigitCountPattern(int digitCount)
	{
		Integer digitCountI = digitCount;

		if (digitCountPatterns.containsKey(digitCountI))
			return digitCountPatterns.get(digitCountI);
		else {
			java.util.regex.Pattern pattern = java.util.regex.Pattern
					.compile("(\\d{1," + digitCount + "})(\\d*+)");

			digitCountPatterns.put(digitCountI, pattern);
			return pattern;
		}
	}

	/**
	 * Returns the number of digits in the given number
	 * 
	 * @param number
	 *            the number whose digit count is returned
	 * @return the number of digits in the given number
	 */
	static int digitCount(int number)
	{
		return String.valueOf(number).length();
	}

	/**
	 * <p>
	 * Returns whether the specified mapping name is an "any group".
	 * </p>
	 * 
	 * <p>
	 * <b>Note</b>: these are groups with an occurrence of 0, for example,
	 * "groupName[0]". However "[0]" is not an "any group", but instead
	 * refers to the match itself.
	 * </p>
	 * 
	 * <p>
	 * Any groups allow referring to the first matched group in the case of
	 * multiple groups with the same name. If there is only one group, the
	 * "any group" is the first group.
	 * </p>
	 * 
	 * @param mappingName
	 *            the mapping name for the group
	 * @return <code>true</code> if the given group is an "any group"
	 */
	static boolean isAnyGroup(String mappingName)
	{
		// greater than, because "[0]" is not an "any group"
		// (requires a group name)
		return mappingName.length() > "[0]".length() &&
				mappingName.endsWith("[0]");
	}

	/**
	 * Removes the trailing "[0]" for the inputted "any group".
	 * 
	 * @param mappingName
	 *            must be an "any group"
	 * @return the group name for this "any group"
	 */
	static String anyGroupName(String mappingName)
	{
		return mappingName.substring(0, mappingName.length() -
				"[0]".length());
	}

	/**
	 * Surrounds the passed string in a non-capture group.
	 * 
	 * <p><b>Note</b>: If the pattern is compiled with the {@link Pattern#ONLY_CAPTURING_GROUPS} flag, then the
	 * group will be a
	 * capture group, and the {@link #totalGroups} will increase by one to
	 * reflect this.</p>
	 * 
	 * @param str
	 *            the string to surround
	 * @return the RegEx for the given string surrounded by a non-capture
	 *         group.
	 */
	static String nonCaptureGroup(String str)
	{
		return startNonCaptureGroup() + str + ")";
	}

	/**
	 * Returns the string that represents the start of a non-capture group,
	 * "(?:".
	 * 
	 * @return the string that represents the start of a non-capture group
	 */
	static String startNonCaptureGroup()
	{
		// if (!supportedSyntax(NONCAPTURE_GROUPS)) {
		// totalGroups++;
		// return "(";
		// } else
		return "(?:";
	}

	/**
	 * <p>
	 * Returns a regular expression that matches the given target group.
	 * </p>
	 */
	static String acceptTestingGroup(int targetGroup)
	{
		return "(?=\\" + targetGroup + ")";
	}

	/**
	 * <p>
	 * Returns a regular expression which fails if the specified target
	 * group matches.
	 * </p>
	 */
	static String failTestingGroup(int targetGroup)
	{
		return "(?!\\" + targetGroup + ")";
	}

	/**
	 * Returns a regular expression that always fails.
	 * 
	 * @return a regular expression that always fails
	 */
	static String fail()
	{
		// Use basic syntax to allow the internal pattern to be used in languages which don't support assertions
		// return "\\b\\B";

		// Fails fast (and is self documenting).
		// If single-line (no "m" flag), max 4 probes (if text starts with "f", "fa", "fai" or "fail")
		// if multi-line, N probes
		// return "^fail^";

		// Uses assertion, because it's possible java optimizes for it
		// Fails fast - 0 probes
		return "(?!)";
	}

	/**
	 * @throws IllegalArgumentException
	 *             if the string does not contain a parsable integer.
	 * @see Integer#parseInt(String)
	 */
	static int parseInt(String string)
	{
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Cannot parse integer: " +
					string);
		}
	}

	/**
	 * Returns a String that is never used as a mapping name
	 * 
	 * @return
	 */
	static String neverUsedMappingName()
	{
		return "$neverUsed";
	}

	/**
	 * Indicates whether the specified group name is an unnamed group
	 * 
	 * @param groupName
	 *            the group name
	 * @return <code>true</code> if, and only if, <code>groupName</code> is
	 *         the name of an unnamed group (e.g. [1])
	 */
	static boolean isUnnamedGroup(String groupName)
	{
		return groupName.charAt(0) == '[';
	}

	/**
	 * Returns a regular expression that matches the given digits literally.
	 * 
	 * @param trailingDigits
	 *            the literal digits that follow a back reference
	 */
	static String fixTrailing(String trailingDigits)
	{
		if (trailingDigits.length() == 0)
			return "";

		return "[" + trailingDigits.charAt(0) + "]" +
				trailingDigits.substring(1);
	}
}