// TODO: verify group names consisting of solely numbers are handled correctly
/*
 * TODO: need to throw correct exception for cases:
 * 1) Integer values too high
 * 2) Illegal integer values
 */

package info.codesaway.util.regex;

import static info.codesaway.util.regex.Matcher.getAbsoluteGroupIndex;
import static info.codesaway.util.regex.Matcher.noNamedGroup;
import static info.codesaway.util.regex.RefactorUtility.fullGroupName;
import static info.codesaway.util.regex.RefactorUtility.parseInt;
import static info.codesaway.util.regex.RegExPlusSupport.setLastMatcher;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

// TODO: finish documenting
// mention that all compiled patterns are now cached

/**
 * A compiled representation of a regular expression.
 *
 * <p>This class is an extension
 * of Java's {@link java.util.regex.Pattern} class. Javadocs were copied and
 * appended with the added functionality.</p>
 *
 * <p>A regular expression, specified as a string, must first be compiled into
 * an instance of this class. The resulting pattern can then be used to create
 * a {@link Matcher} object that can match arbitrary {@linkplain java.lang.CharSequence character sequences} against the
 * regular
 * expression. All of the state involved in performing a match resides in the
 * matcher, so many matchers can share the same pattern.
 *
 * <p>A typical invocation sequence is thus</p>
 *
 * <blockquote><pre>
 * Pattern p = Pattern.{@link #compile compile}("a*b");
 * Matcher m = p.{@link #matcher matcher}("aaaaab");
 * boolean b = m.{@link Matcher#matches matches}();</pre></blockquote>
 *
 * <p>A {@link #matches matches} method is defined by this class as a
 * convenience for when a regular expression is used just once. This method
 * compiles an expression and matches an input sequence against it in a single
 * invocation. The statement</p>
 *
 * <blockquote><pre>
 * boolean b = Pattern.matches("a*b", "aaaaab");</pre></blockquote>
 *
 * is equivalent to the three statements above, though for repeated matches it
 * is less efficient since it does not allow the compiled pattern to be reused.
 *
 * <p>Instances of this class are immutable and are safe for use by multiple
 * concurrent threads. Instances of the {@link Matcher} class are not safe for
 * such use.</p>
 *
 * <h1 id="sum">Summary of regular-expression constructs</h1>
 *
 * <table border="0" cellpadding="1" cellspacing="0" summary="Regular expression constructs, and what they match">
 *
 * <tr align="left">
 * <th bgcolor="#CCCCFF" align="left" id="construct">Construct</th>
 * <th bgcolor="#CCCCFF" align="left" id="matches">Matches</th>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="characters">Characters</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct characters"><i>x</i></td>
 * <td headers="matches">The character <i>x</i></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\\</tt></td>
 * <td headers="matches">The backslash character</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\0</tt><i>n</i></td>
 * <td headers="matches">The character with octal value <tt>0</tt><i>n</i>
 * (0&nbsp;<tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;7)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\0</tt><i>nn</i></td>
 * <td headers="matches">The character with octal value <tt>0</tt><i>nn</i>
 * (0&nbsp;<tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;7)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\0</tt><i>mnn</i></td>
 * <td headers="matches">The character with octal value <tt>0</tt><i>mnn</i>
 * (0&nbsp;<tt>&lt;=</tt>&nbsp;<i>m</i>&nbsp;<tt>&lt;=</tt>&nbsp;3, 0&nbsp;
 * <tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;7)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\x</tt><i>hh</i></td>
 * <td headers="matches">The character with
 * hexadecimal&nbsp;value&nbsp;<tt>0x</tt><i>hh</i></td>
 * </tr>
 * <tr>
 * <td valign="top"
 * headers="construct characters"><tt>\x{</tt><i>hhh</i><tt>..}</tt></td>
 * <td headers="matches">The character with
 * hexadecimal&nbsp;value&nbsp;<tt>0x</tt><i>hhh</i><tt>..</tt></td>
 * </tr>
 * <tr>
 * <td valign="top"
 * headers="construct characters"><tt>&#92;u</tt><i>hhhh</i></td>
 * <td headers="matches">The character with
 * hexadecimal&nbsp;value&nbsp;<tt>0x</tt><i>hhhh</i></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="matches"><tt>\t</tt></td>
 * <td headers="matches">The tab character (<tt>'&#92;u0009'</tt>)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\n</tt></td>
 * <td headers="matches">The newline (line feed) character
 * (<tt>'&#92;u000A'</tt>)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\r</tt></td>
 * <td headers="matches">The carriage-return character
 * (<tt>'&#92;u000D'</tt>)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\f</tt></td>
 * <td headers="matches">The form-feed character (<tt>'&#92;u000C'</tt>)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\a</tt></td>
 * <td headers="matches">The alert (bell) character (<tt>'&#92;u0007'</tt>)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\e</tt></td>
 * <td headers="matches">The escape character (<tt>'&#92;u001B'</tt>)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct characters"><tt>\c</tt><i>x</i></td>
 * <td headers="matches">The control character corresponding to <i>x</i></td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="classes">Character classes</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct classes"><tt>[abc]</tt></td>
 * <td headers="matches"><tt>a</tt>, <tt>b</tt>, or <tt>c</tt> (simple
 * class)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct classes"><tt>[^abc]</tt></td>
 * <td headers="matches">Any character except <tt>a</tt>, <tt>b</tt>, or
 * <tt>c</tt> (negation)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct classes"><tt>[a-zA-Z]</tt></td>
 * <td headers="matches"><tt>a</tt> through <tt>z</tt> or <tt>A</tt> through
 * <tt>Z</tt>, inclusive (range)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct classes"><tt>[a-d[m-p]]</tt></td>
 * <td headers="matches"><tt>a</tt> through <tt>d</tt>, or <tt>m</tt> through
 * <tt>p</tt>: <tt>[a-dm-p]</tt> (union)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct classes"><tt>[a-z&amp;&amp;[def]]</tt></td>
 * <td headers="matches"><tt>d</tt>, <tt>e</tt>, or <tt>f</tt> (intersection)
 * </tr>
 * <tr>
 * <td valign="top" headers="construct classes"><tt>[a-z&amp;&amp;[^bc]]</tt></td>
 * <td headers="matches"><tt>a</tt> through <tt>z</tt>, except for <tt>b</tt>
 * and <tt>c</tt>: <tt>[ad-z]</tt> (subtraction)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct classes"><tt>[a-z&amp;&amp;[^m-p]]</tt></td>
 * <td headers="matches"><tt>a</tt> through <tt>z</tt>, and not <tt>m</tt>
 * through <tt>p</tt>: <tt>[a-lq-z]</tt>(subtraction)</td>
 * </tr>
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 *
 * <tr align="left">
 * <th colspan="2" id="predef">Predefined character classes</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct predef"><tt>.</tt></td>
 * <td headers="matches">Any character (may or may not match
 * <a href="#lt">line terminators</a>)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct predef"><tt>\X</tt></td>
 * <td headers="matches">Single grapheme - equivalent to
 * <tt>(?&gt;\P{M}\p{M}*)</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct predef"><tt>\d</tt></td>
 * <td headers="matches">A digit: <tt>[0-9]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct predef"><tt>\D</tt></td>
 * <td headers="matches">A non-digit: <tt>[^0-9]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct predef"><tt>\s</tt></td>
 * <td headers="matches">A whitespace character: <tt>[ \t\n\x0B\f\r]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct predef"><tt>\S</tt></td>
 * <td headers="matches">A non-whitespace character: <tt>[^\s]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct predef"><tt>\w</tt></td>
 * <td headers="matches">A word character: <tt>[a-zA-Z_0-9]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct predef"><tt>\W</tt></td>
 * <td headers="matches">A non-word character: <tt>[^\w]</tt></td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="posix">POSIX character classes<b> (US-ASCII
 * only)</b></th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Lower}</tt></td>
 * <td headers="matches">A lower-case alphabetic character: <tt>[a-z]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Upper}</tt></td>
 * <td headers="matches">An upper-case alphabetic character: <tt>[A-Z]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{ASCII}</tt></td>
 * <td headers="matches">All ASCII: <tt>[\x00-\x7F]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Alpha}</tt></td>
 * <td headers="matches">An alphabetic
 * character: <tt>[\p{Lower}\p{Upper}]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Digit}</tt></td>
 * <td headers="matches">A decimal digit: <tt>[0-9]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Alnum}</tt></td>
 * <td headers="matches">An alphanumeric character:
 * <tt>[\p{Alpha}\p{Digit}]</tt>
 * </td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Punct}</tt></td>
 * <td headers="matches">Punctuation: One of
 * <tt>!"#$%&amp;'()*+,-./:;&lt;=&gt;?@[\]^_`{|}~</tt></td>
 * </tr>
 * <!-- <tt>[\!"#\$%&'\(\)\*\+,\-\./:;\<=\>\?@\[\\\]\^_`\{\|\}~]</tt>
 * <tt>[\X21-\X2F\X31-\X40\X5B-\X60\X7B-\X7E]</tt> -->
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Graph}</tt></td>
 * <td headers="matches">A visible character: <tt>[\p{Alnum}\p{Punct}]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Print}</tt></td>
 * <td headers="matches">A printable character: <tt>[\p{Graph}\x20]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Blank}</tt></td>
 * <td headers="matches">A space or a tab: <tt>[ \t]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Cntrl}</tt></td>
 * <td headers="matches">A control character: <tt>[\x00-\x1F\x7F]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{XDigit}</tt></td>
 * <td headers="matches">A hexadecimal digit: <tt>[0-9a-fA-F]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posix"><tt>\p{Space}</tt></td>
 * <td headers="matches">A whitespace character: <tt>[ \t\n\x0B\f\r]</tt></td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="posixclass">POSIX character classes (US-ASCII
 * only)<br><p style="margin-top: 0px; font-weight: normal">(equivalent to the
 * above POSIX classes - only allowed in a <a
 * href="#cc">character class</a>)</p></th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:lower:]</tt></td>
 * <td headers="matches">A lower-case alphabetic character: <tt>[a-z]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:upper:]</tt></td>
 * <td headers="matches">An upper-case alphabetic character: <tt>[A-Z]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:ascii:]</tt></td>
 * <td headers="matches">All ASCII: <tt>[\x00-\x7F]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:alpha:]</tt></td>
 * <td headers="matches">An alphabetic
 * character: <tt>[[:lower:][:upper:]]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:digit:]</tt></td>
 * <td headers="matches">A decimal digit: <tt>[0-9]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:alnum:]</tt></td>
 * <td headers="matches">An alphanumeric character:
 * <tt>[[:alpha:][:digit:]]</tt>
 * </td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:punct:]</tt></td>
 * <td headers="matches">Punctuation: One of
 * <tt>!"#$%&amp;'()*+,-./:;&lt;=&gt;?@[\]^_`{|}~</tt></td>
 * </tr>
 * <!-- <tt>[\!"#\$%&'\(\)\*\+,\-\./:;\<=\>\?@\[\\\]\^_`\{\|\}~]</tt>
 * <tt>[\X21-\X2F\X31-\X40\X5B-\X60\X7B-\X7E]</tt> -->
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:graph:]</tt></td>
 * <td headers="matches">A visible character: <tt>[[:alnum:][:punct:]]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:print:]</tt></td>
 * <td headers="matches">A printable character: <tt>[[:graph:]\x20]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:blank:]</tt></td>
 * <td headers="matches">A space or a tab: <tt>[ \t]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:cntrl:]</tt></td>
 * <td headers="matches">A control character: <tt>[\x00-\x1F\x7F]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:xdigit:]</tt></td>
 * <td headers="matches">A hexadecimal digit: <tt>[0-9a-fA-F]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:space:]</tt></td>
 * <td headers="matches">A whitespace character: <tt>[ \t\n\x0B\f\r]</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct posixclass"><tt>[:word:]</tt></td>
 * <td headers="matches">A word character: <tt>[\w]</tt></td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2">java.lang.Character classes (simple
 * <a href="#jcc">java character type</a>)</th>
 * </tr>
 *
 * <tr>
 * <td valign="top"><tt>\p{javaLowerCase}</tt></td>
 * <td>Equivalent to java.lang.Character.isLowerCase()</td>
 * </tr>
 * <tr>
 * <td valign="top"><tt>\p{javaUpperCase}</tt></td>
 * <td>Equivalent to java.lang.Character.isUpperCase()</td>
 * </tr>
 * <tr>
 * <td valign="top"><tt>\p{javaWhitespace}</tt></td>
 * <td>Equivalent to java.lang.Character.isWhitespace()</td>
 * </tr>
 * <tr>
 * <td valign="top"><tt>\p{javaMirrored}</tt></td>
 * <td>Equivalent to java.lang.Character.isMirrored()</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="unicode">Classes for Unicode blocks and categories</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct unicode"><tt>\p{InGreek}</tt></td>
 * <td headers="matches">A character in the Greek&nbsp;block (simple
 * <a href="#ubc">block</a>)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct unicode"><tt>\p{Lu}</tt></td>
 * <td headers="matches">An uppercase letter (simple
 * <a href="#ubc">category</a>)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct unicode"><tt>\p{Sc}</tt></td>
 * <td headers="matches">A currency symbol</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct unicode"><tt>\P{InGreek}</tt></td>
 * <td headers="matches">Any character except one in the Greek block
 * (negation)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct unicode">
 * <tt>[\p{L}&amp;&amp;[^\p{Lu}]]&nbsp;</tt></td>
 * <td headers="matches">Any letter except an uppercase letter
 * (subtraction)</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="bounds">Boundary matchers</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct bounds"><tt>^</tt></td>
 * <td headers="matches">The beginning of a line</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct bounds"><tt>$</tt></td>
 * <td headers="matches">The end of a line</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct bounds"><tt>\b</tt></td>
 * <td headers="matches">A word boundary</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct bounds"><tt>\B</tt></td>
 * <td headers="matches">A non-word boundary</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct bounds"><tt>\A</tt></td>
 * <td headers="matches">The beginning of the input</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct bounds"><tt>\G</tt></td>
 * <td headers="matches">The end of the previous match</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct bounds"><tt>\Z</tt></td>
 * <td headers="matches">The end of the input but for the final
 * <a href="#lt">terminator</a>, if&nbsp;any</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct bounds"><tt>\z</tt></td>
 * <td headers="matches">The end of the input</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="greedy">Greedy quantifiers</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct greedy"><i>X</i><tt>?</tt></td>
 * <td headers="matches"><i>X</i>, once or not at all</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct greedy"><i>X</i><tt>*</tt></td>
 * <td headers="matches"><i>X</i>, zero or more times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct greedy"><i>X</i><tt>+</tt></td>
 * <td headers="matches"><i>X</i>, one or more times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct greedy">
 * <i>X</i><tt>{</tt><i>n</i><tt>}</tt></td>
 * <td headers="matches"><i>X</i>, exactly <i>n</i> times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct greedy">
 * <i>X</i><tt>{</tt><i>n</i><tt>,}</tt></td>
 * <td headers="matches"><i>X</i>, at least <i>n</i> times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct greedy">
 * <i>X</i><tt>{</tt><i>n</i><tt>,</tt><i>m</i><tt>}</tt></td>
 * <td headers="matches"><i>X</i>, at least <i>n</i> but not more than <i>m</i>
 * times</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="reluc">Reluctant quantifiers</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct reluc"><i>X</i><tt>??</tt></td>
 * <td headers="matches"><i>X</i>, once or not at all</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct reluc"><i>X</i><tt>*?</tt></td>
 * <td headers="matches"><i>X</i>, zero or more times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct reluc"><i>X</i><tt>+?</tt></td>
 * <td headers="matches"><i>X</i>, one or more times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct reluc">
 * <i>X</i><tt>{</tt><i>n</i><tt>}?</tt></td>
 * <td headers="matches"><i>X</i>, exactly <i>n</i> times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct reluc">
 * <i>X</i><tt>{</tt><i>n</i><tt>,}?</tt></td>
 * <td headers="matches"><i>X</i>, at least <i>n</i> times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct reluc">
 * <i>X</i><tt>{</tt><i>n</i><tt>,</tt><i>m</i><tt>}?</tt></td>
 * <td headers="matches"><i>X</i>, at least <i>n</i> but not more than <i>m</i>
 * times</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="poss">Possessive quantifiers</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct poss"><i>X</i><tt>?+</tt></td>
 * <td headers="matches"><i>X</i>, once or not at all</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct poss"><i>X</i><tt>*+</tt></td>
 * <td headers="matches"><i>X</i>, zero or more times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct poss"><i>X</i><tt>++</tt></td>
 * <td headers="matches"><i>X</i>, one or more times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct poss">
 * <i>X</i><tt>{</tt><i>n</i><tt>}+</tt></td>
 * <td headers="matches"><i>X</i>, exactly <i>n</i> times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct poss">
 * <i>X</i><tt>{</tt><i>n</i><tt>,}+</tt></td>
 * <td headers="matches"><i>X</i>, at least <i>n</i> times</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct poss">
 * <i>X</i><tt>{</tt><i>n</i><tt>,</tt><i>m</i><tt>}+</tt></td>
 * <td headers="matches"><i>X</i>, at least <i>n</i> but not more than <i>m</i>
 * times</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="logical">Logical operators</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct logical"><i>XY</i></td>
 * <td headers="matches"><i>X</i> followed by <i>Y</i></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct logical"><i>X</i><tt>|</tt><i>Y</i></td>
 * <td headers="matches">Either <i>X</i> or <i>Y</i></td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="capturing">Capturing</th>
 * </tr>
 * <tr>
 * <td valign="top"
 * headers="construct capturing"><tt>(</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches">X, as a <a href="#cg">capturing
 * group</a></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct capturing">
 * <tt>(?&lt;</tt><i>name</i><tt>&gt;</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches">X, as a <a href="#groupname">named-capturing
 * group</a></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct capturing">
 * <tt>(?'</tt><i>name</i><tt>'</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches">X, as a named-capturing group</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct capturing">
 * <tt>(?P&lt;</tt><i>name</i><tt>&gt;</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches">X, as a named-capturing group</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="backref">Back references</th>
 * </tr>
 *
 * <tr>
 * <td valign="bottom" headers="construct backref"><tt>\</tt><i>n</i></td>
 * <td valign="bottom" headers="matches">Whatever the <i>n</i><sup>th</sup>
 * <a href="#cg">capturing group</a> matched</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref"><tt>\g</tt><i>n</i></td>
 * <td valign="bottom" headers="matches">Whatever the <i>n</i><sup>th</sup>
 * capturing group matched</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref">
 * <tt>\g{</tt><i>n</i><tt>}</tt></td>
 * <td valign="bottom" headers="matches">Whatever the <i>n</i><sup>th</sup>
 * capturing group matched</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref">&nbsp;</td>
 * <td valign="bottom" headers="matches">&nbsp;</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref"><tt>\g</tt><i>-n</i></td>
 * <td valign="bottom" headers="matches">Relative back reference</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref">
 * <tt>\g{</tt><i>-n</i><tt>}</tt></td>
 * <td valign="bottom" headers="matches">Relative back reference</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref">&nbsp;</td>
 * <td valign="bottom" headers="matches">&nbsp;</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref">
 * <tt>\k&lt;</tt><i>name</i><tt>&gt;</tt></td>
 * <td valign="bottom" headers="matches">Whatever the <a
 * href="#groupname">named-capturing group</a> "name" matched</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref">
 * <tt>\k'</tt><i>name</i><tt>'</tt></td>
 * <td valign="bottom" headers="matches">Whatever the named-capturing group
 * "name" matched</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref">
 * <tt>\g{</tt><i>name</i><tt>}</tt></td>
 * <td valign="bottom" headers="matches">Whatever the named-capturing group
 * "name" matched</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref">
 * <tt>\k{</tt><i>name</i><tt>}</tt></td>
 * <td valign="bottom" headers="matches">Whatever the named-capturing group
 * "name" matched</td>
 * </tr>
 * <tr>
 * <td valign="bottom" headers="construct backref">
 * <tt>(?P=</tt><i>name</i><tt>)</tt></td>
 * <td valign="bottom" headers="matches">Whatever the named-capturing group
 * "name" matched</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="quot">Quotation</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct quot"><tt>\</tt></td>
 * <td headers="matches">Nothing, but quotes the following character</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct quot"><tt>\Q</tt></td>
 * <td headers="matches">Nothing, but quotes all characters until
 * <tt>\E</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct quot"><tt>\E</tt></td>
 * <td headers="matches">Nothing, but ends quoting started by <tt>\Q</tt></td>
 * </tr>
 * <!-- Metachars: !$()*+.<>?[\]^{|} -->
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="special">Special constructs (non-capturing)</th>
 * </tr>
 *
 * <tr>
 * <td valign="top"
 * headers="construct special"><tt>(?:</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches"><i>X</i>, as a non-capturing group</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct special">
 * <tt>(?idmsuxJn-idmsuxJn)&nbsp;</tt></td>
 * <td headers="matches">Nothing, but turns match flags {@link #CASE_INSENSITIVE i} {@link #UNIX_LINES d}
 * {@link #MULTILINE m} {@link #DOTALL s} {@link #UNICODE_CASE u} {@link #COMMENTS x} {@link #DUPLICATE_NAMES J}
 * {@link #EXPLICIT_CAPTURE n} on - off</td>
 * </tr>
 * <tr>
 * <td valign="top"
 * headers="construct special"><tt>(?idmsuxJn-idmsuxJn:</tt><i
 * >X</i><tt>)</tt>&nbsp;&nbsp;</td>
 * <td headers="matches"><i>X</i>, as a <a href="#cg">non-capturing group</a>
 * with the given flags {@link #CASE_INSENSITIVE i} {@link #UNIX_LINES d} {@link #MULTILINE m} {@link #DOTALL s}
 * {@link #UNICODE_CASE u} {@link #COMMENTS x} {@link #DUPLICATE_NAMES J} {@link #EXPLICIT_CAPTURE n} on
 * - off</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct special">
 * <tt>(?&gt;</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches"><i>X</i>, as an independent (atomic), non-capturing
 * group</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct special">
 * <tt>(?|</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches"><i>X</i>, as a <a href="#branchreset">"branch reset"
 * pattern</a></td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="assert">Assertions (non-capturing)</th>
 * </tr>
 *
 * <tr>
 * <td valign="top"
 * headers="construct assert"><tt>(?=</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches"><i>X</i>, via zero-width positive lookahead</td>
 * </tr>
 * <tr>
 * <td valign="top"
 * headers="construct assert"><tt>(?!</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches"><i>X</i>, via zero-width negative lookahead</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct assert">
 * <tt>(?&lt;=</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches"><i>X</i>, via zero-width positive lookbehind</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct assert">
 * <tt>(?&lt;!</tt><i>X</i><tt>)</tt></td>
 * <td headers="matches"><i>X</i>, via zero-width negative lookbehind</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="comment">Comment (non-capturing)</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct comment">
 * <tt>(?x:#</tt>comment<tt>\n)</tt></td>
 * <td headers="matches">comment (cannot contain a
 * <a href="#lt">line terminator</a>)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct comment">
 * <tt>(?xd:#</tt>comment<tt>\n)</tt></td>
 * <td headers="matches">comment (cannot contain '\n')</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct comment">
 * <tt>(?#</tt>comment<tt>)</tt></td>
 * <td headers="matches">comment (cannot contain a close parenthesis)</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="conditional">Conditional patterns (non-capturing)<br>
 * <span style="font-weight: normal"><tt>(?(condition)yes-pattern)</tt><br>
 * <tt>(?(condition)yes-pattern|no-pattern)</tt></span></th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct conditional">
 * <tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct conditional">
 * <tt>(?(</tt><i>n</i><tt>)...)</tt></td>
 * <td headers="matches">absolute reference condition</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct conditional">
 * <tt>(?(</tt><i>-n</i><tt>)...)</tt></td>
 * <td headers="matches">relative reference condition</td>
 * </tr>
 * <tr>
 * <tr>
 * <td valign="bottom" headers="construct backref">&nbsp;</td>
 * <td valign="bottom" headers="matches">&nbsp;</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct conditional">
 * <tt>(?(&lt;</tt><i>name</i><tt>&gt;)...)</tt></td>
 * <td headers="matches"><a href="#groupname">named reference condition</a></td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct conditional">
 * <tt>(?('</tt><i>name</i><tt>')...)</tt></td>
 * <td headers="matches">named reference condition</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct conditional">
 * <tt>(?(</tt><i>name</i><tt>)...)</tt></td>
 * <td headers="matches">named reference condition</td>
 * </tr>
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct conditional">
 * <tt>(?(</tt><i>assert</i><tt>)...)</tt></td>
 * <td headers="matches">assert condition</td>
 * </tr>
 *
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr align="left">
 * <th colspan="2" id="numrange">Numeric ranges (non-capturing)</th>
 * </tr>
 *
 * <tr>
 * <td valign="top" headers="construct numrange">
 * <tt>(?Z[</tt><i>start</i><tt>..</tt><i>end</i><tt>])</tt></td>
 * <td headers="matches">matches a <a href="#numericrange">numeric range</a>
 * (allowing for leading zeros)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct numrange">
 * <tt>(?Z16[</tt><i>start</i><tt>..</tt><i>end</i><tt>])</tt></td>
 * <td headers="matches">matches a numeric range in
 * base 16 (allowing for leading zeros)</td>
 * </tr>
 * <tr>
 * <th>&nbsp;</th>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct numrange">
 * <tt>(?NZ[</tt><i>start</i><tt>..</tt><i>end</i><tt>])</tt></td>
 * <td headers="matches">matches a numeric range
 * (not allowing for leading zeros)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct numrange">
 * <tt>(?NZ16[</tt><i>start</i><tt>..</tt><i>end</i><tt>])</tt></td>
 * <td headers="matches">matches a numeric range in
 * base 16 (not allowing for leading zeros)</td>
 * </tr>
 * </table>
 *
 * <hr>
 *
 * <h1 id="bs">Backslashes, escapes, and quoting</h1>
 *
 * <p>The backslash character (<tt>'\'</tt>) serves to introduce escaped
 * constructs, as defined in the table above, as well as to quote characters
 * that otherwise would be interpreted as unescaped constructs. Thus the
 * expression <tt>\\</tt> matches a single backslash and <tt>\{</tt> matches a
 * left brace.</p>
 *
 * <p>It is an error to use a backslash prior to any alphabetic character that
 * does not denote an escaped construct; these are reserved for future
 * extensions to the regular-expression language. A backslash may be used
 * prior to a non-alphabetic character regardless of whether that character is
 * part of an unescaped construct.</p>
 *
 * <p>Backslashes within string literals in Java source code are interpreted
 * as required by the <a
 * href="http://java.sun.com/docs/books/jls">Java Language
 * Specification</a> as either <a
 * href=
 * "http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#100850"
 * >Unicode
 * escapes</a> or other <a
 * href=
 * "http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#101089"
 * >character
 * escapes</a>. It is therefore necessary to double backslashes in string
 * literals that represent regular expressions to protect them from
 * interpretation by the Java bytecode compiler. The string literal
 * <tt>"&#92;b"</tt>, for example, matches a single backspace character when
 * interpreted as a regular expression, while <tt>"&#92;&#92;b"</tt> matches a
 * word boundary. The string literal <tt>"&#92;(hello&#92;)"</tt> is illegal
 * and leads to a compile-time error; in order to match the string
 * <tt>(hello)</tt> the string literal <tt>"&#92;&#92;(hello&#92;&#92;)"</tt>
 * must be used.</p>
 *
 * <h1 id="cc">Character Classes</h1>
 *
 * <p>Character classes may appear within other character classes, and
 * may be composed by the union operator (implicit) and the intersection
 * operator (<tt>&amp;&amp;</tt>).
 * The union operator denotes a class that contains every character that is
 * in at least one of its operand classes. The intersection operator
 * denotes a class that contains every character that is in both of its
 * operand classes.</p>
 *
 * <p>The precedence of character-class operators is as follows, from
 * highest to lowest:</p>
 *
 * <blockquote><table border="0" cellpadding="1" cellspacing="0"
 * summary="Precedence of character class operators.">
 * <tr><th>1&nbsp;&nbsp;&nbsp;&nbsp;</th>
 * <td>Literal escape&nbsp;&nbsp;&nbsp;&nbsp;</td>
 * <td><tt>\x</tt></td></tr>
 * <tr><th>2&nbsp;&nbsp;&nbsp;&nbsp;</th>
 * <td>Grouping</td>
 * <td><tt>[...]</tt></td></tr>
 * <tr><th>3&nbsp;&nbsp;&nbsp;&nbsp;</th>
 * <td>Range</td>
 * <td><tt>a-z</tt></td></tr>
 * <tr><th>4&nbsp;&nbsp;&nbsp;&nbsp;</th>
 * <td>Union</td>
 * <td><tt>[a-e][i-u]</tt></td></tr>
 * <tr><th>5&nbsp;&nbsp;&nbsp;&nbsp;</th>
 * <td>Intersection</td>
 * <td><tt>[a-z&amp;&amp;[aeiou]]</tt></td></tr>
 * </table></blockquote>
 *
 * <p>Note that a different set of metacharacters are in effect inside
 * a character class than outside a character class. For instance, the
 * regular expression <tt>.</tt> loses its special meaning inside a
 * character class, while the expression <tt>-</tt> becomes a range
 * forming metacharacter.</p>
 *
 * <h1 id="lt">Line terminators</h1>
 *
 * <p>A <i>line terminator</i> is a one- or two-character sequence that marks
 * the end of a line of the input character sequence. The following are
 * recognized as line terminators:</p>
 *
 * <ul>
 *
 * <li>A newline (line feed) character&nbsp;(<tt>'\n'</tt>),</li>
 *
 * <li>A carriage-return character followed immediately by a newline
 * character&nbsp;(<tt>"\r\n"</tt>),</li>
 *
 * <li>A standalone carriage-return character&nbsp;(<tt>'\r'</tt>),</li>
 *
 * <li>A next-line character&nbsp;(<tt>'&#92;u0085'</tt>),</li>
 *
 * <li>A line-separator character&nbsp;(<tt>'&#92;u2028'</tt>), or</li>
 *
 * <li>A paragraph-separator character&nbsp;(<tt>'&#92;u2029</tt>).</li>
 *
 * </ul>
 * <p>If {@link #UNIX_LINES} mode is activated, then the only line terminators
 * recognized are newline characters.</p>
 *
 * <p>The regular expression <tt>.</tt> matches any character except a line
 * terminator unless the {@link #DOTALL} flag is specified.</p>
 *
 * <p>By default, the regular expressions <tt>^</tt> and <tt>$</tt> ignore
 * line terminators and only match at the beginning and the end, respectively,
 * of the entire input sequence. If {@link #MULTILINE} mode is activated then
 * <tt>^</tt> matches at the beginning of input and after any line terminator
 * except at the end of input. When in {@link #MULTILINE} mode <tt>$</tt>
 * matches just before a line terminator or the end of the input sequence.</p>
 *
 * <h1 id="cg">Groups and capturing</h1>
 *
 * <h2 id="gnumber">Group number</h2>
 *
 * <p>Capturing groups are numbered by counting their opening parentheses from
 * left to right. In the expression <tt>((A)(B(C)))</tt>, for example, there
 * are four such groups:</p>
 *
 * <blockquote><table cellpadding=1 cellspacing=0
 * summary="Capturing group numberings">
 * <tr><th>1&nbsp;&nbsp;&nbsp;&nbsp;</th>
 * <td><tt>((A)(B(C)))</tt></td></tr>
 * <tr><th>2&nbsp;&nbsp;&nbsp;&nbsp;</th>
 * <td><tt>(A)</tt></td></tr>
 * <tr><th>3&nbsp;&nbsp;&nbsp;&nbsp;</th>
 * <td><tt>(B(C))</tt></td></tr>
 * <tr><th>4&nbsp;&nbsp;&nbsp;&nbsp;</th>
 * <td><tt>(C)</tt></td></tr>
 * </table></blockquote>
 *
 * <p>Group zero always stands for the entire expression.</p>
 *
 * <p>Capturing groups are so named because, during a match, each subsequence
 * of the input sequence that matches such a group is saved. The captured
 * subsequence may be used later in the expression, via a back reference, and
 * may also be retrieved from the matcher once the match operation is
 * complete.</p>
 *
 * <p><b>Note</b>: To use .NET's numbering for capture groups (instead of
 * Java's), specify the {@link #DOTNET_NUMBERING} flag when compiling a
 * pattern.</p>
 *
 * <h2 id="groupname">Group name</h2>
 * <p>A capturing group can also be assigned a "name", a <tt>named-capturing
 * group</tt>,
 * and then be back-referenced later by the "name". Group names are composed of
 * the following characters:
 *
 * <ul>
 * <li>The uppercase letters <tt>'A'</tt> through <tt>'Z'</tt>
 * (<tt>'&#92;u0041'</tt>&nbsp;through&nbsp;<tt>'&#92;u005a'</tt>),</li>
 * <li>The lowercase letters <tt>'a'</tt> through <tt>'z'</tt>
 * (<tt>'&#92;u0061'</tt>&nbsp;through&nbsp;<tt>'&#92;u007a'</tt>),</li>
 * <li>The digits <tt>'0'</tt> through <tt>'9'</tt>
 * (<tt>'&#92;u0030'</tt>&nbsp;through&nbsp;<tt>'&#92;u0039'</tt>),</li>
 * <li>The underscore character <tt>'_'</tt>
 * (<tt>'&#92;u005f'</tt>),</li>
 * </ul>
 *
 * <p>A <tt>named-capturing group</tt> is still numbered as described in
 * <a href="#gnumber">Group number</a>.</p>
 *
 * <p>The captured input associated with a group is always the subsequence
 * that the group most recently matched. If a group is evaluated a second time
 * because of quantification then its previously-captured value, if any, will
 * be retained if the second evaluation fails. Matching the string
 * <tt>"aba"</tt> against the expression <tt>(a(b)?)+</tt>, for example, leaves
 * group two set to <tt>"b"</tt>. All captured input is discarded at the
 * beginning of each match.</p>
 *
 * <p>Groups beginning with <tt>(?</tt> are either pure, <i>non-capturing</i>
 * groups
 * that do not capture text and do not count towards the group total, or
 * <i>named-capturing</i> groups.</p>
 *
 * <p><b>Note</b>: by default, capture group names must be unique, and if
 * multiple groups
 * with the same name exist,
 * a {@link PatternSyntaxException} is thrown. By setting the {@link #DUPLICATE_NAMES} flag, multiple capture groups
 * with
 * the same name are allowed.</p>
 *
 * <h2 id="group">Group</h2>
 *
 * <p>A <i>group</i> is either the name of a <a
 * href="Pattern.html#groupname">named-capturing group</a> or a string of
 * the form <tt>groupName[occurrence]</tt>.</p>
 *
 * <p>Use a positive occurrence (starting with 1) to refer to a specific
 * occurrence of the group. A negative occurrence is a relative occurrence of
 * the group. If the <i>occurrence</i> is omitted, or zero, the reference is to
 * the
 * first <i>matched</i> group with the specified group name. For example,
 * <tt>groupName</tt> and <tt>groupName[0]</tt> both refer to the first
 * <i>matched</i> occurrence of "groupName".</p>
 *
 * <p>This syntax allows referring to any
 * capture group in the pattern - even if the case where multiple groups
 * have the same name (see {@link #DUPLICATE_NAMES}), or the same
 * number (see <a href="Pattern.html#branchreset">"branch reset"
 * pattern</a>).</p>
 *
 * <p>Using this syntax, to refer to</p>
 *
 * <ul>
 * <li><p><b>any group</b></p>
 * <dl>
 * <dt><i>groupName</i></dt>
 * <dd>the group index wrapped in square brackets - a negative number
 * is a relative reference</dd>
 * <dt><i>occurrence</i></dt>
 * <dd> In a <a href="#branchreset">"branch reset" pattern</a> more than one
 * occurrence of the group may exist.</dd>
 * <dt><i>example</i></dt>
 * <dd>group "[1]" is equivalent to group 1; group "[1][2]" is the second
 * occurrence of group 1</dd>
 * </dl>
 * </li>
 *
 * <li><p><b>a named group</b></p>
 * <dl>
 * <dt><i>groupName</i></dt>
 * <dd>name of a <a href="Pattern.html#groupname">named-capturing group</a>
 * <dt><i>occurrence</i></dt>
 * <dd> If the {@link Pattern#DUPLICATE_NAMES} flag is set, more than one
 * occurrence of the group may exist.</dd>
 * <dt><i>example</i></dt>
 * <dd>"myGroup" refers to the first <i>matched</i> occurrence of the named
 * group, "myGroup", and "myGroup[1]" refers to just the first occurrence.</dd>
 * </dl>
 * </li>
 * </ul>
 *
 * <h2 id="branchreset">"Branch reset" pattern</h2>
 *
 * <p>Quoted from the <a href="http://pcre.org/pcre.txt">PCRE manual</a> (the
 * DUPLICATE SUBPATTERN NUMBERS section)</p>
 *
 * <blockquote>
 * <p>Perl 5.10 introduced a feature where each alternative in a subpattern
 * uses the same numbers for its capturing parentheses. Such a subpattern starts
 * with <tt>(?|</tt> and is itself a non-capturing subpattern. This construct is
 * useful when you want to capture part, but
 * not all, of one of a number of alternatives.</p>
 *
 * <p>Inside a <i>branch reset pattern</i>, capture groups are numbered as
 * usual,
 * but the number is reset at the start of each branch. The numbers of any
 * capturing buffers that follow the subpattern start after the highest number
 * used in any branch.</p>
 * </blockquote>
 *
 * <p>The following example is taken from the Perl documentation. The numbers
 * underneath show in which buffer the captured content will be stored.</p>
 *
 * <blockquote><tt> # before&nbsp;&nbsp;---------------branch-reset-----------
 * after<br>
 * / ( a )&nbsp;&nbsp;(?| x ( y ) z | (p (q) r) | (t) u (v) ) ( z ) /x<br>
 * # 1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2&nbsp;&nbsp;3&nbsp;
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;3
 * &nbsp;&nbsp;&nbsp;&nbsp;4</tt></blockquote><br>
 *
 * <p>As a note, nested <i>branch reset patterns</i> are fully supported:</p>
 *
 * <blockquote>
 * <p><tt>/&nbsp;</tt><tt>(?| ( 1a ) ( 2a ) | ( 1b )
 * (?| ( 2b1 ) | ( 2b2 ) ) )</tt><tt>&nbsp;/x<br>
 * #&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2</tt></p>
 * </blockquote>
 *
 * <p><b>Note</b>: if the {@link #DOTNET_NUMBERING} flag is set, named capture
 * groups inside
 * of a <i>branch reset pattern</i> will be numbered as if they were unnamed
 * groups. The group remains a named group, and can still be referred by
 * name.</p>
 *
 * <blockquote><tt>(?|(?&lt;One&gt;1a)(2a)|(1b)(?&lt;Two&gt;2b))<br>
 * # &nbsp;1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2
 * &nbsp;&nbsp;&nbsp;1&nbsp;&nbsp;&nbsp;2</tt></blockquote>
 *
 * <h1 id="unicode_support">Unicode support</h1>
 *
 * <p>This class is in conformance with Level 1 of <a
 * href="http://www.unicode.org/reports/tr18/"><i>Unicode Technical
 * Standard #18: Unicode Regular Expression Guidelines</i></a>, plus RL2.1
 * Canonical Equivalents.</p>
 *
 * <p>Unicode escape sequences such as <tt>&#92;u2014</tt> in Java source code
 * are processed as described in <a
 * href=
 * "http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#100850"
 * >\u00A73.3</a>
 * of the Java Language Specification. Such escape sequences are also
 * implemented directly by the regular-expression parser so that Unicode
 * escapes can be used in expressions that are read from files or from the
 * keyboard. Thus the strings <tt>"&#92;u2014"</tt> and <tt>"\\u2014"</tt>,
 * while not equal, compile into the same pattern, which matches the character
 * with hexadecimal value <tt>0x2014</tt>.</p>
 *
 * <p id="ubc">Unicode blocks and categories are written with the
 * <tt>\p</tt> and <tt>\P</tt> constructs as in
 * Perl. <tt>\p{</tt><i>prop</i><tt>}</tt> matches if the input has the
 * property <i>prop</i>, while <tt>\P{</tt><i>prop</i><tt>}</tt> does not match
 * if
 * the input has that property. Blocks are specified with the prefix
 * <tt>In</tt>, as in <tt>InMongolian</tt>. Categories may be specified with
 * the optional prefix <tt>Is</tt>: Both <tt>\p{L}</tt> and <tt>\p{IsL}</tt>
 * denote the category of Unicode letters. Blocks and categories can be used
 * both inside and outside of a character class.</p>
 *
 * <p>The supported categories are those of
 * <a href="http://www.unicode.org/unicode/standard/standard.html">
 * <i>The Unicode Standard</i></a> in the version specified by the {@link java.lang.Character Character} class. The
 * category names are those
 * defined in the Standard, both normative and informative.
 * The block names supported by <code>Pattern</code> are the valid block names
 * accepted and defined by {@link java.lang.Character.UnicodeBlock#forName(String) UnicodeBlock.forName} .</p>
 *
 * <p id="jcc">Categories that behave like the java.lang.Character
 * boolean is<i>methodname</i> methods (except for the deprecated ones) are
 * available through the same <tt>\p{</tt><i>prop</i><tt>}</tt> syntax where
 * the specified property has the name <tt>java<i>methodname</i></tt>.</p>
 *
 * <h1 id="perl_comparison">Comparison to Perl 5</h1>
 *
 * <p>The <code>Pattern</code> engine performs traditional NFA-based matching
 * with ordered alternation as occurs in Perl 5.
 *
 * <p>Perl constructs not supported by this class:</p>
 *
 * <ul>
 *
 * <li><p>The conditional constructs <tt>(?{</tt><i>X</i><tt>})</tt>,</p></li>
 *
 * <li><p> The embedded code constructs <tt>(?{</tt><i>code</i><tt>})</tt>
 * and <tt>(??{</tt><i>code</i><tt>})</tt>, and</p></li>
 *
 * <li><p> The preprocessing operations <tt>\l</tt> <tt>&#92;u</tt>,
 * <tt>\L</tt>, and <tt>\U</tt>. </p></li>
 *
 * </ul>
 *
 * <p>Constructs supported by this class but not by Perl:</p>
 *
 * <ul>
 *
 * <li><p>Possessive quantifiers, which greedily match as much as they can
 * and do not back off, even when doing so would allow the overall match to
 * succeed.</p></li>
 *
 * <li><p>Character-class union and intersection as described
 * <a href="#cc">above</a>.</p></li>
 *
 * </ul>
 *
 * <p>Notable differences from Perl:</p>
 *
 * <ul>
 *
 * <li><p>In Perl,<tt>\1</tt> through <tt>\9</tt> are always interpreted
 * as back references; a backslash-escaped number greater than <tt>9</tt> is
 * treated as a back reference if at least that many subexpressions exist,
 * otherwise it is interpreted, if possible, as an octal escape. In this
 * class octal escapes must always begin with a zero. In this class,
 * <tt>\1</tt> through <tt>\9</tt> are always interpreted as back
 * references, and a larger number is accepted as a back reference if at
 * least that many subexpressions exist at that point in the regular
 * expression, otherwise the parser will drop digits until the number is
 * smaller or equal to the existing number of groups or it is one
 * digit.</p></li>
 *
 * <li><p><b>Note</b>: specify the {@link #PERL_OCTAL} flag when compiling a pattern
 * to use Perl's octal syntax (as described above), instead of Java's.</p></li>
 *
 * <li><p>Perl uses the <tt>g</tt> flag to request a match that resumes
 * where the last match left off. This functionality is provided implicitly
 * by the {@link Matcher} class: Repeated invocations of the {@link Matcher#find find} method will resume where the last
 * match left off,
 * unless the matcher is reset.</p></li>
 *
 * <li><p>In Perl, embedded flags at the top level of an expression affect
 * the whole expression. In this class, embedded flags always take effect
 * at the point at which they appear, whether they are at the top level or
 * within a group; in the latter case, flags are restored at the end of the
 * group just as in Perl.</p></li>
 *
 * <li><p>Perl is forgiving about malformed matching constructs, as in the
 * expression <tt>*a</tt>, as well as dangling brackets, as in the
 * expression <tt>abc]</tt>, and treats them as literals. This
 * class also accepts dangling brackets but is strict about dangling
 * metacharacters like +, ? and *, and will throw a {@link PatternSyntaxException} if it encounters them.</p></li>
 *
 * </ul>
 *
 *
 * <p>For a more precise description of the behavior of regular expression
 * constructs, please see <a href="http://www.oreilly.com/catalog/regex3/">
 * <i>Mastering Regular Expressions, 3nd Edition</i>, Jeffrey E. F. Friedl,
 * O'Reilly and Associates, 2006.</a></p>
 *
 * <h1 id="numericrange">Numeric range</h1>
 *
 * <p>Regular expressions may have extensive functionality, but they are
 * designed to match text, so matching a numeric range requires some extra work.
 * Since the need to match numeric ranges is sometimes necessary, The
 * <code>Pattern</code> class has built-in support for handling them.</p>
 *
 * <p>To allow leading zeros in a match, use the syntax
 * <code>(?Z[start..end])</code>. In this case, the match's width (number of
 * digits matched), is between the number of digits in <i>start</i> and the
 * number of digits in <i>end</i>. For example, <code>(?Z[071..9])</code>
 * matches a number between 9 and 71 with between 1 and 3 digits. As the
 * previous example shows, you can specify a range as <code>[start..end]</code>
 * or as <code>[end..start]</code>. As a note, a range can have a negative
 * number for either its <i>start</i> or <i>end</i>, and the syntax remains the
 * same.</p>
 *
 * <p>In the case that one bound is
 * negative, and the other bound is positive, the match's width is as follows.
 * For a negative number, the number of digits in a match must be between 1 and
 * the number of digits in the negative bound. For a positive number, the number
 * of digits is between 1 and the number of digits in the positive bound.</p>
 *
 * <p>To not allow leading zeros in a match, use the syntax
 * <code>(?NZ[start..end])</code>. In this case, the match will not contain
 * any leading zeros. For example, <code>(?NZ[071..9])</code> will match the
 * "9", in "09", but it won't match the entire "09", since leading zeros are not
 * part of the match.</p>
 *
 * <p>For either format, by default, the numbers are decimal numbers (base 10).
 * If you want to match a range in a different base, specify the base number
 * after the "Z" or "NZ". For example, <code>(?Z16[0..ff])</code> will match a
 * hex number between 0 and 0xFF - for example, "aa".</p>
 *
 * <p>When working with bases above 10, letters are used as digits, for example,
 * in base 16, 'A' through 'F' are used to represent digits 10 through 15. By
 * default, when matching a number, both upper-case and lower-case digits are
 * allowed. For example, <code>(?Z16[0..ff])</code> will match both "AA" and
 * "aa". By specifying an 'L' or a 'U' after the base number, you can force only
 * lower or upper-case digits to match. The regex
 * <code>(?Z16U[0..ff])</code>, for example, will match "AA", but not "aa". Note
 * that regardless of this setting, in the pattern, either upper-case or
 * lower-case digits may be used. For bases 10 or less, this setting
 * has no effect, but, for consistency, can be specified - for example, the regex
 * <code>(?Z8U[0..377])</code> is equivalent to <code>(?Z8[0..377])</code>.</p>
 *
 * @see Pattern#split(CharSequence, String, int)
 * @see Pattern#split(CharSequence, String)
 */
