package info.codesaway.util.regex;

import static java.lang.Character.MAX_RADIX;
import static java.lang.Character.MIN_RADIX;

class RangeMode
{
	// TODO: add to supported regex

	/** Whether the 0 is optional for negative float values (e.g. -.5) */
	private final boolean optionalZeroNegative = true;

	/** Whether the 0 is optional for positive float values (e.g. -.5) **/
	private final boolean optionalZeroPositive = false;

	/** The mode. */
	private final String mode;

	/** Whether the mode allows leading zeros. */
	private final boolean allowsLeadingZeros;

	/**
	 * Whether forced as decimal ('d'), forced as integer ('i'), or not forced as either.
	 * If not forced as a range type, an integer range will be used if both ends are integers;
	 * otherwise, a decimal range will be used.
	 */
	private final String rangeType;

	/** Whether the mode forces decimal mode. */
	private final boolean forcesDecimalMode;

	/** Whether the mode forces integer mode. */
	private final boolean forcesIntegerMode;

	/** The base. */
	private final int base;

	/** Whether to include upper-case, lower-case, or both types of digits (for digits 10 and above). */
	private final String baseMode;

	/** Whether the mode allows lower case. */
	private final boolean allowsLowerCase;

	/** Whether the mode allows upper case. */
	private final boolean allowsUpperCase;

	/** Regular expression used to parse the mode. */
	// TODO: support integer mode ('i') in addition to decimal mode ('d')
	private static final java.util.regex.Pattern rangeModeRegEx = java.util.regex.Pattern
			.compile("^(N?Z)([di]?)(?:(\\d++)([LU])?)?$");

	/**
	 * Instantiates a new range mode.
	 * 
	 * @param mode
	 *            the mode
	 * @throws IllegalArgumentException
	 *             If mode is not a valid, for any reason (exception message contains the specific reason)
	 */
	RangeMode(String mode)
	{
		this.mode = mode;

		java.util.regex.Matcher rangeMode = rangeModeRegEx.matcher(mode);

		if (!rangeMode.matches())
			throw new IllegalArgumentException("Illegal range mode: " + mode);

		allowsLeadingZeros = rangeMode.group(1).equals("Z");
		rangeType = rangeMode.group(2);
		forcesDecimalMode = rangeType.equals("d");
		forcesIntegerMode = rangeType.equals("i");

		base = rangeMode.group(3) == null ? 10 : Integer
				.parseInt(rangeMode.group(3));

		// TODO: include base number in error message

		if (base < MIN_RADIX || base > MAX_RADIX)
			throw new IllegalArgumentException(Refactor.INVALID_BASE);

		baseMode = rangeMode.group(4) == null ? "" : rangeMode.group(4);

		allowsLowerCase = baseMode.contains("L");
		allowsUpperCase = baseMode.contains("U");
	}

	private RangeMode(boolean allowsLeadingZeros, String rangeType,
			int base, String baseMode)
	{
		// TODO: include base number in error message

		if (base < MIN_RADIX || base > MAX_RADIX)
			throw new IllegalArgumentException(Refactor.INVALID_BASE);

		this.allowsLeadingZeros = allowsLeadingZeros;
		this.rangeType = rangeType;
		this.base = base;
		this.baseMode = baseMode;

		mode = (!allowsLeadingZeros ? "N" : "") + "Z" + rangeType + base + baseMode;

		forcesDecimalMode = rangeType.equals("d");
		forcesIntegerMode = rangeType.equals("i");

		allowsLowerCase = baseMode.length() == 0 || baseMode.contains("L");
		allowsUpperCase = baseMode.length() == 0 || baseMode.contains("U");
	}

	public boolean optionalZeroNegative()
	{
		return optionalZeroNegative;
	}

	public boolean optionalZeroPositive()
	{
		return optionalZeroPositive;
	}

	@Override
	public String toString()
	{
		return mode;
	}

	public String mode()
	{
		return mode;
	}

	public boolean allowsLeadingZeros()
	{
		return allowsLeadingZeros;
	}

	public RangeMode allowsLeadingZeros(boolean allowsLeadingZeros)
	{
		return new RangeMode(allowsLeadingZeros, rangeType, base, baseMode);
	}

	public boolean forcesDecimalMode()
	{
		return forcesDecimalMode;
	}

	public boolean forcesIntegerMode()
	{
		return forcesIntegerMode;
	}

	public int base()
	{
		return base;
	}

	public String baseMode()
	{
		return baseMode;
	}

	public boolean allowsLowerCase()
	{
		return allowsLowerCase;
	}

	public boolean allowsUpperCase()
	{
		return allowsUpperCase;
	}

	public String digitRange0()
	{
		return PatternRange.digitRange(0, base - 1, baseMode);
	}

	public String digitRange1()
	{
		return PatternRange.digitRange(1, base - 1, baseMode);
	}

	public int lastDigit()
	{
		return base - 1;
	}

	public char lastCharacterDigit()
	{
		return Character.forDigit(base - 1, base);
	}
}