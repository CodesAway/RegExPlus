package info.codesaway.util.regex;

/**
 * Unchecked exception thrown to indicate a syntax error in a
 * regular-expression pattern.
 * 
 * <p>This class is an extension
 * of Java's {@link java.util.regex.PatternSyntaxException} class. Javadocs were
 * copied and appended with the added functionality.</p>
 */
public class PatternSyntaxException extends
		java.util.regex.PatternSyntaxException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1080052482974889322L;

	/**
	 * Constructs a new instance of this class.
	 * 
	 * @param desc
	 *            A description of the error
	 * 
	 * @param regex
	 *            The erroneous pattern
	 * 
	 * @param index
	 *            The approximate index in the pattern of the error,
	 *            or <tt>-1</tt> if the index is not known
	 */
	public PatternSyntaxException(String desc, String regex, int index)
	{
		super(desc, regex, index);
	}
}