public final class Pattern implements Serializable {
	/**
	 * Regular expression modifier values. Instead of being passed as
	 * arguments, they can also be passed as inline modifiers.
	 * For example, the following statements have the same effect.
	 * <pre>
	 * RegExp r1 = RegExp.compile("abc", Pattern.I|Pattern.M);
	 * RegExp r2 = RegExp.compile("(?im)abc", 0);
	 * </pre>
	 *
	 * The flags are duplicated so that the familiar Perl match flag
	 * names are available.
	 */

	/**
	 * Enables Unix lines mode.
	 *
	 * <p>In this mode, only the <tt>'\n'</tt> line terminator is recognized in
	 * the behavior of <tt>.</tt>, <tt>^</tt>, and <tt>$</tt>.</p>
	 *
	 * <p>Unix lines mode can also be enabled via the embedded flag
	 * expression&nbsp;<tt>(?d)</tt>.</p>
	 */
	public static final int UNIX_LINES = java.util.regex.Pattern.UNIX_LINES;

	/**
	 * Enables case-insensitive matching.
	 *
	 * <p>By default, case-insensitive
	 * matching assumes that only characters in the US-ASCII charset are being
	 * matched. Unicode-aware case-insensitive matching can be enabled by
	 * specifying the {@link #UNICODE_CASE} flag in conjunction with this
	 * flag.</p>
	 *
	 * <p>Case-insensitive matching can also be enabled via the embedded flag
	 * expression&nbsp;<tt>(?i)</tt>.</p>
	 *
	 * <p>Specifying this flag may impose a slight performance penalty.</p>
	 */
	public static final int CASE_INSENSITIVE = java.util.regex.Pattern.CASE_INSENSITIVE;

