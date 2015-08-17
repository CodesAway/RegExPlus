package info.codesaway.util.regex;

import static info.codesaway.util.regex.Comparison.GREATER_THAN;
import static info.codesaway.util.regex.Comparison.GREATER_THAN_EQUAL;
import static info.codesaway.util.regex.Comparison.LESS_THAN;
import static info.codesaway.util.regex.Comparison.LESS_THAN_EQUAL;
import static info.codesaway.util.regex.Comparison.MATCHES_ABOVE;
import static info.codesaway.util.regex.Comparison.MATCHES_BELOW;
import static java.lang.Character.MAX_RADIX;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 
 * Functions used to match a range of values e.g. 1 or 001..999; 001..999;
 * 1..999
 */
public class PatternRange
{
	/**
	 * Pattern used to match a range mode.
	 * 
	 * <p><b>Format</b>: Mode[Base[BaseMode]]</p>
	 * 
	 * <p>Descriptions and valid values:</p>
	 * <ul>
	 * <li><b>Mode</b>: either "Z" (allows leading zeros) or "NZ" (no
	 * leading
	 * zeros)</li>
	 * 
	 * <li><b>Base</b>: the numeric base for start and end (valid bases, 2 -
	 * 36)</li>
	 * 
	 * <li><b>BaseMode</b>: whether to use lower ("L"), upper ("U"), or both
	 * upper and lower (omitted) case digits when specifying digits A-Z for
	 * digit values 10-36.
	 * 
	 * <p>If the result doesn't include "letter digits" or if the base is
	 * ten or less,
	 * <i>BaseMode</i> has no effect, but can be specified (for
	 * consistency).</p></li>
	 * </ul>
	 */
	// private static final java.util.regex.Pattern rangeModeRegEx =
	// java.util.regex.Pattern
	// .compile("^(N?Z)r?(?:(\\d++)([LU])?)?$");
	// static final java.util.regex.Pattern rangeModeRegEx = java.util.regex.Pattern
	// .compile("^(N?Z)(?:(\\d++)([LU])?)?$");
	// static final java.util.regex.Pattern rangeModeRegEx = java.util.regex.Pattern
	// .compile("^(N?Z)(?:(\\d++)([LU])?)?$");

	/**
	 * 
	 * @param start
	 * @param end
	 * @param mode
	 * @return
	 * 
	 * @throws IllegalArgumentException
	 *             If the mode is invalid
	 */
	// static String range(String start, String end, String mode)
	static String range(String start, String end, RangeMode rangeMode)
	{
		int base = rangeMode.base();
		// java.util.regex.Matcher rangeMode = rangeModeRegEx.matcher(mode);

		// if (!rangeMode.matches())
		// throw new IllegalArgumentException("Illegal range mode");

		// boolean allowLeadingZeros = rangeMode.group(1).equals("Z");
		// int base = rangeMode.group(2) == null ? 10 : Integer
		// .parseInt(rangeMode.group(2));
		//
		// if (base < MIN_RADIX || base > MAX_RADIX)
		// throw new IllegalArgumentException(INVALID_BASE);

		start = start.toLowerCase(Locale.ENGLISH);
		end = end.toLowerCase(Locale.ENGLISH);

		// java.util.regex.Pattern validDigits = java.util.regex.Pattern
		// .compile("^-?" + digitRange(0, base - 1, "") + "*+");

		// / TODO: does this allow '-' or the empty string as a valid start/end?
		java.util.regex.Pattern validDigits = java.util.regex.Pattern
				.compile("^-?" + digitRange(0, base - 1, "") + "*+");

		// TODO: don't use matcher - performance hit
		java.util.regex.Matcher validNumber = validDigits.matcher(start);
		validNumber.find();

		if (!validNumber.hitEnd()) {
			throw new PatternSyntaxException(Refactor.INVALID_DIGIT_START, start,
					validNumber.end());
		}

		validNumber.reset(end);
		validNumber.find();

		if (!validNumber.hitEnd()) {
			throw new PatternSyntaxException(Refactor.INVALID_DIGIT_END, end,
					validNumber.end());
		}

		// String baseMode = rangeMode.group(3) == null ? "LU" : rangeMode
		// .group(3).toUpperCase(Locale.ENGLISH);
		// String baseMode = rangeMode.group(3) == null ? "LU" : rangeMode.group(3);

		if (compare(start, end) > 0) {
			// swap values

			String tmpStr = start;
			start = end;
			end = tmpStr;
		}

		boolean negStart = start.charAt(0) == '-';
		boolean negEnd = end.charAt(0) == '-';

		String negS = "", negE = "", posS = "", posE = "";

		if (negStart) {
			if (negEnd) {
				// both start and end are negative
				// e.g. -170 to -16 -> 16 to 170 (with a leading negative
				// sign)
				negS = end.substring(1);
				negE = start.substring(1);
			} else {
				// start is negative and end is non-negative
				negS = compare(start, "0") == 0 ? "0" : "1";
				negE = start.substring(1);

				posS = "0";
				posE = end;
			}
		} else {
			// both are non-negative

			if (negEnd) {
				// special case
				// start = 0 and end = -0
				negS = "0";
				negE = "0";

				posS = "0";
				posE = "0";
			} else {
				posS = start;
				posE = end;
			}
		}

		StringBuilder result = new StringBuilder();

		if (negS.length() != 0) {
			// result.append(range_private("-", negS, negE, allowLeadingZeros,
			// base, baseMode));
			result.append(range_private("-", negS, negE, rangeMode));
		}

		if (posS.length() != 0) {
			if (result.length() != 0)
				result.append('|');

			// result.append(range_private("", posS, posE, allowLeadingZeros,
			// base, baseMode));
			result.append(range_private("", posS, posE, rangeMode));
		}

		return result.toString();
	}

	/*
	 * TODO modify javadoc comments
	 */

