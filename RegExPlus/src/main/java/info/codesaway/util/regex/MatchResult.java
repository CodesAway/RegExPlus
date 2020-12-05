package info.codesaway.util.regex;

import static info.codesaway.util.regex.Pattern.wrapIndex;

import java.util.AbstractList;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * The result of a match operation.
 *
 * <p>This interface is an extension of Java's {@link MatchResult} class.
 * Javadocs were copied and appended with the added functionality.</p>
 *
 * <p>This interface contains query methods used to determine the
 * results of a match against a regular expression. The match boundaries,
 * groups and group boundaries can be seen but not modified through
 * a <code>MatchResult</code>.</p>
 *
 * <br>
 *
 * <p>Starting with Version 0.2, <code>MatchResult</code> extends
 * <code>Iterable&lt;MatchResult&gt;</code> and is Groovified,
 * adding some useful functionality.</p>
 *
 * <p>Given the following MatchResult:</p>
 *
 * <pre><code> Pattern pattern = Pattern.compile("(a)(b)(c)(d)");
 * Matcher matcher = pattern.matcher("abcd");
 * matcher.find();
 *
 * MatchResult result = matcher.toMatchResult();</code></pre>
 *
 * <p>Loop through each group (by group number):</p>
 *
 * <pre><code>&nbsp; <span style='color: green'
 * >// Loops for group = 1 to 3 (group count)</span>
 * &nbsp; for (int group : result.keySet()) {
 * &nbsp; &nbsp; <span style='color: green'>/*
 * &nbsp; &nbsp; * Output:
 * &nbsp; &nbsp; *
 * &nbsp; &nbsp; * result[1] = a
 * &nbsp; &nbsp; * result[2] = b
 * &nbsp; &nbsp; * result[3] = c
 * &nbsp; &nbsp; *<!---->/</span>
 * &nbsp; &nbsp; System.out.printf("result[%d] = %s%n", group, <span
 * >result.group(group));</span>
 * &nbsp; }</code></pre>
 *
 * <p>Loop through each group's value:</p>
 *
 * <pre><code>&nbsp; <span style='color: green'
 * >// Loops for each value</span>
 * &nbsp; for (String groupValue : result.values()) {
 * &nbsp; &nbsp; <span style='color: green'>/*
 * &nbsp; &nbsp; * Output:
 * &nbsp; &nbsp; *
 * &nbsp; &nbsp; * Value = a
 * &nbsp; &nbsp; * Value = b
 * &nbsp; &nbsp; * Value = c
 * &nbsp; &nbsp; *<!---->/</span>
 * &nbsp; &nbsp; System.out.printf("Value = %s%n", groupValue);
 * &nbsp; }</code></pre>
 *
 * @see Matcher
 */
// public interface MatchResult extends Iterable<List<String>>, Map<Integer, String>
// public interface MatchResult extends Iterable<List<String>>
public interface MatchResult
// public interface MatchResult extends Iterable<MatchResult>, Map<Integer, String>
{
	/**
	 * Returns the start index of the match.
	 *
	 * @return The index of the first character matched
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed
	 */
	public int start();

	/**
	 * Returns the start index of the subsequence captured by the given group
	 * during this match.
	 *
	 * <p><a href="Pattern.html#cg">Capturing groups</a> are indexed
	 * from left to right, starting at one. Group zero denotes the entire
	 * pattern, so the expression <i>m.</i><tt>start(0)</tt> is equivalent to
	 * <i>m.</i><tt>start()</tt>.</p>
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 *
	 * @return The index of the first character captured by the group,
	 *         or <tt>-1</tt> if the match was successful but the group
	 *         itself did not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 */
	public int start(int group);

	/**
	 * Returns the start index of the subsequence captured by the given
	 * <a href="Pattern.html#group">group</a> during this match.
	 *
	 * @param group
	 *            A capturing group in this matcher's pattern
	 *
	 * @return The index of the first character captured by the group,
	 *         or <tt>-1</tt> if the match was successful but the group
	 *         itself did not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	public int start(String group);

	/**
	 * Returns the start index of the subsequence captured by the given
	 * <a href="Pattern.html#group">group</a> during this match.
	 *
	 * <p>An invocation of this convenience method of the form</p>
	 *
	 * <blockquote><pre>m.start(groupName, occurrence)</pre></blockquote>
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
	 * @return The index of the first character captured by the group,
	 *         or <tt>-1</tt> if the match was successful but the group
	 *         itself did not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	public int start(String groupName, int occurrence);

	/**
	 * Returns the offset after the last character matched.
	 *
	 * @return The offset after the last character matched
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 */
	public int end();

	/**
	 * Returns the offset after the last character of the subsequence
	 * captured by the given group during this match.
	 *
	 * <p><a href="Pattern.html#cg">Capturing groups</a> are indexed
	 * from left to right, starting at one. Group zero denotes the entire
	 * pattern, so the expression <i>m.</i><tt>end(0)</tt> is equivalent to
	 * <i>m.</i><tt>end()</tt>.</p>
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 *
	 * @return The offset after the last character captured by the group,
	 *         or <tt>-1</tt> if the match was successful
	 *         but the group itself did not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 */
	public int end(int group);

	/**
	 * Returns the offset after the last character of the subsequence
	 * captured by the given <a href="Pattern.html#group">group</a> during this
	 * match.
	 *
	 * @param group
	 *            A capturing group in this matcher's pattern
	 *
	 * @return The offset after the last character captured by the group,
	 *         or <tt>-1</tt> if the match was successful
	 *         but the group itself did not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	public int end(String group);

	/**
	 * Returns the offset after the last character of the subsequence
	 * captured by the given <a href="Pattern.html#group">group</a> during this
	 * match.
	 *
	 * <p>An invocation of this convenience method of the form</p>
	 *
	 * <blockquote><pre>m.end(groupName, occurrence)</pre></blockquote>
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
	 * @return The offset after the last character captured by the group,
	 *         or <tt>-1</tt> if the match was successful
	 *         but the group itself did not match anything
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	public int end(String groupName, int occurrence);

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
	public default int occurrence(final int groupIndex) {
		try {
			String groupName = wrapIndex(Matcher.getAbsoluteGroupIndex(
					groupIndex, this.groupCount()));

			if (this.groupCount(groupName) == 0) {
				throw Matcher.noGroup(groupIndex);
			}

			return this.occurrence(groupName);
		} catch (IndexOutOfBoundsException e) {
			throw Matcher.noGroup(groupIndex);
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
	public int occurrence(String groupName);

	/**
	 * Returns the input subsequence matched by the previous match.
	 *
	 * <p>For a matcher <i>m</i> with input sequence <i>s</i>, the expressions
	 * o<i>m.</i><tt>group()</tt> and <i>s.</i><tt>substring(</tt><i>m.</i>
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
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 */
	public String group();

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
	 * @return The (possibly empty) subsequence captured by the group
	 *         during the previous match, or <tt>null</tt> if the group
	 *         failed to match part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 */
	public String group(int group);

	/**
	 * Returns the input subsequence captured by the given group during the
	 * previous match operation.
	 *
	 * <p><a href="Pattern.html#cg">Capturing groups</a> are indexed from left
	 * to
	 * right, starting at one. Group zero denotes the entire pattern, so the
	 * expression <tt>m.group(0, null)</tt> is equivalent to
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
	 * @return The (possibly empty) subsequence captured by the group
	 *         during the previous match, or <tt>defaultValue</tt> if the group
	 *         failed to match part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 *
	 * @see #group(int)
	 */
	public String group(int group, String defaultValue);

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
	 * @return The (possibly empty) subsequence captured by the group
	 *         during the previous match, or <tt>null</tt> if the group
	 *         failed to match part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	public String group(String group);

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
	 * @return The (possibly empty) subsequence captured by the group
	 *         during the previous match, or <tt>defaultValue</tt> if the group
	 *         failed to match part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 *
	 * @see #group(String)
	 */
	public String group(String group, String defaultValue);

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
	 * @return The (possibly empty) subsequence captured by the group
	 *         during the previous match, or <tt>null</tt> if the group
	 *         failed to match part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	public String group(String groupName, int occurrence);

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
	 * <blockquote><pre>
	 * m.group(groupName, occurrence)</pre></blockquote><br>
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
	 * @return The (possibly empty) subsequence captured by the group
	 *         during the previous match, or <tt>defaultValue</tt> if the group
	 *         failed to match part of the input
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 *
	 * @see #group(String, int)
	 */
	public String group(String groupName, int occurrence,
			String defaultValue);

	/**
	 * Returns the number of capturing groups in this match result's pattern.
	 *
	 * <p>Group zero denotes the entire pattern by convention. It is not
	 * included in this count.</p>
	 *
	 * <p>Any non-negative integer smaller than or equal to the value returned
	 * by this method is guaranteed to be a valid group index for this
	 * matcher.</p>
	 *
	 * @return The number of capturing groups in this matcher result's pattern
	 */
	public default int groupCount() {
		return this.pattern().groupCount();
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
	public default int groupCount(final int group) {
		return this.pattern().groupCount(group);
	}

	/**
	 * Returns the number of capturing groups (with the given group name) in
	 * this match result's pattern.
	 *
	 * <p>Group zero denotes the entire pattern by convention. It is not
	 * included in this count.</p>
	 *
	 * <p>Any non-negative integer smaller than or equal to the value returned
	 * by this method is guaranteed to be a valid occurrence (for a group,
	 * <i>groupName</i><tt>[</tt><i>occurrence</i><tt>]</tt>) for this
	 * matcher.</p>
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
	public default int groupCount(final String groupName) {
		return this.pattern().groupCount(groupName);
	}

	/**
	 * Indicates whether this match has any capturing groups.
	 *
	 * @return <code>true</code> if this match has at least one capturing group; otherwise, <code>false</code>
	 *
	 * @since 0.2
	 */
	public default boolean hasGroup() {
		return this.pattern().hasGroup();
	}

	/**
	 * Indicates whether this match contains the specified group.
	 *
	 * @param group
	 *            The group index for a capturing group in this match's pattern
	 * @return <code>true</code> if this match contains the specified group; otherwise, <code>false</code>.
	 *
	 * @since 0.2
	 */
	public default boolean hasGroup(final int group) {
		return this.pattern().hasGroup(group);
	}

	/**
	 * Indicates whether this match contains the specified group.
	 *
	 * @param group
	 *            A capturing group in this match's pattern
	 * @return <code>true</code> if this match contains the specified group; otherwise, <code>false</code>.
	 *
	 * @since 0.2
	 */
	public default boolean hasGroup(final String group) {
		return this.pattern().hasGroup(group);
	}

	/**
	 * Indicates whether this match contains the specified group.
	 *
	 * @param groupName
	 *            The group name for a capturing group in this match's pattern
	 *
	 * @param occurrence
	 *            The occurrence of the specified group name
	 * @return <code>true</code> if this match contains the specified group; otherwise, <code>false</code>.
	 *
	 * @since 0.2
	 */
	public default boolean hasGroup(final String groupName, final int occurrence) {
		return this.pattern().hasGroup(groupName, occurrence);
	}

	/**
	 * Returns whether the match was successful.
	 *
	 * <p>Note that some patterns, for example <tt>a*</tt>, match the empty
	 * string. This method will return <code>true</code> when the pattern
	 * successfully matches the empty string in the input.</p>
	 *
	 * <p><b>Note</b>: unlike the other methods, this method won't throw an
	 * exception if no match has been attempted - instead, <code>false</code>
	 * will be returned. This method provides a way to check that a match has
	 * been attempted, and that it succeeded.</p>
	 *
	 * @return <code>true</code> if the match was successful
	 */
	public default boolean matched() {
		try {
			return this.start() != -1;
		} catch (RuntimeException e) {
			return false;
		}
	}

	/**
	 * Returns whether the specified group matched any part of the input
	 * sequence.
	 *
	 * <p><a href="Pattern.html#cg">Capturing groups</a> are indexed
	 * from left to right, starting at one. Group zero denotes the entire
	 * pattern, so the expression <tt>m.matched(0)</tt> is equivalent to
	 * <tt>m.matched()</tt>.</p>
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>false</tt> is returned. Note
	 * that some groups, for example <tt>(a*)</tt>, match the empty string. This
	 * method will return <code>true</code> when such a group successfully
	 * matches the empty string in the input.</p>
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 *
	 * @return <code>true</code> if in the previous match, the group matched
	 *         part of the input.
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 */
	public default boolean matched(final int group) {
		return this.matched(group, true);
	}

	/**
	 * Returns whether the specified group matched any part of the input
	 * sequence.
	 *
	 * <p><a href="Pattern.html#cg">Capturing groups</a> are indexed
	 * from left to right, starting at one. Group zero denotes the entire
	 * pattern, so the expression <tt>m.matched(0)</tt> is equivalent to
	 * <tt>m.matched()</tt>.</p>
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>false</tt> is returned. Note
	 * that some groups, for example <tt>(a*)</tt>, match the empty string. This
	 * method will return <code>true</code> when such a group successfully
	 * matches the empty string in the input.</p>
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 * @param validateGroup
	 *            whether to validate the group, and throw an <code>IndexOutOfBoundsException</code> in the case the the
	 *            group is invalid.
	 *
	 * @return <code>true</code> if in the previous match, the group matched
	 *         part of the input.
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If <code>validateGroup</code> is <code>true</code> and there is no capturing group in the pattern
	 *             with the given index
	 */
	public default boolean matched(final int group, final boolean validateGroup) {
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
	 * Returns whether the specified <a href="Pattern.html#group">group</a>
	 * matched any part of the input sequence.
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>false</tt> is returned. Note
	 * that some groups, for example <tt>(a*)</tt>, match the empty string. This
	 * method will return <code>true</code> when such a group successfully
	 * matches the empty string in the input.</p>
	 *
	 * @param group
	 *            A capturing group in this matcher's pattern
	 *
	 * @return <code>true</code> if in the previous match, the group matched
	 *         part of the input.
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	public default boolean matched(final String group) {
		return this.matched(group, true);
	}

	/**
	 * Returns whether the specified <a href="Pattern.html#group">group</a>
	 * matched any part of the input sequence.
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>false</tt> is returned. Note
	 * that some groups, for example <tt>(a*)</tt>, match the empty string. This
	 * method will return <code>true</code> when such a group successfully
	 * matches the empty string in the input.</p>
	 *
	 * @param group
	 *            A capturing group in this matcher's pattern
	 * @param validateGroup
	 *            whether to validate the group, and throw an <code>IllegalArgumentException</code> in the case the the
	 *            group is invalid.
	 *
	 * @return <code>true</code> if in the previous match, the group matched
	 *         part of the input.
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If <code>validateGroup</code> is <code>true</code> and there is no capturing group in the pattern
	 *             of the given group
	 */
	public default boolean matched(final String group, final boolean validateGroup) {
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
	 * Returns whether the specified <a href="Pattern.html#group">group</a>
	 * matched any part of the input sequence.
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>false</tt> is returned. Note
	 * that some groups, for example <tt>(a*)</tt>, match the empty string. This
	 * method will return <code>true</code> when such a group successfully
	 * matches the empty string in the input.</p>
	 *
	 * <p>An invocation of this convenience method of the form</p>
	 *
	 * <blockquote><pre>m.matched(groupName, occurrence)</pre></blockquote>
	 *
	 * <p>behaves in exactly the same way as</p>
	 *
	 * <blockquote><pre>
	 * m.matched(groupName + "[" + occurrence + "]")</pre></blockquote>
	 *
	 * @param groupName
	 *            The group name for a capturing group in this matcher's pattern
	 *
	 * @param occurrence
	 *            The occurrence of the specified group name
	 *
	 * @return <code>true</code> if in the previous match, the group matched
	 *         part of the input.
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	public default boolean matched(final String groupName, final int occurrence) {
		return this.matched(groupName, occurrence, true);
	}

	/**
	 * Returns whether the specified <a href="Pattern.html#group">group</a>
	 * matched any part of the input sequence.
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>false</tt> is returned. Note
	 * that some groups, for example <tt>(a*)</tt>, match the empty string. This
	 * method will return <code>true</code> when such a group successfully
	 * matches the empty string in the input.</p>
	 *
	 * <p>An invocation of this convenience method of the form</p>
	 *
	 * <blockquote><pre>m.matched(groupName, occurrence, validateGroup)</pre></blockquote>
	 *
	 * <p>behaves in exactly the same way as</p>
	 *
	 * <blockquote><pre>
	 * m.matched(groupName + "[" + occurrence + "]", validateGroup)</pre></blockquote>
	 *
	 * @param groupName
	 *            The group name for a capturing group in this matcher's pattern
	 *
	 * @param occurrence
	 *            The occurrence of the specified group name
	 *
	 * @param validateGroup
	 *            whether to validate the group, and throw an <code>IllegalArgumentException</code> in the case the the
	 *            group is invalid.
	 *
	 * @return <code>true</code> if in the previous match, the group matched
	 *         part of the input.
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If <code>validateGroup</code> is <code>true</code> and there is no capturing group in the pattern
	 *             of the given group
	 */
	public default boolean matched(final String groupName, final int occurrence, final boolean validateGroup) {
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
	 * Returns whether the previous match matched the empty string.
	 *
	 * @return <code>true</code> if the previous match matched the empty string.
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 */
	public boolean isEmpty();

	/**
	 * Returns whether the specified group matched the empty string.
	 *
	 * <p><a href="Pattern.html#cg">Capturing groups</a> are indexed
	 * from left to right, starting at one. Group zero denotes the entire
	 * pattern, so the expression <tt>m.isEmpty(0)</tt> is equivalent to
	 * <tt>m.isEmpty()</tt>.</p>
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>false</tt> is returned.</p>
	 *
	 * @param group
	 *            The index of a capturing group in this matcher's pattern
	 *
	 * @return <code>true</code> if in the previous match, the group matched
	 *         the empty string.
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             with the given index
	 */
	public boolean isEmpty(int group);

	/**
	 * Returns whether the specified <a href="Pattern.html#group">group</a>
	 * matched the empty string.
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>false</tt> is returned.</p>
	 *
	 * @param group
	 *            A capturing group in this matcher's pattern
	 *
	 * @return <code>true</code> if in the previous match, the group matched
	 *         the empty string.
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	public boolean isEmpty(String group);

	/**
	 * Returns whether the specified <a href="Pattern.html#group">group</a>
	 * matched the empty string.
	 *
	 * <p>If the match was successful but the group specified failed to match
	 * any part of the input sequence, then <tt>false</tt> is returned.</p>
	 *
	 * <p>An invocation of this convenience method of the form</p>
	 *
	 * <blockquote><pre>m.isEmpty(groupName, occurrence)</pre></blockquote>
	 *
	 * <p>behaves in exactly the same way as</p>
	 *
	 * <blockquote><pre>
	 * m.isEmpty(groupName + "[" + occurrence + "]")</pre></blockquote>
	 *
	 * @param groupName
	 *            The group name for a capturing group in this matcher's pattern
	 *
	 * @param occurrence
	 *            The occurrence of the specified group name
	 *
	 * @return <code>true</code> if in the previous match, the group matched
	 *         the empty string.
	 *
	 * @throws IllegalStateException
	 *             If no match has yet been attempted,
	 *             or if the previous match operation failed
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	public boolean isEmpty(String groupName, int occurrence);

	/**
	 * Returns the pattern that is interpreted by this matcher.
	 *
	 * @return The pattern for which this matcher was created
	 */
	public Pattern pattern();

	/**
	 * Returns the string being matched.
	 *
	 * @return the string being matched
	 */
	public String text();

	/**
	 * Returns the group name (if any) for the specified group.
	 *
	 * <p>If a match has been successful, this function's return is the group
	 * name associated with the given group <b>for this match</b>. This group
	 * name is match independent, except, possibly, when the group is part of a
	 * <a href="#branchreset">"branch reset" pattern</a>.</p>
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
	public String getGroupName(int group);

	/**
	 * @since 0.2
	 */
	// public Entry<Integer, String> getEntry(int group);

	/**
	 * @since 0.2
	 */
	public boolean treatNullAsEmptyString();

	/**
	 * @since 0.2
	 */
	public boolean containsKey(Object key);

	/**
	 * @since 0.2
	 */
	public default boolean containsValue(final Object value) {
		for (int i = 1; i <= this.groupCount(); i++) {
			String group = this.group(i);
			if (Objects.equals(group, value)) {
				return true;
			}
		}

		return false;
	}

	/**
	 *
	 * @since 2.0
	 */
	public default Entry<Integer, String> getEntry(final int group) {
		return new SimpleImmutableEntry<>(group, MatchResult.this.group(group));
	}

	/**
	 * @since 0.2
	 */
	public default Set<Entry<Integer, String>> entrySet() {
		return new AbstractSet<Entry<Integer, String>>() {
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

						return MatchResult.this.getEntry(this.next++);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return MatchResult.this.groupCount() + 1;
			}
		};
	}

	/**
	 * @since 0.2
	 */
	public default String get(final Object key) {
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
	public default Set<Integer> keySet() {
		return new AbstractSet<Integer>() {
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
				return MatchResult.this.groupCount() + 1;
			}
		};
	}

	/**
	 * @since 0.2
	 */
	public default List<String> values() {
		return new AbstractList<String>() {
			@Override
			public String get(final int index) {
				return MatchResult.this.group(index);
			}

			@Override
			public int size() {
				return MatchResult.this.groupCount() + 1;
			}
		};
	}

	/* Groovy methods - makes RegExPlus groovier */

	/**
	 * Alias for {@link #matched()}.
	 *
	 * <p>Coerces a Matcher instance to a boolean value, for use in Groovy truth</p>
	 *
	 * @since 0.2
	 */
	public default boolean asBoolean() {
		return this.matched();
	}

	/**
	 * Alias for {@link #group(int)}
	 *
	 * <p>In Groovy, adds support the subscript operator, e.g. matcher[group], for a regex Matcher.</p>
	 *
	 * @since 0.2
	 */
	public default Object getAt(final int group) {
		return this.group();
	}

	/**
	 * Alias for {@link #group(String)}
	 *
	 * <p>In Groovy, adds support the subscript operator, e.g. matcher[group], for a regex Matcher.</p>
	 *
	 * @since 0.2
	 */
	public default String getAt(final String group) {
		return this.group(group);
	}
}