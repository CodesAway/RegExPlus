package info.codesaway.util.regex;

import java.util.Collection;
import java.util.EnumSet;

// TODO: finish incorporating into RegExPlus, including Groovy functionality
/**
 * @since 0.2
 */
public enum PatternFlag implements PatternOptions
{
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

	;

	private final int flag;
	private final String inlineFlag;

	private PatternFlag(int flag)
	{
		this(flag, "");
	}

	private PatternFlag(int flag, String inlineFlag)
	{
		this.flag = flag;
		this.inlineFlag = inlineFlag;
	}

	/**
	 * {@inheritDoc}
	 */
	public int intValue()
	{
		return flag;
	}
		
	public String getInlineFlag()
	{
		return inlineFlag;
	}
	
	public boolean hasInlineFlag()
	{
		return inlineFlag.length() != 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public EnumSet<PatternFlag> asEnumSet() 
	{
		return EnumSet.of(this);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PatternFlags getFlags()
	{
		return new PatternFlags(flag);
	}

	/* Groovy methods - makes RegExPlus groovier */

	/* Arithmetic operations */
	
	/**
	 * {@inheritDoc}
	 */
	public PatternFlags plus(PatternOptions flags)
	{
		return PatternOptions.StaticMethods.plus(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	public PatternFlags minus(PatternOptions flags)
	{
		return PatternOptions.StaticMethods.minus(this, flags);
	}
	
	/* Bitwise operations */
	
	/**
	 * {@inheritDoc}
	 */
	public PatternFlags or(PatternOptions flags)
	{
		return PatternOptions.StaticMethods.or(this, flags);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PatternFlags and(PatternOptions flags)
	{
		return PatternOptions.StaticMethods.and(this, flags);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PatternFlags xor(PatternOptions flags)
	{
		return PatternOptions.StaticMethods.xor(this, flags);
	}
	
	/**
	 * {@inheritDoc}
	 */
//	public PatternFlags leftShift(PatternFlagInterface flags)
//	{
//		return PatternFlagInterface.StaticMethods.leftShift(this, flags);
//	}

	/**
	 * {@inheritDoc}
	 */
//	public PatternFlags leftShift(Collection<PatternFlag> flags)
//	{
//		return PatternFlagInterface.StaticMethods.leftShift(this, flags);
//	}
	
	/**
	 * {@inheritDoc}
	 */
	public PatternFlags bitwiseNegate()
	{
		return PatternOptions.StaticMethods.bitwiseNegate(this);
	}
}