	/**
	 * <p>
	 * Returns a regular expression that will match a number in the
	 * specified range.
	 * </p>
	 * 
	 * <p>
	 * If <code>allowLeadingZeros</code> is <code>true</code>, the number of
	 * digits in a accepted match is between the number of digits in
	 * <code>startStr</code> and <code>endStr</code> and leading zeros are
	 * allowed.
	 * </p>
	 * 
	 * <p>
	 * If <code>allowLeadingZeros</code> is <code>false</code>, the returned
	 * regular expression will not allow leading zeros in an accepted match.
	 * </p>
	 * 
	 * <p>
	 * This internal method has the following constraints:
	 * </p>
	 * <ol>
	 * <li>startStr and endStr must contain a positive number</li>
	 * <li>startStr &lt;= endStr</li>
	 * </ol>
	 * 
	 * <b>Note</b>: there is no constraint on how large
	 * <code>startStr</code> and <code>endStr</code> may be -
	 * they can be any positive integer.
	 */
	// private static String range_private(String lead, String startStr,
	// String endStr, boolean allowLeadingZeros, int base,
	// String baseMode)
	private static String range_private(String lead, String startStr,
			String endStr, RangeMode rangeMode)
	{
		assert !startStr.startsWith("-") : startStr;
		assert !endStr.startsWith("-") : endStr;
		assert compare(startStr, endStr) <= 0;

		if (rangeMode.allowsLeadingZeros())
			// return rangeZ_private(lead, startStr, endStr, base, baseMode);
			return rangeZ_private(lead, startStr, endStr, rangeMode);

		StringBuilder result = new StringBuilder();

		// remove leading zeros
		String start = removeLeadingZeros(startStr);
		String end = removeLeadingZeros(endStr);

		if (start.length() == end.length())
			// return rangeZ_private(lead, start, end, base, baseMode);
			return rangeZ_private(lead, start, end, rangeMode);

		// int base = rangeMode.base();

		for (int i = start.length(); i <= end.length(); i++) {
			String tmpStart, tmpEnd;

			if (i == start.length()) {
				tmpStart = start;
				// tmpEnd = repeat(Character.forDigit(base - 1, base), i);
				tmpEnd = repeat(rangeMode.lastCharacterDigit(), i);
			} else if (i == end.length()) {
				tmpStart = "1" + repeat('0', i - 1);
				tmpEnd = end;
			} else {
				tmpStart = "1" + repeat('0', i - 1);
				// tmpEnd = repeat(Character.forDigit(base - 1, base), i);
				tmpEnd = repeat(rangeMode.lastCharacterDigit(), i);
			}

			if (result.length() != 0)
				result.insert(0, '|');

			// result.insert(0, rangeZ_private(lead, tmpStart, tmpEnd, base,
			// baseMode));
			result.insert(0, rangeZ_private(lead, tmpStart, tmpEnd, rangeMode));
		}

		return result.toString();
	}

	// public static String optionalTrailingZeros(String rangeRegEx)
	// {
	// return optionalTrailingZeros(rangeRegEx, "");
	// }

	/**
	 * Makes trailing zeros optional.
	 * 
	 * <p><code>rangeRegEx</code> uses the following grammar:</p>
	 * 
	 * <p>branch = parts ('|' parts)*</p>
	 * <p>parts = part+</p>
	 * <p>part = number | '[' (range | number)+ ']'</p>
	 * <p>range = number '-' number</p>
	 * <p>number = [0-9a-zA-Z]</p>
	 * 
	 * @param rangeRegEx
	 *            the range regular expression
	 * @return the given range regular expression, with trailing zeros optional
	 */
	private static String optionalTrailingZeros(String rangeRegEx, String appendToBranchEnd)
	{
		// TODO: optimize created Regex
		// add code to refactor a little and clean up ??
		boolean isInNonCaptureGroup = rangeRegEx.startsWith("(?:") && rangeRegEx.endsWith(")");

		if (isInNonCaptureGroup) {
			rangeRegEx = rangeRegEx.substring("(?:".length(), rangeRegEx.length() - ")".length());
		}

		StringBuilder result = new StringBuilder();
		List<String> parts = new ArrayList<String>();
		List<String> trailingZeros = new ArrayList<String>();
		boolean onTrailingZeros = true;
		// boolean hasLastRepetition = false;

		// for (int i = 0; i < rangeRegEx.length();) {
		for (int i = rangeRegEx.length() - 1; i >= 0;) {
			char c = rangeRegEx.charAt(i);
			String currentPart = null;
			String repetition;

			// Repetition (if any)
			if (c == '}') {
				int startRepetition = rangeRegEx.lastIndexOf('{', i - 1);
				repetition = rangeRegEx.substring(startRepetition, i + 1);
				i = startRepetition - 1;
				c = rangeRegEx.charAt(i);
			} else if (c == '?') {
				repetition = String.valueOf(c);
				i--;
				c = rangeRegEx.charAt(i);
			} else
				repetition = "";

			// Digits
			// if (c == '[') {
			if (c == ']') {
				// include '[' and ']' in part
				// int endCharacterClass = rangeRegEx.indexOf(']', i) + 1;
				int startCharacterClass = rangeRegEx.lastIndexOf('[', i - 1);

				// currentPart = rangeRegEx.substring(i, endCharacterClass);
				currentPart = rangeRegEx.substring(startCharacterClass, i + 1);
				// i = endCharacterClass;
				i = startCharacterClass - 1;
			} else if (Character.digit(c, MAX_RADIX) != -1 || c == '-') {
				currentPart = String.valueOf(c);
				// i++;
				i--;
			}

			if (currentPart != null) {
				// if (currentPart.contains("0")) {

				if (currentPart.contains("0") && onTrailingZeros) {
					// System.out.println(trailingZeros + "\t" + currentPart + "\t" + appendToBranchEnd);
					if (!trailingZeros.isEmpty() || !appendToBranchEnd.equals(currentPart + "*")) {
						// Skip cases like [0-9][0-9]* - should just be [0-9]* instead
						// (Since trailing zeros are optional)

						String wholePart;

						if (repetition.startsWith("{")) {
							// if (trailingZeros.size() == 0)
							// hasLastRepetition = true;

							// Max value, including closing brace ('}')
							String maxValue;
							int commaIndex = repetition.indexOf(',');
							int maxValueIndex = commaIndex != -1 ? commaIndex : 1;

							maxValue = repetition.substring(maxValueIndex);

							wholePart = currentPart + "{0," + maxValue;
						} else {
							wholePart = currentPart + repetition;
						}

						// trailingZeros.add(currentPart);
						trailingZeros.add(0, wholePart);
					}
				} else {
					// parts.addAll(trailingZeros);
					// trailingZeros.clear();
					// parts.add(currentPart);
					onTrailingZeros = false;
					String wholePart = currentPart + repetition;
					parts.add(0, wholePart);
				}
			}

			// if (i == rangeRegEx.length() || rangeRegEx.charAt(i) == '|') {
			if (i == -1 || rangeRegEx.charAt(i) == '|') {
				StringBuilder resultPart = new StringBuilder();

				for (String part : parts) {
					resultPart.append(part);
				}

				// int maxIndex = trailingZeros.size() - (hasLastRepetition ? 1 : 0);

				// for (int j = 0; j < maxIndex; j++) {
				for (String part : trailingZeros) {
					// String part = trailingZeros.get(j);

					resultPart.append("(?:").append(part);
				}

				if (appendToBranchEnd.length() != 0)
					resultPart.append(appendToBranchEnd);

				// if (maxIndex > 0)
				// resultPart.append(repeat(")?", maxIndex));
				resultPart.append(repeat(")?", trailingZeros.size()));

				// if (hasLastRepetition && !trailingZeros.isEmpty())
				// resultPart.append(trailingZeros.get(trailingZeros.size() - 1));

				result.insert(0, resultPart);

				// if (i != rangeRegEx.length()) {
				if (i != -1) {
					parts.clear();
					trailingZeros.clear();
					// result.append('|');
					result.insert(0, '|');
					// i++;
					i--;
					onTrailingZeros = true;
					// hasLastRepetition = false;
				}

				// result.append(resultPart);
			}
		}

		// return rangeRegEx;

		if (isInNonCaptureGroup)
			return noncapture(result).toString();
		else
			return result.toString();
	}

