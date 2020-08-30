package info.codesaway.util.regex;

import java.util.EnumSet;

// TODO: finish incorporating into RegExPlus, including Groovy functionality
/**
 * @since 0.2
 */
public enum PatternFlag implements PatternOptions {
	/**
	 * @see Pattern#CANON_EQ
	 */
	CANON_EQ(Pattern.CANON_EQ),

	/**
	 * @see Pattern#CASE_INSENSITIVE
	 */
	CASE_INSENSITIVE(Pattern.CASE_INSENSITIVE, "i"),

	/**
	 * @see Pattern#COMMENTS
	 */
	COMMENTS(Pattern.COMMENTS, "x"),

	/**
	 * @see Pattern#DOTALL
	 */
	DOTALL(Pattern.DOTALL, "s"),

	/**
	 * @see Pattern#DOTNET_NUMBERING
	 */
	DOTNET_NUMBERING(Pattern.DOTNET_NUMBERING),

	/**
	 * @see Pattern#DUPLICATE_NAMES
	 */
	DUPLICATE_NAMES(Pattern.DUPLICATE_NAMES, "J"),

	/**
	 * @see Pattern#EXPLICIT_CAPTURE
	 */
	EXPLICIT_CAPTURE(Pattern.EXPLICIT_CAPTURE, "n"),

	/**
	 * @see Pattern#LITERAL
	 */
	LITERAL(Pattern.LITERAL),

	/**
	 * @see Pattern#MULTILINE
	 */
	MULTILINE(Pattern.MULTILINE, "m"),

	/**
	 * @see Pattern#PERL_OCTAL
	 */
	PERL_OCTAL(Pattern.PERL_OCTAL, "o"),

	/**
	 * @see Pattern#UNICODE_CASE
	 */
	UNICODE_CASE(Pattern.UNICODE_CASE, "u"),

	/**
	 * @see Pattern#UNIX_LINES
	 */
	UNIX_LINES(Pattern.UNIX_LINES, "d"),

	/**
	 * @see Pattern#VERIFY_GROUPS
	 */
	VERIFY_GROUPS(Pattern.VERIFY_GROUPS, "v"),

	/**
	 * @see Pattern#UNICODE_CHARACTER_CLASS
	 * @since 1.2
	 */
	UNICODE_CHARACTER_CLASS(Pattern.UNICODE_CHARACTER_CLASS, "U");

	;

	private final int flag;
	private final String inlineFlag;

	private PatternFlag(final int flag) {
		this(flag, "");
	}

	private PatternFlag(final int flag, final String inlineFlag) {
		this.flag = flag;
		this.inlineFlag = inlineFlag;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int intValue() {
		return this.flag;
	}

	public String getInlineFlag() {
		return this.inlineFlag;
	}

	public boolean hasInlineFlag() {
		return this.inlineFlag.length() != 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EnumSet<PatternFlag> asEnumSet() {
		return EnumSet.of(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags getFlags() {
		return new PatternFlags(this.flag);
	}

	/* Groovy methods - makes RegExPlus groovier */

	/* Arithmetic operations */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags plus(final PatternOptions flags) {
		return PatternOptions.plus(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags minus(final PatternOptions flags) {
		return PatternOptions.minus(this, flags);
	}

	/* Bitwise operations */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags or(final PatternOptions flags) {
		return PatternOptions.or(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags and(final PatternOptions flags) {
		return PatternOptions.and(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags xor(final PatternOptions flags) {
		return PatternOptions.xor(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	//	public PatternFlags leftShift(PatternFlagInterface flags)
	//	{
	//		return PatternFlagInterface.leftShift(this, flags);
	//	}

	/**
	 * {@inheritDoc}
	 */
	//	public PatternFlags leftShift(Collection<PatternFlag> flags)
	//	{
	//		return PatternFlagInterface.leftShift(this, flags);
	//	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags bitwiseNegate() {
		return PatternOptions.bitwiseNegate(this);
	}
}