	/**
	 * Permits whitespace and comments in pattern.
	 *
	 * <p>In this mode, whitespace is ignored, and embedded comments starting
	 * with <tt>#</tt> are ignored until the end of a line.</p>
	 *
	 * <p>Comments mode can also be enabled via the embedded flag
	 * expression&nbsp; <tt>(?x)</tt>.</p>
	 */
	public static final int COMMENTS = java.util.regex.Pattern.COMMENTS;

	/**
	 * Enables multiline mode.
	 *
	 * <p>In multiline mode the expressions <tt>^</tt> and <tt>$</tt> match just
	 * after or just before, respectively, a line terminator or the end of the
	 * input sequence. By default these expressions only match at the
	 * beginning
	 * and the end of the entire input sequence.</p>
	 *
	 * <p>Multiline mode can also be enabled via the embedded flag
	 * expression&nbsp;<tt>(?m)</tt>.</p>
	 */
	public static final int MULTILINE = java.util.regex.Pattern.MULTILINE;

	/**
	 * Enables literal parsing of the pattern.
	 *
	 * <p>When this flag is specified then the input string that specifies
	 * the
	 * pattern is treated as a sequence of literal characters. Metacharacters or
	 * escape sequences in the input sequence will be given no special
	 * meaning.</p>
	 *
	 * <p>The flags CASE_INSENSITIVE and UNICODE_CASE retain their impact on
	 * matching when used in conjunction with this flag. The other flags become
	 * superfluous.</p>
	 *
	 * <p>There is no embedded flag character for enabling literal
	 * parsing.</p>
	 */
	public static final int LITERAL = java.util.regex.Pattern.LITERAL;