	// private static String rangeZ_private(String lead, String start,
	// String end, int base, String baseMode)
	private static String rangeZ_private(String lead, String start,
			String end, RangeMode rangeMode)
	{
		// TODO: optimize branches - expanding them isn't necessary in Java
		// TODO: dive into test cases for each branch
		String baseMode = rangeMode.baseMode();

		if (start.length() == 1 && end.length() == 1) {
			int digit1 = digit(start.charAt(0));
			int digit2 = digit(end.charAt(0));

			if (digit1 == digit2)
				return lead + start;
			else
				return lead + digitRange(digit1, digit2, baseMode);
		} else {
			boolean optZero;
			int digit1, digit2;
			String newStart, newEnd;

			if (start.length() < end.length()) {
				// optional zero (e.g. 1-17)

				digit1 = 0;
				digit2 = digit(end.charAt(0));

				newStart = start;
				newEnd = end.substring(1);

				optZero = true;
			} else if (end.length() < start.length()) {
				// optional zero (e.g. 01-7)

				digit1 = digit(start.charAt(0));
				digit2 = 0;

				newStart = start.substring(1);
				newEnd = end;

				optZero = true;
			} else {
				digit1 = digit(start.charAt(0));
				digit2 = digit(end.charAt(0));

				newStart = start.substring(1);
				newEnd = end.substring(1);

				optZero = false;
			}

			if (digit1 == digit2) {
				String newLead = lead + forDigit(digit1, baseMode) +
						(optZero ? "?" : "");

				// return rangeZ_private(newLead, newStart, newEnd, base,
				// baseMode);
				return rangeZ_private(newLead, newStart, newEnd, rangeMode);
			}

			StringBuilder result = new StringBuilder();

			if (!newStart.equals(repeat('0', newStart.length()))) {
				String newLead = lead + forDigit(digit1, baseMode);

				if (optZero && digit1 == 0)
					newLead += "?";

				// result.insert(0, rangeZ_private(newLead, newStart,
				// repeat(Character.forDigit(base - 1, base), newEnd
				// .length()), base, baseMode));

				// result.insert(0, rangeZ_private(newLead, newStart,
				// repeat(Character.forDigit(base - 1, base), newEnd
				// .length()), rangeMode));

				result.insert(0, rangeZ_private(newLead, newStart,
						repeat(rangeMode.lastCharacterDigit(), newEnd
								.length()), rangeMode));
				digit1++;
			}

			boolean needSecondGroup;

			// TODO: verify case doesn't matter
			// if (!newEnd.equals(repeat(Character.forDigit(base - 1, base),
			// newEnd.length()))) {
			if (!newEnd.equals(repeat(rangeMode.lastCharacterDigit(), newEnd.length()))) {
				needSecondGroup = true;
				digit2--;
			} else
				needSecondGroup = false;

			if (digit1 <= digit2) {
				if (result.length() != 0) {
					result.insert(0, '|');
				}

				StringBuilder newLead = new StringBuilder();

				newLead.append(lead).append(
						digitRange(digit1, digit2, baseMode));

				int useLength;

				if (optZero && digit1 == 0) {
					newLead.append('?');
					useLength = newStart.length();
				} else
					useLength = newEnd.length();

				// Previously commented out
				// result.insert(0, rangeZ_private(newLead, repeat("0",
				// useLength), repeat(Character.digit(base-1, base),
				// newEnd.length())));

				/*
				 * refactored to decrease pattern length (uses "{M, N}"
				 * patternSyntax)
				 * 
				 * result.insert(0, rangeZ_private(newLead, repeat("0",
				 * useLength), repeat(Character.digit(base-1, base),
				 * newEnd.length())));
				 */

				int minNum = Math.min(useLength, newEnd.length());
				int maxNum = Math.max(useLength, newEnd.length());

				// if (digit1 == 0 && digit2 == base - 1) {
				if (digit1 == 0 && digit2 == rangeMode.lastDigit()) {
					// Use lead, not newLead, since newLead includes 0..base-1 range
					newLead.setLength(0);
					newLead.append(lead);
					maxNum++;

					if (!optZero)
						minNum++;
				}

				if (minNum == maxNum) {
					if (minNum == 1) {
						// range(newLead, "0", "9") -> newLead + "[0-9]"
						// result.insert(0, newLead +
						// digitRange(0, base - 1, baseMode));
						result.insert(0, newLead + rangeMode.digitRange0());
					} else {
						// e.g. useLength == 2
						// range(newLead, "00", "99") -> newLead +
						// "[0-9]{2}"
						// result.insert(0, newLead +
						// digitRange(0, base - 1, baseMode) + "{" +
						// minNum + "}");
						result.insert(0, newLead +
								rangeMode.digitRange0() + "{" +
								minNum + "}");
					}
				} else {
					// result.insert(0, newLead +
					// digitRange(0, base - 1, baseMode) + "{" +
					// minNum + "," + maxNum + "}");
					result.insert(0, newLead +
							rangeMode.digitRange0() + "{" +
							minNum + "," + maxNum + "}");
				}
			}

			if (needSecondGroup) {
				if (result.length() != 0) {
					result.insert(0, '|');
				}

				String newLead = lead + forDigit(digit2 + 1, baseMode);

				if (optZero && digit2 + 1 == 0)
					newLead += "?";

				// result.insert(0, rangeZ_private(newLead, repeat('0', newEnd
				// .length()), newEnd, base, baseMode));
				result.insert(0, rangeZ_private(newLead, repeat('0', newEnd
						.length()), newEnd, rangeMode));
			}

			return result.toString();
		}
	}

	/** Regular expression to match a floating point value. */
	public static final Pattern floatValue = Pattern.lazyCompile("(?<iPart>(?<isNegative>-)?[0-9a-zA-Z]++)?"
			+ "(?>(?J)"
			// e.g. .123
			+ "\\.(?<fPart>[0-9a-zA-Z]++)|"
			// checks whether float or integer
			+ "(?('iPart')" + "\\.?+(?<fPart>(?<=\\.))?+|"
			// No iPart nor an fPart - not a valid number, fail
			+ "(*F)))");

