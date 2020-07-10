package info.codesaway.util.regex;

public enum Comparison {
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
	private Comparison(final String toString) {
		this.toString = toString;

		// "<" and "<="
		this.matchesBelow = toString.startsWith("<");

		// ">" and ">="
		this.matchesAbove = toString.startsWith(">");

		// "<=" and ">="
		this.allowsEqual = toString.endsWith("=");
	}

	@Override
	public String toString() {
		return this.toString;
	}

	public boolean matchesBelow() {
		return this.matchesBelow;
	}

	public boolean matchesAbove() {
		return this.matchesAbove;
	}

	public boolean allowsEqual() {
		return this.allowsEqual;
	}

	/**
	 * Returns the <code>Comparison</code> with the reverse greater than /
	 * less than of this <code>Comparison</code> (keeps whether allows
	 * equal).
	 *
	 * <p>This is most commonly seen when changing the sign to account for a
	 * negative.</p>
	 *
	 * <pre>-x &lt; y
	 * x &gt; y
	 *
	 * -x &gt; y
	 * x &lt; -y
	 *
	 * -x &lt;= y
	 * x &gt;= y
	 *
	 * -x &gt;= y
	 * x &lt;= -y</pre>
	 *
	 * @return the <code>Comparison</code> which has the reverse of this
	 *         <code>Comparison</code>'s greater than / less than sign
	 *         (keeps whether allows equal)
	 */
	public Comparison negate() {
		return Comparison.valueOf(!this.matchesBelow(), this.allowsEqual());
	}

	public static Comparison valueOf(final boolean matchesBelow,
			final boolean allowsEqual) {
		if (matchesBelow) {
			return allowsEqual ? LESS_THAN_EQUAL : LESS_THAN;
		} else {
			// matches above
			return allowsEqual ? GREATER_THAN_EQUAL : GREATER_THAN;
		}
	}
}