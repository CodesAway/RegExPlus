package info.codesaway.util.regex;

public enum Comparison
{
	// LESS_THAN(true, false), LESS_THAN_EQUAL(true, true), GREATER_THAN(
	// false, false), GREATER_THAN_EQUAL(false, true);
	LESS_THAN("<"), LESS_THAN_EQUAL("<="), GREATER_THAN(">"), GREATER_THAN_EQUAL(">=");

	public static final boolean MATCHES_BELOW = true;
	public static final boolean MATCHES_ABOVE = false;
	public static final boolean ALLOWS_EQUAL = true;

	private final String toString;
	private final boolean matchesBelow;
	private final boolean matchesAbove;
	private final boolean allowsEqual;

	/**
	 * @param matchesBelow
	 * @param allowsEqual
	 */
	// private Comparison(boolean matchesBelow, boolean allowsEqual)
	// {
	// this.matchesBelow = matchesBelow;
	// this.allowsEqual = allowsEqual;
	// }
	private Comparison(String toString)
	{
		this.toString = toString;

		// "<" and "<="
		matchesBelow = toString.startsWith("<");

		// ">" and ">="
		matchesAbove = toString.startsWith(">");

		// "<=" and ">="
		allowsEqual = toString.endsWith("=");
	}

	@Override
	public String toString()
	{
		return toString;
	}

	public boolean matchesBelow()
	{
		return matchesBelow;
	}

	public boolean matchesAbove()
	{
		return matchesAbove;
	}

	public boolean allowsEqual()
	{
		return allowsEqual;
	}

	/**
	 * Returns the <code>Comparison</code> with the reverse greater than /
	 * less than of this <code>Comparison</code> (keeps whether allows
	 * equal).</p>
	 * 
	 * <p>This is most commonly seen when changing the sign to account for a
	 * negative.</p>
	 * 
	 * <pre>-x < y
	 * x > y
	 * 
	 * -x > y
	 * x < -y
	 * 
	 * -x <= y
	 * x >= y
	 * 
	 * -x >= y
	 * x <= -y</pre>
	 * 
	 * @return the <code>Comparison</code> which has the reverse of this
	 *         <code>Comparison</code>'s greater than / less than sign
	 *         (keeps whether allows equal)
	 */
	public Comparison negate()
	{
		return Comparison.valueOf(!matchesBelow(), allowsEqual());
	}

	public static Comparison valueOf(boolean matchesBelow,
			boolean allowsEqual)
	{
		if (matchesBelow) {
			return allowsEqual ? LESS_THAN_EQUAL : LESS_THAN;
		} else {
			// matches above
			return allowsEqual ? GREATER_THAN_EQUAL : GREATER_THAN;
		}
	}
}