	/**
	 * Extracts the integer and fractional part from a number.
	 * 
	 * @param number
	 *            the number
	 * @return a String[] containing two elements, the integer and
	 *         fractional part
	 * @throws IllegalArgumentException
	 *             If <code>number</code> is not a valid number.
	 */
	private static String[] extractParts(String number)
	{
		// number = simplify(number);

		// System.out.println(number);
		Matcher matcher = floatValue.matcher(number);

		if (!matcher.matches())
			throw new IllegalArgumentException("Invalid number: " + number);

		// boolean isPositive = !matcher.matched("isNegative");
		String iPart = matcher.group("iPart");
		String fPart = matcher.group("fPart");

		return new String[] { iPart, fPart };
	}

	static String boundedRange(String start, boolean inclusiveStart, String end, boolean inclusiveEnd,
			RangeMode rangeMode)
	{
		// TODO: add support for int values (e.g. 1.0..10.0)

		// System.out.println(start + "\t" + end);

		// int base = 10;
		// String baseMode = "";

		String[] startParts = extractParts(start);
		String iPartStart = startParts[0] == null ? "0" : startParts[0];
		String fPartStart = startParts[1] == null ? "0" : startParts[1];

		String[] endParts = extractParts(end);
		String iPartEnd = endParts[0] == null ? "0" : endParts[0];
		String fPartEnd = endParts[1] == null ? "0" : endParts[1];

		if (startParts[1] == null && endParts[1] == null && !rangeMode.forcesDecimalMode()) {
			// Both integers
			// e.g. 1..5

			// TODO: add mode attribute "d" - decimal mode

			return iPartRange(iPartStart, inclusiveStart, iPartEnd, inclusiveEnd, rangeMode);
		}

		// System.out.println(iPartStart + "\t" + fPartStart + "\t\t" + iPartEnd + "\t" + fPartEnd);
		int compare = compare(iPartStart, iPartEnd, false);

		// System.out.println(compare);
		// if (compare(iPartStart, iPartEnd) == 0) {
		if (compare == 0) {
			return fPartRange(iPartStart, fPartStart, inclusiveStart, fPartEnd, inclusiveEnd, rangeMode);
		}

		// if (compare(iPartStart, iPartEnd) != 0) {
		if (compare > 0) {
			// switch start and end values
			// String[] parts = startParts;
			// startParts = endParts;
			// endParts = parts;

			boolean inclusiveTemp = inclusiveStart;
			inclusiveStart = inclusiveEnd;
			inclusiveEnd = inclusiveTemp;

			String iPartTemp = iPartStart;
			iPartStart = iPartEnd;
			iPartEnd = iPartTemp;

			String fPartTemp = fPartStart;
			fPartStart = fPartEnd;
			fPartEnd = fPartTemp;
		}

		// e.g. 1.2..12.5
		StringBuilder result = new StringBuilder();

		// put large value first - ensures longest match
		result.append(fPartRange(Comparison.valueOf(MATCHES_BELOW, inclusiveEnd), iPartEnd, fPartEnd, rangeMode));

		// System.out.println("Result check 1: " + result);
		boolean isNegativeZeroStart = compare(iPartStart, "-0", false) == 0;
		// boolean isNegativeStart = iPartStart.startsWith("-");
		boolean isNegativeStart = compare(iPartStart, "0") < 0;
		boolean isNegativeEnd = iPartEnd.startsWith("-");
		// boolean isNegativeEnd = compare(iPartEnd, "0") < 0;

		String ceilStart;
		boolean startWithNextInt = isNegativeStart || !inclusiveStart || compare(fPartStart, "0") != 0;

		// TODO: test corner cases near 0, including case of -0
		if (startWithNextInt)
			ceilStart = isNegativeZeroStart ? "0" : addOne(iPartStart);
		else
			ceilStart = iPartStart;

		// if (compare(ceilStart, iPartEnd) != 0) {
		if (compare(ceilStart, iPartEnd) < 0) {
			// TODO: test corner cases near 0
			String iPartEndMinusOne = subtractOne(iPartEnd);

			if (result.length() != 0)
				result.append('|');

			// Surround in non-capture group
			result.append("(?:");
			// System.out.println(ceilStart + " " + iPartEndMinusOne);

			// TODO: test support for crossing zero

			// result.append(range_private("", ceilStart, iPartEndMinusOne, false, base, baseMode));
			result.append(range(ceilStart, iPartEndMinusOne, rangeMode.allowsLeadingZeros(false)));
			result.append(')');

			// Match any decimal (decimal point is optional)
			// result.append("(?:\\.").append(digitRange(0, base - 1, baseMode)).append("*)?");
			result.append("(?:\\.").append(rangeMode.digitRange0()).append("*)?");
		}

		if (isNegativeStart ^ isNegativeEnd) {
			// Crosses zero
			// Need to match -1 < x < 0

			if (result.length() != 0)
				result.append('|');

			// result.append(matchesNegativeZeroFloats(true, base, baseMode));
			result.append(matchesNegativeZeroFloats(rangeMode));
		}

		// TODO: ensure largest value is first
		if (startWithNextInt) {
			String range = fPartRange(Comparison.valueOf(MATCHES_ABOVE, inclusiveStart), iPartStart,
					fPartStart, rangeMode);

			// System.out.println("upper bound: " + iPartStart);

			if (range.length() != 0) {
				if (result.length() != 0)
					result.append('|');

				result.append(range);
			}
		}

		return result.toString();
		// }

		// return fPartRange(Comparison.valueOf(MATCHES_ABOVE, inclusiveStart), iPartStart, fPartStart);
	}

