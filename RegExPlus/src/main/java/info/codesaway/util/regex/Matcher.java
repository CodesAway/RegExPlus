package info.codesaway.util.regex;

// TODO: replace doesn't correctly handle references with named subgroups
// (if case of duplicates, use the first matching one - not necessarily the
// first occurrence)

// specifying the empty string for the group will find the first non-null group
// (same functionality as getUsedGroup in the (extended) Pattern class)

// TODO: overload matched method to allow checking for matched group without throwing error if it doesn't exist
import static info.codesaway.util.regex.Pattern.getMappingName;
import static info.codesaway.util.regex.Pattern.wrapIndex;
import static info.codesaway.util.regex.RefactorUtility.fullGroupName;
import static info.codesaway.util.regex.RefactorUtility.parseInt;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An engine that performs match operations on a {@link java.lang.CharSequence
 * </code>character sequence<code>} by interpreting a {@link Pattern}.
 *
 * <p>This class is an extension
 * of Java's {@link java.util.regex.Matcher} class. Javadocs were copied and
 * appended with the added functionality.</p>
 *
 * <p>A matcher is created from a pattern by invoking the pattern's {@link Pattern#matcher matcher} method. Once
 * created, a matcher can be used
 * to perform three different kinds of match operations:</p>
 *
 * <ul>
 *
 * <li><p>The {@link #matches matches} method attempts to match the entire
 * input sequence against the pattern.</p></li>
 *
 * <li><p>The {@link #lookingAt lookingAt} method attempts to match the
 * input sequence, starting at the beginning, against the pattern.</p></li>
 *
 * <li><p>The {@link #find find} method scans the input sequence looking for
 * the next subsequence that matches the pattern.</p></li>
 *
 * </ul>
 *
 * <p>Each of these methods returns a boolean indicating success or failure.
 * More information about a successful match can be obtained by querying the
 * state of the matcher.</p>
 *
 * <p>A matcher finds matches in a subset of its input called the
 * <i>region</i>. By default, the region contains all of the matcher's input.
 * The region can be modified via the{@link #region region} method and queried
 * via the {@link #regionStart regionStart} and {@link #regionEnd regionEnd} methods. The way that the region boundaries
 * interact with some pattern
 * constructs can be changed. See {@link #useAnchoringBounds
 * useAnchoringBounds} and {@link #useTransparentBounds useTransparentBounds} for more details.</p>
 *
 * <p>This class also defines methods for replacing matched subsequences with
 * new strings whose contents can, if desired, be computed from the match
 * result. The {@link #appendReplacement appendReplacement} and {@link #appendTail appendTail} methods can be used in
 * tandem in order to
 * collect the result into an existing string buffer, or the more convenient {@link #replaceAll replaceAll} method can
 * be used to create a string in which
 * every matching subsequence in the input sequence is replaced.</p>
 *
 * <p>The explicit state of a matcher includes the start and end indices of
 * the most recent successful match. It also includes the start and end
 * indices of the input subsequence captured by each <a
 * href="Pattern.html#cg">capturing group</a> in the pattern as well as a total
 * count of such subsequences. As a convenience, methods are also provided for
 * returning these captured subsequences in string form.</p>
 *
 * <p>The explicit state of a matcher is initially undefined; attempting to
 * query any part of it before a successful match will cause an {@link IllegalStateException} to be thrown. The explicit
 * state of a matcher
 * is recomputed by every match operation.</p>
 *
 * <p>The implicit state of a matcher includes the input character sequence as
 * well as the <i>append position</i>, which is initially zero and is updated
 * by the {@link #appendReplacement appendReplacement} method.</p>
 *
 * <p>A matcher may be reset explicitly by invoking its {@link #reset()} method
 * or, if a new input sequence is desired, its {@link #reset(java.lang.CharSequence) reset(CharSequence)} method.
 * Resetting
 * a matcher discards its explicit state information and sets the append
 * position to zero.</p>
 *
 * <p>Instances of this class are not safe for use by multiple concurrent
 * threads.</p>
 */
public final class Matcher implements MatchResult, Cloneable {
	/**
	 * The internal {@link java.util.regex.Matcher} object for this matcher.
	 */
	private final java.util.regex.Matcher internalMatcher;

	/**
	 * The matcher current being used to perform matches.
	 *
	 * <p>Sometimes the current matcher is a different matcher than the internal
	 * one, so this acts as a pointer to the matcher used to perform
	 * matches.</p>
	 *
	 * <p>For example, the {@link #proceed()} method makes use of a separate
	 * matcher to perform its matches than the internal one. In this case,
	 * this object points to that temporary matcher, instead of the internal
	 * matcher.</p>
	 */
	private java.util.regex.Matcher usedMatcher;

	/**
	 * The Pattern object that created this Matcher.
	 */
	private Pattern parentPattern;

	/**
	 * Whether group names (for named capture groups) are case-insensitive.
	 */
	// private boolean caseInsensitiveGroupNames;

	/**
	 * The index of the last position appended in a substitution.
	 */
	private int lastAppendPosition = 0;

	/**
	 * The original string being matched.
	 */
	private CharSequence text;

	/**
	 * @since 0.2
	 */
	private boolean treatNullAsEmptyString = false;

	/**
	 * @since 0.2
	 */
	private boolean isMatchResult = false;

	/**
	 * @param matcher the Java Matcher
	 * @since 0.2
	 */
	public Matcher(final java.util.regex.Matcher matcher) {
		this(matcher, new Pattern(matcher.pattern()), getText(matcher));
	}

	/**
	 * Gets the text field from a Java matcher.
	 *
	 * @param matcher
	 * @return
	 * @since 0.2
	 */
	private static CharSequence getText(final java.util.regex.Matcher matcher) {
		try {
			// Uses reflection
			Field field = matcher.getClass().getDeclaredField("text");

			// Necessary, since private field (doen't affect actual field)
			field.setAccessible(true);

			// Return CharSequence text field
			return (CharSequence) field.get(matcher);
		} catch (RuntimeException | NoSuchFieldException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}

	// public Matcher(CharSequence text, java.util.regex.Pattern pattern)
	// {
	// this(pattern, text);
	// }
	//
	// public Matcher(java.util.regex.Pattern pattern, CharSequence text)
	// {
	// this(pattern.matcher(text), new Pattern(pattern), text);
	// }

	/**
	 * All matchers have the state used by Pattern during a match.
	 *
	 * @param matcher
	 *            the internal {@link java.util.regex.Matcher}
	 * @param parent
	 *            the parent pattern
	 * @param text
	 *            the input text
	 */
	Matcher(final java.util.regex.Matcher matcher, final Pattern parent, final CharSequence text) {
		this.internalMatcher = matcher;
		this.parentPattern = parent;
		this.text = text;

		// this.caseInsensitiveGroupNames = parent.has(CASE_INSENSITIVE_NAMES);

		// Put fields into initial states
		this.resetPrivate();
	}

	/**
	 * Returns whether this matcher uses case-insensitive group names.
	 *
	 * @return if this matches uses case-insensitive group
	 *         names
	 */
	// private void setCaseInsensitiveGroupNames(boolean
	// caseInsensitiveGroupNames)
	// {
	// this.caseInsensitiveGroupNames = caseInsensitiveGroupNames;
	// }

	/**
	 * @return the internal {@link java.util.regex.Matcher} used by this
	 *         matcher.
	 */
	// public java.util.regex.Matcher getInternalMatcher()
	// {
	// return internalMatcher;
	// }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pattern pattern() {
		return this.parentPattern;
	}

	/**
	 * Returns the match state of this matcher as a {@link MatchResult}.
	 * The result is unaffected by subsequent operations performed upon this
	 * matcher.
	 *
	 * @return a <code>MatchResult</code> with the state of this matcher
	 */
	public MatchResult toMatchResult() {
		if (this.isMatchResult()) {
			return this;
		}

		Matcher matcher = this.clone();
		matcher.isMatchResult(true);

		return matcher;
	}

	@Override
	public Matcher clone() {
		return new Matcher(
				(java.util.regex.Matcher) this.usedMatcher.toMatchResult(),
				this.parentPattern, this.text.toString()).treatNullAsEmptyString(this.treatNullAsEmptyString);
	}

	/**
	 * @return <code>true</code> if this object is a match result
	 * @since 0.2
	 */
	public boolean isMatchResult() {
		return this.isMatchResult;
	}

	/**
	 *
	 * @param isMatchResult <code>true</code> if this object is a match result
	 * @since 0.2
	 */
	private Matcher isMatchResult(final boolean isMatchResult) {
		this.isMatchResult = isMatchResult;
		return this;
	}

	/**
	 * Changes the <tt>Pattern</tt> that this <tt>Matcher</tt> uses to find
	 * matches with.
	 *
	 * <p>This method causes this matcher to lose information about the groups
	 * of the last match that occurred. The matcher's position in the input is
	 * maintained and its last append position is unaffected.</p>
	 *
	 * @param newPattern
	 *            The new pattern used by this matcher
	 * @return This matcher
	 * @throws IllegalArgumentException
	 *             If newPattern is <tt>null</tt>
	 */
	public Matcher usePattern(final Pattern newPattern) {
		if (newPattern == null) {
			throw new IllegalArgumentException("Pattern cannot be null");
		}

		java.util.regex.Pattern pattern = newPattern.getInternalPattern();

		this.internalMatcher.usePattern(pattern);

		if (this.usedMatcher != this.internalMatcher) {
			this.usedMatcher.usePattern(pattern);
		}

		this.parentPattern = newPattern;
		// this.caseInsensitiveGroupNames =
		// newPattern.has(CASE_INSENSITIVE_NAMES);
		return this;
	}

	/**
	 * Resets this matcher.
	 *
	 * <p>Resetting a matcher discards all of its explicit state information and
	 * sets its append position to zero. The matcher's region is set to the
	 * default region, which is its entire character sequence. The anchoring and
	 * transparency of this matcher's region boundaries are unaffected.</p>
	 *
	 * @return This matcher
	 */
	public Matcher reset() {
		this.internalMatcher.reset();
		return this.resetPrivate();
	}

	/**
	 * Resets this matcher with a new input sequence.
	 *
	 * <p>Resetting a matcher discards all of its explicit state information and
	 * sets its append position to zero. The matcher's region is set to the
	 * default region, which is its entire character sequence. The anchoring and
	 * transparency of this matcher's region boundaries are unaffected.</p>
	 *
	 * @param input
	 *            The new input character sequence
	 *
	 * @return This matcher
	 */
	public Matcher reset(final CharSequence input) {
		this.internalMatcher.reset(input);
		this.text = input;
		return this.resetPrivate();
	}

	/**
	 * Resets the fields in this class.
	 *
	 * @return this matcher
	 */
	private Matcher resetPrivate() {
		this.lastAppendPosition = 0;
		this.usedMatcher = this.internalMatcher;
		return this;
	}

	/**
	 * Returns the start index of the previous match.
	 *
	 * @return The index of the first character matched
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 */
	@Override
	public int start() {
		return this.usedMatcher.start();
	}

	/**
	 * Returns the start index of the subsequence captured by the given group
	 * during the previous match operation.
	 *
	 * <p><a href="Pattern.html#cg">Capturing groups</a> are indexed
	 * from left to right, starting at one. Group zero denotes the entire
	 * pattern, so the expression <i>m.</i><tt>start(0)</tt> is equivalent to
	 * <i>m.</i><tt>start()</tt>.</p>
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 *
	 * @return The index of the first character captured by the group, or
	 *         <tt>-1</tt> if the match was successful but the group itself did
	 *         not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 */
	@Override
	public int start(final int group) {
		return this.usedMatcher.start(this.getGroupIndex(group));
	}

	/**
	 * Returns the start index of the subsequence captured by the given
	 * <a href="Pattern.html#group">group</a> during the previous match
	 * operation.
	 *
	 * @param group
	 *            A capturing group in this matcher's pattern
	 *
	 * @return The index of the first character captured by the group, or
	 *         <tt>-1</tt> if the match was successful but the group itself did
	 *         not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public int start(final String group) {
		return this.usedMatcher.start(this.getGroupIndex(group));
	}

	/**
	 * Returns the start index of the subsequence captured by the given
	 * <a href="Pattern.html#group">group</a> during the previous match
	 * operation.
	 *
	 * <p>An invocation of this convenience method of the form</p>
	 *
	 * <blockquote><pre>
	 * m.start(groupName, occurrence)</pre></blockquote>
	 *
	 * <p>behaves in exactly the same way as</p>
	 *
	 * <blockquote><pre>
	 * m.start(groupName + "[" + occurrence + "]")</pre></blockquote>
	 *
	 * @param groupName
	 *            The group name for a capturing group in this matcher's pattern
	 *
	 * @param occurrence
	 *            The occurrence of the specified group name
	 *
	 * @return The index of the first character captured by the group, or
	 *         <tt>-1</tt> if the match was successful but the group itself did
	 *         not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public int start(final String groupName, final int occurrence) {
		return this.usedMatcher.start(this.getGroupIndex(groupName, occurrence));
	}

	/**
	 * Returns the offset after the last character matched.
	 *
	 * @return The offset after the last character matched
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 */
	@Override
	public int end() {
		return this.usedMatcher.end();
	}

	/**
	 * Returns the offset after the last character of the subsequence captured
	 * by the given group during the previous match operation.
	 *
	 * <p><a href="Pattern.html#cg">Capturing groups</a> are indexed
	 * from left to right, starting at one. Group zero denotes the entire
	 * pattern, so the expression <i>m.</i><tt>end(0)</tt> is equivalent to
	 * <i>m.</i><tt>end()</tt>.</p>
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 *
	 * @return The offset after the last character captured by the group, or
	 *         <tt>-1</tt> if the match was successful but the group itself did
	 *         not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 */
	@Override
	public int end(final int group) {
		return this.usedMatcher.end(this.getGroupIndex(group));
	}

	/**
	 * Returns the offset after the last character of the subsequence captured
	 * by the given <a href="Pattern.html#group">group</a> during the previous
	 * match operation.
	 *
	 * @param group
	 *            A capturing group in this matcher's pattern
	 *
	 * @return The offset after the last character captured by the group, or
	 *         <tt>-1</tt> if the match was successful but the group itself did
	 *         not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public int end(final String group) {
		return this.usedMatcher.end(this.getGroupIndex(group));
	}

	/**
	 * Returns the offset after the last character of the subsequence captured
	 * by the given <a href="Pattern.html#group">group</a> during the previous
	 * match operation.
	 *
	 * <p>An invocation of this convenience method of the form</p>
	 *
	 * <blockquote><pre>
	 * m.end(groupName, occurrence)</pre></blockquote>
	 *
	 * <p>behaves in exactly the same way as</p>
	 *
	 * <blockquote><pre>
	 * m.end(groupName + "[" + occurrence + "]")</pre></blockquote>
	 *
	 * @param groupName
	 *            The group name for a capturing group in this matcher's pattern
	 *
	 * @param occurrence
	 *            The occurrence of the specified group name
	 *
	 * @return The offset after the last character captured by the group, or
	 *         <tt>-1</tt> if the match was successful but the group itself did
	 *         not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public int end(final String groupName, final int occurrence) {
		return this.usedMatcher.end(this.getGroupIndex(groupName, occurrence));
	}

	/**
	 * Returns the occurrence of the first <i>matched</i> group with the given
	 * index.
	 *
	 * <p><a href="Pattern.html#branchreset">Branch reset</a> patterns allow
	 * multiple capture groups with the same group index to exist.
	 * This method offers a way to determine which occurrence matched.</p>
	 *
	 * @param groupIndex
	 *            the name of the group
	 *
	 * @return the occurrence for the first <i>matched</i> group with the given
	 *         index
	 *
	 */
	@Override
	public int occurrence(final int groupIndex) {
		try {
			String groupName = wrapIndex(getAbsoluteGroupIndex(groupIndex,
					this.groupCount()));

			if (this.groupCount(groupName) == 0) {
				throw noGroup(groupIndex);
			}

			return this.occurrence(groupName);
		} catch (IndexOutOfBoundsException e) {
			throw noGroup(groupIndex);
		}
	}

	/**
	 * Returns the occurrence of the first <i>matched</i> group with the given
	 * name.
	 *
	 * <p><a href="Pattern.html#branchreset">Branch reset</a> patterns and the {@link Pattern#DUPLICATE_NAMES} flag
	 * allow multiple capture groups with the same group name to exist.
	 * This method offers a way to determine which occurrence matched.</p>
	 *
	 * @param groupName
	 *            the name of the group
	 *
	 * @return the occurrence of the first <i>matched</i> group with the given
	 *         name.
	 *
	 */
	@Override
	public int occurrence(final String groupName) {
		int groupCount = this.groupCount(groupName);

		if (groupCount == 0) {
			throw noNamedGroup(groupName);
		}

		int occurrence = 0;
		Integer groupIndexI;

		while ((groupIndexI = this.parentPattern.getMappedIndex(groupName,
				++occurrence)) != null) {
			// if matched group

			if (this.usedMatcher.start(groupIndexI) != -1) {
				return occurrence;
			}
		}

		// if no group matched anything (i.e. all null)
		return -1;
	}

	/**
	 * Returns the input subsequence matched by the previous match.
	 *
	 * <p>For a matcher <i>m</i> with input sequence <i>s</i>, the expressions
	 * <i>m.</i><tt>group()</tt> and <i>s.</i><tt>substring(</tt><i>m.</i>
	 * <tt>start(),</tt>&nbsp;<i>m.</i><tt>end())</tt> are equivalent.</p>
	 *
	 * <p>Note that some patterns, for example <tt>a*</tt>, match the empty
	 * string. This method will return the empty string when the pattern
	 * successfully matches the empty string in the input.</p>
	 *
	 * @return The (possibly empty) subsequence matched by the previous match,
	 *         in string form
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 */
	@Override
	public String group() {
		return this.groupPrivate(0);
	}

	/**
	 * Returns the input subsequence captured by the given group during the
	 * previous match operation.
	 *
	 * <p>For a matcher <i>m</i>, input sequence <i>s</i>, and group index
	 * <i>g</i>, the expressions <i>m.</i><tt>group(</tt><i>g</i><tt>)</tt> and
	 * <i>s.</i><tt>substring(</tt><i>m.</i><tt>start(</tt><i>g</i><tt>),</tt>
	 * &nbsp;<i>m.</i><tt>end(</tt><i>g</i><tt>))</tt> are equivalent.</p>
	 *
	 * <p><a href="Pattern.html#cg">Capturing groups</a> are indexed
	 * from left to right, starting at one. Group zero denotes the entire
	 * pattern, so the expression <tt>m.group(0)</tt> is equivalent to
	 * <tt>m.group()</tt>.</p>
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>null</tt> is returned. Note that
	 * some groups, for example <tt>(a*)</tt>, match the empty string. This
	 * method will return the empty string when such a group successfully
	 * matches the empty string in the input.</p>
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 *
	 * @return The (possibly empty) subsequence captured by the group during the
	 *         previous match, or <tt>null</tt> if the group failed to match
	 *         part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 */
	@Override
	public String group(final int group) {
		return this.groupPrivate(this.getGroupIndex(group));
	}

	/**
	 * Returns the input subsequence captured by the given
	 * <a href="Pattern.html#group">group</a> during the previous match
	 * operation.
	 *
	 * <p><a href="Pattern.html#cg">Capturing groups</a> are indexed
	 * from left to right, starting at one. Group zero denotes the entire
	 * pattern, so the expression <tt>m.group(0, null)</tt> is equivalent to
	 * <tt>m.group()</tt>.</p>
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>defaultValue</tt> is returned,
	 * whereas {@link #group(int)} would return <code>null</code>. Otherwise,
	 * the return is equivalent to that of <i>m.</i><tt>group(group)</tt>.</p>
	 *
	 * <p>As a note,</p>
	 *
	 * <blockquote><pre>m.group(group, null)</pre></blockquote>
	 *
	 * <p>behaves in exactly the same way as</p>
	 *
	 * <blockquote><pre>m.group(group)</pre></blockquote>
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 *
	 * @param defaultValue
	 *            The string to return if {@link #group(int)} would return
	 *            <code>null</code>
	 *
	 * @return The (possibly empty) subsequence captured by the group during the
	 *         previous match, or <tt>defaultValue</tt> if the group failed to
	 *         match part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 *
	 * @see #group(int)
	 */
	@Override
	public String group(final int group, final String defaultValue) {
		return this.groupPrivate(this.getGroupIndex(group), defaultValue);
	}

	/**
	 * Returns the input subsequence captured by the given
	 * <a href="Pattern.html#group">group</a> during the previous match
	 * operation.
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>null</tt> is returned. Note that
	 * some groups, for example <tt>(a*)</tt>, match the empty string. This
	 * method will return the empty string when such a group successfully
	 * matches the empty string in the input.</p>
	 *
	 * @param group
	 *            A capturing group in this matcher's pattern
	 *
	 * @return The (possibly empty) subsequence captured by the group during the
	 *         previous match, or <tt>null</tt> if the group failed to match
	 *         part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public String group(final String group) {
		return this.groupPrivate(this.getGroupIndex(group));
	}

	/**
	 * Returns the input subsequence captured by the given
	 * <a href="Pattern.html#group">group</a> during the previous match
	 * operation.
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>defaultValue</tt> is returned,
	 * whereas {@link #group(String)} would return <code>null</code>. Otherwise,
	 * the return is equivalent to that of <i>m.</i><tt>group(group)</tt>.</p>
	 *
	 * <p>As a note,</p>
	 *
	 * <blockquote><pre>m.group(group, null)</pre></blockquote>
	 *
	 * <p>behaves in exactly the same way as</p>
	 *
	 * <blockquote><pre>m.group(group)</pre></blockquote>
	 *
	 * @param group
	 *            A capturing group in this matcher's pattern
	 *
	 * @param defaultValue
	 *            The string to return if {@link #group(String)} would return
	 *            <code>null</code>
	 *
	 * @return The (possibly empty) subsequence captured by the group during the
	 *         previous match, or <tt>defaultValue</tt> if the group failed to
	 *         match part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 *
	 * @see #group(String)
	 */
	@Override
	public String group(final String group, final String defaultValue) {
		return this.groupPrivate(this.getGroupIndex(group), defaultValue);
	}

	/**
	 * Returns the input subsequence captured by the given
	 * <a href="Pattern.html#group">group</a> during the previous match
	 * operation.
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>null</tt> is returned. Note that
	 * some groups, for example <tt>(a*)</tt>, match the empty string. This
	 * method will return the empty string when such a group successfully
	 * matches the empty string in the input.</p>
	 *
	 * <p>An invocation of this convenience method of the form</p>
	 *
	 * <blockquote><pre>m.group(groupName, occurrence)</pre></blockquote>
	 *
	 * <p>behaves in exactly the same way as</p>
	 *
	 * <blockquote><pre>
	 * m.group(groupName + "[" + occurrence + "]")</pre></blockquote>
	 *
	 * @param groupName
	 *            The group name for a capturing group in this matcher's pattern
	 *
	 * @param occurrence
	 *            The occurrence of the specified group name
	 *
	 * @return The (possibly empty) subsequence captured by the group during the
	 *         previous match, or <tt>null</tt> if the group failed to match
	 *         part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public String group(final String groupName, final int occurrence) {
		return this.groupPrivate(this.getGroupIndex(groupName, occurrence));
	}

	/**
	 * Returns the input subsequence captured by the given
	 * <a href="Pattern.html#group">group</a> during the previous match
	 * operation.
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>defaultValue</tt> is returned,
	 * whereas {@link #group(String, int)} would return <code>null</code>.
	 * Otherwise, the return is equivalent to that of
	 * <i>m.</i><tt>group(groupName, occurrence)</tt>.</p>
	 *
	 * <p>As a note,</p>
	 *
	 * <blockquote><pre>m.group(groupName, occurrence, null)</pre></blockquote>
	 *
	 * <p>behaves in exactly the same way as</p>
	 *
	 * <blockquote><pre>m.group(groupName, occurrence)</pre></blockquote><br>
	 *
	 * <p>An invocation of this convenience method of the form</p>
	 *
	 * <blockquote><pre>
	 * m.group(groupName, occurrence, defaultValue)</pre></blockquote>
	 *
	 * <p>behaves in exactly the same way as</p>
	 *
	 * <blockquote><pre>
	 * m.group(groupName + "[" + occurrence + "]", defaultValue)</pre>
	 * </blockquote>
	 *
	 * @param groupName
	 *            The group name for a capturing group in this matcher's pattern
	 *
	 * @param occurrence
	 *            The occurrence of the specified group name
	 *
	 * @param defaultValue
	 *            The string to return if {@link #group(String, int)} would
	 *            return <code>null</code>
	 *
	 * @return The (possibly empty) subsequence captured by the group during the
	 *         previous match, or <tt>defaultValue</tt> if the group failed to
	 *         match part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 *
	 * @see #group(String, int)
	 */
	@Override
	public String group(final String groupName, final int occurrence, final String defaultValue) {
		return this.groupPrivate(this.getGroupIndex(groupName, occurrence), defaultValue);
	}

	/**
	 * Returns the captured contents of the group with the given index.
	 *
	 * @param mappedIndex
	 *            the index for the group (in the internal matcher) whose
	 *            contents are returned
	 * @return the captured contents of the group with the given index
	 * @see java.util.regex.Matcher#group(int)
	 */
	private String groupPrivate(final int mappedIndex) {
		String value = this.usedMatcher.group(mappedIndex);

		return value == null && this.treatNullAsEmptyString ? "" : value;
	}

	private String groupPrivate(final int mappedIndex, final String defaultValue) {
		boolean matched = this.usedMatcher.start(mappedIndex) != -1;

		return (matched ? this.usedMatcher.group(mappedIndex) : defaultValue);
	}

	/**
	 * Returns the number of capturing groups in this matcher's pattern.
	 *
	 * <p>Group zero denotes the entire pattern by convention. It is not
	 * included in this count.
	 *
	 * <p>Any non-negative integer smaller than or equal to the value returned
	 * by this method is guaranteed to be a valid group index for this
	 * matcher.</p>
	 *
	 * @return The number of capturing groups in this matcher's pattern
	 */
	@Override
	public int groupCount() {
		return this.parentPattern.groupCount();
	}

	/**
	 * Returns the number of capturing groups (with the given group index) in
	 * this matcher's pattern.
	 *
	 * <p><b>Note</b>: in most cases, this return will be 1 - the only exception
	 * is in the case
	 * of a "branch reset" pattern, where there may be multiple groups with the
	 * same group index.
	 *
	 * <p>For example, </p>
	 * <blockquote><pre><code><span style="color: green"
	 * >// Outputs 2, since there are two groups <!--
	 * -->that have the group index of 1</span>
	 * System.out.println(Pattern.compile("(?|(1a)|(1b))").groupCount(1));</code
	 * ></pre>
	 * </blockquote>
	 *
	 * <p>Group zero denotes the entire pattern by convention. It is not
	 * included in this count.</p>
	 *
	 * <p>Any non-negative integer smaller than or equal to the value returned
	 * by this method is guaranteed to be a valid occurrence (for a <a
	 * href="#group">group</a>,
	 * <i>groupName</i><tt>[</tt><i>occurrence</i><tt>]</tt>) for this
	 * matcher.</p>
	 *
	 * <p><b>Note</b>: unlike other methods, this
	 * method doesn't throw an exception if the specified group doesn't exist.
	 * Instead, zero is returned, since the number of groups with the
	 * (non-existent) group name is zero.
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 *
	 * @return The number of capturing groups (with the given group index) in
	 *         this matcher's pattern
	 * @since 0.2
	 */
	@Override
	public int groupCount(final int group) {
		return this.parentPattern.groupCount(group);
	}

	/**
	 * Returns the number of capturing groups (with the given group name) in
	 * this matcher's pattern.
	 *
	 * <p>Group zero denotes the entire pattern by convention. It is not
	 * included in this count.</p>
	 *
	 * <p>Any non-negative integer smaller than or equal to the value returned
	 * by this method is guaranteed to be a valid occurrence (for a group,
	 * <i>groupName</i><tt>[</tt><i>occurrence</i><tt>]</tt>) for this matcher.
	 * </p>
	 *
	 * <p>If <code>groupName</code> is the empty string, this method's return is
	 * equal to the return from {@link #groupCount()}.</p>
	 *
	 * <p><b>Note</b>: unlike other methods, this
	 * method doesn't throw an exception if the specified group doesn't exist.
	 * Instead, zero is returned, since the number of groups with the given
	 * (non-existent) group name is zero.</p>
	 *
	 * @param groupName
	 *            The group name for a capturing group in this matcher's pattern
	 *
	 * @return The number of capturing groups (with the given group name) in
	 *         this matcher's pattern
	 */
	@Override
	public int groupCount(final String groupName) {
		return this.parentPattern.groupCount(groupName);
	}

	/**
	 * Indicates whether this matcher has any capturing groups.
	 *
	 * @return <code>true</code> if this matcher has at least one capturing group; otherwise, <code>false</code>
	 *
	 * @since 0.2
	 */
	@Override
	public boolean hasGroup() {
		// return groupCount() > 0;
		return this.parentPattern.hasGroup();
	}

	/**
	 * Indicates whether this matcher contains the specified group.
	 *
	 * @param group
	 *            The group index for a capturing group in this matcher's pattern
	 * @return <code>true</code> if this matcher contains the specified group; otherwise, <code>false</code>.
	 *
	 * @since 0.2
	 */
	@Override
	public boolean hasGroup(final int group) {
		// return containsKey(group);
		// return groupCount(group) > 0;
		return this.parentPattern.hasGroup(group);
	}

	/**
	 * Indicates whether this matcher contains the specified group.
	 *
	 * @param group
	 *            A capturing group in this matcher's pattern
	 * @return <code>true</code> if this matcher contains the specified group; otherwise, <code>false</code>.
	 *
	 * @since 0.2
	 */
	@Override
	public boolean hasGroup(final String group) {
		// return containsKey(group);
		return this.parentPattern.hasGroup(group);
	}

	/**
	 * Indicates whether this matcher contains the specified group.
	 *
	 * @param groupName
	 *            The group name for a capturing group in this matcher's pattern
	 *
	 * @param occurrence
	 *            The occurrence of the specified group name
	 * @return <code>true</code> if this matcher contains the specified group; otherwise, <code>false</code>.
	 *
	 * @since 0.2
	 */
	@Override
	public boolean hasGroup(final String groupName, final int occurrence) {
		// return hasGroup(groupName + wrapIndex(occurrence));
		return this.parentPattern.hasGroup(groupName, occurrence);
	}

	/**
	 * Attempts to match the entire region against the pattern.
	 *
	 * <p>If the match succeeds then more information can be obtained via the
	 * <tt>start</tt>, <tt>end</tt>, and <tt>group</tt> methods.</p>
	 *
	 * @return <tt>true</tt> if, and only if, the entire region sequence matches
	 *         this matcher's pattern
	 */
	public boolean matches() {
		// TODO: use internalMatcher or useMatcher ??
		return this.internalMatcher.matches();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean matched() {
		try {
			return this.start() != -1;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 */
	@Override
	public boolean matched(final int group) {
		return this.matched(group, true);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If <code>validateGroup</code> is <code>true</code> and there is no capturing group in the pattern
	 *             with the given index
	 */
	@Override
	public boolean matched(final int group, final boolean validateGroup) {
		try {
			return this.start(group) != -1;
		} catch (IndexOutOfBoundsException e) {
			if (validateGroup) {
				throw e;
			}

			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public boolean matched(final String group) {
		return this.matched(group, true);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If <code>validateGroup</code> is <code>true</code> and there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public boolean matched(final String group, final boolean validateGroup) {
		try {
			return this.start(group) != -1;
		} catch (IllegalArgumentException e) {
			if (validateGroup) {
				throw e;
			}

			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public boolean matched(final String groupName, final int occurrence) {
		return this.matched(groupName, occurrence, true);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If <code>validateGroup</code> is <code>true</code> and there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public boolean matched(final String groupName, final int occurrence, final boolean validateGroup) {
		try {
			return this.start(groupName, occurrence) != -1;
		} catch (IllegalArgumentException e) {
			if (validateGroup) {
				throw e;
			}

			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 */
	@Override
	public boolean isEmpty() {
		return this.isEmptyPrivate(0);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 */
	@Override
	public boolean isEmpty(final int group) {
		return this.isEmptyPrivate(this.getGroupIndex(group));
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public boolean isEmpty(final String group) {
		return this.isEmptyPrivate(this.getGroupIndex(group));
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	@Override
	public boolean isEmpty(final String groupName, final int occurrence) {
		return this.isEmptyPrivate(this.getGroupIndex(groupName, occurrence));
	}

	private boolean isEmptyPrivate(final int mappedIndex) {
		int start = this.usedMatcher.start(mappedIndex);
		int end = this.usedMatcher.end(mappedIndex);

		// length zero match, and group actually matched (start != -1)
		return start == end && start != -1;
	}

	/**
	 * Attempts to find the next subsequence of the input sequence that matches
	 * the pattern.
	 *
	 * <p>This method starts at the beginning of this matcher's region, or, if a
	 * previous invocation of the method was successful and the matcher has not
	 * since been reset, at the first character not matched by the previous
	 * match.</p>
	 *
	 * <p>If the match succeeds then more information can be obtained via the
	 * <tt>start</tt>, <tt>end</tt>, and <tt>group</tt> methods.</p>
	 *
	 * @return <tt>true</tt> if, and only if, a subsequence of the input
	 *         sequence matches this matcher's pattern
	 */
	public boolean find() {
		// if (originalMatch != null)
		// {
		// internalMatcher.region(originalMatch.regionStart(), originalMatch
		// .regionEnd());
		//
		// originalMatch = null;
		// }

		boolean found;

		if (this.usedMatcher != this.internalMatcher) {
			// System.out.println("Test: " + useMatcher.end());
			found = this.internalMatcher.find(this.usedMatcher.end());
			this.usedMatcher = this.internalMatcher;
		} else {
			found = this.internalMatcher.find();
		}

		// boolean found = useMatcher.find();

		// lastResult = (java.util.regex.Matcher)
		// internalMatcher.toMatchResult();

		// lastResult = (java.util.regex.Matcher) useMatcher.toMatchResult();
		// originalResult = lastResult;
		// checkShorter = true;

		return found;
	}

	// private java.util.regex.Matcher originalResult;
	// private java.util.regex.Matcher lastResult = null;
	// private boolean checkShorter = true;

	// TODO: can find each matching string
	// cannot find each combination of group captures, for the same string
	// public boolean proceed()
	// {
	// // TODO: optimize finding of next match
	// // Ensure original matcher's region is obeyed
	//
	// // TODO: use JRegex as tester
	// // Note: sometimes it doesn't mimic Java, so use best judgement
	// int start;
	//
	// if (lastResult != null) {
	// if (useMatcher == internalMatcher)
	// useMatcher = parentPattern.getInternalPattern().matcher(text);
	//
	// if (checkShorter) {
	// /* Proceed to find all shorter matches */
	// int matchStart = lastResult.start();
	// int matchEnd = lastResult.end();
	//
	// while (matchStart != matchEnd) {
	// useMatcher.region(matchStart, --matchEnd);
	//
	// if (useMatcher.matches()) {
	// lastResult = (java.util.regex.Matcher) useMatcher.toMatchResult();
	// return true;
	// }
	// }
	//
	// checkShorter = false;
	// lastResult = originalResult;
	// }
	//
	// if (!checkShorter) {/* Proceed to find all longer matches */
	// int matchStart = lastResult.start();
	// int matchEnd = lastResult.end();
	// int regionEnd = originalResult.regionEnd();
	//
	// while (matchEnd != regionEnd) {
	// useMatcher.region(matchStart, ++matchEnd);
	//
	// if (useMatcher.matches()) {
	// lastResult = (java.util.regex.Matcher) useMatcher.toMatchResult();
	// return true;
	// }
	// }
	// }
	//
	// // internalMatcher.region(originalResult.regionStart(), originalResult
	// // .regionEnd());
	// // internalMatcher.region(internalMatcher.regionStart(), internalMatcher
	// // .regionEnd());
	// start = originalResult.start() + 1;
	// useMatcher = internalMatcher;
	// } else
	// start = 0;
	//
	// if (start > text.length())
	// return false;
	//
	// // No other match available, find next
	//
	// // TODO: move position to <start>, don't reset matcher
	// boolean found = useMatcher.find(start);
	//
	// lastResult = (java.util.regex.Matcher) useMatcher.toMatchResult();
	// originalResult = lastResult;
	// checkShorter = true;
	//
	// return found;
	// }

	/**
	 * Resets this matcher and then attempts to find the next subsequence of the
	 * input sequence that matches the pattern, starting at the specified index.
	 *
	 * <p>If the match succeeds then more information can be obtained via the
	 * <tt>start</tt>, <tt>end</tt>, and <tt>group</tt> methods, and subsequent
	 * invocations of the {@link #find()} method will start at the first
	 * character not matched by this match.</p>
	 *
	 * @param start
	 *            0-based start index
	 *
	 * @throws IndexOutOfBoundsException
	 *             If start is less than zero or if start is greater than the
	 *             length of the input sequence.
	 *
	 * @return <tt>true</tt> if, and only if, a subsequence of the input
	 *         sequence starting at the given index matches this matcher's
	 *         pattern
	 */
	public boolean find(final int start) {
		// TODO: handle case useMatcher != internalMatcher

		// boolean find = this.internalMatcher.find(start);
		boolean find = this.usedMatcher.find(start);
		this.resetPrivate();

		// TODO: before or after resetPrivate() ??
		// lastMatch = (Matcher) toMatchResult();
		// originalMatch = lastMatch;
		// checkShorter = true;

		return find;
	}

	/**
	 * Attempts to match the input sequence, starting at the beginning of the
	 * region, against the pattern.
	 *
	 * <p>Like the {@link #matches matches} method, this method always starts at
	 * the beginning of the region; unlike that method, it does not require that
	 * the entire region be matched.</p>
	 *
	 * <p>If the match succeeds then more information can be obtained via the
	 * <tt>start</tt>, <tt>end</tt>, and <tt>group</tt> methods.</p>
	 *
	 * @return <tt>true</tt> if, and only if, a prefix of the input sequence
	 *         matches this matcher's pattern
	 */
	public boolean lookingAt() {
		return this.internalMatcher.lookingAt();
	}

	/**
	 * Returns a literal replacement <code>String</code> for the specified
	 * <code>String</code>.
	 *
	 * <p>This method produces a <code>String</code> that will work as a literal
	 * replacement <code>s</code> in the <code>appendReplacement</code> method
	 * of the {@link Matcher} class. The <code>String</code> produced will match
	 * the sequence of characters in <code>s</code> treated as a literal
	 * sequence. Slashes ('\') and dollar signs ('$') will be given no special
	 * meaning.</p>
	 *
	 * @param s
	 *            The string to be literalized
	 * @return A literal string replacement
	 */
	public static String quoteReplacement(final String s) {
		return java.util.regex.Matcher.quoteReplacement(s);
	}

	private final java.util.regex.Pattern replacementPart = java.util.regex.Pattern
			.compile("\\G(?:(-?\\d++)|" + fullGroupName + ")\\}");

	/**
	 * <p>Gets the replacement string, replacing any group references with their actual value</p>
	 *
	 * <p>The replacement string may contain references to subsequences captured
	 * during the previous match: Each occurrence of <tt>$</tt><i>g</i>
	 * will be replaced by the result of evaluating
	 *
	 * {@link #group(int) group}<tt>(</tt><i>g</i><tt>)</tt>. The first number
	 * after the <tt>$</tt> is always treated as part of the group reference.
	 * Subsequent numbers are incorporated into g if they would form a legal
	 * group reference. Only the numerals '0' through '9' are considered as
	 * potential components of the group reference. If the second group matched
	 * the string <tt>"foo"</tt>, for example, then passing the replacement
	 * string <tt>"$2bar"</tt> would cause <tt>"foobar"</tt> to be appended to
	 * the string buffer. A dollar sign (<tt>$</tt>) may be included as a
	 * literal in the replacement string by preceding it with a backslash
	 * (<tt>\$</tt>).</p>
	 *
	 * <p>Note that backslashes (<tt>\</tt>) and dollar signs (<tt>$</tt>) in
	 * the replacement string may cause the results to be different than if it
	 * were being treated as a literal replacement string. Dollar signs may be
	 * treated as references to captured subsequences as described above, and
	 * backslashes are used to escape literal characters in the replacement
	 * string.</p>
	 *
	 * @param replacement
	 *            The replacement string
	 *
	 * @return the replacement string replacing any group references with their group value from this Matcher
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If the replacement string refers to a named-capturing
	 *             group that does not exist in the pattern
	 *
	 * @throws IndexOutOfBoundsException
	 *             If the replacement string refers to a capturing group
	 *             that does not exist in the pattern
	 */
	public String getReplacement(final String replacement) {
		int cursor = 0;
		StringBuilder result = new StringBuilder();

		while (cursor < replacement.length()) {
			char nextChar = replacement.charAt(cursor);

			if (nextChar == '\\') {
				cursor++;
				nextChar = replacement.charAt(cursor);
				result.append(nextChar);
				cursor++;
			} else if (nextChar == '$') {
				// Skip past $
				cursor++;

				// A StringIndexOutOfBoundsException is thrown if
				// this "$" is the last character in replacement
				// string in current implementation, a IAE might be
				// more appropriate.
				nextChar = replacement.charAt(cursor);

				int mappedIndex;

				if (nextChar == '<') {
					// e.g. $<name>
					// (Java's named group reference)

					cursor++;
					StringBuilder gsb = new StringBuilder();
					while (cursor < replacement.length()) {
						nextChar = replacement.charAt(cursor);

						if (nextChar >= 'a' && nextChar <= 'z' ||
								nextChar >= 'A' && nextChar <= 'Z' ||
								nextChar >= '0' && nextChar <= '9') {
							gsb.append(nextChar);
							cursor++;
						} else {
							break;
						}
					}
					if (gsb.length() == 0) {
						throw new IllegalArgumentException(
								"named capturing group has 0 length name");
					}
					if (nextChar != '>') {
						throw new IllegalArgumentException(
								"named capturing group is missing trailing '>'");
					}
					String gname = gsb.toString();
					String mappingName = getMappingName(gname, 1);
					if (!this.parentPattern.getGroupMapping()
							.containsKey(
									mappingName)) {
						throw new IllegalArgumentException(
								"No group with name <" + gname + ">");
					}
					mappedIndex = this.parentPattern.getGroupMapping()
							.get(
									mappingName);
					cursor++;
				} else if (nextChar == '{') {
					// TODO: don't create a matcher - performance hit
					java.util.regex.Matcher matcher = this.replacementPart
							.matcher(replacement);

					if (!matcher.find(++cursor)) {
						throw new IllegalArgumentException(
								"Illegal group reference");
					}

					String numberGroup = matcher.group(1);
					String match = matcher.group();

					if (!match.endsWith("}")) {
						throw new IllegalArgumentException(
								"named capturing group is missing trailing '}'");
					}

					cursor += match.length();

					// Get mapped index
					// String refGroupName = refGroup.toString();

					// if (false) {
					// if (isNum && refGroupName.length() != 0) {
					// int refNum;
					// try {
					// refNum = Integer.parseInt(refGroupName);
					// } catch (NumberFormatException e) {
					// throw new IllegalArgumentException(
					// "Illegal group reference");
					// }
					//
					// mappedIndex = getGroupIndex(refNum);
					// } else {
					// mappedIndex = getGroupIndex(refGroupName);

					if (numberGroup != null) {
						int groupIndex = Integer.parseInt(numberGroup);
						mappedIndex = this.getGroupIndex(groupIndex);
					} else {
						String groupName = matcher.group(2);

						if (groupName.length() == 0) {
							throw new IllegalArgumentException(
									"named capturing group has 0 length name");
						}

						String groupOccurrence = matcher.group(3);

						if (groupOccurrence == null) {
							mappedIndex = this.getGroupIndex(groupName);
						} else {
							int occurrence = Integer.parseInt(groupOccurrence);
							mappedIndex = this.getGroupIndex(groupName, occurrence);
						}
					}
					// }
				}
				// else if (nextChar == '$'){
				// result.append('$');
				// cursor++;
				// continue;
				// }
				else {
					// e.g $123
					// (original functionality)

					// The first number is always a group
					int refNum = nextChar - '0';
					if ((refNum < 0) || (refNum > 9)) {
						throw new IllegalArgumentException(
								"Illegal group reference");
					}

					cursor++;
					// Capture the largest legal group string
					boolean done = false;
					while (!done) {
						if (cursor >= replacement.length()) {
							break;
						}
						int nextDigit = replacement.charAt(cursor) - '0';
						if ((nextDigit < 0) || (nextDigit > 9)) { // not a
							// number
							break;
						}
						int newRefNum = (refNum * 10) + nextDigit;
						if (this.groupCount() < newRefNum) {
							done = true;
						} else {
							refNum = newRefNum;
							cursor++;
						}
					}

					mappedIndex = this.getGroupIndex(refNum);
				}

				// Append group
				String group = this.groupPrivate(mappedIndex);

				if (group != null) {
					result.append(group);
				}
			} else {
				result.append(nextChar);
				cursor++;
			}
		}

		return result.toString();
	}

	/**
	 * Implements a non-terminal append-and-replace step.
	 *
	 * <p>This method performs the following actions:</p>
	 *
	 * <ol>
	 *
	 * <li>
	 * <p>It reads characters from the input sequence, starting at the append
	 * position, and appends them to the given string buffer. It stops after
	 * reading the last character preceding the previous match, that is, the
	 * character at index {@link #start()}&nbsp;<tt>-</tt>&nbsp;<tt>1</tt>.</p>
	 * </li>
	 *
	 * <li>
	 * <p>It appends the given replacement string to the string buffer.</p>
	 * </li>
	 *
	 * <li>
	 * <p>It sets the append position of this matcher to the index of the last
	 * character matched, plus one, that is, to {@link #end()}.</p>
	 * </li>
	 *
	 * </ol>
	 *
	 * <p>The replacement string may contain references to subsequences captured
	 * during the previous match: Each occurrence of <tt>$</tt><i>g</i>
	 * will be replaced by the result of evaluating
	 *
	 * {@link #group(int) group}<tt>(</tt><i>g</i><tt>)</tt>. The first number
	 * after the <tt>$</tt> is always treated as part of the group reference.
	 * Subsequent numbers are incorporated into g if they would form a legal
	 * group reference. Only the numerals '0' through '9' are considered as
	 * potential components of the group reference. If the second group matched
	 * the string <tt>"foo"</tt>, for example, then passing the replacement
	 * string <tt>"$2bar"</tt> would cause <tt>"foobar"</tt> to be appended to
	 * the string buffer. A dollar sign (<tt>$</tt>) may be included as a
	 * literal in the replacement string by preceding it with a backslash
	 * (<tt>\$</tt>).</p>
	 *
	 * <p>Note that backslashes (<tt>\</tt>) and dollar signs (<tt>$</tt>) in
	 * the replacement string may cause the results to be different than if it
	 * were being treated as a literal replacement string. Dollar signs may be
	 * treated as references to captured subsequences as described above, and
	 * backslashes are used to escape literal characters in the replacement
	 * string.</p>
	 *
	 * <p>This method is intended to be used in a loop together with the {@link #appendTail appendTail} and
	 * {@link #find find} methods. The
	 * following code, for example, writes <tt>one dog two dogs in the
	 * yard</tt> to the standard-outputSyntax stream:</p>
	 *
	 * <blockquote>
	 *
	 * <pre>
	 * Pattern p = Pattern.compile(&quot;cat&quot;);
	 * Matcher m = p.matcher(&quot;one cat two cats in the yard&quot;);
	 * StringBuffer sb = new StringBuffer();
	 * while (m.find()) {
	 * &nbsp;&nbsp;&nbsp;&nbsp;m.appendReplacement(sb, &quot;dog&quot;);
	 * }
	 * m.appendTail(sb);
	 * System.out.println(sb.toString());
	 * </pre>
	 *
	 * </blockquote>
	 *
	 * @param sb
	 *            The target string buffer
	 *
	 * @param replacement
	 *            The replacement string
	 *
	 * @return This matcher
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If the replacement string refers to a named-capturing
	 *             group that does not exist in the pattern
	 *
	 * @throws IndexOutOfBoundsException
	 *             If the replacement string refers to a capturing group
	 *             that does not exist in the pattern
	 */
	public Matcher appendReplacement(final StringBuffer sb, final String replacement) {
		int first = this.start();
		int last = this.end();

		// Append the intervening text
		sb.append(this.getSubSequence(this.lastAppendPosition, first));
		// Append the match substitution
		sb.append(this.getReplacement(replacement));

		this.lastAppendPosition = last;
		return this;
	}

	/**
	 * Implements a terminal append-and-replace step.
	 *
	 * <p>This method reads characters from the input sequence, starting at the
	 * append position, and appends them to the given string buffer. It is
	 * intended to be invoked after one or more invocations of the {@link #appendReplacement appendReplacement} method
	 * in order to copy the
	 * remainder of the input sequence.</p>
	 *
	 * @param sb
	 *            The target string buffer
	 *
	 * @return The target string buffer
	 */
	public StringBuffer appendTail(final StringBuffer sb) {
		sb.append(this.text, this.lastAppendPosition, this.getTextLength());
		return sb;
	}

	/**
	 * Replaces every subsequence of the input sequence that matches the pattern
	 * with the given replacement string.
	 *
	 * <p>This method first resets this matcher. It then scans the input
	 * sequence
	 * looking for matches of the pattern. Characters that are not part of any
	 * match are appended directly to the result string; each match is replaced
	 * in the result by the replacement string. The replacement string may
	 * contain references to captured subsequences as in the {@link #appendReplacement appendReplacement} method.</p>
	 *
	 * <p>Note that backslashes (<tt>\</tt>) and dollar signs (<tt>$</tt>) in
	 * the replacement string may cause the results to be different than if it
	 * were being treated as a literal replacement string. Dollar signs may be
	 * treated as references to captured subsequences as described above, and
	 * backslashes are used to escape literal characters in the replacement
	 * string.</p>
	 *
	 * <p>Given the regular expression <tt>a*b</tt>, the input
	 * <tt>"aabfooaabfooabfoob"</tt>, and the replacement string <tt>"-"</tt>,
	 * an invocation of this method on a matcher for that expression would yield
	 * the string <tt>"-foo-foo-foo-"</tt>.</p>
	 *
	 * <p>Invoking this method changes this matcher's state. If the matcher
	 * is to be used in further matching operations then it should first be
	 * reset.</p>
	 *
	 * @param replacement
	 *            The replacement string
	 *
	 * @return The string constructed by replacing each matching subsequence by
	 *         the replacement string, substituting captured subsequences as
	 *         needed
	 */
	public String replaceAll(final String replacement) {
		this.reset();
		boolean result = this.find();
		if (result) {
			StringBuffer sb = new StringBuffer();
			do {
				this.appendReplacement(sb, replacement);
				result = this.find();
			} while (result);
			this.appendTail(sb);
			return sb.toString();
		}
		return this.text.toString();
	}

	/**
	 * Replaces the first subsequence of the input sequence that matches the
	 * pattern with the given replacement string.
	 *
	 * <p>This method first resets this matcher. It then scans the input
	 * sequence looking for a match of the pattern. Characters that are not part
	 * of the match are appended directly to the result string; the match is
	 * replaced in the result by the replacement string. The replacement string
	 * may contain references to captured subsequences as in the {@link #appendReplacement appendReplacement} method.
	 *
	 * <p>Note that backslashes (<tt>\</tt>) and dollar signs (<tt>$</tt>) in
	 * the replacement string may cause the results to be different than if it
	 * were being treated as a literal replacement string. Dollar signs may be
	 * treated as references to captured subsequences as described above, and
	 * backslashes are used to escape literal characters in the replacement
	 * string.</p>
	 *
	 * <p>Given the regular expression <tt>dog</tt>, the input
	 * <tt>"zzzdogzzzdogzzz"</tt>, and the replacement string <tt>"cat"</tt>, an
	 * invocation of this method on a matcher for that expression would yield
	 * the string <tt>"zzzcatzzzdogzzz"</tt>.</p>
	 *
	 * <p>Invoking this method changes this matcher's state. If the matcher
	 * is to be used in further matching operations then it should first be
	 * reset.</p>
	 *
	 * @param replacement
	 *            The replacement string
	 * @return The string constructed by replacing the first matching
	 *         subsequence by the replacement string, substituting captured
	 *         subsequences as needed
	 */
	public String replaceFirst(final String replacement) {
		if (replacement == null) {
			throw new NullPointerException("replacement");
		}
		this.reset();
		if (!this.find()) {
			return this.text.toString();
		}
		StringBuffer sb = new StringBuffer();
		this.appendReplacement(sb, replacement);
		this.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Sets the limits of this matcher's region. The region is the part of the
	 * input sequence that will be searched to find a match. Invoking this
	 * method resets the matcher, and then sets the region to start at the index
	 * specified by the <code>start</code> parameter and end at the index
	 * specified by the <code>end</code> parameter.
	 *
	 * <p>Depending on the transparency and anchoring being used (see {@link #useTransparentBounds useTransparentBounds}
	 * and {@link #useAnchoringBounds useAnchoringBounds}), certain constructs such
	 * as anchors may behave differently at or around the boundaries of the
	 * region.</p>
	 *
	 * @param start
	 *            The index to start searching at (inclusive)
	 * @param end
	 *            The index to end searching at (exclusive)
	 * @throws IndexOutOfBoundsException
	 *             If start or end is less than zero, if start is greater than
	 *             the length of the input sequence, if end is greater than the
	 *             length of the input sequence, or if start is greater than
	 *             end.
	 * @return this matcher
	 */
	public Matcher region(final int start, final int end) {
		this.internalMatcher.region(start, end);
		return this.resetPrivate();
	}

	/**
	 * Reports the start index of this matcher's region. The searches this
	 * matcher conducts are limited to finding matches within {@link #regionStart regionStart} (inclusive) and
	 * {@link #regionEnd
	 * regionEnd} (exclusive).
	 *
	 * @return The starting point of this matcher's region
	 */
	public int regionStart() {
		return this.internalMatcher.regionStart();
	}

	/**
	 * Reports the end index (exclusive) of this matcher's region. The searches
	 * this matcher conducts are limited to finding matches within {@link #regionStart regionStart} (inclusive) and
	 * {@link #regionEnd
	 * regionEnd} (exclusive).
	 *
	 * @return the ending point of this matcher's region
	 */
	public int regionEnd() {
		return this.internalMatcher.regionEnd();
	}

	/**
	 * Queries the transparency of region bounds for this matcher.
	 *
	 * <p>This method returns <tt>true</tt> if this matcher uses
	 * <i>transparent</i> bounds, <tt>false</tt> if it uses <i>opaque</i>
	 * bounds.</p>
	 *
	 * <p>See {@link #useTransparentBounds useTransparentBounds} for a
	 * description of transparent and opaque bounds.</p>
	 *
	 * <p>By default, a matcher uses opaque region boundaries.</p>
	 *
	 * @return <tt>true</tt> iff this matcher is using transparent bounds,
	 *         <tt>false</tt> otherwise.
	 * @see Matcher#useTransparentBounds(boolean)
	 */
	public boolean hasTransparentBounds() {
		return this.internalMatcher.hasTransparentBounds();
	}

	/**
	 * Sets the transparency of region bounds for this matcher.
	 *
	 * <p>Invoking this method with an argument of <tt>true</tt> will set this
	 * matcher to use <i>transparent</i> bounds. If the boolean argument is
	 * <tt>false</tt>, then <i>opaque</i> bounds will be used.</p>
	 *
	 * <p>Using transparent bounds, the boundaries of this matcher's region are
	 * transparent to lookahead, lookbehind, and boundary matching constructs.
	 * Those constructs can see beyond the boundaries of the region to see if a
	 * match is appropriate.</p>
	 *
	 * <p>Using opaque bounds, the boundaries of this matcher's region are
	 * opaque to lookahead, lookbehind, and boundary matching constructs that
	 * may try to see beyond them. Those constructs cannot look past the
	 * boundaries so they will fail to match anything outside of the region.</p>
	 *
	 * <p>By default, a matcher uses opaque bounds.</p>
	 *
	 * @param b
	 *            a boolean indicating whether to use opaque or transparent
	 *            regions
	 * @return this matcher
	 * @see Matcher#hasTransparentBounds
	 */
	public Matcher useTransparentBounds(final boolean b) {
		this.internalMatcher.useTransparentBounds(b);
		return this;
	}

	/**
	 * Queries the anchoring of region bounds for this matcher.
	 *
	 * <p>This method returns <tt>true</tt> if this matcher uses
	 * <i>anchoring</i> bounds, <tt>false</tt> otherwise.</p>
	 *
	 * <p>See {@link #useAnchoringBounds useAnchoringBounds} for a description
	 * of anchoring bounds.</p>
	 *
	 * <p>By default, a matcher uses anchoring region boundaries.</p>
	 *
	 * @return <tt>true</tt> iff this matcher is using anchoring bounds,
	 *         <tt>false</tt> otherwise.
	 * @see Matcher#useAnchoringBounds(boolean)
	 */
	public boolean hasAnchoringBounds() {
		return this.internalMatcher.hasAnchoringBounds();
	}

	/**
	 * Sets the anchoring of region bounds for this matcher.
	 *
	 * <p>Invoking this method with an argument of <tt>true</tt> will set this
	 * matcher to use <i>anchoring</i> bounds. If the boolean argument is
	 * <tt>false</tt>, then <i>non-anchoring</i> bounds will be used.</p>
	 *
	 * <p>Using anchoring bounds, the boundaries of this matcher's region match
	 * anchors such as ^ and $.</p>
	 *
	 * <p>Without anchoring bounds, the boundaries of this matcher's region will
	 * not match anchors such as ^ and $.</p>
	 *
	 * <p>By default, a matcher uses anchoring region boundaries.</p>
	 *
	 * @param b
	 *            a boolean indicating whether or not to use anchoring bounds.
	 * @return this matcher
	 * @see Matcher#hasAnchoringBounds
	 */
	public Matcher useAnchoringBounds(final boolean b) {
		this.internalMatcher.useAnchoringBounds(b);
		return this;
	}

	/**
	 * <p>Returns the string representation of this matcher. The string
	 * representation of a <code>Matcher</code> contains information that may be
	 * useful for debugging. The exact format is unspecified.</p>
	 *
	 * @return The string representation of this matcher
	 */
	@Override
	public String toString() {
		String group;

		try {
			group = this.group();
		} catch (IllegalStateException e) {
			group = null;
		}

		StringBuffer sb = new StringBuffer();
		sb.append("info.codesaway.util.regex.Matcher");
		sb.append("[pattern=" + this.pattern());
		sb.append(" region=");
		sb.append(this.regionStart() + "," + this.regionEnd());
		sb.append(" lastmatch=");
		if (group != null) {
			sb.append(group);
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Returns true if the end of input was hit by the search engine in the last
	 * match operation performed by this matcher.
	 *
	 * <p>When this method returns true, then it is possible that more input
	 * would have changed the result of the last search.</p>
	 *
	 * @return true iff the end of input was hit in the last match; false
	 *         otherwise
	 */
	public boolean hitEnd() {
		// TOOD: which should I use ??
		// return this.internalMatcher.hitEnd();
		return this.usedMatcher.hitEnd();
	}

	/**
	 * Returns true if more input could change a positive match into a negative
	 * one.
	 *
	 * <p>If this method returns true, and a match was found, then more input
	 * could cause the match to be lost. If this method returns false and a
	 * match was found, then more input might change the match but the match
	 * won't be lost. If a match was not found, then requireEnd has no
	 * meaning.</p>
	 *
	 * @return true iff more input could change a positive match into a negative
	 *         one.
	 */
	public boolean requireEnd() {
		// TODO: which should I use ??
		// return this.internalMatcher.requireEnd();
		return this.usedMatcher.requireEnd();
	}

	/**
	 * Returns the end index of the text.
	 *
	 * @return the index after the last character in the text
	 */
	int getTextLength() {
		return this.text.length();
	}

	/**
	 * Generates a String from this Matcher's input in the specified range.
	 *
	 * @param beginIndex
	 *            the beginning index, inclusive
	 * @param endIndex
	 *            the ending index, exclusive
	 * @return A String generated from this Matcher's input
	 */
	CharSequence getSubSequence(final int beginIndex, final int endIndex) {
		return this.text.subSequence(beginIndex, endIndex);
	}

	/**
	 * Returns the string being matched.
	 *
	 * @return the string being matched
	 */
	@Override
	public String text() {
		return this.text.toString();
	}

	/**
	 * Returns the group name (if any) for the specified group.
	 *
	 * <p>If a match has been successful, this function's return is the group
	 * name associated with the given group <b>for this match</b>. This group
	 * name is match independent, except, possibly, when the group is part of a
	 * <a href="Pattern.html#branchreset">"branch reset" pattern</a>.</p>
	 *
	 * <p>If there is no successful match, this function's return is the group
	 * name, only in the case that it is match independent. The only case where
	 * the group name is not match independent is when the group is part of a
	 * "branch reset" subpattern, and there are at least two groups with the
	 * given number.</p>
	 *
	 * <p>For example, in the pattern
	 * <code>(?|(?&lt;group1a&gt;1a)|(?&lt;group1b&gt;1b)</code>, the group name
	 * for group 1 depends on whether the pattern matches "1a" or "1b". In this
	 * case, an IllegalStateException is thrown, because a match is required to
	 * determine the group name.</p>
	 *
	 * <p>If there is more than one occurrence of the group, the returned group
	 * name includes the occurrence - for example, <code>myGroup[1]</code>. If
	 * there is only one occurrence of the group, only the group name is
	 * returned - for example, <code>myGroup</code>.</p>
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 * @return the group name for the specified group, or <code>null</code> if
	 *         the group has no associated group name
	 *
	 * @throws IllegalStateException
	 *             If the group name is match dependent, and no match has yet
	 *             been attempted, or if the previous match operation failed
	 */
	@Override
	public String getGroupName(final int group) {
		Integer groupIndex = this.getGroupIndex(group);
		Map<String, Integer> groupMapping = this.parentPattern.getGroupMapping();

		for (Entry<String, Integer> entry : groupMapping.entrySet()) {
			if (entry.getValue().equals(groupIndex)) {
				if (entry.getKey().charAt(0) != '[') {
					String mappingName = entry.getKey();
					String groupName = mappingName.substring(0, mappingName
							.indexOf("["));

					int groupCount = this.groupCount(groupName);

					return groupCount == 1 ? groupName : mappingName;
				}
			}
		}

		return null;
	}

	/*
	 * Returns the given group name, adjusting the case, if necessary, based on
	 * the {@link #CASE_INSENSITIVE_NAMES} flag.
	 *
	 * @param groupName
	 * the group name
	 *
	 * @param groupOccurrence
	 * the group occurrence
	 *
	 * @param totalOccurrences
	 * the total occurrences
	 *
	 * @return the absolute occurrence
	 */
	// String handleCase(String groupName)
	// {
	// return parentPattern.handleCase(groupName);
	// }

	/**
	 * Handles errors related to specifying a non-existent group for a
	 * parameter.
	 *
	 * @param group
	 *            the index for the non-existent group
	 */
	static IndexOutOfBoundsException noGroup(final int group) {
		return new IndexOutOfBoundsException("No group " + group);
	}

	/**
	 * Handles errors related to specifying a non-existing group as parameter.
	 *
	 * @param group
	 *            the non-existent group
	 */
	static IllegalArgumentException noGroup(final String group) {
		return new IllegalArgumentException("No group <" + group + ">");
	}

	static IllegalArgumentException noGroup(final String groupName, final int occurence) {
		return noGroup(getMappingName(groupName, occurence));
	}

	/**
	 * Handles errors related to specifying a non-existent (named) group as a
	 * parameter.
	 *
	 * @param group
	 *            the non-existent group
	 */
	static IllegalArgumentException noNamedGroup(final String group) {
		return new IllegalArgumentException("No group with name <" + group +
				">");
	}

	/**
	 * Returns the absolute group for the given group.
	 *
	 * <p>If the input is negative (a relative group), then it is converted to
	 * an absolute group index.</p>
	 *
	 * @param index
	 *            the index
	 * @param groupCount
	 *            the group count
	 * @return the absolute group index
	 * @throws ArrayIndexOutOfBoundsException
	 *             If group is a relative group (that is, negative), which
	 *             doesn't refer to a valid group.
	 * @throws IndexOutOfBoundsException
	 *             If <code>index</code> in greater than <code>groupCount</code>
	 */
	static int getAbsoluteGroupIndex(int index, final int groupCount) {
		if (index < 0) {
			// group is relative
			// e.g. -1 with groupCount == 5 -> 5
			index += groupCount + 1;

			if (index <= 0) {
				throw new ArrayIndexOutOfBoundsException(
						"Index: " + index + ", Size: " + groupCount);
			}
		} else if (index > groupCount) {
			throw new IndexOutOfBoundsException(
					"Index: " + index + ", Size: " + groupCount);
		}

		return index;
	}

	/**
	 * Returns the mapped index for the specified group.
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed; only thrown if {@link #groupCount(String)
	 *             groupCount("[" + group + "]")} >= 2.
	 */
	Integer getGroupIndex(final int group) {
		try {
			String groupName = wrapIndex(getAbsoluteGroupIndex(group,
					this.groupCount()));

			return this.getGroupIndex0(groupName, "[0]");
		} catch (IndexOutOfBoundsException e) {
			throw noGroup(group);
		} catch (IllegalArgumentException e) {
			throw noGroup(group);
		}
	}

	private static String[] parseGroup(final String group) {
		String groupName;
		String occurrence;

		int bracketIndex = group.indexOf('[');

		if (bracketIndex != -1) {
			// Has bracket
			// [1], [-1], [1][1], groupName[occurrence]

			int lastBracketIndex = group.lastIndexOf('[');

			if (bracketIndex == lastBracketIndex) {
				// Has only one bracket
				// [1], [-1], groupName[occurrence]

				if (bracketIndex == 0) {
					// Starts with bracket
					// [1], [-1]

					if (!group.endsWith("]")) {
						throw noNamedGroup(group);
					}

					groupName = group;
					occurrence = null;
				} else {
					// Has bracket, not a beginning
					// groupName[occurrence]

					if (!group.endsWith("]")) {
						throw noNamedGroup(group);
					}

					groupName = group.substring(0, bracketIndex);

					// Get part between brackets
					occurrence = group.substring(bracketIndex + 1, group.length() - 1);
				}
			} else {
				// Has two brackets
				// [1][1], [1][-1]

				if (bracketIndex != 0) {
					throw noNamedGroup(group);
				}

				int closeBracket = group.indexOf(']');

				if (closeBracket != lastBracketIndex - 1) {
					throw noNamedGroup(group);
				}

				if (!group.endsWith("]")) {
					throw noNamedGroup(group);
				}

				groupName = group.substring(0, lastBracketIndex);

				if (groupName.indexOf('[', 1) != -1) {
					// Has multiple opening brackets
					// [[1]
					throw noNamedGroup(group);
				}

				occurrence = group.substring(lastBracketIndex + 1, group.length() - 1);
			}
		} else {
			// Has no bracket, just a group name
			groupName = group;
			occurrence = null;
		}

		// TODO: is there a better way to handle this case?
		if (groupName.startsWith("-")) {
			// Not allowed; for example, -1 isn't a valid group name ('-' isn't allowed in group names)
			throw noNamedGroup(group);
		}

		return new String[] { groupName, occurrence };
	}

	/**
	 * Returns the mapped index for the specified group.
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	Integer getGroupIndex(final String group) {
		// java.util.regex.Matcher matcher = fullGroupName.matcher(group);
		//
		// if (!matcher.matches())
		// throw noNamedGroup(group);
		//
		// String part1 = matcher.group(1);
		// String part2 = matcher.group(2);

		String[] parts = parseGroup(group);
		String part1 = parts[0];
		String part2 = parts[1];

		String groupName;
		try {
			groupName = this.parentPattern.normalizeGroupName(part1);
		} catch (IllegalArgumentException e) {
			throw noNamedGroup(group);
		}
		// String groupName = handleCase(matcher.group(1));
		String groupOccurrence = part2;

		if (groupOccurrence != null) {
			int occurrence;
			try {
				occurrence = getAbsoluteGroupIndex(
						parseInt(groupOccurrence),
						this.groupCount(groupName));
			} catch (IndexOutOfBoundsException e) {
				throw noGroup(group);
			} catch (IllegalArgumentException e) {
				// Changed so that when not using matcher, if the occurrence isn't a number, the correct error is thrown
				throw noNamedGroup(group);
			}

			if (groupName.length() == 0) {
				return this.getGroupIndex(occurrence);
			} else if (occurrence != 0) {
				return this.getGroupIndex(groupName, occurrence);
			}
		}

		return this.getGroupIndex0(groupName, groupOccurrence == null ? ""
				: "[" +
						groupOccurrence + "]");
	}

	/**
	 * Returns the mapped index for the specified group.
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	Integer getGroupIndex(String groupName, int occurrence) {
		// groupName = handleCase(groupName);
		try {
			groupName = this.parentPattern.normalizeGroupName(groupName);
		} catch (IllegalArgumentException e) {
			throw noGroup(groupName, occurrence);
		}

		if (groupName.length() == 0) {
			return this.getGroupIndex(occurrence);
		} else if (occurrence == 0) {
			return this.getGroupIndex0(groupName, "[0]");
		}

		int passedOccurrence = occurrence;

		try {
			occurrence = getAbsoluteGroupIndex(occurrence,
					this.groupCount(groupName));
		} catch (IndexOutOfBoundsException e) {
			throw noGroup(groupName, passedOccurrence);
		}

		Integer groupIndexI = this.parentPattern.getMappedIndex(groupName,
				occurrence);

		if (groupIndexI == null) {
			if (groupName.charAt(0) == '[' && occurrence == 1) {
				// ex. [3][1] - treat like [3]

				Integer tmp = this.parentPattern.getGroupMapping().get(groupName);

				// System.out.println(groupName + "\t" + passedOccurrence);

				if (tmp == null) {
					throw noGroup(groupName, passedOccurrence);
				}

				/*
				 * if found, return group
				 *
				 * can't be a "branch reset" pattern (-2), because if it were
				 * then the first occurrence MUST exist, and groupIndexI would
				 * not be null
				 */
				return tmp;
			}

			throw noGroup(groupName, passedOccurrence);
		}

		return groupIndexI;
	}

	/**
	 * Returns the mapped index for the first matching occurrence of the
	 * specified group name, or the first occurrence if there are no
	 * matches.
	 *
	 * @param occurrence0
	 *            string appended after group name in exception, if thrown
	 * @throws IllegalArgumentException
	 *             if the specified group never occurs
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed; only thrown if {@link #groupCount(String)
	 *             groupCount(groupName)} >= 2.
	 */
	private Integer getGroupIndex0(String groupName, final String occurrence0) {
		int groupCount = this.groupCount(groupName);

		if (groupCount == 0) {
			try {
				// Check if numbered group
				int groupNumber = Integer.parseInt(groupName);

				groupCount = this.groupCount(groupNumber);

				if (groupCount == 0) {
					throw noGroup(groupName + occurrence0);
				}

				groupName = wrapIndex(groupNumber);
			} catch (RuntimeException e) {
				if (occurrence0.length() == 0) {
					throw noNamedGroup(groupName);
				} else {
					throw noGroup(groupName + occurrence0);
				}
			}
		}

		Integer firstGroupIndex = this.parentPattern.getMappedIndex(groupName, 1);

		if (groupCount == 1) {
			return firstGroupIndex;
		}

		int occurrence = 0;
		Integer groupIndexI;

		while ((groupIndexI = this.parentPattern.getMappedIndex(groupName,
				++occurrence)) != null) {
			// if matched group

			if (this.usedMatcher.start(groupIndexI) != -1) {
				return groupIndexI;
			}
		}

		// if no group matched anything (i.e. all null)

		// return the index for the first group
		return firstGroupIndex;
	}

	/**
	 * @since 0.2
	 */
	@Override
	public boolean treatNullAsEmptyString() {
		return this.treatNullAsEmptyString;
	}

	/**
	 * @param treatNullAsEmptyString whether this Matcher should treat null as the empty string
	 * @return <code>true</code> if should treat <code>null</code> as the empty string
	 * @since 0.2
	 */
	public Matcher treatNullAsEmptyString(final boolean treatNullAsEmptyString) {
		this.treatNullAsEmptyString = treatNullAsEmptyString;
		return this;
	}

	// public boolean containsKey(Object key)
	// {
	// if (key instanceof CharSequence) {
	// try {
	// getGroupIndex(key.toString());
	// return true;
	// } catch (IllegalArgumentException e) {
	// return false;
	// }
	// } else if (key instanceof Number) {
	// Number index = (Number) key;
	//
	// try {
	// getAbsoluteGroupIndex(index.intValue(),
	// groupCount());
	// return true;
	// } catch (IndexOutOfBoundsException e) {
	// return false;
	// }
	// }
	//
	// throw new IllegalArgumentException("Invalid group name/index: "
	// + key);
	// }
	// };

	/**
	 * @throws IllegalArgumentException
	 *             If key is not a <code>CharSequence</code> or
	 *             <code>Number</code>
	 * @since 0.2
	 */
	@Override
	@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
	public boolean containsKey(final Object key) {
		if (key instanceof CharSequence) {
			try {
				this.getGroupIndex(key.toString());
				return true;
			} catch (IllegalArgumentException e) {
				return false;
			}
		} else if (key instanceof Number) {
			Number index = (Number) key;

			try {
				getAbsoluteGroupIndex(index.intValue(), this.groupCount());
				return true;
			} catch (IndexOutOfBoundsException e) {
				return false;
			}
		}

		throw new IllegalArgumentException("Requires a group name/index: " + key);
	}

	/**
	 * @since 0.2
	 */
	@Override
	public boolean containsValue(final Object value) {
		for (int i = 1; i <= this.groupCount(); i++) {
			String group = this.group(i);
			if (Objects.equals(group, value)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @since 0.2
	 */
	// private static final Comparator<Entry<? extends Integer, String>>
	// entryComparator = new Comparator<Entry<? extends Integer, String>>() {
	//
	// public int compare(Entry<? extends Integer, String> o1,
	// Entry<? extends Integer, String> o2)
	// {
	// Integer index1 = o1.getKey();
	// Integer index2 = o2.getKey();
	//
	// return index1.compareTo(index2);
	// }
	// };

	/**
	 * @since 0.2
	 */
	private final Set<Entry<Integer, String>> entrySet = new AbstractSet<Entry<Integer, String>>() {
		@Override
		public Iterator<Entry<Integer, String>> iterator() {
			return new Iterator<Entry<Integer, String>>() {
				private int next = 0;

				@Override
				public boolean hasNext() {
					return this.next < size();
				}

				@Override
				public Entry<Integer, String> next() {
					if (this.next >= size()) {
						throw new NoSuchElementException(String.valueOf(this.next));
					}

					return Matcher.this.getEntry(this.next++);
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public int size() {
			return Matcher.this.groupCount() + 1;
		}
	};

	/**
	 * @since 0.2
	 */
	@Override
	public Set<Entry<Integer, String>> entrySet() {
		return this.entrySet;
	}

	/**
	 * @since 0.2
	 */
	@Override
	public String get(final Object key) {
		if (key instanceof CharSequence) {
			return this.group(key.toString());
		} else if (key instanceof Number) {
			Number index = (Number) key;

			return this.group(index.intValue());
		}

		throw new IllegalArgumentException("No group " + key);
	}

	/**
	 * @since 0.2
	 */
	private final Set<Integer> keySet = new AbstractSet<Integer>() {
		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				private int next = 0;

				@Override
				public boolean hasNext() {
					return this.next < size();
				}

				@Override
				public Integer next() {
					if (this.next >= size()) {
						throw new NoSuchElementException(String.valueOf(this.next));
					}

					return this.next++;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public int size() {
			return Matcher.this.groupCount() + 1;
		}
	};

	/**
	 * @since 0.2
	 */
	@Override
	public Set<Integer> keySet() {
		return this.keySet;
	}

	/**
	 * Returns the size of a matcher. For a <code>MatchResult</code> this is the number of groups (excluding group 0).
	 * Otherwise, it is the total number of matches.
	 *
	 * @since 0.2
	 */
	@Override
	public int size() {
		if (this.isMatchResult()) {
			return this.groupCount();
		} else {
			// TODO: cache matches ??
			int count = 0;
			Matcher matcher = this.clone().reset();

			while (matcher.find()) {
				count++;
			}

			return count;
		}
	}

	/**
	 * @since 0.2
	 */
	private final List<String> values = new AbstractList<String>() {
		@Override
		public String get(final int index) {
			return Matcher.this.group(index);
		}

		@Override
		public int size() {
			return Matcher.this.groupCount() + 1;
		}
	};

	/**
	 * @since 0.2
	 */
	@Override
	public List<String> values() {
		return this.values;
	}

	/**
	 * @since 0.2
	 */
	private final Map<Integer, Entry<Integer, String>> entryCache = new HashMap<>(
			2);

	/**
	 *
	 * @since 0.2
	 */
	public Entry<Integer, String> getEntry(final int group) {
		Entry<Integer, String> entry = this.entryCache.get(group);

		if (entry != null) {
			return entry;
		}

		entry = new Entry<Integer, String>() {
			/**
			 * <code>Map.Entry</code> interface method, not supported by this
			 * implementation.
			 *
			 * @throws UnsupportedOperationException
			 *             Always
			 */
			@Override
			public String setValue(final String value) {
				throw new UnsupportedOperationException();
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public String getValue() {
				return Matcher.this.group(group);
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public Integer getKey() {
				return group;
			}

			@Override
			public String toString() {
				return this.getKey() + "=" + this.getValue();
			}
		};

		this.entryCache.put(group, entry);
		return entry;
	}

	/**
	 * Maintains compatibility with Groovy regular expressions
	 *
	 * @since 0.2
	 */
	/**
	 * Returns an {@link java.util.Iterator} which traverses each match.
	 *
	 * @return an Iterator for a Matcher
	 * @since 0.2
	 */
	// Copied from Groovy
	// public Iterator<List<String>> iterator()
	@Override
	public Iterator<MatchResult> iterator() {
		// Iterator doesn't modify this Matcher
		final Matcher matcher = this.clone().reset();

		return new MatchResultIterator(this);
	}

	// suggestion from SpotBugs
	private static class MatchResultIterator implements Iterator<MatchResult> {
		private boolean found /* = false */;
		private boolean done /* = false */;

		private final Matcher matcher;

		public MatchResultIterator(final Matcher matcher) {
			this.matcher = matcher;
		}

		@Override
		public boolean hasNext() {
			if (this.done) {
				return false;
			}
			if (!this.found) {
				this.found = this.matcher.find();
				if (!this.found) {
					this.done = true;
				}
			}
			return this.found;
		}

		// public List<String> next()
		@Override
		public MatchResult next() {
			if (!this.found) {
				if (!this.hasNext()) {
					throw new NoSuchElementException();
				}
			}
			this.found = false;

			// List<String> list = new ArrayList<String>(matcher.groupCount() + 1);
			//
			// for (int i = 0; i <= matcher.groupCount(); i++) {
			// list.add(matcher.group(i));
			// }
			//
			// return list;
			return this.matcher.toMatchResult();
			// return matcher;

			// return matcher.toMatchResult();
			// if (matcher.groupCount() > 0) {
			// // are we using groups?
			// // yes, so return the specified group as list
			// List<String> list = new ArrayList<String>(matcher.groupCount());
			// for (int i = 0; i <= matcher.groupCount(); i++) {
			// list.add(matcher.group(i));
			// }
			// return list;
			// } else {
			// // not using groups, so return the nth
			// // occurrence of the pattern
			// return matcher.group();
			// }
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Gets each match as a MatchResult
	 * @return the results
	 */
	public List<MatchResult> getResults() {
		List<MatchResult> results = new ArrayList<>();

		Iterator<MatchResult> iterator = this.iterator();

		while (iterator.hasNext()) {
			results.add(iterator.next());
		}

		return results;
	}

	/* Groovy methods - makes RegExPlus groovier */

	/**
	 * Alias for {@link #find()}.
	 *
	 * <p>Coerces a Matcher instance to a boolean value, for use in Groovy truth</p>
	 *
	 * @since 0.2
	 */
	@Override
	public boolean asBoolean() {
		RegExPlusSupport.setLastMatcher(this);
		return this.isMatchResult() ? this.matched() : this.find();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getAt(int idx) {
		if (this.isMatchResult()) {
			return this.group(idx);
		} else {
			// TODO: optimize code
			// NOTE: can produce different results than Groovy, since this code doesn't affect this Matcher
			// (iterator does not affect this matcher)
			List<MatchResult> results = this.getResults();
			int count = results.size();

			if (idx < -count || idx >= count) {
				throw new IndexOutOfBoundsException("index is out of range " + (-count) + ".." + (count - 1)
						+ " (index = " + idx + ")");
			}

			if (idx < 0) {
				idx += count;
			}

			return results.get(idx);
		}
	}

	/**
	 *
	 * @param indexes the indexes
	 * @return for a MatchResult, the specified group; otherwise, the specified MatchResult
	 */
	public List getAt(final Collection<?> indexes) {
		List matches = new ArrayList();

		if (this.isMatchResult()) {
			List<Integer> indexList = this.flattenGroupIndexes(indexes);
			for (Integer group : indexList) {
				matches.add(this.usedMatcher.group(group));
			}
		} else {
			// TODO: optimize code
			// NOTE: can produce different results than Groovy, since this code doesn't affect this Matcher
			// (iterator does not affect this matcher)
			List<Integer> indexList = flatten(indexes);
			List<MatchResult> results = this.getResults();
			int count = results.size();

			for (int idx : indexList) {
				if (idx < -count || idx >= count) {
					throw new IndexOutOfBoundsException("index is out of range " + (-count) + ".." + (count - 1)
							+ " (index = " + idx + ")");
				}

				if (idx < 0) {
					idx += count;
				}

				matches.add(results.get(idx));
			}
		}

		return matches;
	}

	/*
	 * Slightly modified from DefaultGroovyMethods.getAt(Collection),
	 * and modified to run in Java
	 */
	private List<Integer> flattenGroupIndexes(final Collection<?> indices) {
		List<Integer> answer = new ArrayList<>(indices.size());
		for (Object value : indices) {
			if (value instanceof List) {
				answer.addAll(this.flattenGroupIndexes((List<?>) value));
			} else if (value instanceof CharSequence) {
				CharSequence group = (CharSequence) value;
				// System.out.println(group + ": " + getGroupIndex(group.toString()));
				answer.add(this.getGroupIndex(group.toString()));
			} else {
				int idx = intUnbox(value);
				answer.add(this.getGroupIndex(idx));
			}
		}
		return answer;
	}

	/* Slightly modified from DefaultGroovyMethods.getAt(Collection), and modified to run in Java */
	private static List<Integer> flatten(final Collection<?> indices) {
		List<Integer> answer = new ArrayList<>(indices.size());
		for (Object value : indices) {
			if (value instanceof List) {
				answer.addAll(flatten((List<?>) value));
			} else {
				int idx = intUnbox(value);
				answer.add(idx);
			}
		}
		return answer;
	}

	/* Groovy helper methods, required by the getAt method (copied from Groovy code) */
	private static int intUnbox(final Object value) {
		Number n = castToNumber(value);
		return n.intValue();
	}

	private static Number castToNumber(final Object object) {
		// default to Number class in exception details, else use the specified Number subtype.
		return castToNumber(object, Number.class);
	}

	private static Number castToNumber(final Object object, final Class type) {
		if (object instanceof Number) {
			return (Number) object;
		}
		if (object instanceof Character) {
			return Integer.valueOf(((Character) object).charValue());
		}
		if (object instanceof String) {
			String c = (String) object;
			if (c.length() == 1) {
				return Integer.valueOf(c.charAt(0));
			} else {
				throw new ClassCastException(makeMessage(c, type));
			}
		}
		throw new ClassCastException(makeMessage(object, type));
	}

	static String makeMessage(Object objectToCast, final Class classToCastTo) {
		String classToCastFrom;
		if (objectToCast != null) {
			classToCastFrom = objectToCast.getClass().getName();
		} else {
			objectToCast = "null";
			classToCastFrom = "null";
		}
		return "Cannot cast object '" + objectToCast + "' " +
				"with class '" + classToCastFrom + "' " +
				"to class '" + classToCastTo.getName() + "'";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAt(final String group) {
		return this.group(group);
	}

	public MatchResult next() {
		MatchResult result = this.toMatchResult();
		this.find();
		return result;
	}

	// public Matcher next()
	// {
	// // find();
	// System.out.println("Next: " + this);
	// return this;
	// }

	// public String getProperty(String name)
	// {
	// return group(name);
	// }

	/**
	 * Returns this <code>Matcher</code>.
	 *
	 * <p>Added for consistency for use in Groovy, since +javaMatcher is also supported.
	 * This method ensures that the 'positive' operator will return a RegExPlus Matcher, for both cases:</p>
	 *
	 * <ol>
	 * <li><b>Promoting a Java Matcher</b>: <code>+javaMatcher</code></li>
	 * <li><b>When used on an existing RegExPlus Matcher</b>: <code>+regexPlusMatcher</code></li>
	 * </ol>
	 *
	 * @return this <code>Matcher</code>.
	 */
	public Matcher positive() {
		return this;
	}
}