	/**
	 * Enables dotall mode.
	 *
	 * <p>In dotall mode, the expression <tt>.</tt> matches any character,
	 * including a line terminator. By default this expression does not match
	 * line terminators.</p>
	 *
	 * <p>Dotall mode can also be enabled via the embedded flag expression&nbsp;
	 * <tt>(?s)</tt>. (The <tt>s</tt> is a mnemonic for "single-line" mode,
	 * which is what this is called in Perl.)</p>
	 */
	public static final int DOTALL = java.util.regex.Pattern.DOTALL;

	/**
	 * Enables Unicode-aware case folding.
	 *
	 * <p>When this flag is specified then case-insensitive matching, when
	 * enabled by the {@link #CASE_INSENSITIVE} flag, is done in a manner
	 * consistent with the Unicode Standard. By default, case-insensitive
	 * matching assumes that only characters in the US-ASCII charset are being
	 * matched.</p>
	 *
	 * <p>Unicode-aware case folding can also be enabled via the embedded flag
	 * expression&nbsp;<tt>(?u)</tt>.</p>
	 *
	 * <p>Specifying this flag may impose a performance penalty.</p>
	 */
	public static final int UNICODE_CASE = java.util.regex.Pattern.UNICODE_CASE;

	/**
	 * Enables canonical equivalence.
	 *
	 * <p>When this flag is specified then two characters will be considered to
	 * match if, and only if, their full canonical decompositions match. The
	 * expression <tt>"a&#92;u030A"</tt>, for example, will match the string
	 * <tt>"&#92;u00E5"</tt> when this flag is specified. By default, matching
	 * does not take canonical equivalence into account.</p>
	 *
	 * <p>There is no embedded flag character for enabling canonical
	 * equivalence.</p>
	 *
	 * <p>Specifying this flag may impose a performance penalty.</p>
	 */
	public static final int CANON_EQ = java.util.regex.Pattern.CANON_EQ;

	/**
	 * Allows duplicate capture group names in pattern.
	 *
	 * <p>If a pattern has this flag set, multiple capture groups with the same
	 * name are allowed. By default, capture group names must be unique.</p>
	 *
	 * <p>Allowing duplicate names can also be enabled via the embedded flag
	 * expression <code>(?J)</code>.</p>
	 */
	public static final int DUPLICATE_NAMES = 0x80000000;

	/**
	 * When compiling a pattern, verifies that all referenced groups exist.
	 *
	 * <p>If this flag is set, a {@link PatternSyntaxException} will be thrown
	 * if the pattern contains a reference to a non-existent group, whereas, by
	 * default, no exception would be thrown.</p>
	 *
	 * <p>Verification of groups can also be enabled via the embedded flag
	 * expression <code>(?v)</code>.</p>
	 */
	public static final int VERIFY_GROUPS = 0x40000000;

	/**
	 * Use Perl's octal syntax (instead of Java's).
	 *
	 * <p>That is, <tt>\</tt><i>n</i> is a back reference if at least that many
	 * groups have
	 * occurred at the current point in the pattern. Otherwise, up to the first
	 * three (octal) digits are used to form an octal code, and any additional
	 * trailing digits will be treated literally.</p>
	 *
	 * <p>Using Perl's octal syntax can also be enabled via the embedded flag
	 * expression <code>(?o)</code>.</p>
	 */
	public static final int PERL_OCTAL = 0x20000000;

	/**
	 * Use .NET numbering for capture groups (instead of Java's).
	 *
	 * <p>In .NET, <i>named-capture groups</i> are numbered like unnamed groups,
	 * but numbering of named groups starts after all unnamed groups have been
	 * counted.</p>
	 *
	 * <p>For example, the expression
	 * <tt>((?&lt;One&gt;A)B)?(?&lt;Two&gt;C)(D)</tt>
	 * produces the following capturing groups by number and name.</p>
	 *
	 * <blockquote><table cellpadding=1 cellspacing=0
	 * summary="Capturing group numberings for .NET">
	 *
	 * <tr>
	 * <th bgcolor="#CCCCFF"
	 * align="left">Number<tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</tt>
	 * </th>
	 * <th bgcolor="#CCCCFF"
	 * align="left">Name<tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</tt
	 * ></th>
	 * <th bgcolor="#CCCCFF" align="left">Pattern</th>
	 * </tr>
	 *
	 * <tr>
	 * <td>0</td>
	 * <td><tt>none</tt></td>
	 * <td><tt>((?&lt;One&gt;A)B)?(?&lt;Two&gt;C)(D)</tt></td>
	 * </tr>
	 *
	 * <tr>
	 * <td>1</td>
	 * <td><tt>none</tt></td>
	 * <td><tt>((?&lt;One&gt;A)B)</tt></td>
	 * </tr>
	 *
	 * <tr>
	 * <td>2</td>
	 * <td><tt>none</tt></td>
	 * <td><tt>(D)</tt></td>
	 * </tr>
	 *
	 * <tr>
	 * <td>3</td>
	 * <td>One</td>
	 * <td><tt>(?&lt;One&gt;A)</tt></td>
	 * </tr>
	 *
	 * <tr>
	 * <td>4</td>
	 * <td>Two</td>
	 * <td><tt>(?&lt;Two&gt;C)</tt></td>
	 * </tr>
	 *
	 * </table></blockquote>
	 */
	// TODO: check about having inline modifier (doubtful)
	public static final int DOTNET_NUMBERING = 0x10000000;

	/**
	 * Enables explicit capture mode.
	 *
	 * <p>In this mode, unnamed capture groups don't capture - that is, they
	 * are treated like non-capture groups. However, named capture groups can
	 * still be used for capturing (and they acquire numbers in the usual
	 * way).</p>
	 *
	 * <p>Explicit capture mode can also be enabled via the embedded flag
	 * expression <tt>(?n)</tt>.</p>
	 *
	 * <p><b>Note</b>: this feature is taken from .NET.</p>
	 */
	public static final int EXPLICIT_CAPTURE = 0x8000000;

	/*
	 * Pattern has only two serialized components: The pattern string and the
	 * flags, which are all that is needed to recompile the pattern when it is
	 * deserialized.
	 */

	/**
	 * <p>use serialVersionUID from Merlin b59 for interoperability</p>
	 *
	 * <p><b>Note</b>: this is the same <code>serialVersionUID</code> used in
	 * the Java <code>Pattern</code> class.</p>
	 */
	private static final long serialVersionUID = 5073258162644648461L;

	/** the internal {@link java.util.regex.Pattern} object for this pattern. */
	private transient java.util.regex.Pattern internalPattern;

	/** The pattern */
	private final String pattern;

	/** The flags. */
	private final int flags;

	/**
	 * Boolean indicating this Pattern is compiled; this is necessary in order
	 * to lazily compile deserialized Patterns.
	 */
	// TODO: learn about Serialization and test Class for correctness
	private transient volatile boolean compiled = false;

	/**
	 * <p>The number of capturing groups in this Pattern.</p>
	 *
	 * <p><b>Note</b>: this is the number of capture groups in the inputted
	 * pattern, which is not necessarily the actual number of capturing groups
	 * in the (refactored) internal pattern.</p>
	 */
	private transient int capturingGroupCount;

	/**
	 * Indicates whether groups were added when creating the internal pattern.
	 *
	 * <p><b>Note</b>: Some refactorings add capture groups (e.g. "branch reset" subpattern, which are invisible to
	 * outside users, except when using the internal pattern to create a matcher
	 */
	private transient boolean addedGroups;

	/**
	 * A map with mappings from a "mapping name" to the actual group number in
	 * the internal pattern.
	 *
	 * <p><b>Note</b>: both named and unnamed groups are included</p>
	 */
	private transient Map<String, Integer> groupMapping;

	/**
	 * A map with mappings from the group name to the group count for
	 * the group.
	 */
	private transient Map<String, Integer> groupCounts;

	/**
	 * @since 0.2
	 */
	// TODO: update cache to detect common pattern which are equivalent ??
	// e.g. "(?i)abc" = "abc" with Case-insensitive flag
	private static final Map<PatternCacheKey, Pattern> patternCache = new Hashtable<>();

	/**
	 * A pattern with the RegEx being the empty string
	 */
	// public static final Pattern EMPTY_PATTERN = Pattern.compile("");

	/**
	 * A compiled Java pattern with the RegEx being the empty string.
	 */
	static final java.util.regex.Pattern JAVA_EMPTY_PATTERN = java.util.regex.Pattern.compile("");

	/**
	 * Whether to use lazy compiling or to compile on creation (default, how Java patterns are done)
	 *
	 * <p><b>Note</b>: changing this setting will not affect <code>Pattern</code>s that are already created. To
	 * force a Pattern to compile, call the {@link #forceCompile()} method.</p>
	 */
	public static boolean lazyCompiling = false;

	/**
	 * Pattern used with the {@link #naturalCompareTo(CharSequence, CharSequence)} function
	 * to provide a natural sort
	 */
	private static final java.util.regex.Pattern naturalSort = java.util.regex.Pattern
			.compile("\\G(?:(\\D++)|0*(\\d++)|$)");

	/** The natural comparator */
	private static Comparator<String> naturalComparator = Pattern::naturalCompareTo;

	/**
	 * Returns a comparator which sorts using {@link #naturalCompareTo(CharSequence, CharSequence)}, to treat embedded
	 * numbers as numbers, instead of comparing them lexicographically.
	 *
	 * <p><b>NOTE</b>: This comparator is case-sensitive, mimicking String comparisons.</p>
	 *
	 * @return the natural comparator
	 * @since 0.2
	 */
	public static Comparator<String> getNaturalComparator() {
		return naturalComparator;
	}

	private static class PatternCacheKey {
		private final String regex;
		private final int flags;