	static String unboundedRange(Comparison comparison, String value, RangeMode rangeMode)
	{
		// TODO: handle cases near 0 (e.g. >=-0.5)

		// TODO: optimize use of regexes
		// TODO: add case if value is integer (i.e. fPart is null)

		// TODO: handle case of -0

		// TODO: (option) allow cases like .1 and -.1 (implicit zero)
		// TODO: (option) allow leading zeros - ex. -05 ??
		// TODO: (option) don't allow trailing zeros - ex. 5.50 ??

		// int base = 10;
		// String baseMode = "";

		StringBuilder result = new StringBuilder();

		String[] parts = extractParts(value);
		String iPart = parts[0] == null ? "0" : parts[0];
		String fPart = parts[1] == null ? "0" : parts[1];

		if (parts[1] == null && !rangeMode.forcesDecimalMode()) {
			// Integer
			// e.g. >1

			return iPartRange(comparison, iPart, rangeMode);
		} else if (rangeMode.forcesIntegerMode()) {
			// Forced to integer
			// e.g. >1.5 === >= 2
			boolean matchesBelow = comparison.matchesBelow();
			String bound = matchesBelow ? floor(iPart, fPart) : ceil(iPart, fPart);

			return iPartRange(Comparison.valueOf(matchesBelow, true), bound, rangeMode);
		}

		boolean isNegative = iPart.startsWith("-");
		boolean matchingNegativeZero = isNegative && compare(iPart, "0") == 0;

		// if (result.length() != 0)
		// result.append('|');

		String fPartRange;
		boolean skipDecimal = false;

		if (!isNegative && comparison == GREATER_THAN_EQUAL && compare(fPart, "0") == 0 && isPowerOfTen(iPart)) {
			skipDecimal = true;
			fPartRange = "";
		} else
			fPartRange = fPartRange(comparison, iPart, fPart, rangeMode);

		// Done first, since alternations should be longest number first
		// (ensures largest match - e.g. 181 instead of 18; -181 instead of -18)
		if (!isNegative && comparison.matchesBelow() || isNegative && comparison.matchesAbove())
			result.append(fPartRange);

		// TODO: add optimization in case iPart is power of 10 and fPart is
		// 0

		/* Integer values, optional decimal */

		Comparison compareAllowingEqual = Comparison.valueOf(comparison
				.matchesBelow(), true);

		/* matchesBelow / matchesAbove - documented */

		String start, end;

		if (comparison.matchesBelow()) {
			start = "0";
			end = skipDecimal ? iPart : subtractOne(iPart);
		} else {
			start = matchingNegativeZero ? "-0" : "-1";
			end = skipDecimal ? iPart : addOne(iPart);
		}

		// greater than (or equal) to -x / less than (or equal) to x
		if (isNegative == comparison.matchesAbove()) {
			// start <= end / start >= end
			// if (compareAllowingEqual.compareTo(compare(start, end))) {
			if (compare(start, iPart) != 0) {
				// Range [0..iPart) / Range (iPart..-1]
				// (allowing for optional decimal)
				if (result.length() != 0)
					result.append('|');

				// result.append(Pattern.range(start, end, "NZ")).append(
				// "(?:\\.").append(digitRange(0, base - 1, baseMode))
				// .append("*)?");
				result.append(Pattern.range(start, end, rangeMode.allowsLeadingZeros(false).mode()))
						.append("(?:\\.").append(rangeMode.digitRange0())
						.append("*)?");
			}

			if (!matchingNegativeZero) {
				if (result.length() != 0)
					result.append('|');

				// Matches -1 < x < 0
				// result.append(matchesNegativeZeroFloats(true, base, baseMode));
				result.append(matchesNegativeZeroFloats(rangeMode));
			}

			if (comparison.matchesBelow())
				end = "-1";
			else
				end = "0";
		}

		if (result.length() != 0)
			result.append('|');

		// Matches x <= end / x >= end
		// result.append(iPartRange(compareAllowingEqual, end, rangeMode)).append(
		// "(?:\\.[0-9]*)?");
		result.append(iPartRange(compareAllowingEqual, end, rangeMode)).append(
				"(?:\\.").append(rangeMode.digitRange0()).append("*)?");

		// Done last, since alternations should be shortest number last
		// (ensures largest match - e.g. 181 instead of 18; -181 instead of -18)
		if (fPartRange.length() != 0) {
			if (!isNegative && comparison.matchesAbove() || isNegative && comparison.matchesBelow()) {
				if (result.length() != 0)
					result.append('|');

				result.append(fPartRange);
			}
		}

		return result.toString();
	}

	/**
	 * Returns a regular expression that matches any positive number
	 * (excludes zero).
	 * 
	 * @param allowLeadingZeros
	 *            whether to allow leading zeros
	 * @param base
	 *            the base
	 * @param baseMode
	 *            the base mode
	 * @return a regular expression that matches any positive number
	 *         (excludes zero).
	 */
	// private static String matchPositive(boolean allowLeadingZeros,
	// int base, String baseMode)
	private static String matchPositive(boolean allowsLeadingZeros, RangeMode rangeMode)
	{
		// int base = rangeMode.base();

		StringBuilder result = new StringBuilder();

		if (rangeMode.allowsLeadingZeros())
			result.append("0*+");

		// result.append(digitRange(1, base - 1, baseMode));
		result.append(rangeMode.digitRange1());
		// result.append(digitRange(0, base - 1, baseMode));
		result.append(rangeMode.digitRange0());
		result.append("*");

		return result.toString();
	}

	private static String iPartRange(String iPartStart, boolean inclusiveStart,
			String iPartEnd, boolean inclusiveEnd, RangeMode rangeMode)
	{
		// TODO: check corner cases (e.g. iPartStart==iPartEnd)

		if (!inclusiveStart)
			iPartStart = addOne(iPartStart);

		if (!inclusiveEnd)
			iPartEnd = subtractOne(iPartEnd);

		return range(iPartStart, iPartEnd, rangeMode);
	}

	private static String iPartRange(Comparison comparison, String iPart, RangeMode rangeMode)
	{
		// >= 0
		// if (compare(iPart, "0") == 0 && comparison == GREATER_THAN_EQUAL)
		// return "[1-9]?[0-9]+";

		// TODO: check corner cases (near zero)
		if (comparison == LESS_THAN) {
			// x < 5 === x <= 4
			// x < -5 === x <= -6

			iPart = subtractOne(iPart);
			comparison = LESS_THAN_EQUAL;
		} else if (comparison == GREATER_THAN) {
			// x > 5 === x >= 6
			// x > -5 === x >= -4

			iPart = addOne(iPart);
			comparison = GREATER_THAN_EQUAL;
		}

		// System.out.println(comparison + " " + iPart);
		// TODO: handle cases around 0

		// int base = 10;
		// String baseMode = "";
		// String mode = "NZ" + base + baseMode;

		StringBuilder result = new StringBuilder();
		boolean isNegative = iPart.startsWith("-");
		boolean crossesZeroBound = false;

		if (isNegative && comparison.matchesAbove())
			crossesZeroBound = true;

		if (!isNegative && comparison.matchesBelow())
			crossesZeroBound = true;

		if (isNegative) {
			// if (!crossesZeroBound) {
			iPart = iPart.substring(1);
			// comparison = comparison.negate();
			// }
		}

		boolean isPowerOfTen;
		int powerOfTen;
		boolean isZero = compare(iPart, "0") == 0;

		if (isNegative && comparison.matchesAbove()) {
			isPowerOfTen = false;
			powerOfTen = 0;
		} else if (!isNegative && comparison.matchesBelow()) {
			isPowerOfTen = false;
			powerOfTen = 0;
		} else {
			// String digits = isNegative ? iPart.substring(1) : iPart;
			String digits = iPart;
			isPowerOfTen = isPowerOfTen(digits);

			powerOfTen = isPowerOfTen ? digits.length() - 1 : isZero ? 0 : digits.length();
		}

		// Larger values first
		// (ensures largest number is matched, and not just a part
		if (!isNegative && crossesZeroBound || isNegative && !crossesZeroBound)
			result.append('-');

		// result.append(abovePowerTenInteger(powerOfTen, base, baseMode));
		result.append(abovePowerTenInteger(powerOfTen, rangeMode));

		// if (isPowerOfTen(iPart)) {
		// -[1-9][0-9]*

		// }

		// TODO: should I update method to instead return power of 10
		// (and return -1, if not power of ten)
		if (!isPowerOfTen) {
			if (!isZero) {
				result.append('|');
				// Match [iPart..10^n-1] - n is nearest power of ten

				// String end = powerOfTen == 0 ? "0" : repeat('9', powerOfTen);
				String start = iPart;
				String end = crossesZeroBound ? "1" : repeat('9', powerOfTen);

				// System.out.println(iPart + "\t" + end);

				if (compare(start, end) > 0) {
					String temp = start;
					start = end;
					end = temp;
				}

				// String range = crossesZeroBound
				// ? range(iPart, end, mode)
				// : range_private("", iPart, end, false, base, baseMode);
				// String range = range_private(isNegative ? "-" : "", start, end, false, base, baseMode);
				String range = range_private(isNegative ? "-" : "", start, end, rangeMode.allowsLeadingZeros(false));

				// result.append(range(iPart, end, mode));

				// if (isNegative && crossesZeroBound)
				// result.append("-");

				result.append(range);
			}

			// System.out.println("10^" + powerOfTen);

			if (crossesZeroBound || isZero)
				result.append("|0");

			// noncapture(result);
		}

		// if (isNegative && !crossesZeroBound)
		// result.insert(0, '-');

		String resultStr = result.toString();

		// Surround in non-capture group, if necessary
		return resultStr.contains("|") ? "(?:" + result + ")" : resultStr;
	}

