package info.codesaway.util.regex;

import static info.codesaway.util.regex.Refactor.newLine;

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

	/** the additional details (or <code>null</code> if none) */
	private final String additionalDetails;

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
		this(desc, regex, index, null);
	}

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
	 * @param additionalDetails
	 *            the additional details (or <code>null</code> if there are none)
	 */
	public PatternSyntaxException(String desc, String regex, int index, String additionalDetails)
	{
		super(desc, regex, index);
		this.additionalDetails = additionalDetails;
	}

	String nl = System.getProperty("line.separator");

	// Copied from Java's PatternSyntaxException and tweaked so to have caret position be accurate when text
	// is possibly line wrapped (such as in RegExPlus Tasker plugin, where error message is shown in a text box)
	private String getBaseMessage()
	{
		String desc = getDescription();
		int index = getIndex();
		String pattern = getPattern();

		StringBuffer sb = new StringBuffer();
		sb.append(desc);
		if (index >= 0) {
			sb.append(" near index ");
			sb.append(index);
		}
		sb.append(nl);
		sb.append(pattern);
		if (index >= 0) {
			sb.append(nl);
			for (int i = 0; i < index; i++) {
				// Output character based on respective character in pattern
				// (allows line wrapping to occur similarly in pattern and caret position,
				// in the case that line wrapping occurs on error message -
				// like if put in text box like in RegExPlus Tasker plug-in)

				char c = pattern.charAt(i);

				if (Character.isWhitespace(c))
				{
					sb.append(c);
				}
				else
				{
					// Non-break space
					sb.append((char) 160);
				}

			}
			sb.append('^');
		}
		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMessage()
	{
		return additionalDetails == null
				? getBaseMessage()
				: getBaseMessage() + newLine + newLine + additionalDetails;
	}
}
