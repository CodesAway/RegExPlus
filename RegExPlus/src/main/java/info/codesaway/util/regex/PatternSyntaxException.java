package info.codesaway.util.regex;

import static info.codesaway.util.regex.Refactor.newLine;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unchecked exception thrown to indicate a syntax error in a
 * regular-expression pattern.
 *
 * <p>This class is an extension
 * of Java's {@link java.util.regex.PatternSyntaxException} class. Javadocs were
 * copied and appended with the added functionality.</p>
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class PatternSyntaxException extends
		java.util.regex.PatternSyntaxException {
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
	public PatternSyntaxException(final String desc, final String regex, final int index) {
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
	public PatternSyntaxException(final String desc, final String regex, final int index,
			final String additionalDetails) {
		super(desc, regex, index);
		this.additionalDetails = additionalDetails;
	}

	String nl = System.getProperty("line.separator");

	// Copied from Java's PatternSyntaxException and tweaked so to have caret position be accurate when text
	// is possibly line wrapped (such as in RegExPlus Tasker plugin, where error message is shown in a text box)
	private String getBaseMessage() {
		String desc = this.getDescription();
		int index = this.getIndex();
		String pattern = this.getPattern();

		StringBuffer sb = new StringBuffer();
		sb.append(desc);
		if (index >= 0) {
			sb.append(" near index ");
			sb.append(index);
		}
		sb.append(this.nl);
		sb.append(pattern);
		if (index >= 0) {
			sb.append(this.nl);
			for (int i = 0; i < index; i++) {
				// Output character based on respective character in pattern
				// (allows line wrapping to occur similarly in pattern and caret position,
				// in the case that line wrapping occurs on error message -
				// like if put in text box like in RegExPlus Tasker plug-in)

				char c = pattern.charAt(i);

				if (Character.isWhitespace(c)) {
					sb.append(c);
				} else {
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
	public String getMessage() {
		return this.additionalDetails == null
				? this.getBaseMessage()
				: this.getBaseMessage() + newLine + newLine + this.additionalDetails;
	}
}