	private static String fPartRange(String iPart, String fPartStart, boolean inclusiveStart,
			String fPartEnd, boolean inclusiveEnd, RangeMode rangeMode)
	{
		// TODO: check cases of when fPartStart == fPartEnd
		// Check cases of when fPartStart == fPartEnd == 0
		// Check cases of when fPartStart == 0

		int maxLength;
		String fPartStartUsed;
		String fPartEndUsed;

		if (fPartStart == null) {
			// represents -infinity (for decimal places)
			// e.g. 1 <= x <= 1.5

			fPartStart = repeat('0', fPartEnd.length());

			// Don't include -0 as match
			inclusiveStart = compare(iPart, "-0", false) != 0;

			maxLength = fPartEnd.length();
			fPartStartUsed = fPartStart;
			fPartEndUsed = fPartEnd;
		} else if (fPartEnd == null) {
			// represents infinity (for decimal places)
			// e.g. 1.5 <= x < 2

			fPartEnd = "1" + repeat('0', fPartStart.length());
			inclusiveEnd = false;

			maxLength = fPartStart.length();
			fPartStartUsed = fPartStart;
			fPartEndUsed = fPartEnd;
		} else {
			maxLength = Math.max(fPartStart.length(), fPartEnd.length());
			fPartStartUsed = withTrailingZeros(fPartStart, maxLength);
			fPartEndUsed = withTrailingZeros(fPartEnd, maxLength);

			if (compare(fPartStartUsed, fPartEndUsed) > 0) {
				// fPartStart > fPartEnd
				// (swap start and end)
				// fPartStart must be less than fPartEnd
				// (even if negative iPart)

				String fPartUsedTemp = fPartStartUsed;
				fPartStartUsed = fPartEndUsed;
				fPartEndUsed = fPartUsedTemp;

				String fPartTemp = fPartStart;
				fPartStart = fPartEnd;
				fPartEnd = fPartTemp;

				boolean inclusiveTemp = inclusiveStart;
				inclusiveStart = inclusiveEnd;
				inclusiveEnd = inclusiveTemp;
			}
		}

		// int base = 10;
		// String baseMode = "";
		// String mode = "Z" + base + baseMode;
		// boolean isNegative = iPart.startsWith("-");
		// boolean matchingNegativeZero = compare(iPart, "-0", false) == 0;
		boolean isFPartStartZero = compare(fPartStart, "0") == 0;
		// boolean isFPartEndZero = compare(fPartEnd, "0") == 0;
		boolean optionalDecimalPoint = isFPartStartZero && inclusiveStart && compare(iPart, "-0", false) != 0;

		// if (matchingNegativeZero && isFPartStartZero)
		// {
		// TODO: incorporate into code better

		// don't include -0 as a match
		// inclusiveStart = false;
		// }

		// fPartStartUsed = removeTrailingZeros(fPartStartUsed);
		// fPartEndUsed = removeTrailingZeros(fPartEndUsed);

		// String fPartEndUsedMinusOne = subtractOne(fPartEndUsed);

		StringBuilder result = new StringBuilder();

		// Note: Moved to end of method
		// result.append(iPart);

		// if (optionalDecimalPoint)
		// result.append("(?:");

		// result.append("\\.(?:");

		// TODO: are these correct steps for negative numbers, as well ??
		if (!inclusiveStart)
			fPartStartUsed = withLeadingZeros(addOne(fPartStartUsed), maxLength);

		// TODO: ensure correct order for negative numbers
		if (inclusiveEnd) {
			if (result.length() != 0)
				result.append('|');

			// TODO: are these correct steps for negative numbers, as well ??
			result.append(optionalTrailingZeros(fPartEndUsed, "0*"));
			// result.append(fPartEndUsed).append("0*");
		}

		// System.out.println(fPartStartUsed + "\t" + fPartEndUsedMinusOne);
		// TODO: ensure correct order for negative numbers

		// if (!isFPartEndZero) {
		if (compare(fPartStartUsed, fPartEndUsed) < 0) {
			String fPartEndUsedMinusOne = withLeadingZeros(subtractOne(fPartEndUsed), maxLength);

			// System.out.println(fPartStartUsed + "\t" + fPartEndUsedMinusOne);
			if (result.length() != 0)
				result.append('|');

			// iPart === "-0" && fPartStartUsed == 0
			// boolean negativeZeroMatch = compare(iPart, "-0", false) == 0 && compare(fPartStartUsed, "0") == 0;

			// if (negativeZeroMatch)
			// {
			// // Include case -0.0x
			// // TODO: prevent outside cases
			// // e.g. -0.005..0.5 shouldn't match -0.01
			// result.append(fPartRange("-0", "0", false, fPartEndUsedMinusOne, true, rangeMode));
			// // result.append('|').append("0").append(matchPositive(true, rangeMode));
			// }
			// else
			// {
			// System.out.println("Range: " + range(fPartStartUsed, fPartEndUsedMinusOne, "Z"));
			// result.append(optionalTrailingZeros(range(fPartStartUsed, fPartEndUsedMinusOne, mode),
			// digitRange(0, base - 1, baseMode) + "*"));
			result.append(optionalTrailingZeros(
					range(fPartStartUsed, fPartEndUsedMinusOne, rangeMode.allowsLeadingZeros(true)),
					rangeMode.digitRange0() + "*"));
			// }

			// System.out.println(iPart + ": " + optionalTrailingZeros(range(fPartStartUsed, fPartEndUsedMinusOne,
			// rangeMode),
			// rangeMode.digitRange0() + "*"));
		}

		if (!inclusiveStart) {
			if (result.length() != 0)
				result.append('|');

			result.append(withTrailingZeros(fPartStart, maxLength)).append(matchPositive(true, rangeMode));
		}

		if (result.length() == 0)
			return "";

		result.insert(0, "\\.(?:");
		result.append(")");

		if (optionalDecimalPoint) {
			result.insert(0, "(?:");
			result.append(")?");
		}

		result.insert(0, iPart);

		// Note: inserted above
		// result.append(iPart);

		// if (optionalDecimalPoint)
		// result.append("(?:");

		// result.append("\\.(?:");

		return result.toString();
	}