		public PatternCacheKey(final String regex, final int flags) {
			this.regex = regex;
			this.flags = flags;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.flags;
			result = prime * result + ((this.regex == null) ? 0 : this.regex.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (this.getClass() != obj.getClass()) {
				return false;
			}
			PatternCacheKey other = (PatternCacheKey) obj;
			if (this.flags != other.flags) {
				return false;
			}
			if (this.regex == null) {
				if (other.regex != null) {
					return false;
				}
			} else if (!this.regex.equals(other.regex)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return this.flags + ": " + this.regex;
		}
	}

	/**
	 * Compiles the given regular expression into a pattern.
	 *
	 * @param regex
	 *            The expression to be compiled
	 * @return The compiled <code>Pattern</code>
	 *
	 * @throws PatternSyntaxException
	 *             If the expression's patternSyntax is invalid
	 */
	public static Pattern compile(final String regex) {
		return compile(regex, 0);
	}

	/**
	 * Compiles the given regular expression into a pattern with the given
	 * flags.
	 *
	 * @param regex
	 *            The expression to be compiled
	 *
	 * @param flags
	 *            the flags
	 *
	 * @return The compiled <code>Pattern</code>
	 *
	 * @throws PatternSyntaxException
	 *             If the expression's patternSyntax is invalid
	 */
	public static Pattern compile(final String regex, final PatternOptions... flags) {
		return compile(regex, new PatternFlags(flags).intValue());
	}

	public static Pattern compile(final String regex, final int... flags) {
		int flagsTotal = 0;

		for (int flag : flags) {
			flagsTotal |= flag;
		}

		return compile(regex, flagsTotal);
	}

	/**
	 * Compiles the given regular expression into a pattern with the given
	 * flags.
	 *
	 * @param regex
	 *            The expression to be compiled
	 *
	 * @param flags
	 *            Match flags, a bit mask that may include {@link #CASE_INSENSITIVE}, {@link #MULTILINE},&nbsp;
	 *            {@link #DOTALL}, {@link #UNICODE_CASE}, {@link #CANON_EQ}, {@link #UNIX_LINES}, {@link #LITERAL},
	 *            {@link #COMMENTS},
	 *
	 *            <p>{@link #DUPLICATE_NAMES}, {@link #VERIFY_GROUPS}, {@link #PERL_OCTAL}, {@link #DOTNET_NUMBERING},
	 *            and {@link #EXPLICIT_CAPTURE}</p>
	 *
	 * @return The compiled <code>Pattern</code>
	 *
	 * @throws IllegalArgumentException
	 *             If bit values other than those corresponding to the defined
	 *             match flags are set in <tt>flags</tt>
	 *
	 * @throws PatternSyntaxException
	 *             If the expression's patternSyntax is invalid
	 */
	public static Pattern compile(final String regex, final int flags) {
		return compile(regex, flags, lazyCompiling);
	}

	static Pattern lazyCompile(final String regex) {
		return lazyCompile(regex, 0);
	}

	private static Pattern lazyCompile(final String regex, final int flags) {
		return compile(regex, flags, true);
	}

	public static Pattern compile(final String regex, final int flags, final boolean lazyCompiling) {
		PatternCacheKey key = new PatternCacheKey(regex, flags);

		Pattern cachedPattern = patternCache.get(key);

		if (cachedPattern != null) {
			return cachedPattern;
		}

		synchronized (patternCache) {
			// System.out.println("Caching (" + flags + "): " + regex);
			cachedPattern = patternCache.get(key);

			if (cachedPattern != null) {
				return cachedPattern;
			}
		}

		Pattern newPattern = new Pattern(regex, flags, lazyCompiling);

		synchronized (patternCache) {
			patternCache.put(key, newPattern);
		}

		return newPattern;
	}

	/**
	 *
	 * @param pattern
	 * @return
	 * @since 0.2
	 */
	public static Pattern valueOf(final java.util.regex.Pattern pattern) {
		PatternCacheKey key = new PatternCacheKey(pattern.pattern(), pattern.flags());
		Pattern cachedPattern = patternCache.get(key);

		if (cachedPattern != null) {
			return cachedPattern;
		}

		synchronized (patternCache) {
			cachedPattern = patternCache.get(key);

			if (cachedPattern != null) {
				return cachedPattern;
			}

			Pattern newPattern = new Pattern(pattern);
			patternCache.put(key, newPattern);

			return newPattern;
		}
	}

	/**
	 * Forces this <code>Pattern</code> to compile.
	 *
	 * <p>If the pattern has already been compiled, then this method returns immediately. Otherwise, the pattern is
	 * compiled.</p>
	 *
	 * <p>Starting with version 0.2 of RegExPlus, patterns can be lazily compiled (compiled when needed) by
	 * setting the {@link #lazyCompiling} static field to <code>true</code>. Calling this method forces these lazily
	 * compiled patterns to compile.
	 *
	 * @since 0.2
	 */
	public Pattern forceCompile() {
		if (!this.compiled) {
			synchronized (this) {
				if (!this.compiled) {
					this.compile();
				}
			}
		}

		return this;
	}

	/**
	 * Gets the internal pattern
	 *
	 * @return The internal {@link java.util.regex.Pattern} used by this
	 *         pattern.
	 */
	public java.util.regex.Pattern getInternalPattern() {
		this.forceCompile();
		return this.internalPattern;
	}

	/**
	 * Returns the regular expression from which the internal pattern was
	 * compiled.
	 *
	 * @return The source of the internal pattern
	 */
	public String internalPattern() {
		return this.getInternalPattern().pattern();
	}

	/**
	 * Returns the regular expression from which this pattern was compiled.
	 *
	 * @return The source of this pattern
	 */
	public String pattern() {
		return this.pattern;
	}

	/**
	 * Indicates whether additional capture groups were added to the internal pattern when refactoring the compiled
	 * regular expression.
	 *
	 * @return
	 * @since 0.2
	 */
	public boolean addedGroups() {
		this.forceCompile();
		return this.addedGroups;
	}

	public List<String> getGroupNames() {
		List<String> groupNames = new ArrayList<>();

		for (Entry<String, Integer> groupEntry : this.groupCounts.entrySet()) {
			String groupName = groupEntry.getKey();
			int occurrences = groupEntry.getValue();

			if (occurrences == 1) {
				groupNames.add(groupName);
			} else {
				for (int i = 1; i <= occurrences; i++) {
					groupNames.add(groupName + "[" + i + "]");
				}
			}
		}

		return Collections.unmodifiableList(groupNames);
	}

	/**
	 * Returns the number of capturing groups in this pattern.
	 *
	 * <p>Group zero denotes the entire pattern by convention. It is not
	 * included in this count.</p>
	 *
	 * <p>Any non-negative integer smaller than or equal to the value returned
	 * by this method is guaranteed to be a valid group index for this
	 * matcher.</p>
	 *
	 * @return The number of capturing groups in this matcher's pattern
	 */
	public int groupCount() {
		this.forceCompile();
		return this.capturingGroupCount;
	}

	/**
	 * Returns the number of capturing groups (with the given group index) in
	 * this pattern.
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
	 *            The group index for a capturing group in this matcher's
	 *            pattern
	 *
	 * @return The number of capturing groups (with the given group index) in
	 *         this matcher's pattern
	 * @since 0.2
	 */
	public int groupCount(final int group) {
		String groupName;

		try {
			int index = getAbsoluteGroupIndex(group, this.groupCount());
			groupName = wrapIndex(index);
		} catch (IndexOutOfBoundsException e) {
			return 0;
		} catch (IllegalArgumentException e) {
			return 0;
		}

		Integer groupCount = this.groupCounts.get(groupName);
		return groupCount != null ? groupCount : 0;

		// return groupCount(wrapIndex(group));
	}

	/**
	 * Returns the number of capturing groups (with the given group name) in
	 * this pattern.
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
	 * <p>If <code>groupName</code> is the empty string, this method's return is
	 * equal to the return from {@link #groupCount()}.</p>
	 *
	 * <p><b>Note</b>: unlike other methods, this
	 * method doesn't throw an exception if the specified group doesn't exist.
	 * Instead, zero is returned, since the number of groups with the
	 * (non-existent) group name is zero.
	 *
	 * @param groupName
	 *            The group name for a capturing group in this matcher's pattern
	 *
	 * @return The number of capturing groups (with the given group name) in
	 *         this matcher's pattern
	 */
	public int groupCount(String groupName) {
		this.forceCompile();

		try {
			groupName = this.normalizeGroupName(groupName);
		} catch (IllegalArgumentException e) {
			/*
			 * groupName is a relative unnamed group (e.g.
			 * "[-4]"), which doesn't exist, or an unnamed group whose index is
			 * not a parsable integer (e.g. "[a]")
			 */

			// Illegal group means the group count is 0
			// TODO: if not parsable int, throw exception
			return 0;
		}

		Integer groupCount = this.groupCounts.get(groupName);
		return groupCount != null ? groupCount : 0;
	}

	/**
	 * Indicates whether this pattern has any capturing groups.
	 *
	 * @return <code>true</code> if this pattern has at least one capturing group; otherwise, <code>false</code>
	 *
	 * @since 0.2
	 */
	public boolean hasGroup() {
		return this.groupCount() > 0;
	}

	/**
	 * Indicates whether this pattern contains the specified group.
	 *
	 * @param group
	 *            The group index for a capturing group in this pattern
	 * @return <code>true</code> if this pattern contains the specified group; otherwise, <code>false</code>.
	 *
	 * @since 0.2
	 */
	public boolean hasGroup(final int group) {
		return this.groupCount(group) > 0;
	}

	/**
	 * Indicates whether this pattern contains the specified group.
	 *
	 * @param group
	 *            A capturing group in this pattern
	 * @return <code>true</code> if this pattern contains the specified group; otherwise, <code>false</code>.
	 *
	 * @since 0.2
	 */
	public boolean hasGroup(final String group) {
		// TODO: parse out group name and occurrence and pass to hasGroup(groupName, occurrence)
		java.util.regex.Matcher matcher = fullGroupName.matcher(group);

		if (!matcher.matches()) {
			return false;
		}

		String groupName = matcher.group(1);
		String groupOccurrence = matcher.group(2);
		int occurrence = groupOccurrence != null ? parseInt(groupOccurrence) : 0;

		return this.hasGroup(groupName, occurrence);
		// return containsKey(group);
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
	public boolean hasGroup(final String groupName, final int occurrence) {
		int groupCount = this.groupCount(groupName);

		if (groupCount == 0) {
			return false;
		}

		try {
			getAbsoluteGroupIndex(occurrence, groupCount);
			return true;
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
	}

	/**
	 * Returns the group mapping.
	 *
	 * @return The group mapping
	 */
	Map<String, Integer> getGroupMapping() {
		return this.groupMapping;
	}

	/**
	 * Returns the string representation of this pattern. This is the regular
	 * expression from which this pattern was compiled.
	 *
	 * @return The string representation of this pattern
	 */
	@Override
	public String toString() {
		return this.pattern;
	}

	/**
	 * Creates a matcher that will match the empty string against this pattern.
	 *
	 * <p>This is commonly used to initialize an "empty" matcher with a later call to {@link Matcher#reset(CharSequence)}.</p>
	 *
	 * <p>This method can also be used in Java 8 as a Supplier via pattern::matcher</p>
	 * @return
	 * @since 1.0
	 */
	public Matcher matcher() {
		return this.matcher("");
	}

	/**
	 * Creates a matcher that will match the given input against this
	 * pattern.
	 *
	 * @param input
	 *            The character sequence to be matched
	 *
	 * @return A new matcher for this pattern
	 */
	public Matcher matcher(final CharSequence input) {
		this.forceCompile();
		return new Matcher(this.internalPattern.matcher(input), this, input);
	}

	/**
	 * Indicates whether the given input <i>partially</i> matches this
	 * <code>Pattern</code>.
	 *
	 * <p>For the given input to be a partial match, it must be the prefix
	 * of
	 * some valid match. Conversely, if this method returns <code>false</code>,
	 * then appending characters to the given input will <b>never</b>
	 * yield a
	 * match.</p>
	 *
	 * <p>For example, given the following pattern to match a decimal number
	 *
	 * <blockquote><pre>
	 * Pattern p = Pattern.compile("\\d+\\.\\d+");</pre></blockquote>
	 *
	 * The following calls return <code>true</code>
	 *
	 * <blockquote><pre>
	 * p.isPartialMatch("");
	 * p.isPartialMatch("1");
	 * p.isPartialMatch("2");
	 * p.isPartialMatch("9");
	 * p.isPartialMatch("123");
	 * p.isPartialMatch("123.");
	 * p.isPartialMatch("123.456");
	 * <span style="color: green;">// p.matcher("123.456").matches() <span
	 * >would also return true (see note below)</span></span></pre></blockquote>
	 *
	 * Whereas these calls return <code>false</code>
	 *
	 * <blockquote><pre>
	 * p.isPartialMatch("a");
	 * p.isPartialMatch(".");
	 * p.isPartialMatch(".4");
	 * p.isPartialMatch(".45");
	 * p.isPartialMatch(".456");</pre></blockquote>
	 *
	 * <p><b id="isPartialMatch_note">Note</b>: if the
	 * given input would match the pattern, this method
	 * returns <code>true</code>. That is, a match is also a partial match.</p>
	 *
	 * @param input
	 *            The character sequence to be matched
	 *
	 * @return <tt>true</tt> if, and only if, the given input partially
	 *         matches
	 *         this pattern
	 */
	/*
	 * Original Source:
	 *
	 * http://forums.sun.com/thread.jspa?messageID=4425768#4425768
	 */
	public boolean isPartialMatch(final CharSequence input) {
		Matcher m = setLastMatcher(this.matcher(input));

		if (m.matches()) {
			return true;
		}

		return m.hitEnd();
	}

	/**
	 * Returns this pattern's match flags.
	 *
	 * @return The match flags specified when this pattern was compiled
	 */
	public int flags() {
		return this.flags;
	}

	/**
	 * @return
	 * @since 0.2
	 */
	public PatternFlags getFlags() {
		return new PatternFlags(this.flags());
	}

	/**
	 * @param pattern
	 * @return
	 */
	public static Pattern normalize(final java.util.regex.Pattern pattern) {
		int flags = pattern.flags();

		if (flags == 0) {
			// return this;

			// Done this way to allow lazy compiling, since normalized forms aren't used for matchers (in my code)
			// Could instead use new Pattern(pattern), same end result
			// TODO: instead make valueOf method lazily compilable
			return lazyCompile(pattern.pattern());
		}

		/* Inline modifiers */
		String unixLines = (flags & UNIX_LINES) != 0 ? "d" : "";
		String caseInsensitive = (flags & CASE_INSENSITIVE) != 0 ? "i" : "";
		String comments = (flags & COMMENTS) != 0 ? "x" : "";
		String multiline = (flags & MULTILINE) != 0 ? "m" : "";
		String dotall = (flags & DOTALL) != 0 ? "s" : "";
		String unicodeCase = (flags & UNICODE_CASE) != 0 ? "u" : "";
		String duplicateNames = (flags & DUPLICATE_NAMES) != 0 ? "J" : "";
		String explicitCapture = (flags & EXPLICIT_CAPTURE) != 0 ? "n" : "";
		String perlOctal = (flags & PERL_OCTAL) != 0 ? "o" : "";
		String verifyGroups = (flags & VERIFY_GROUPS) != 0 ? "v" : "";

		/* No inline modifier */
		int canonEq = (flags & CANON_EQ) != 0 ? CANON_EQ : 0;
		// int verifyGroups = has(VERIFY_GROUPS) ? VERIFY_GROUPS : 0;
		// int perlOctal = has(PERL_OCTAL) ? PERL_OCTAL : 0;
		int dotnetNumbering = (flags & DOTNET_NUMBERING) != 0 ? DOTNET_NUMBERING : 0;

		if ((flags & LITERAL) != 0) {
			// Not sure if CANON_EQ is included for literal, docs says no, so following the docs

			String newFlags = caseInsensitive + unicodeCase;

			if (newFlags.length() != 0) {
				newFlags = "(?" + newFlags + ")";
			}

			// TODO: see if cannot add constructor that removes need to recompile pattern
			// Note that java 5 has a bug, where RegExPlus refactors quote blocks with escaped metacharacters
			// Other java versions, a quoted section does not need refactoring
			return lazyCompile(newFlags + quote(pattern.pattern()));
		} else {
			String newFlags = unixLines + caseInsensitive + comments + multiline + dotall + unicodeCase + duplicateNames
					+ explicitCapture + perlOctal + verifyGroups;

			if (newFlags.length() == 0) {
				// No changes are necessary, since no flags can be inlined
				// return this;
				return lazyCompile(pattern.pattern());
			} else {
				newFlags = "(?" + newFlags + ")";

				// TODO: see if cannot add constructor that removes need to recompile pattern
				return lazyCompile(newFlags + pattern.pattern(), canonEq | dotnetNumbering);
				// return Pattern.compile(flags + pattern(), canonEq | verifyGroups | dotnetNumbering);
				// return Pattern.compile(flags + pattern(), canonEq | verifyGroups | perlOctal | dotnetNumbering);
			}
		}
	}

	/**
	 * Normalizes the pattern by inlining all possible flags.
	 *
	 * <p><b>Note</b>: the returned pattern matches the exact same inputs as this pattern.</p>
	 *
	 * @return the normalized pattern
	 */
	public Pattern normalize() {
		if (this.flags() == 0) {
			return this;
		}

		/* Inline modifiers */
		String unixLines = this.has(UNIX_LINES) ? "d" : "";
		String caseInsensitive = this.has(CASE_INSENSITIVE) ? "i" : "";
		String comments = this.has(COMMENTS) ? "x" : "";
		String multiline = this.has(MULTILINE) ? "m" : "";
		String dotall = this.has(DOTALL) ? "s" : "";
		String unicodeCase = this.has(UNICODE_CASE) ? "u" : "";
		String duplicateNames = this.has(DUPLICATE_NAMES) ? "J" : "";
		String explicitCapture = this.has(EXPLICIT_CAPTURE) ? "n" : "";
		String perlOctal = this.has(PERL_OCTAL) ? "o" : "";
		String verifyGroups = this.has(VERIFY_GROUPS) ? "v" : "";

		/* No inline modifier */
		int canonEq = this.has(CANON_EQ) ? CANON_EQ : 0;
		// int verifyGroups = has(VERIFY_GROUPS) ? VERIFY_GROUPS : 0;
		// int perlOctal = has(PERL_OCTAL) ? PERL_OCTAL : 0;
		int dotnetNumbering = this.has(DOTNET_NUMBERING) ? DOTNET_NUMBERING : 0;

		if (this.has(LITERAL)) {
			// Not sure if CANON_EQ is included for literal, docs says no, so following the docs

			@SuppressWarnings("hiding")
			String flags = caseInsensitive + unicodeCase;

			if (flags.length() != 0) {
				flags = "(?" + flags + ")";
			}

			// TODO: see if cannot add constructor that removes need to recompile pattern
			// Note that java 5 has a bug, where RegExPlus refactors quote blocks with escaped metacharacters
			// Other java versions, a quoted section does not need refactoring
			return lazyCompile(flags + quote(this.pattern()));
		} else {
			@SuppressWarnings("hiding")
			String flags = unixLines + caseInsensitive + comments + multiline + dotall + unicodeCase + duplicateNames
					+ explicitCapture + perlOctal + verifyGroups;

			if (flags.length() == 0) {
				// No changes are necessary, since no flags can be inlined
				return this;
			} else {
				flags = "(?" + flags + ")";

				// TODO: see if cannot add constructor that removes need to recompile pattern
				return lazyCompile(flags + this.pattern(), canonEq | dotnetNumbering);
				// return Pattern.compile(flags + pattern(), canonEq | verifyGroups | dotnetNumbering);
				// return Pattern.compile(flags + pattern(), canonEq | verifyGroups | perlOctal | dotnetNumbering);
			}
		}
	}

	/**
	 * Compiles the given regular expression and attempts to match the given
	 * input against it.
	 *
	 * <p>An invocation of this convenience method of the form</p>
	 *
	 * <blockquote><pre>
	 * Pattern.matches(regex, input);</pre></blockquote>
	 *
	 * behaves in exactly the same way as the expression
	 *
	 * <blockquote><pre>
	 * Pattern.compile(regex).matcher(input).matches()</pre></blockquote>
	 *
	 * <p>If a pattern is to be used multiple times, compiling it once and
	 * reusing it will be more efficient than invoking this method each
	 * time.</p>
	 *
	 * @param regex
	 *            The expression to be compiled
	 *
	 * @param input
	 *            The character sequence to be matched
	 *
	 * @return <tt>true</tt> if, and only if, the entire region
	 *         sequence matches
	 *         this matcher's pattern
	 *
	 * @throws PatternSyntaxException
	 *             If the expression's patternSyntax is invalid
	 */
	public static boolean matches(final String regex, final CharSequence input) {
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(input);
		return m.matches();
	}

	/**
	 * Replaces the first substring of the given input sequence that
	 * matches the
	 * given regular expression with the given replacement.
	 *
	 * <p>An invocation of this method of the form&nbsp;
	 * <tt>Pattern.replaceFirst(</tt><i>input</i><tt>,</tt>
	 * <i>regex</i><tt>,</tt> <i>replacement</i><tt>)</tt> yields exactly the
	 * same result as the expression
	 *
	 * <blockquote><tt> {@link Pattern}.{@link Pattern#compile
	 * compile}(</tt><i>regex</i><tt
	 * >).{@link Pattern#matcher(java.lang.CharSequence)
	 * matcher}(</tt><i>input</i><tt>).{@link Matcher#replaceFirst
	 * replaceFirst}(</tt><i>replacement</i><tt>)</tt></blockquote>
	 *
	 * <p>Note that backslashes (<tt>\</tt>) and dollar signs (<tt>$</tt>) in
	 * the replacement string may cause the results to be different than if it
	 * were being treated as a literal replacement string; see {@link Matcher#replaceFirst}. Use
	 * {@link Matcher#quoteReplacement} to
	 * suppress the special meaning of these characters, if desired.</p>
	 *
	 * <p><b>Note</b>: this function serves as a substitute for {@link String#replaceFirst(String, String)}.</p>
	 *
	 * @param input
	 *            The character sequence to be matched
	 * @param regex
	 *            The regular expression to which the input sequence is to be
	 *            matched
	 * @param replacement
	 *            The string to be substituted for the first match
	 *
	 * @return The resulting <tt>String</tt>
	 *
	 * @throws PatternSyntaxException
	 *             If the regular expression's patternSyntax is invalid
	 */
	public static String replaceFirst(final CharSequence input, final String regex, final String replacement) {
		return Pattern.compile(regex).matcher(input).replaceFirst(replacement);
	}

	/**
	 * Replaces each substring of the given input sequence that matches
	 * the
	 * given regular expression with the given replacement.
	 *
	 * <p>An invocation of this method of the form&nbsp;
	 * <tt>Pattern.replaceAll(</tt><i>input</i><tt>,</tt>
	 * <i>regex</i><tt>,</tt>
	 * <i>replacement</i><tt>)</tt> yields exactly the same result as the
	 * expression
	 *
	 * <blockquote><tt> {@link Pattern}.{@link Pattern#compile
	 * compile}(</tt><i>regex</i><tt
	 * >).{@link Pattern#matcher(java.lang.CharSequence)
	 * matcher}(</tt><i>input</i><tt>).{@link Matcher#replaceAll
	 * replaceAll}(</tt><i>replacement</i><tt>)</tt></blockquote>
	 *
	 * <p>Note that backslashes (<tt>\</tt>) and dollar signs (<tt>$</tt>) in
	 * the replacement string may cause the results to be different than if it
	 * were being treated as a literal replacement string; see {@link Matcher#replaceAll}. Use
	 * {@link Matcher#quoteReplacement} to
	 * suppress the special meaning of these characters, if desired.</p>
	 *
	 * <p><b>Note</b>: this function serves as a substitute for {@link String#replaceAll(String, String)}.</p>
	 *
	 * @param input
	 *            The character sequence to be matched
	 * @param regex
	 *            The regular expression to which the input sequence is to be
	 *            matched
	 * @param replacement
	 *            The string to be substituted for each match
	 *
	 * @return The resulting <tt>String</tt>
	 *
	 * @throws PatternSyntaxException
	 *             If the regular expression's patternSyntax is invalid
	 */
	public static String replaceAll(final CharSequence input, final String regex, final String replacement) {
		return Pattern.compile(regex).matcher(input).replaceAll(replacement);
	}

	/**
	 * Splits the given input sequence around matches of the given regular
	 * expression.
	 *
	 * <p>
	 * The array returned by this method contains each substring of the
	 * input
	 * sequence that is terminated by another substring that matches the given
	 * expression or is terminated by the end of the string. The substrings in
	 * the array are in the order in which they occur in this string. If the
	 * expression does not match any part of the input then the resulting
	 * array
	 * has just one element, namely the input sequence.
	 *
	 * <p>
	 * The <tt>limit</tt> parameter controls the number of times the pattern is
	 * applied and therefore affects the length of the resulting array. If the
	 * limit <i>n</i> is greater than zero then the pattern will be applied at
	 * most <i>n</i>&nbsp;-&nbsp;1 times, the array's length will be no greater
	 * than <i>n</i>, and the array's last entry will contain all input
	 * beyond
	 * the last matched delimiter. If <i>n</i> is non-positive then the pattern
	 * will be applied as many times as possible and the array can have any
	 * length. If <i>n</i> is zero then the pattern will be applied as many
	 * times as possible, the array can have any length, and trailing empty
	 * strings will be discarded.
	 *
	 * <p>
	 * The string <tt>"boo:and:foo"</tt>, for example, yields the following
	 * results with these parameters:
	 *
	 * <blockquote>
	 * <table cellpadding=1 cellspacing=0
	 * summary="Split example showing regex, limit, and result">
	 * <tr>
	 * <th>Regex</th>
	 * <th>Limit</th>
	 * <th>Result</th>
	 * </tr>
	 * <tr>
	 * <td align=center>:</td>
	 * <td align=center>2</td>
	 * <td><tt>{ "boo", "and:foo" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>:</td>
	 * <td align=center>5</td>
	 * <td><tt>{ "boo", "and", "foo" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>:</td>
	 * <td align=center>-2</td>
	 * <td><tt>{ "boo", "and", "foo" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>o</td>
	 * <td align=center>5</td>
	 * <td><tt>{ "b", "", ":and:f", "", "" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>o</td>
	 * <td align=center>-2</td>
	 * <td><tt>{ "b", "", ":and:f", "", "" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>o</td>
	 * <td align=center>0</td>
	 * <td><tt>{ "b", "", ":and:f" }</tt></td>
	 * </tr>
	 * </table>
	 * </blockquote>
	 *
	 * <p>
	 * An invocation of this method of the&nbsp;form&nbsp;
	 * <tt>Pattern.split(</tt><i>input</i><tt>,</tt>
	 * <i>regex</i><tt>,</tt>
	 * <i>n</i><tt>)</tt> yields the same result as the expression
	 *
	 * <blockquote><tt> {@link Pattern}.{@link Pattern#compile
	 * compile}(</tt><i>regex</i><tt>).{@link Pattern#split(CharSequence, int)
	 * split}(</tt><i>input</i><tt>,</tt><i>limit</i><tt>)</tt></blockquote>
	 *
	 * <p><b>Note</b>: this function serves as a substitute for {@link String#split(String, int)}.</p>
	 *
	 * @param input
	 *            The character sequence to be split
	 * @param regex
	 *            The delimiting regular expression
	 * @param limit
	 *            The result threshold, as described above
	 *
	 * @return The array of strings computed by splitting the input
	 *         sequence around matches of the given regular expression
	 *
	 * @throws PatternSyntaxException
	 *             If the regular expression's patternSyntax is invalid
	 */
	public static String[] split(final CharSequence input, final String regex, final int limit) {
		return Pattern.compile(regex).split(input, limit);
	}

	/**
	 * Splits this string around matches of the given regular expression.
	 *
	 * <p>
	 * This method works as if by invoking the three-argument {@link #split(CharSequence, String, int) split} method
	 * with the given
	 * input sequence, expression and a limit argument of zero. Trailing
	 * empty
	 * strings are therefore not included in the resulting array.
	 * </p>
	 *
	 * <p>
	 * The string <tt>"boo:and:foo"</tt>, for example, yields the following
	 * results with these expressions:
	 *
	 * <blockquote>
	 * <table cellpadding=1 cellspacing=0
	 * summary="Split examples showing regex and result">
	 * <tr>
	 * <th>Regex</th>
	 * <th>Result</th>
	 * </tr>
	 * <tr>
	 * <td align=center>:</td>
	 * <td><tt>{ "boo", "and", "foo" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>o</td>
	 * <td><tt>{ "b", "", ":and:f" }</tt></td>
	 * </tr>
	 * </table>
	 * </blockquote>
	 *
	 * <p><b>Note</b>: this function serves as a substitute for {@link String#split(String)}.</p>
	 *
	 * @param input
	 *            The character sequence to be split
	 * @param regex
	 *            The delimiting regular expression
	 *
	 * @return The array of strings computed by splitting the input
	 *         sequence
	 *         around matches of the given regular expression
	 *
	 * @throws PatternSyntaxException
	 *             If the regular expression's patternSyntax is invalid
	 */
	public static String[] split(final CharSequence input, final String regex) {
		return Pattern.compile(regex).split(input, 0);
	}

	/**
	 * Splits the given input sequence around matches of this pattern.
	 *
	 * <p>
	 * The array returned by this method contains each substring of the
	 * input
	 * sequence that is terminated by another subsequence that matches this
	 * pattern or is terminated by the end of the input sequence. The
	 * substrings
	 * in the array are in the order in which they occur in the input. If
	 * this
	 * pattern does not match any subsequence of the input then the
	 * resulting
	 * array has just one element, namely the input sequence in string
	 * form.
	 *
	 * <p>
	 * The <tt>limit</tt> parameter controls the number of times the pattern is
	 * applied and therefore affects the length of the resulting array. If the
	 * limit <i>n</i> is greater than zero then the pattern will be applied at
	 * most <i>n</i>&nbsp;-&nbsp;1 times, the array's length will be no greater
	 * than <i>n</i>, and the array's last entry will contain all input
	 * beyond
	 * the last matched delimiter. If <i>n</i> is non-positive then the pattern
	 * will be applied as many times as possible and the array can have any
	 * length. If <i>n</i> is zero then the pattern will be applied as many
	 * times as possible, the array can have any length, and trailing empty
	 * strings will be discarded.
	 *
	 * <p>
	 * The input <tt>"boo:and:foo"</tt>, for example, yields the following
	 * results with these parameters:
	 * </p>
	 *
	 * <blockquote>
	 * <table cellpadding="1" cellspacing="0" summary="Split examples showing regex, limit, and result">
	 * <tr>
	 * <th>
	 * <P align="left">
	 * <i>Regex&nbsp;&nbsp;&nbsp;&nbsp;</i></th>
	 * <th>
	 * <P align="left">
	 * <i>Limit&nbsp;&nbsp;&nbsp;&nbsp;</i></th>
	 * <th>
	 * <P align="left">
	 * <i>Result&nbsp;&nbsp;&nbsp;&nbsp;</i></th>
	 * </tr>
	 * <tr>
	 * <td align=center>:</td>
	 * <td align=center>2</td>
	 * <td><tt>{ "boo", "and:foo" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>:</td>
	 * <td align=center>5</td>
	 * <td><tt>{ "boo", "and", "foo" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>:</td>
	 * <td align=center>-2</td>
	 * <td><tt>{ "boo", "and", "foo" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>o</td>
	 * <td align=center>5</td>
	 * <td><tt>{ "b", "", ":and:f", "", "" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>o</td>
	 * <td align=center>-2</td>
	 * <td><tt>{ "b", "", ":and:f", "", "" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>o</td>
	 * <td align=center>0</td>
	 * <td><tt>{ "b", "", ":and:f" }</tt></td>
	 * </tr>
	 * </table>
	 * </blockquote>
	 *
	 *
	 * @param input
	 *            The character sequence to be split
	 *
	 * @param limit
	 *            The result threshold, as described above
	 *
	 * @return The array of strings computed by splitting the input around
	 *         matches of this pattern
	 */
	public String[] split(final CharSequence input, final int limit) {
		return this.getInternalPattern().split(input, limit);
	}

	/**
	 * Splits the given input sequence around matches of this pattern.
	 *
	 * <p>
	 * This method works as if by invoking the two-argument {@link #split(java.lang.CharSequence, int) split} method
	 * with the given
	 * input sequence and a limit argument of zero. Trailing empty strings
	 * are
	 * therefore not included in the resulting array.
	 * </p>
	 *
	 * <p>
	 * The input <tt>"boo:and:foo"</tt>, for example, yields the following
	 * results with these expressions:
	 *
	 * <blockquote>
	 * <table cellpadding=1 cellspacing=0 summary="Split examples showing regex and result">
	 * <tr>
	 * <th>
	 * <P align="left">
	 * <i>Regex&nbsp;&nbsp;&nbsp;&nbsp;</i></th>
	 * <th>
	 * <P align="left">
	 * <i>Result</i></th>
	 * </tr>
	 * <tr>
	 * <td align=center>:</td>
	 * <td><tt>{ "boo", "and", "foo" }</tt></td>
	 * </tr>
	 * <tr>
	 * <td align=center>o</td>
	 * <td><tt>{ "b", "", ":and:f" }</tt></td>
	 * </tr>
	 * </table>
	 * </blockquote>
	 *
	 *
	 * @param input
	 *            The character sequence to be split
	 *
	 * @return The array of strings computed by splitting the input around
	 *         matches of this pattern
	 */
	public String[] split(final CharSequence input) {
		return this.getInternalPattern().split(input, 0);
	}

	/**
	 * Returns a literal pattern <code>String</code> for the specified
	 * <code>String</code>.
	 *
	 * <p>
	 * This method produces a <code>String</code> that can be used to create a
	 * <code>Pattern</code> that would match the string <code>s</code> as if it
	 * were a literal pattern.
	 * </p>
	 *
	 * <p>
	 * Metacharacters or escape sequences in the input sequence will be
	 * given no
	 * special meaning.
	 * </p>
	 *
	 * @param s
	 *            The string to be literalized
	 * @return A literal string replacement
	 */
	public static String quote(final String s) {
		return java.util.regex.Pattern.quote(s);
	}

	/**
	 * Java regular expression metacharacters.
	 *
	 * <p><code>\.*?+[]{|()^$#</code></p>
	 *
	 * <p><b>Note:</b> '#' is included in the escapped metacharacters in case {@link Pattern#COMMENTS COMMENTS} is
	 * enabled, and the escaped text in only a part of the regular expression, which occurs when the COMMENTS flag is
	 * enabled.</p>
	 *
	 * <p><b>Note</b>: ']' is included in the case that the regular expression is used in a regex tool that
	 * requires the closing brace to be escaped (for example, Javascript). Java does not require it to be
	 * escaped, but escaping it does no harm.</p>
	 *
	 * <p><b>Note</b>: '}' is included in the case that the regular expression is used in a regex tool that
	 * requires the closing curly brace to be escaped (for example, Android development). Java does not require it to be
	 * escaped, but escaping it does no harm.</p>
	 */
	public static final String REGEX_METACHARACTERS = "\\.*?+[]{}|()^$#";

	/**
	 * Java regular expression metacharacters when within a character class (for example, <code>[abc]</code>).
	 *
	 * <p><code>^-[]\#&amp;</code></p>
	 *
	 * <p><b>Note</b>: '#' is included in the escapped metacharacters in case {@link Pattern#COMMENTS COMMENTS} is
	 * enabled, and the escaped text in only a part of the regular expression, which occurs when the COMMENTS flag is
	 * enabled.</p>
	 */
	public static final String REGEX_CHAR_CLASS_METACHARACTERS = "^-[]\\#&";

	/** Pattern to match any character. */
	// private static final Pattern anyCharacterPattern = compile(".", DOTALL);

	/**
	 * Pattern used to escape metacharacters.
	 */
	// static final Pattern ESCAPE_REGEX_METACHARACTERS = escapeMetacharacters(REGEX_METACHARACTERS);

	/**
	 * Pattern used to escape metacharacters found in a character class.
	 */
	// static final Pattern ESCAPE_REGEX_CHAR_CLASS_METACHARS = escapeMetacharacters(REGEX_CHAR_CLASS_METACHARACTERS);

	/**
	 * Returns a literal pattern <code>String</code> for the specified
	 * <code>String</code>.
	 *
	 * <p>This method produces a <code>String</code> that can be used to
	 * create a <code>Pattern</code> that would match the string <code>s</code> as
	 * if it were a literal pattern.</p>
	 *
	 * <p>Metacharacters or escape sequences in the input sequence will be given no special meaning.</p>
	 *
	 * <p>This method escapes the metacharacters specified by {@link #REGEX_METACHARACTERS}:
	 * <code>\.*?+[]{|()^$#</code></p>
	 *
	 * <p><b>Note</b>: this function escapes each metacharacter individually,
	 * whereas {@link Pattern#quote(String)} uses a <code>\Q..\E</code> block. This
	 * method can be used to create a regular expression to use with tools that don't support <code>\Q..\E</code>
	 * blocks.</p>
	 *
	 * @param s
	 *            The string to be literalized
	 * @return A literal string replacement
	 */
	public static String literal(final String s) {
		return literal(s, REGEX_METACHARACTERS);
	}

	/**
	 * Returns a literal pattern <code>String</code> for the specified
	 * <code>String</code>.
	 *
	 * <p>This method produces a <code>String</code> that can be used to
	 * create a <code>Pattern</code> that would match the string <code>s</code> as
	 * if it were a literal pattern.</p>
	 *
	 * <p>The specified <code>metacharacters</code> or escape sequences in the input sequence will be given no special
	 * meaning.</p>
	 *
	 * <p><b>Note</b>: this function escapes each metacharacter individually,
	 * whereas {@link Pattern#quote(String)} uses a <code>\Q..\E</code> block. This
	 * method can be used to create a regular expression to use with tools that don't support <code>\Q..\E</code>
	 * blocks.</p>
	 *
	 * @param s
	 *            The string to be literalized
	 * @param metacharacters
	 *            the metacharacters to escape
	 * @return A literal string replacement
	 */
	public static String literal(final String s, final String metacharacters) {
		// literal.length() will be at least s.length, but no more than 2*s.length()
		StringBuilder literal = new StringBuilder(s.length());

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
				literal.append(c);
				continue;
			}

			if (metacharacters.indexOf(c) != -1) {
				literal.append('\\');
			}

			literal.append(c);
		}

		return literal.toString();

		// return literal(s, escapeMetacharacters(metacharacters));
	}

	/**
	 * Returns a literal pattern <code>String</code> for the specified
	 * <code>String</code>.
	 *
	 * <p>This method produces a <code>String</code> that can be used to
	 * create a <code>Pattern</code> that would match the string <code>s</code> as
	 * if it were a literal pattern.</p>
	 *
	 * <p>Metacharacters or escape sequences in the input sequence will be given no special meaning.</p>
	 *
	 * <p>This method escapes the following metacharacters: <code></code></p>
	 *
	 * <p><b>Note</b>: this function escapes each metacharacter individually,
	 * whereas {@link Pattern#quote(String)} uses a <code>\Q..\E</code> block. This
	 * method can be used to create a regular expression to use with tools that don't support <code>\Q..\E</code>
	 * blocks.</p>
	 *
	 * @param s
	 *            The string to be literalized
	 * @param escapeMetachars
	 *            pattern to match the metacharacters to escape
	 * @return A literal string replacement
	 */
	// static String literal(String s, Pattern escapeMetachars)
	// {
	// return escapeMetachars.matcher(s).replaceAll("\\\\$0");
	// }

	/**
	 * Returns a pattern which will match any single metacharacter in the specified metacharacters.
	 *
	 * @param metacharacters
	 *            the metacharacters
	 * @return a pattern which will match any single metacharacter in the specified metacharacters
	 */
	// private static Pattern escapeMetacharacters(String metacharacters)
	// {
	// return compile("[" + literal(metacharacters, anyCharacterPattern) + "]", 0, false);
	// }

	/**
	 * Recompile the Pattern instance from a stream. The original pattern string
	 * is read in and the object tree is recompiled from it.
	 */
	private void readObject(final java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
		// Read in all fields
		s.defaultReadObject();

		// this.caseInsensitiveGroupNames = has(CASE_INSENSITIVE_NAMES);
		this.groupMapping = new HashMap<>(2);

		// if length > 0, the Pattern is lazily compiled
		this.compiled = false;
		if (this.pattern.length() == 0) {
			this.initializeEmptyPattern();
		}

		// TODO: cache pattern here??
	}

	private void initializeForZeroGroups() {
		/*
		 * Expected results (with 0 groups):
		 *
		 * groupMapping : {[0][1]=0}
		 * -size: 1
		 */
		this.groupMapping = new HashMap<>(1);

		// Map [i][1] -> i (e.g. [0][1]=0)
		this.groupMapping.put(getMappingName(0, 1), 0);

		/*
		 * Expected results (with 0 groups):
		 *
		 * groupCounts: {=0, [0]=1}
		 * -size: 2
		 */
		this.groupCounts = new HashMap<>(2);

		// Map empty string group name to group count
		this.groupCounts.put("", 0);

		// Map [i] -> 1 (e.g. [0]=1)
		this.groupCounts.put(wrapIndex(0), 1);

		/* Initialize pattern values */
		this.capturingGroupCount = 0;
		this.addedGroups = false;
	}

	private void initializeEmptyPattern() {
		this.initializeForZeroGroups();
		this.setInternalPattern("");
		this.compiled = true;
	}

	/**
	 * This private constructor is used to create all Patterns (other than those upcasted from Java patterns).
	 * The pattern string and match flags are all that is needed to completely describe a
	 * Pattern.
	 *
	 * @param regex
	 *            the expression to be compiled
	 * @param flags
	 *            match flags bit mask
	 * @param lazyCompiling
	 *            whether to lazily compile pattern
	 */
	private Pattern(final String regex, final int flags, final boolean lazyCompiling) {
		// TODO: derive method to perform a simple check on regex to see if refactoring is necessary
		// many regexes don't need to be refactored, try to detect some

		// Optimize refactoring

		this.pattern = regex;
		this.flags = flags;

		// this.caseInsensitiveGroupNames = has(CASE_INSENSITIVE_NAMES);

		if (regex.length() > 0) {
			if (!lazyCompiling) {
				this.compile();
			}
		} else {
			this.initializeEmptyPattern();
		}
	}

	/**
	 * Constructor to create a Pattern object from a Java Pattern.
	 *
	 * <p><b>Note</b>: no compiling or refactoring of the pattern is performed,
	 * since the Pattern is already compiled and valid for use by Java's Regular
	 * Expression engine.</p>
	 *
	 * @param pattern
	 *            the pattern
	 * @since 0.2
	 */
	Pattern(final java.util.regex.Pattern pattern) {
		this.pattern = pattern.pattern();
		this.flags = pattern.flags();

		int groupCount = pattern.matcher("").groupCount();
		this.capturingGroupCount = groupCount;

		// Initialize groupMapping and groupCounts
		this.groupMapping = new HashMap<>(groupCount + 1);
		this.groupCounts = new HashMap<>(groupCount + 2);

		/*
		 * Expected results (with three groups):
		 *
		 * groupMapping : {[0][1]=0, [1][1]=1, [2][1]=2, [3][1]=3}
		 * -size: groupCount + 1
		 *
		 * groupCounts: {=3, [0]=1, [1]=1, [2]=1, [3]=1}
		 * -size: groupCount + 2
		 */

		// Map empty string group name to group count
		this.groupCounts.put("", groupCount);

		for (int i = 0; i <= groupCount; i++) {
			String groupName = wrapIndex(i);

			// Map [i][1] -> i (e.g. [0][1]=0)
			this.groupMapping.put(getMappingName(groupName, 1), i);

			// Map [i] -> 1 (e.g. [0]=1)
			this.groupCounts.put(groupName, 1);
		}

		// For Java 7, adds named groups (if any)
		try {
			// Get map of group names -> group index
			Field field;
			field = pattern.getClass().getDeclaredField("namedGroups");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<String, Integer> javaGroupMapping = (Map<String, Integer>) field.get(pattern);

			// Null if doesn't have any named groups
			if (javaGroupMapping != null) {
				// Add entries for each named group
				for (Entry<String, Integer> entry : javaGroupMapping.entrySet()) {
					String groupName = entry.getKey();
					Integer groupNumber = entry.getValue();

					// Map groupName[1] -> groupNumber
					this.groupMapping.put(getMappingName(groupName, 1), groupNumber);

					// Map groupName -> 1 (e.g. [0]=1)
					// (Always valid, since Java doesn't allow duplicate groups)
					this.groupCounts.put(groupName, 1);
				}
			}
		} catch (Exception e) {
			// Do nothing
		}

		this.internalPattern = pattern;
		this.compiled = true;
	}

	/* Methods called ONLY when initializing a Pattern */
	void setCapturingGroupCount(final int capturingGroupCount) {
		this.capturingGroupCount = capturingGroupCount;
	}

	void setAddedGroups(final boolean addedGroups) {
		this.addedGroups = addedGroups;
	}

	int getGroupCount(final String groupName) {
		Integer groupCount = this.groupCounts.get(groupName);
		return (groupCount == null ? 0 : groupCount);
	}

	/**
	 * Gets the mapping from group names to group counts
	 *
	 * <p>For named groups, the key is the group name</p>
	 * <p>For unnamed groups, the key is the group number surrounded by '[' and ']'. For example, group 1 would be the
	 * key "[1]" in the returned map</p>
	 *
	 * @return an unmodifiable map from group names to group counts
	 */
	public Map<String, Integer> getGroupCounts() {
		return Collections.unmodifiableMap(this.groupCounts);
	}

	void setGroupCounts(final Map<String, Integer> groupCounts) {
		this.groupCounts = groupCounts;
	}

	/**
	 * Refactors the regular expression to an equivalent form usable by Java's {@link java.util.regex.Pattern} class,
	 * and compiles the internal
	 * <code>Pattern</code> using the refactored regular expression.
	 */
	private void compile() {
		// System.out.println("Compiling (" + flags + "): " + pattern);

		String refactoredPattern;
		Refactor refactor;

		if (this.has(LITERAL)) {
			this.initializeForZeroGroups();
			refactor = null;
			refactoredPattern = this.pattern;
		} else {
			this.groupMapping = new HashMap<>(2);
			// refactor <pattern> to be used as a RegEx pattern
			// Refactor refactor = new Refactor(pattern);

			refactor = new Refactor(this);
			refactoredPattern = refactor.toString();
		}

		try {
			// System.out.println(refactoredPattern);

			// setInternalPattern(refactor.toString());
			this.setInternalPattern(refactoredPattern);
		} catch (java.util.regex.PatternSyntaxException e) {
			// // on error, show error using the original pattern
			// // (not refactored form)
			//
			// System.out.println(e.getMessage());
			//
			String desc = e.getDescription();
			int index;

			try {
				// TODO: can sometimes be incorrect with subpatterns
				// e.g. "(?+1)(?<test>*)"
				index = refactor == null ? -1 : refactor.changes.getOriginalIndex(e.getIndex());
			} catch (IllegalArgumentException e1) {
				// Unknown original index
				// Can occurn, for example, in the regex "(?<test>*\\01)"
				index = -1;
			}

			// System.out.println(refactor.result);
			throw new PatternSyntaxException(desc, this.pattern, index);
			//
			// PatternErrorMessage errorMessage = PatternErrorMessage
			// .getValue(desc);
			//
			// if (errorMessage != null)
			// throw new PatternSyntaxException(errorMessage, pattern, index);
			// else
			// throw new PatternSyntaxException(desc, pattern, index);
		}
		// catch (info.codesaway.util.regex.PatternSyntaxException e)
		// {
		// int index = refactor.changes.getOriginalIndex(e.getIndex());
		// PatternErrorMessage errorMessage = e.getErrorMessage();
		//
		// if (errorMessage != null)
		// throw new PatternSyntaxException(errorMessage, pattern, index);
		// else
		// {
		// int errorCode = e.getErrorCode();
		// String desc = e.getDescription();
		//
		// throw new PatternSyntaxException(errorCode, desc, pattern, index);
		// }
		// }

		this.compiled = true;
	}

	/**
	 * Sets the internal <code>Pattern</code> to the <code>Pattern</code>
	 * returned when calling
	 *
	 * <blockquote><tt>{@link java.util.regex.Pattern}.</ {@link java.util.regex.Pattern#compile(String, int)
	 * compile}(</tt><i>regex</i> <tt>,</tt> <i>flags</i><tt>)</tt>
	 * </blockquote>
	 *
	 * @param regex
	 *            the expression to be compiled
	 * @see java.util.regex.Pattern#compile(String, int)
	 */
	private void setInternalPattern(final String regex) {
		// keep all flags except those introduced in this class
		this.internalPattern = java.util.regex.Pattern.compile(regex,
				this.flags & ~(DUPLICATE_NAMES | VERIFY_GROUPS | DOTNET_NUMBERING | EXPLICIT_CAPTURE | PERL_OCTAL));
	}

	/**
	 * Indicates whether a particular flag is set or not.
	 *
	 * @param f
	 *            the flag to test
	 * @return <code>true</code> if the particular flag is set
	 */
	boolean has(final int f) {
		return (this.flags & f) != 0;
	}

	/**
	 * @param flag
	 * @return
	 * @since 0.2
	 */
	public boolean has(final PatternFlag flag) {
		return this.has(flag.intValue());
	}

	/**
	 * Compares two character sequences lexigraphically,
	 * except that embedded numbers are treated numerically.
	 *
	 * <p>For example, when using this method, "1.2.9.1" is less than
	 * "1.2.10.5", whereas a lexigraphical comparison would yield the
	 * opposite.</p>
	 *
	 * <p>When comparing, leading zeros are ignored, unless the inputted
	 * sequences are otherwise
	 * equivalent. If the two inputs are identical, then 0 is
	 * returned. Otherwise, the left-most number
	 * where the number of leading zeros differs is used to determine the
	 * ordering. In this case, the one with more leading zeros is first.</p>
	 *
	 * <p>For example, the below list is sorted in increasing order:</p>
	 *
	 * <ol>
	 * <li>2009-1-2</li>
	 * <li>2009-01-05</li>
	 * <li>2009-01-5</li>
	 * <li>2009-1-05</li>
	 * <li>2009-1-5</li>
	 * </ol>
	 *
	 * <p>This function can be used to compare versions, dates, and other
	 * numeric based data. Since the comparison is done from left to right, the
	 * format must have the most significant part first. For example, in a date
	 * format, that would be year, month, and then day to sort in chronological
	 * order.</p>
	 *
	 * <p>Note that for correct sorting of numeric based data, the format's must
	 * be identical - otherwise, where the formats differ, the sorting is based
	 * on the ascii value of the change in the format.
	 * For example, the date "2009-1-5" is less
	 * than "2009.1.2", but not chronologically before. This result is due to
	 * the ascii value for '-' (&#92;u2d) being less than the ascii value for
	 * '.' (&#92;u2e).</p>
	 *
	 * <p>This method can be called in the compare function of a {@link Comparator} object
	 * to provide sorting.</p>
	 *
	 * <blockquote><pre>
	 * Comparator&lt;String&gt; comparator = new Comparator&lt;String&gt;() {
	 *
	 * &nbsp;&nbsp;public int compare(String o1, String o2) {
	 * &nbsp;&nbsp;&nbsp;&nbsp;return naturalCompareTo(o1, o2);
	 * &nbsp;&nbsp;}
	 * };</pre></blockquote>
	 *
	 * @param value1
	 *            the first character sequence
	 * @param value2
	 *            the second character sequence
	 * @return 0 if the two values are equal, -1 if the first value is
	 *         "less than" the second, and 1 if the first value is
	 *         "greater than" the second.
	 *
	 * @see String#compareTo(String)
	 */
	public static int naturalCompareTo(final CharSequence value1, final CharSequence value2) {
		// XXX: don't use regex - performance hit
		java.util.regex.Matcher matcher1 = naturalSort.matcher(value1);
		java.util.regex.Matcher matcher2 = naturalSort.matcher(value2);

		// left-most number with different number of leading zeros
		// (used to break ties if strings are otherwise equivalent)
		int leftMostDifference = 0;

		while (matcher1.find() && matcher2.find()) {
			String match1 = matcher1.group();
			String match2 = matcher2.group();

			if (match1.length() == 0) {

				if (match2.length() == 0) {
					// equivalent - return leftMostDifference
					return leftMostDifference;
				}

				// string2 is "longer"
				return -1;
			} else if (match2.length() == 0) {
				// string1 is "longer"
				return 1;
			}

			boolean isNumber1 = matcher1.start(2) != -1;
			boolean isNumber2 = matcher2.start(2) != -1;

			if (isNumber1 && isNumber2) {
				// both numbers - compare numerically

				String number1 = matcher1.group(2);
				String number2 = matcher2.group(2);

				if (number1.length() != number2.length()) {
					return number1.length() < number2.length() ? -1 : 1;
				}

				int compareTo = number1.compareTo(number2);

				if (compareTo != 0) {
					return compareTo;
				}

				if (leftMostDifference == 0) {
					// only do once - for left-most difference

					if (match1.length() != match2.length()) {
						// different number of leading zeros
						// e.g. "01" and "1"

						// more leading zeros first "01" < "1"
						leftMostDifference = match1.length() > match2.length() ? -1 : 1;
					}
				}
			} else {
				int compareTo = match1.compareTo(match2);

				if (compareTo != 0) {
					return compareTo;
				}
			}
		}

		return 0;
	}

	/**
	 * Normalizes the group name.
	 *
	 * @param groupName
	 *            the group name
	 * @return the normalized group name
	 * @throws IllegalArgumentException
	 *             If <code>groupName</code> is a relative unnamed group (e.g.
	 *             "[-4]"), which doesn't exist, or if <code>groupName</code> is
	 *             an unnamed group whose index is not a parsable integer (e.g.
	 *             "[a]")
	 */
	String normalizeGroupName(final String groupName) {
		// System.out.println(groupName);

		if (groupName.startsWith("[") && groupName.endsWith("]")) {
			try {
				int index = getAbsoluteGroupIndex(parseInt(groupName.substring(1, groupName.length() - 1)),
						this.groupCount());
				return wrapIndex(index);
			} catch (IndexOutOfBoundsException e) {
				throw noNamedGroup(groupName);
			} catch (IllegalArgumentException e) {
				throw noNamedGroup(groupName);
			}
		}
		// else if (groupCounts.get(groupName) == null) {
		// try {
		// // Check if numbered group
		// int groupNumber = Integer.parseInt(groupName);
		//
		// if (groupCount(groupNumber) != 0)
		// groupName = wrapIndex(getAbsoluteGroupIndex(groupNumber, groupCount()));
		// } catch (Exception e) {
		// }
		// }

		return groupName;
	}

	/* Groovy methods - makes RegExPlus groovier */

	/**
	 * 'Case' implementation for this class, which allows
	 * testing a String against a number of regular expressions (<b>in Groovy only</b>).
	 * For example:
	 * <pre>switch( str ) {
	 * case +/one/ :
	 * // the regex 'one' matches the value of str
	 * }
	 * </pre>
	 *
	 * @param switchValue
	 *            the switch value
	 * @return <code>true</code> if the <code>switchValue</code> is deemed to match this <code>Pattern</code>
	 * @since 0.2
	 */
	public boolean isCase(final Object switchValue) {
		if (switchValue == null) {
			// return caseValue == null;

			// Since this != null, always return false if switch value is null
			return false;
		}

		final Matcher matcher = this.matcher(switchValue.toString());
		if (matcher.matches()) {
			RegExPlusSupport.setLastMatcher(matcher);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Alias for {@link #getInternalPattern()}.
	 *
	 * @return the regular expression pattern
	 * @since 0.2
	 */
	public java.util.regex.Pattern bitwiseNegate() {
		return this.getInternalPattern();
	}

	/**
	 * Returns this <code>Pattern</code>.
	 *
	 * <p>Added for consistency for use in Groovy, since both +charSequence and +javaPattern are also supported.
	 * This method ensures that the 'positive' operator will return a RegExPlus Pattern, for all three cases:</p>
	 *
	 * <ol>
	 * <li><b>Compiling a CharSequence regex</b>: <code>+charSequence</code></li>
	 * <li><b>Promoting a Java Pattern</b>: <code>+javaPattern</code></li>
	 * <li><b>When used on an existing RegExPlus Pattern</b>: <code>+regexPlusPattern</code></li>
	 * </ol>
	 *
	 * @return this <code>Pattern</code>.
	 */
	public Pattern positive() {
		return this;
	}

	/**
	 * @param regex
	 * @return
	 * @since 0.2
	 */
	public Pattern or(final CharSequence regex) {
		Pattern pattern1 = this.normalize();

		return or(pattern1.pattern(), pattern1.flags(), regex, 0);
	}

	/**
	 *
	 * @param pattern
	 * @return
	 * @since 0.2
	 */
	public Pattern or(final Pattern pattern) {
		Pattern pattern1 = this.normalize();
		Pattern pattern2 = pattern.normalize();

		return or(pattern1.pattern(), pattern1.flags(), pattern2.pattern(), pattern2.flags());
	}

	/**
	 *
	 * @param pattern
	 * @return
	 * @since 0.2
	 */
	public Pattern or(final java.util.regex.Pattern pattern) {
		Pattern pattern1 = this.normalize();
		Pattern pattern2 = normalize(pattern);

		return or(pattern1.pattern(), pattern1.flags(), pattern2.pattern(), pattern2.flags());
	}

	private static Pattern or(final String regex1, final int flags1, final CharSequence regex2, final int flags2) {
		if (flags1 != flags2) {
			throw new IllegalArgumentException(
					"Flags in normalized patterns must be identical to 'or' them.\n" + "Normalized <this>:\n"
							+ "Flags:    " + new PatternFlags(flags1) + "\n" + "Pattern:  " + regex1 + "\n\n" +

							"Normalized <other>:\n" + "Flags:   " + new PatternFlags(flags2) + "\n" + "Pattern: "
							+ regex2);
		}

		return lazyCompile("(?|(?:" + regex1 + ")|(?:" + regex2 + "))");
	}

	public Pattern or(final PatternFlag flag) {
		return this.or(flag.intValue());
	}

	public Pattern or(final Set<PatternFlag> flags) {
		return this.or(PatternFlags.intValue(flags));
	}

	public Pattern or(final int flags) {
		return lazyCompile(this.pattern(), this.flags() | flags);
	}

	/*
	 * TODO: implement 'and' using positive look-aheads.
	 * Works, but what would it match - the entire string?? nothing?
	 * Not sure how to implement to be most effective.
	 */

	/*
	 * TODO: implement a negate operation ('negative' operator) - returns a Pattern which matches everything
	 * that this Pattern does not.
	 */

	/**
	 *
	 * @param regex
	 * @return
	 * @since 0.2
	 */
	public Pattern plus(final CharSequence regex) {
		return lazyCompile(this.pattern() + regex, this.flags());
	}

	/**
	 * @param pattern
	 * @return
	 * @since 0.2
	 */
	public Pattern plus(final Pattern pattern) {
		Pattern pattern1 = this.normalize();
		Pattern pattern2 = pattern.normalize();

		return plus(pattern1.pattern(), pattern1.flags(), pattern2.pattern(), pattern2.flags());
	}

	/**
	 * @param pattern
	 * @return
	 * @since 0.2
	 */
	public Pattern plus(final java.util.regex.Pattern pattern) {
		Pattern pattern1 = this.normalize();
		Pattern pattern2 = normalize(pattern);

		return plus(pattern1.pattern(), pattern1.flags(), pattern2.pattern(), pattern2.flags());
	}

	private static Pattern plus(final String regex1, final int flags1, final CharSequence regex2, final int flags2) {
		if (flags2 != 0 && flags2 != flags1) {
			throw new IllegalArgumentException(
					"Flags in normalized patterns must be 0 or the same as the first pattern to 'add' to existing pattern.\n"
							+ "Pattern 1:\n" + "Flags:   " + new PatternFlags(flags1) + "\n" + "Pattern: " + regex1
							+ "\n\n"

							+ "Normalized Pattern 2:\n" + "Flags:   " + new PatternFlags(flags2) + "\n" + "Pattern: "
							+ regex2);
		}

		return lazyCompile(regex1 + regex2, flags1);
	}

	/**
	 * Returns the actual group number (in the internal pattern) for the given
	 * mapping name.
	 *
	 * @param groupName
	 *            the group name
	 * @param occurrence
	 *            the occurrence
	 * @return the mapped index
	 */
	Integer getMappedIndex(final String groupName, final int occurrence) {
		String mappingName = getMappingName(groupName, occurrence);
		return this.getMappedIndex(mappingName);
	}

	/**
	 * Returns the actual group number (in the internal pattern) for the given
	 * mapping name.
	 *
	 * @param mappingName
	 *            the mapping name
	 * @return the mapped index
	 */
	Integer getMappedIndex(final String mappingName) {
		return this.getGroupMapping().get(mappingName);
	}

	/**
	 * Returns the (internally) used string for mapping a group and occurrence
	 * (in the original pattern) to its group index (in the refactored pattern).
	 *
	 * @param groupName
	 *            the group name
	 * @param occurrence
	 *            the occurrence
	 * @return groupName + "[" + occurrence + "]"
	 */
	static String getMappingName(final String groupName, final int occurrence) {
		return groupName + "[" + occurrence + "]";
	}

	/**
	 * Returns the (internally) used string for mapping a group and occurrence
	 * (in the original pattern) to its group index (in the refactored pattern).
	 *
	 * @param groupIndex
	 *            the group index
	 * @param occurrence
	 *            the occurrence
	 * @return "[" groupIndex + "][" + occurrence + "]"
	 */
	static String getMappingName(final int groupIndex, final int occurrence) {
		return getMappingName(wrapIndex(groupIndex), occurrence);
	}

	/**
	 * Returns the group name for the given group index in a "branch reset"
	 * subpattern
	 *
	 * @param groupIndex
	 *            the group number
	 * @return the group name for the given group index in a "branch reset"
	 *         subpattern
	 */
	static String wrapIndex(final int groupIndex) {
		return "[" + groupIndex + "]";
	}

	/**
	 * Returns the given group name, adjusting the case based on
	 * the {@link #CASE_INSENSITIVE_NAMES} flag.
	 *
	 * @param groupName
	 *            the group name
	 * @return the group name, adjusting the case based on the {@link #CASE_INSENSITIVE_NAMES} flag
	 */
	// String handleCase(String groupName)
	// {
	// return hasCaseInsensitiveGroupNames()
	// ? groupName.toLowerCase(Locale.ENGLISH) : groupName;
	// }

	/**
	 * Returns a regular expression that matches the specified numeric range.
	 * The returned expression is wrapped in a non-capture group to allow
	 * easy integration.
	 *
	 * <p>The <tt>mode</tt> parameter has the same form as the leading part of
	 * a <a href="#numericrange">numeric range</a>. The return from
	 * <tt>range(</tt><i>start</i><tt>, </tt><i>end</i><tt>,
	 * </tt><i>mode</i><tt>)</tt> is equivalent to the internal representation
	 * of a numeric range.</p>
	 *
	 * <p><b>Format for <i>mode</i> parameter</b>: Mode[Base[BaseMode]]</p>
	 *
	 * <p>Descriptions and valid values:</p>
	 * <ul>
	 * <li><b>Mode</b>: either "Z" (allows leading zeros) or "NZ" (no
	 * leading zeros)</li>
	 *
	 * <li><b>Base</b>: the numeric base for start and end (valid bases, 2 -
	 * 36)</li>
	 *
	 * <li><b>BaseMode</b>: whether to allow lower ("L"), upper ("U"), or both
	 * upper and lower-case digts (omit BaseMode). This mode applies only when
	 * matching
	 * numbers in bases above 10. Note that this only affects matching, and that
	 * both upper lower-case digits can be specified as part of the range in
	 * the <i>start</i> and <i>end</i> parameters, regardless of this setting.
	 *
	 * <p>If the result doesn't include "letter digits" or if the base is
	 * ten or less,
	 * <i>BaseMode</i> has no effect, but can be specified (for
	 * consistency).</p></li>
	 * </ul>
	 *
	 * @param start
	 *            the start of the range
	 * @param end
	 *            the end of the range
	 * @param mode
	 *            a string in the format described above that specifies the mode
	 *            for the numeric range
	 * @return a regular expression that matches the specified numeric range,
	 *         wrapped in a non-capture group for easy integration
	 * @throws IllegalArgumentException
	 *             If <i>mode</i> is not in the correct form, as described above
	 */
	public static String range(final int start, final int end, final String mode) {
		// java.util.regex.Matcher rangeMode = Range.rangeModeRegEx.matcher(mode);

		// if (!rangeMode.matches())
		// throw new IllegalArgumentException("Illegal range mode");

		// int base = rangeMode.group(2) == null ? 10 : Integer.parseInt(rangeMode
		// .group(2));

		RangeMode rangeMode = new RangeMode(mode);
		int base = rangeMode.base();

		// return "(?:" +
		// Range.range(Integer.toString(start, base), Integer.toString(
		// end, base), mode) + ")";
		return "(?:" + PatternRange.range(Integer.toString(start, base), Integer.toString(end, base), rangeMode) + ")";
	}

	/**
	 * Returns a regular expression that matches the specified numeric range.
	 * The returned expression is wrapped in a non-capture group to allow
	 * easy integration.
	 *
	 * <p>The <tt>mode</tt> parameter has the same form as the leading part of
	 * a <a href="#numericrange">numeric range</a>. The return from
	 * <tt>range(</tt><i>start</i><tt>, </tt><i>end</i><tt>,
	 * </tt><i>mode</i><tt>)</tt> is equivalent to the internal representation
	 * of a numeric range.</p>
	 *
	 * <p><b>Format for <i>mode</i> parameter</b>: Mode[Base[BaseMode]]</p>
	 *
	 * <p>Descriptions and valid values:</p>
	 * <ul>
	 * <li><b>Mode</b>: either "Z" (allows leading zeros) or "NZ" (no
	 * leading zeros)</li>
	 *
	 * <li><b>Base</b>: the numeric base for start and end (valid bases, 2 -
	 * 36)</li>
	 *
	 * <li><b>BaseMode</b>: whether to allow lower ("L"), upper ("U"), or both
	 * upper and lower-case digts (omit BaseMode). This mode applies only when
	 * matching
	 * numbers in bases above 10. Note that this only affects matching, and that
	 * both upper lower-case digits can be specified as part of the range in
	 * the <i>start</i> and <i>end</i> parameters, regardless of this setting.
	 *
	 * <p>If the result doesn't include "letter digits" or if the base is
	 * ten or less,
	 * <i>BaseMode</i> has no effect, but can be specified (for
	 * consistency).</p></li>
	 * </ul>
	 *
	 * @param start
	 *            the start of the range
	 * @param end
	 *            the end of the range
	 * @param mode
	 *            a string in the format described above that specifies the mode
	 *            for the numeric range
	 * @return a regular expression that matches the specified numeric range,
	 *         wrapped in a non-capture group for easy integration
	 * @throws NullPointerException
	 *             If either <i>start</i> or <i>end</i> is null
	 * @throws IllegalArgumentException
	 *             If either <i>start</i> or <i>end</i>
	 *             is the empty string or contains invalid digits for the
	 *             specified base; also thrown if
	 *             <i>mode</i> is not in the correct form, as described above
	 */
	public static String range(final String start, final String end, final String mode) {
		if (start == null) {
			throw new NullPointerException("Start value cannot be null");
		}

		if (start.length() == 0) {
			throw new IllegalArgumentException("Start value cannot be the empty string");
		}

		if (end == null) {
			throw new NullPointerException("End value cannot be null");
		}

		if (end.length() == 0) {
			throw new IllegalArgumentException("End value cannot be the empty string");
		}

		return "(?:" + PatternRange.range(start, end, new RangeMode(mode)) + ")";
	}

	/**
	 * Get ThreadLocal for matcher
	 *
	 * <p>This is to help handle the fact that the Matcher is not thread-safe</p>
	 * @param regex The expression to be compiled
	 * @return a ThreadLocal matcher for the specified regex
	 * @since 1.0
	 */
	public static ThreadLocal<Matcher> getThreadLocalMatcher(final String regex) {
		Pattern pattern = Pattern.compile(regex);
		return ThreadLocal.withInitial(pattern::matcher);
	}
}