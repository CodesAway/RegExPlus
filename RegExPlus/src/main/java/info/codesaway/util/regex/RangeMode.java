package info.codesaway.util.regex;

import static java.lang.Character.MAX_RADIX;
import static java.lang.Character.MIN_RADIX;

class RangeMode {
	// TODO: implement functionality that uses these and make non-static
	// Changed to static because of SpotBugs warning
	/** Whether the 0 is optional for negative float values (e.g. -.5) */
	private static final boolean optionalZeroNegative = true;

	/** Whether the 0 is optional for positive float values (e.g. -.5) **/
	private static final boolean optionalZeroPositive = false;

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
	RangeMode(final String mode) {
		this.mode = mode;

		java.util.regex.Matcher rangeMode = rangeModeRegEx.matcher(mode);

		if (!rangeMode.matches()) {
			throw new IllegalArgumentException("Illegal range mode: " + mode);
		}

		this.allowsLeadingZeros = rangeMode.group(1).equals("Z");
		this.rangeType = rangeMode.group(2);
		this.forcesDecimalMode = this.rangeType.equals("d");
		this.forcesIntegerMode = this.rangeType.equals("i");

		this.base = rangeMode.group(3) == null ? 10
				: Integer
						.parseInt(rangeMode.group(3));

		// TODO: include base number in error message

		if (this.base < MIN_RADIX || this.base > MAX_RADIX) {
			throw new IllegalArgumentException(Refactor.INVALID_BASE);
		}

		this.baseMode = rangeMode.group(4) == null ? "" : rangeMode.group(4);

		this.allowsLowerCase = this.baseMode.contains("L");
		this.allowsUpperCase = this.baseMode.contains("U");
	}

	private RangeMode(final boolean allowsLeadingZeros, final String rangeType,
			final int base, final String baseMode) {
		// TODO: include base number in error message

		if (base < MIN_RADIX || base > MAX_RADIX) {
			throw new IllegalArgumentException(Refactor.INVALID_BASE);
		}

		this.allowsLeadingZeros = allowsLeadingZeros;
		this.rangeType = rangeType;
		this.base = base;
		this.baseMode = baseMode;

		this.mode = (!allowsLeadingZeros ? "N" : "") + "Z" + rangeType + base + baseMode;

		this.forcesDecimalMode = rangeType.equals("d");
		this.forcesIntegerMode = rangeType.equals("i");

		this.allowsLowerCase = baseMode.length() == 0 || baseMode.contains("L");
		this.allowsUpperCase = baseMode.length() == 0 || baseMode.contains("U");
	}

	public boolean optionalZeroNegative() {
		return RangeMode.optionalZeroNegative;
	}

	public boolean optionalZeroPositive() {
		return RangeMode.optionalZeroPositive;
	}

	@Override
	public String toString() {
		return this.mode;
	}

	public String mode() {
		return this.mode;
	}

	public boolean allowsLeadingZeros() {
		return this.allowsLeadingZeros;
	}

	public RangeMode allowsLeadingZeros(final boolean allowsLeadingZeros) {
		return new RangeMode(allowsLeadingZeros, this.rangeType, this.base, this.baseMode);
	}

	public boolean forcesDecimalMode() {
		return this.forcesDecimalMode;
	}

	public boolean forcesIntegerMode() {
		return this.forcesIntegerMode;
	}

	public int base() {
		return this.base;
	}

	public String baseMode() {
		return this.baseMode;
	}

	public boolean allowsLowerCase() {
		return this.allowsLowerCase;
	}

	public boolean allowsUpperCase() {
		return this.allowsUpperCase;
	}

	public String digitRange0() {
		return PatternRange.digitRange(0, this.base - 1, this.baseMode);
	}

	public String digitRange1() {
		return PatternRange.digitRange(1, this.base - 1, this.baseMode);
	}

	public int lastDigit() {
		return this.base - 1;
	}

	public char lastCharacterDigit() {
		return Character.forDigit(this.base - 1, this.base);
	}
}