	private static String fPartRange(Comparison comparison, String iPart, String fPart, RangeMode rangeMode)
	{
		boolean isNegative = iPart.startsWith("-");

		if (isNegative)
			comparison = comparison.negate();

		if (comparison.matchesAbove())
			return fPartRange(iPart, fPart, comparison.allowsEqual(), null, false, rangeMode);
		else
			return fPartRange(iPart, null, true, fPart, comparison.allowsEqual(), rangeMode);
	}

	private static String ceil(String iPart, String fPart)
	{
		if (fPart == null || fPart.length() == 0 || compare(fPart, "0") == 0) {
			// An integer - return the integer
			return iPart;
		} else if (iPart.startsWith("-")) {
			// Negative number - return the integer
			// e.g. ceil(-1.5) == -1

			// if iPart == -0, remove negative sign (keep any leading zeros)
			return compare(iPart, "0") == 0 ? iPart.substring(1) : iPart;
		} else {
			// Positive number - return the integer + 1
			// e.g. ceil(1.5) == 2
			return addOne(iPart);
		}
	}

	private static String floor(String iPart, String fPart)
	{
		if (fPart == null || fPart.length() == 0 || compare(fPart, "0") == 0) {
			// An integer - return the integer
			return iPart;
		} else if (iPart.startsWith("-")) {
			// Negative number - return the integer - 1
			// e.g. floor(-1.5) == -2
			return subtractOne(iPart);
		} else {
			// Positive number - return the integer
			// e.g. floor(1.5) == 1
			return iPart;
		}
	}

	/**
	 * Adds one to the given number.
	 * 
	 * @param number
	 *            the number
	 * @return <code>number</code> + 1
	 */
	private static String addOne(String number)
	{
		// TODO: check for better method
		return new BigInteger(number).add(BigInteger.ONE).toString();
	}

	/**
	 * Subtracts one from the given number.
	 * 
	 * @param number
	 *            the number
	 * @return
	 *         <code>number</code> - 1
	 */
	private static String subtractOne(String number)
	{
		// TODO: check for better method
		return new BigInteger(number).subtract(BigInteger.ONE).toString();
	}

	/**
	 * Checks if <code>iPart</code> is a power of ten (for example, "100").
	 * 
	 * @param iPart
	 *            the integer part
	 * @return <code>true</code>, if <code>iPart</code> is a power of ten
	 */
	private static boolean isPowerOfTen(String iPart)
	{
		boolean hasDigitOne = false;

		for (int i = 0; i < iPart.length(); i++) {
			if (iPart.charAt(i) != '0') {
				if (hasDigitOne)
					return false;

				if (iPart.charAt(i) != '1')
					return false;

				hasDigitOne = true;
			}
		}

		return hasDigitOne;
	}

	// private static String abovePowerTenInteger(int power, int base,
	// String baseMode)
	private static String abovePowerTenInteger(int power, RangeMode rangeMode)
	{
		// String range0 = digitRange(0, base - 1, baseMode);
		// String range1 = digitRange(1, base - 1, baseMode);
		String range0 = rangeMode.digitRange0();
		String range1 = rangeMode.digitRange1();

		StringBuilder result = new StringBuilder();

		result.append(range1).append(range0);

		if (power == 0)
			result.append('*');
		else if (power == 1)
			result.append('+');
		else
			result.append("{" + power + ",}");

		return result.toString();
	}

	/**
	 * Returns a regular expression that matches <code>-1 &lt; x &lt; 0</code>.
	 * 
	 * @param optionalZero
	 *            indicates whether the leading zero is option (e.g. -.5) or whether it must be included (e.g. -0.5)
	 * @param base
	 *            the base
	 * @param baseMode
	 *            the base mode
	 * @return the string
	 */
	// private static String matchesNegativeZeroFloats(boolean optionalZero, int base, String baseMode)
	private static String matchesNegativeZeroFloats(RangeMode rangeMode)
	{
		boolean optionalZero = rangeMode.optionalZeroNegative();

		// TODO: add support for having optionalZero as false
		// TODO: add support for having positive 0 optional
		// return "-0" + (optionalZero ? "?" : "") + "\\." + matchPositive(true, base, baseMode);
		return "-0" + (optionalZero ? "?" : "") + "\\." + matchPositive(true, rangeMode);
	}

	/**
	 * Surrounds the passed StringBuilder in a non-capture group (modifies
	 * argument).
	 * 
	 * @param sb
	 *            the StringBuilder to surround
	 * @return the RegEx for the given StringBuilder surrounded by a
	 *         non-capture group.
	 */
	private static StringBuilder noncapture(StringBuilder sb)
	{
		sb.insert(0, "(?:");
		sb.append(')');

		return sb;
	}

	/**
	 * Compares two integers numerically
	 * 
	 * @param value1
	 * @param value2
	 * @return
	 */
	public static int compare(String value1, String value2)
	{
		return compare(value1, value2, true);
	}

	public static int compare(String value1, String value2, boolean treatNegativeZeroAsZero)
	{
		value1 = simplify(value1, treatNegativeZeroAsZero);
		value2 = simplify(value2, treatNegativeZeroAsZero);

		int sign = sign_fin(value1);

		if (sign != sign_fin(value2)) {
			return sign;
		}

		if (value1.length() != value2.length()) {
			// e.g. 321 < 1234
			// e.g. -321 > -1234

			return (value1.length() - value2.length()) * sign;
		} else {
			assert value1.length() == value2.length() : value1 + "\t" + value2;

			// e.g. 123 < 234
			// e.g. -123 > -234

			return value1.compareTo(value2) * sign;
		}
	}

	/**
	 * Checks if the given number is negative or position.
	 * 
	 * @param number
	 *            the (string) number to check
	 * 
	 * @return -1 if the number is negative (has a '-' as the first
	 *         character); 1 otherwise.
	 */
	private static int sign_fin(String number)
	{
		return number.charAt(0) == '-' ? -1 : 1;
	}

	/**
	 * Simplifies the sign, and removes leading zeros.
	 * 
	 * <p>Simplifies leading
	 * "+" and "-" sign(s) to a single "-" sign if negative, and no sign, if
	 * positive. Removes any leading zeros in the number.</p>
	 * 
	 * @param number
	 *            the (string) number to simplify
	 * @return the inputted number, simplifying the sign and removing
	 *         leading zeros.
	 */
	// private static String simplify(String number)
	// {
	// return simplify(number, true);
	// }

	/**
	 * 
	 * @param number
	 * @param treatNegativeZeroAsZero
	 * @return
	 */
	private static String simplify(String number, boolean treatNegativeZeroAsZero)
	{
		int sign = 1, i = 0;

		// simplify leading "+" and "-"
		while (number.charAt(i) == '-' || number.charAt(i) == '+') {
			if (number.charAt(i) == '-') {
				sign *= -1;
			}

			i++;
		}

		// remove leading zeros (won't remove last zero, in case number = 0)
		while (i + 1 < number.length() && number.charAt(i) == '0') {
			i++;
		}

		String simplifiedNumber = number.substring(i);

		// TODO: test for accurracy

		if (simplifiedNumber.equals("0") && treatNegativeZeroAsZero) {
			return "0";
		}

		return (sign == -1 ? "-" : "") + simplifiedNumber;
	}

	/**
	 * Removes the leading zeros.
	 * 
	 * @param number
	 *            the number
	 * 
	 * @return the number with leading zeros removed
	 */
	private static String removeLeadingZeros(String number)
	{
		int start = 0;

		while (start < number.length() - 1 && number.charAt(start) == '0') {
			start++;
		}

		return number.substring(start);
	}

	/**
	 * Removes the trailing zeros.
	 * 
	 * @param number
	 *            the number
	 * @return the number with trailing zeros removed
	 */
	// private static String removeTrailingZeros(String number)
	// {
	// int end = number.length();
	//
	// while (end > 1 && number.charAt(end - 1) == '0') {
	// end--;
	// }
	//
	// return number.substring(0, end);
	// }

	/**
	 * Returns the numeric value of the character ch in the {@link Character#MAX_RADIX MAX_RADIX}.
	 * 
	 * @param ch
	 *            the character to be converted.
	 * @return the numeric value represented by the character in the max
	 *         radix.
	 * @see Character#digit(char, int)
	 */
	private static int digit(char ch)
	{
		return Character.digit(ch, MAX_RADIX);
	}

	private static String forDigit(int digit, String baseMode)
	{
		StringBuilder forDigit = new StringBuilder();
		boolean containsL = baseMode.length() == 0 || baseMode.contains("L");
		boolean containsU = baseMode.length() == 0 || baseMode.contains("U");

		if (containsL)
			forDigit.append(Character.forDigit(digit, MAX_RADIX));

		if (containsU && digit >= 10 || !containsL) {
			forDigit
					.append((char) (Character.forDigit(digit, MAX_RADIX) - (digit >= 10 ? ' '
							: 0)));
		}

		return forDigit.length() == 1 ? forDigit.toString() : "[" +
				forDigit + "]";
	}

	/**
	 * Return a regular expression that matches the given range of digits.
	 * 
	 * @param start
	 *            the start digit
	 * @param end
	 *            the end digit
	 * @param baseMode
	 *            the L/U switch
	 * @return a regular expression that matches the given range of digits
	 */
	// TODO: make private
	public static String digitRange(int start, int end, String baseMode)
	{
		if (start == end)
			return forDigit(start, baseMode);

		StringBuilder digitRange = new StringBuilder("[");

		if (start < 9) {
			char startDigit = Character.forDigit(start, MAX_RADIX);
			char endDigit = Character.forDigit(Math.min(end, 9), MAX_RADIX);

			if (startDigit == endDigit)
				digitRange.append(startDigit);
			else
				digitRange.append(startDigit).append('-').append(endDigit);
		}

		if (end >= 10) {
			boolean containsL = baseMode.length() == 0 || baseMode.contains("L");
			boolean containsU = baseMode.length() == 0 || baseMode.contains("U");

			char startDigit = Character.forDigit(Math.max(start, 10),
					MAX_RADIX);
			char endDigit = Character.forDigit(end, MAX_RADIX);

			if (containsL) {
				if (startDigit == endDigit)
					digitRange.append(startDigit);
				else
					digitRange.append(startDigit).append('-').append(
							endDigit);
			}

			if (containsU) {
				if (startDigit == endDigit)
					digitRange.append((char) (startDigit - ' '));
				else
					digitRange.append((char) (startDigit - ' '))
							.append('-').append((char) (endDigit - ' '));
			}
		}

		return digitRange.append(']').toString();
	}

	/**
	 * Returns a String with the specified character repeated.
	 * 
	 * @param character
	 *            <code>char</code> to repeat
	 * @param count
	 *            number of times to repeat the character
	 * @return a String with the given character repeated <code>count</code>
	 *         times
	 * @throws IllegalArgumentException
	 *             If <code>count</code> &lt; 0.
	 */
	private static String repeat(char character, int count)
	{
		if (count < 0)
			throw new IllegalArgumentException(
					"Count cannot be negative: " + count);

		if (count == 0)
			return "";

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < count; i++) {
			result.append(character);
		}

		return result.toString();
	}

	/**
	 * Returns a String with the specified string repeated.
	 * 
	 * @param string
	 *            the string to repeat
	 * @param count
	 *            number of times to repeat the string
	 * @return a String with the given string repeated <code>count</code>
	 *         times
	 * @throws IllegalArgumentException
	 *             If <code>count</code> &lt; 0.
	 */
	private static String repeat(String string, int count)
	{
		if (count < 0)
			throw new IllegalArgumentException(
					"Count cannot be negative: " + count);

		if (count == 0)
			return "";

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < count; i++) {
			result.append(string);
		}

		return result.toString();
	}

	/**
	 * Adds leading zeros to the number, to ensure a specific size
	 * 
	 * @param number
	 *            the number
	 * @param size
	 *            the total size (leading zeros added, as necessary)
	 * @return the number with a length of at least <code>size</code>, padding with leading zeros, as needed
	 */
	private static String withLeadingZeros(String number, int size)
	{
		if (number.length() >= size)
			return number;

		return repeat('0', size - number.length()) + number;
	}

	/**
	 * Adds trailing zeros to the number, to ensure a specific size
	 * 
	 * @param number
	 *            the number
	 * @param size
	 *            the total size (trailing zeros added, as necessary)
	 * @return the number with a length of at least <code>size</code>, padding with trailing zeros, as needed
	 */
	private static String withTrailingZeros(String number, int size)
	{
		if (number.length() >= size)
			return number;

		return number + repeat('0', size - number.length());
	}
}