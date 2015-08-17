// TODO: verify group names consisting of solely numbers are handled correctly

package info.codesaway.util.regex;

import static info.codesaway.util.regex.Pattern.PatternErrorMessage.*;
import static info.codesaway.util.regex.Pattern.RefactorUtility.afterRefactor;
import static info.codesaway.util.regex.Pattern.RefactorUtility.digitCount;
import static info.codesaway.util.regex.Pattern.RefactorUtility.getDigitCountPattern;
import static info.codesaway.util.regex.Pattern.RefactorUtility.getRefactorPattern;
import static info.codesaway.util.regex.Pattern.RefactorUtility.hexCodeFormat;
import static info.codesaway.util.regex.Pattern.RefactorUtility.perl_octal;
import static info.codesaway.util.regex.Pattern.RefactorUtility.posixClasses;
import static info.codesaway.util.regex.Pattern.RefactorUtility.preRefactor;
import static info.codesaway.util.regex.Pattern.RefactorUtility.unicodeFormat;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import info.codesaway.util.Differences;

/**
 * A compiled representation of a regular expression.
 * 
 * <p>This class is an extension
 * of Java's {@link java.util.regex.Pattern} class. Javadocs were copied and
 * appended with the added functionality.</p>
 * 
 * <p>A regular expression, specified as a string, must first be compiled into
 * an instance of this class. The resulting pattern can then be used to create
 * a {@link Matcher} object that can match arbitrary
 * {@linkplain java.lang.CharSequence character sequences} against the
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
 * <a name="sum"> <h4>Summary of regular-expression
 * constructs</h4>
 * 
 * <table border="0" cellpadding="1" cellspacing="0"
 * summary="Regular expression constructs, and what they match">
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
 * <td valign="top" headers="construct classes"><tt>[a-z&&[def]]</tt></td>
 * <td headers="matches"><tt>d</tt>, <tt>e</tt>, or <tt>f</tt> (intersection)
 * </tr>
 * <tr>
 * <td valign="top" headers="construct classes"><tt>[a-z&&[^bc]]</tt></td>
 * <td headers="matches"><tt>a</tt> through <tt>z</tt>, except for <tt>b</tt>
 * and <tt>c</tt>: <tt>[ad-z]</tt> (subtraction)</td>
 * </tr>
 * <tr>
 * <td valign="top" headers="construct classes"><tt>[a-z&&[^m-p]]</tt></td>
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
 * <tt>(?>\P{M}\p{M}*)</tt></td>
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
 * <th colspan="2" id="posix">POSIX character classes</b> (US-ASCII
 * only)<b></th>
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
 * <tt>!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~</tt></td>
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
 * only)<br /><p style="margin-top: 0px; font-weight: normal">(equivalent to the
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
 * <tt>!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~</tt></td>
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
 * <tt>[\p{L}&&[^\p{Lu}]]&nbsp;</tt></td>
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
 * <a href="#lt"terminator</a>, if&nbsp;any</td>
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
 * <td headers="matches">Nothing, but turns match flags
 * {@link #CASE_INSENSITIVE i} {@link #UNIX_LINES d} {@link #MULTILINE m}
 * {@link #DOTALL s} {@link #UNICODE_CASE u} {@link #COMMENTS x}
 * {@link #DUPLICATE_NAMES J} {@link #EXPLICIT_CAPTURE n} on - off</td>
 * </tr>
 * <tr>
 * <td valign="top"
 * headers="construct special"><tt>(?idmsuxJn-idmsuxJn:</tt><i
 * >X</i><tt>)</tt>&nbsp;&nbsp;</td>
 * <td headers="matches"><i>X</i>, as a <a href="#cg">non-capturing group</a>
 * with the given flags {@link #CASE_INSENSITIVE i} {@link #UNIX_LINES d}
 * {@link #MULTILINE m} {@link #DOTALL s} {@link #UNICODE_CASE u}
 * {@link #COMMENTS x} {@link #DUPLICATE_NAMES J} {@link #EXPLICIT_CAPTURE n} on
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
 * <th colspan="2" id="conditional">Conditional patterns (non-capturing)<br
 * /><span style="font-weight: normal"><tt>(?(condition)yes-pattern)</tt><br
 * /><tt>(?(condition)yes-pattern|no-pattern)</tt></span></th>
 * </tr>
 * 
 * <tr>
 * <td valign="top" headers="construct conditional">
 * <tt>&nbsp;</td>
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
 * <td valign="top" headers="construct conditional">
 * <tt>(?(&lt;</tt><i>name</i><tt>&gt;)...)</tt></td>
 * <td headers="matches"><a href="#groupname">named reference condition</td>
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
 * <a name="bs">
 * <h4>Backslashes, escapes, and quoting</h4>
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
 * <a name="cc">
 * <h4>Character Classes</h4>
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
 * <td><tt>[a-z&&[aeiou]]</tt></td></tr>
 * </table></blockquote>
 * 
 * <p>Note that a different set of metacharacters are in effect inside
 * a character class than outside a character class. For instance, the
 * regular expression <tt>.</tt> loses its special meaning inside a
 * character class, while the expression <tt>-</tt> becomes a range
 * forming metacharacter.</p>
 * 
 * <a name="lt">
 * <h4>Line terminators</h4>
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
 * <a name="cg">
 * <h4>Groups and capturing</h4>
 * 
 * <a name="gnumber">
 * <h5>Group number</h5>
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
 * <a name="groupname">
 * <h5>Group name</h5>
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
 * a {@link PatternSyntaxException} is thrown. By setting the
 * {@link #DUPLICATE_NAMES} flag, multiple capture groups with
 * the same name are allowed.</p>
 * 
 * <a name="group">
 * <h5>Group</h5>
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
 * <a name="branchreset">
 * <h5>"Branch reset" pattern</h5>
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
 * after<br />
 * / ( a )&nbsp;&nbsp;(?| x ( y ) z | (p (q) r) | (t) u (v) ) ( z ) /x<br />
 * # 1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2&nbsp;&nbsp;3&nbsp;
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;3
 * &nbsp;&nbsp;&nbsp;&nbsp;4</tt></blockquote><br />
 * 
 * <p>As a note, nested <i>branch reset patterns</i> are fully supported:</p>
 * 
 * <blockquote>
 * <p><tt>/&nbsp;</tt><tt>(?| ( 1a ) ( 2a ) | ( 1b )
 * (?| ( 2b1 ) | ( 2b2 ) ) )</tt><tt>&nbsp;/x<br />
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
 * <blockquote><tt>(?|(?&lt;One&gt;1a)(2a)|(1b)(?&lt;Two&gt;2b))<br />
 * # &nbsp;1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2
 * &nbsp;&nbsp;&nbsp;1&nbsp;&nbsp;&nbsp;2</tt></blockquote>
 * 
 * <a name="unicode_support">
 * <h4>Unicode support</h4>
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
 * <a name="ubc"> <p>Unicode blocks and categories are written with the
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
 * <i>The Unicode Standard</i></a> in the version specified by the
 * {@link java.lang.Character Character} class. The category names are those
 * defined in the Standard, both normative and informative.
 * The block names supported by <code>Pattern</code> are the valid block names
 * accepted and defined by
 * {@link java.lang.Character.UnicodeBlock#forName(String) UnicodeBlock.forName}
 * .</p>
 * 
 * <a name="jcc"> <p>Categories that behave like the java.lang.Character
 * boolean is<i>methodname</i> methods (except for the deprecated ones) are
 * available through the same <tt>\p{</tt><i>prop</i><tt>}</tt> syntax where
 * the specified property has the name <tt>java<i>methodname</i></tt>.</p>
 * 
 * <a name="perl_comparison">
 * <h4>Comparison to Perl 5</h4>
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
 * <p><b>Note</b>: specify the {@link #PERL_OCTAL} flag when compiling a pattern
 * to use Perl's octal syntax (as described above), instead of Java's.</p>
 * 
 * <li><p>Perl uses the <tt>g</tt> flag to request a match that resumes
 * where the last match left off. This functionality is provided implicitly
 * by the {@link Matcher} class: Repeated invocations of the
 * {@link Matcher#find find} method will resume where the last match left off,
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
 * metacharacters like +, ? and *, and will throw a
 * {@link PatternSyntaxException} if it encounters them.</p></li>
 * 
 * </ul>
 * 
 * 
 * <p>For a more precise description of the behavior of regular expression
 * constructs, please see <a href="http://www.oreilly.com/catalog/regex3/">
 * <i>Mastering Regular Expressions, 3nd Edition</i>, Jeffrey E. F. Friedl,
 * O'Reilly and Associates, 2006.</a></p>
 * 
 * <a name="numericrange">
 * <h4>Numeric range</h4>
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
 * has no effect, but, for consistency, can be specified - the regex
 * <code>(?Z8U[0..377])</code> is equivalent to <code>(?Z8[0..377])</code>.</p>
 * 
 * @see Pattern#split(CharSequence, String, int)
 * @see Pattern#split(CharSequence, String)
 */
public final class Pattern implements Serializable
{
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
	private String pattern;

	/** The flags. */
	private int flags;

	/**
	 * Boolean indicating this Pattern is compiled; this is necessary in order
	 * to lazily compile deserialized Patterns.
	 */
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
	 * A map with mappings from a "mapping name" to the actual group number in
	 * the internal pattern.
	 * 
	 * <p><b>Note</b>: both named and unnamed groups are included</p>
	 */
	private transient Map<String, Integer> groupMapping;

	/**
	 * A map with mappings from the group name to the number of occurrences of
	 * the group.
	 */
	private transient Map<String, Integer> groupNameOccurrences;

	/**
	 * Pattern used to escape metacharacters.
	 */
	// TODO: javascript needs to escape ']'
	// escapes '#' (in case Pattern.COMMENTS is enabled)
	static final java.util.regex.Pattern escapeMetachars = java.util.regex.Pattern
			.compile("[\\\\.*?+\\[{|()^$#]");

	/**
	 * Pattern used to escape metacharacters found in a character class.
	 */
	// TODO: java needs to escape '[' in char class
	// escapes '#' (in case Pattern.COMMENTS is enabled)
	static final java.util.regex.Pattern escapeClassMetachars = java.util.regex.Pattern
			.compile("[\\^\\-\\[\\\\#]");

	/**
	 * Used to differentiate between named and unnamed capture groups, back
	 * references, etc. during refactoring. Different steps may be taken
	 * depending if a name or number is used.
	 */
	private static final boolean FOR_NAME = true;

	/** Used to specify that the operation occurred during the prerefactor step. */
	private static final boolean DURING_PREREFACTOR = true;

	/** Used as the mapped index for a group whose target is unknown. */
	static final int TARGET_UNKNOWN = -1;

	/**
	 * A compiled pattern with the RegEx being the empty string.
	 */
	static final java.util.regex.Pattern EMPTY_PATTERN = java.util.regex.Pattern
			.compile("");

	/**
	 * Pattern used with the
	 * {@link #naturalCompareTo(CharSequence, CharSequence)} function
	 * to provide a natural sort
	 */
	private static final java.util.regex.Pattern naturalSort = java.util.regex.Pattern
			.compile("\\G(?:(\\D++)|0*(\\d++)|$)");

	/**
	 * Compiles the given regular expression into a pattern. </p>
	 * 
	 * @param regex
	 *            The expression to be compiled
	 * @return The compiled <code>Pattern</code>
	 * 
	 * @throws PatternSyntaxException
	 *             If the expression's patternSyntax is invalid
	 */
	public static Pattern compile(String regex)
	{
		return new Pattern(regex, 0);
	}

	/**
	 * Compiles the given regular expression into a pattern with the given
	 * flags. </p>
	 * 
	 * @param regex
	 *            The expression to be compiled
	 * 
	 * @param flags
	 *            Match flags, a bit mask that may include
	 *            {@link #CASE_INSENSITIVE}, {@link #MULTILINE},&nbsp;
	 *            {@link #DOTALL}, {@link #UNICODE_CASE}, {@link #CANON_EQ},
	 *            {@link #UNIX_LINES}, {@link #LITERAL}, {@link #COMMENTS},
	 * 
	 *            <p>{@link #DUPLICATE_NAMES}, {@link #VERIFY_GROUPS},
	 *            {@link #PERL_OCTAL}, {@link #DOTNET_NUMBERING}, and
	 *            {@link #EXPLICIT_CAPTURE}</p>
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
	public static Pattern compile(String regex, int flags)
	{
		return new Pattern(regex, flags);
	}

	/**
	 * Gets the internal pattern.
	 * 
	 * @return The internal {@link java.util.regex.Pattern} used by this
	 *         pattern.
	 */
	java.util.regex.Pattern getInternalPattern()
	{
		return this.internalPattern;
	}

	/**
	 * Returns the regular expression from which the internal pattern was
	 * compiled.
	 * 
	 * @return The source of the internal pattern
	 */
	String internalPattern()
	{
		return this.getInternalPattern().pattern();
	}

	/**
	 * Returns the regular expression from which this pattern was compiled.
	 * 
	 * @return The source of this pattern
	 */
	public String pattern()
	{
		return pattern;
	}

	/**
	 * Returns the number of capturing groups in this matcher's pattern.
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
	public int groupCount()
	{
		return this.capturingGroupCount;
	}

	/**
	 * Sets the number of capture groups.
	 * 
	 * @param capturingGroupCount
	 *            the number of capture groups
	 */
	void setCapturingGroupCount(int capturingGroupCount)
	{
		this.capturingGroupCount = capturingGroupCount;
	}

	/**
	 * Returns the number of capturing groups (with the given group name) in
	 * this matcher's pattern.
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
	public int groupCount(String groupName)
	{
		Integer occurrences = groupNameOccurrences.get(groupName);

		if (occurrences == null)
			return 0;

		return occurrences;
	}

	/**
	 * Returns the group mapping.
	 * 
	 * @return The group mapping
	 */
	Map<String, Integer> getGroupMapping()
	{
		return this.groupMapping;
	}

	/**
	 * Sets the group name occurrences.
	 * 
	 * @param groupNameOccurrences
	 *            the group name occurrences
	 */
	void setGroupNameOccurrences(Map<String, Integer> groupNameOccurrences)
	{
		this.groupNameOccurrences = groupNameOccurrences;
	}

	/**
	 * Returns the string representation of this pattern. This is the regular
	 * expression from which this pattern was compiled.
	 * 
	 * @return The string representation of this pattern
	 */
	@Override
	public String toString()
	{
		return pattern;
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
	public Matcher matcher(CharSequence input)
	{
		if (!compiled) {
			synchronized (this) {
				if (!compiled)
					compile();
			}
		}

		return new Matcher(internalPattern.matcher(input), this, input);
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
	 *Pattern p = Pattern.compile("\\d+\\.\\d+");</pre></blockquote>
	 * 
	 * The following calls return <code>true</code>
	 * 
	 * <blockquote><pre>
	 *p.isPartialMatch("");
	 *p.isPartialMatch("1");
	 *p.isPartialMatch("2");
	 *p.isPartialMatch("9");
	 *p.isPartialMatch("123");
	 *p.isPartialMatch("123.");
	 *p.isPartialMatch("123.456");
	 *<span style="color: #008000;">// p.matcher("123.456").matches() <span
	 *>would also return true (see note below)</span></span></pre></blockquote>
	 * 
	 * Whereas these calls return <code>false</code>
	 * 
	 * <blockquote><pre>
	 *p.isPartialMatch("a");
	 *p.isPartialMatch(".");
	 *p.isPartialMatch(".4");
	 *p.isPartialMatch(".45");
	 *p.isPartialMatch(".456");</pre></blockquote>
	 * 
	 * <p><b id="isPartialMatch(java.lang.CharSequence)_note">Note</b>: if the
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
	public boolean isPartialMatch(CharSequence input)
	{
		Matcher m = this.matcher(input);

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
	public int flags()
	{
		return this.flags;
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
	public static boolean matches(String regex, CharSequence input)
	{
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
	 * were being treated as a literal replacement string; see
	 * {@link Matcher#replaceFirst}. Use {@link Matcher#quoteReplacement} to
	 * suppress the special meaning of these characters, if desired.</p>
	 * 
	 * <p><b>Note</b>: this function serves as a substitute for
	 * {@link String#replaceFirst(String, String)}.</p>
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
	public static String replaceFirst(CharSequence input, String regex,
			String replacement)
	{
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
	 * were being treated as a literal replacement string; see
	 * {@link Matcher#replaceAll}. Use {@link Matcher#quoteReplacement} to
	 * suppress the special meaning of these characters, if desired.</p>
	 * 
	 * <p><b>Note</b>: this function serves as a substitute for
	 * {@link String#replaceAll(String, String)}.</p>
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
	public static String replaceAll(CharSequence input, String regex,
			String replacement)
	{
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
	 * <p><b>Note</b>: this function serves as a substitute for
	 * {@link String#split(String, int)}.</p>
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
	public static String[] split(CharSequence input, String regex, int limit)
	{
		return Pattern.compile(regex).split(input, limit);
	}

	/**
	 * Splits this string around matches of the given regular expression.
	 * 
	 * <p>
	 * This method works as if by invoking the three-argument
	 * {@link #split(CharSequence, String, int) split} method with the given
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
	 * <p><b>Note</b>: this function serves as a substitute for
	 * {@link String#split(String)}.</p>
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
	public static String[] split(CharSequence input, String regex)
	{
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
	 * <table cellpadding=1 cellspacing=0 *
	 * summary="Split examples showing regex, limit, and result">
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
	public String[] split(CharSequence input, int limit)
	{
		return this.internalPattern.split(input, limit);
	}

	/**
	 * Splits the given input sequence around matches of this pattern.
	 * 
	 * <p>
	 * This method works as if by invoking the two-argument
	 * {@link #split(java.lang.CharSequence, int) split} method with the given
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
	 * <table cellpadding=1 cellspacing=0 *
	 * summary="Split examples showing regex and result">
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
	public String[] split(CharSequence input)
	{
		return this.internalPattern.split(input, 0);
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
	public static String quote(String s)
	{
		return java.util.regex.Pattern.quote(s);
	}

	/**
	 * Recompile the Pattern instance from a stream. The original pattern string
	 * is read in and the object tree is recompiled from it.
	 */
	private void readObject(java.io.ObjectInputStream s)
			throws java.io.IOException, ClassNotFoundException
	{
		// Read in all fields
		s.defaultReadObject();

		// this.caseInsensitiveGroupNames = has(CASE_INSENSITIVE_NAMES);
		this.groupMapping = new HashMap<String, Integer>(2);

		// if length > 0, the Pattern is lazily compiled
		compiled = false;
		if (pattern.length() == 0) {
			capturingGroupCount = 0;
			setInternalPattern("");
			compiled = true;
		}
	}

	/**
	 * This private constructor is used to create all Patterns. The pattern
	 * string and match flags are all that is needed to completely describe a
	 * Pattern.
	 * 
	 * @param regex
	 *            the expression to be compiled
	 * @param flags
	 *            match flags bit mask
	 */
	private Pattern(String regex, int flags)
	{
		// only run online
		// try {
		// getClass().getProtectionDomain().getCodeSource()
		// .getLocation().toString();
		// throw new AssertionError();
		// } catch (Exception e) {
		// }

		this.pattern = regex;
		this.flags = flags;

		// this.caseInsensitiveGroupNames = has(CASE_INSENSITIVE_NAMES);
		this.groupMapping = new HashMap<String, Integer>(2);

		if (regex.length() > 0) {
			compile();
		} else {
			capturingGroupCount = 0;
			setInternalPattern("");
			compiled = true;
		}
	}

	/**
	 * Refactors the regular expression to an equivalent form usable by Java's
	 * {@link java.util.regex.Pattern} class, and compiles the internal
	 * <code>Pattern</code> using the refactored regular expression.
	 */
	private void compile()
	{
		// refactor <pattern> to be used as a RegEx pattern
		Refactor refactor = new Refactor(pattern);

		try {
			setInternalPattern(refactor.toString());
		} catch (java.util.regex.PatternSyntaxException e) {
			// // on error, show error using the original pattern
			// // (not refactored form)
			//
			// System.out.println(e.getMessage());
			//
			String desc = e.getDescription();
			int index = refactor.changes.getOriginalIndex(e.getIndex());

			throw new PatternSyntaxException(desc, pattern, index);
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

		compiled = true;
	}

	/**
	 * Sets the internal <code>Pattern</code> to the <code>Pattern</code>
	 * returned when calling
	 * 
	 * <blockquote><tt>{@link java.util.regex.Pattern}.</
	 * {@link java.util.regex.Pattern#compile(String, int)
	 * compile}(</tt><i>regex</i> <tt>,</tt> <i>flags</i><tt>)</tt>
	 * </blockquote>
	 * 
	 * @param regex
	 *            the expression to be compiled
	 * @see java.util.regex.Pattern#compile(String, int)
	 */
	private void setInternalPattern(String regex)
	{
		// keep all flags except those introduced in this class
		this.internalPattern = java.util.regex.Pattern.compile(regex, flags
				& ~(DUPLICATE_NAMES | VERIFY_GROUPS | DOTNET_NUMBERING
				| EXPLICIT_CAPTURE | PERL_OCTAL));
		// this.internalPattern = java.util.regex.Pattern.compile(regex, flags
		// & ~(DUPLICATE_NAMES | CASE_INSENSITIVE_NAMES
		// | VERIFY_GROUPS | ONLY_CAPTURING_GROUPS));
	}

	/**
	 * Indicates whether a particular flag is set or not.
	 * 
	 * @param f
	 *            the flag to test
	 * @return <code>true</code> if the particular flag is set
	 */
	boolean has(int f)
	{
		return (flags & f) != 0;
	}

	/**
	 * Compares two character sequences lexigraphically,
	 * except that embedded numbers are treated numerically.</p>
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
	 * <p>This method can be called in the compare function of a
	 * {@link Comparator} object
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
	public static int naturalCompareTo(CharSequence value1, CharSequence value2)
	{
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

				if (compareTo != 0)
					return compareTo;

				if (leftMostDifference == 0) {
					// only do once - for left-most difference

					if (match1.length() != match2.length()) {
						// different number of leading zeros
						// e.g. "01" and "1"

						// more leading zeros first "01" < "1"
						leftMostDifference = match1.length() > match2.length()
								? -1 : 1;
					}
				}
			} else {
				int compareTo = match1.compareTo(match2);

				if (compareTo != 0)
					return compareTo;
			}
		}

		return 0;
	}

	/**
	 * Contains utility functions / fields that are used when refactoring the
	 * inputted regular expression. These functions / fields are only meant to
	 * be called from the {@link Refactor} class.
	 */
	static class RefactorUtility
	{
		/** String pattern to match a group name (excluding occurrence). */
		private static final String groupName = "\\w++";

		/**
		 * String pattern to match an optional group name (excluding
		 * occurrence).
		 */
		private static final String optGroupName = "\\w*+";

		/**
		 * String pattern to match a group name (including occurrence)
		 * 
		 * <p>(2 groups)</p>
		 */
		static final java.util.regex.Pattern fullGroupName = java.util.regex.Pattern
				.compile("(\\[-?\\d++]|" + optGroupName
				+ ")(?:\\[(-?\\d++)])?");

		/**
		 * String pattern to match an "any group"
		 * 
		 * (e.g. groupName, groupName[0])
		 * 
		 * <p>(1 group)</p>
		 */
		private static final String fullGroupName0 = "(\\[-?\\d++]|"
				+ groupName + ")(?:\\[0++])?";

		/**
		 * Formats an integer character code (0 through 255) as
		 * <code>\x</code><i>hh</i>.
		 */
		static final String hexCodeFormat = "\\x%1$02x";

		/**
		 * Formats an integer character code as
		 * <code>&#92;u</code><i>hhhh</i>.
		 */
		static final String unicodeFormat = "\\u%1$04x";

		static final Map<String, String> posixClasses = new HashMap<String, String>(
				13);

		static {
			posixClasses.put("alnum", "Alnum");
			posixClasses.put("alpha", "Alpha");
			posixClasses.put("ascii", "ASCII");
			posixClasses.put("blank", "Blank");
			posixClasses.put("cntrl", "Cntrl");
			posixClasses.put("digit", "Digit");
			posixClasses.put("graph", "Graph");
			posixClasses.put("lower", "Lower");
			posixClasses.put("print", "Print");
			posixClasses.put("punct", "Punct");
			posixClasses.put("space", "Space");
			posixClasses.put("upper", "Upper");
			posixClasses.put("xdigit", "XDigit");
		}

		/**
		 * Pattern to match parts of the inputted regular expression that need
		 * to be processed before refactoring.
		 */
		static final java.util.regex.Pattern preRefactor = java.util.regex.Pattern
				.compile(

		/*
		 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:" (also
		 * matches a non-capture group - onFlags/offFlags are omitted)
		 * 
		 * group: onFlags (empty string if none) group + 1: offFlags
		 * (empty string if none; null, if omitted) (2 groups)
		 */
		"\\Q(?\\E(\\w*+)(?:-(\\w*+))?[:\\)]|"

		/*
		 * matches a named capture group
		 * "(?<name>" (form 0)
		 * "(?'name'" (form 1)
		 * "(?P<name>" (form 2)
		 * 
		 * group: everything after first symbol
		 * group + 1: the name
		 * (6 groups)
		 */

		/* form 0 - 2 start */
		+ "\\Q(?\\E(?:"

		/* form 0 */
		+ "<((" + groupName + ")>?)|"

		/* form 1 */
		+ "'((" + optGroupName + ")'?)|"

		/* form 2 */
		+ "P<((" + optGroupName + ")>?)"

		/* form 0 - 2 end */
		+ ")|"

		/*
		 * matches an unnamed capture group "(" - not followed by a "?"
		 * 
		 * group: everything (1 group)
		 */
		+ "(\\((?!\\?))|"

		/*
		 * matches a back reference (by name)
		 * "\g{name}" (form 0)
		 * "\k<name>" (form 1)
		 * "\k'name'" (form 2)
		 * "\k{name}" (form 3)
		 * "(?P=name)" (form 4)
		 * 
		 * group : the name
		 * 
		 * (5 groups)
		 * (<name> can only be an "any group" (e.g. groupName[0]))
		 */

		/* form 0 */
		+ "\\Q\\g{\\E" + fullGroupName0 + "}|"

		/* form 1 - 3 start */
		+ "\\\\k(?:"

		/* form 1 */
		+ "<" + fullGroupName0 + ">|"

		/* form 2 */
		+ "'" + fullGroupName0 + "'|"

		/* form 3 */
		+ "\\{" + fullGroupName0 + "}"

		/* form 1 - 3 end */
		+ ")|"

		/* form 4 */
		+ "\\Q(?P=\\E" + fullGroupName0 + "\\)|"

		/*
		 * matches an assert condition
		 * "(?(?=", "(?(?!", "(?(?<=), or "(?(?<!"
		 * 
		 * group: the assert part (inside the parentheses)
		 * (1 group)
		 */
		+ "\\Q(?(\\E(\\?<?[!=])|"

		/*
		 * matches a reference condition (by number) matches
		 * "(?(n)" or "(?(-n)"
		 * 
		 * group: the number
		 * (1 group)
		 */
		+ "\\Q(?(\\E(-?\\d++)\\)|"

		/*
		 * matches a reference condition (by name)
		 * "(?(<name>)" (form 0),
		 * "(?('name')" (form 1), or
		 * "(?(name)" (form 2)
		 * 
		 * group: everything after first symbol (excluding ")")
		 * group + 1: the name
		 * group + 2: the occurrence (if specified)
		 * (9 groups)
		 */

		/* form 0 - 2 start */
		+ "\\Q(?(\\E(?:"

		/* form 0 */
		+ "<(" + fullGroupName + ">?)|"

		/* form 1 */
		+ "'(" + fullGroupName + "'?)|"

		/* form 2 */
		+ "(" + fullGroupName + ")"

		/* form 0 - 2 end */
		+ ")\\)?|"

		/*
		 * matches comment group
		 * "(?#comment) - comment cannot contain ")"
		 * 
		 * group: everything
		 * (1 group)
		 */
		+ "(\\Q(?#\\E[^\\)]*+\\)?)|"

		/*
		 * matches a "branch reset" subpattern "(?|"
		 * 
		 * group: everything
		 * (1 group)
		 */
		+ "(\\Q(?|\\E)|"

		/* an open parenthesis - depth + 1 */
		+ "\\(|"

		/* a closed parenthesis - depth - 1 */
		+ "\\)|"

		/* a pike (|) - only react if inside condition */
		+ "\\||"

		/* an open square bracket - starts a character class */
		+ "\\[|"

		/* a closed square bracket - ends a character class */
		+ "\\]|"

		/* a "#" - starts comments when COMMENTS flag is enabled */
		+ "#|"

		/* matches a \Q..\E block */
		+ "\\\\Q.*?(?:\\\\E|$)|"

		/* matches an escaped character */
		+ "\\\\[^Q]");

		/**
		 * Pattern to match parts of the inputted regular expression that need
		 * to be processed after refactoring.
		 */
		static final java.util.regex.Pattern afterRefactor = java.util.regex.Pattern
				.compile(

		/*
		 * matches:
		 * "\g{##group-mappingName}"
		 * "\g{##branchGroup-mappingName}"
		 * "\g{##test-mappingName}"
		 * "\g{##testF-mappingName}"
		 * 
		 * group: the number - mapped to the position to show the error
		 * group + 1: type ("group", "test", or "testF")
		 * group + 2: mappingName
		 * (3 groups)
		 */
		"\\Q\\g{\\E(\\d++)(group|branchGroup|testF?)-([^}]++)\\}|"

		/*
		 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:"
		 * (also matches a non-capture group - onFlags/offFlags are omitted)
		 * 
		 * group: onFlags (empty string if none)
		 * group + 1: offFlags (empty string if none; null, if omitted)
		 * (2 groups)
		 */
		+ "\\Q(?\\E(\\w*+)(?:-(\\w*+))?[:\\)]|"

		/*
		 * matches "\x{hhh..}" - a hex code
		 * 
		 * group: the number
		 * (1 group)
		 */
		+ "\\Q\\x{\\E([0-9a-fA-F]++)}|"

		/*
		 * matches "\xh" or "\xhh" - a hex code
		 * 
		 * group: the number
		 * (1 group)
		 */
		+ "\\\\x([0-9a-fA-F]{1,2})|"

		/*
		 * matches a unicode character
		 * 
		 * group: the number
		 * (1 group)
		 */
		+ "\\\\u([0-9a-fA-F]{1,4})|"

		/*
		 * matches a POSIX character class
		 * 
		 * group: "^" or "" (whether to negate or not)
		 * group + 1: the class name
		 * (2 group)
		 */
		+ "\\[:(\\^?)([A-Za-z]++):]|"

		/*
		 * matches a control character - \cA through \cZ
		 * 
		 * These are equivalent to \x01 through \x1A (26 decimal).
		 * 
		 * group: the control character's letter
		 * (either upper and lower case are allowed)
		 * (1 group)
		 */
		+ "\\\\c([A-Za-z])|"

		/*
		 * matches an unnamed capture group "(" - not followed by a "?"
		 * 
		 * group: everything (1 group)
		 */
		+ "(\\((?!\\?))|"

		/* an open parenthesis - depth + 1 */
		+ "\\(|"

		/* a closed parenthesis - depth - 1 */
		+ "\\)|"

		/* an open square bracket - starts a character class */
		+ "\\[|"

		/* a closed square bracket - ends a character class */
		+ "\\]|"

		/* a "#" - starts comments when COMMENTS flag is enabled */
		+ "#|"

		/* matches an escaped character */
		+ "\\\\.");

		/**
		 * A cache of Patterns used during the refactoring.
		 * 
		 * <p>The <code>Integer</code> key is the number of digits in the number
		 * of capture groups for the inputted regular expression.</p>
		 */
		// private static final Map<Integer, java.util.regex.Pattern>
		// refactorPatterns = new HashMap<Integer, java.util.regex.Pattern>();

		/**
		 * Pattern used during refactoring.
		 */
		private static final java.util.regex.Pattern refactorPattern = createRefactorPattern();

		/**
		 * A cache of Patterns used to match a number with a given digit count.
		 * 
		 * <p>The <code>Integer</code> key is the number of digits, and the
		 * <code>Pattern</code> value is the pattern to match a number with that
		 * many digits. The digits are in the first group, and any trailing
		 * digits are in the second.</p>
		 */
		private static final Map<Integer, java.util.regex.Pattern> digitCountPatterns = new HashMap<Integer, java.util.regex.Pattern>(
				2);

		static final java.util.regex.Pattern perl_octal = java.util.regex.Pattern
				.compile("^([0-3]?[0-7]{1,2})(\\d*+)$");

		/**
		 * Returns the pattern to use when refactoring an inputted regular
		 * expression.
		 * 
		 * @param groupCount
		 *            number of capture groups in the inputted regular
		 *            expression
		 * @return the pattern to use when refactoring
		 */
		// static java.util.regex.Pattern getRefactorPattern(int groupCount)
		// {
		// // number of digits in the largest group index
		// // (e.g. 999 would be 3, and 1000 would be 4)
		// Integer digitCount = digitCount(groupCount);
		//
		// if (!refactorPatterns.containsKey(digitCount)) {
		// java.util.regex.Pattern fixup = createRefactorPattern();
		//
		// refactorPatterns.put(digitCount, fixup);
		// return fixup;
		// }
		//
		// return refactorPatterns.get(digitCount);
		// }

		/**
		 * Returns the pattern to use when refactoring an inputted regular
		 * expression.
		 * 
		 * @return the pattern to use when refactoring
		 */
		static java.util.regex.Pattern getRefactorPattern()
		{
			return refactorPattern;
		}

		/**
		 * Returns a pattern that matches <code>digitCount</code> digits
		 * followed by zero or more digits. The pattern has two capture groups.
		 * The required digits are in the first group, and any trailing digits
		 * are in the second.
		 * 
		 * @param digitCount
		 *            the number of digits the pattern should match
		 * @return a pattern that matches <code>digitCount</code> digits
		 *         followed by zero or more digits
		 */
		static java.util.regex.Pattern getDigitCountPattern(int digitCount)
		{
			Integer digitCountI = digitCount;

			if (digitCountPatterns.containsKey(digitCountI))
				return digitCountPatterns.get(digitCountI);
			else {
				java.util.regex.Pattern pattern = java.util.regex.Pattern
						.compile("(\\d{1," + digitCount + "})(\\d*+)");

				digitCountPatterns.put(digitCountI, pattern);
				return pattern;
			}
		}

		/**
		 * Returns the number of digits in the given number
		 * 
		 * @param number
		 *            the number whose digit count is returned
		 * @return the number of digits in the given number
		 */
		static int digitCount(int number)
		{
			return String.valueOf(number).length();
		}

		/**
		 * Creates the Pattern that is used when refactoring a regular
		 * expression with a group count of the specified number of digits.
		 * 
		 * @return the <code>Patter</code> to use when refactoring a regex
		 */
		private static java.util.regex.Pattern createRefactorPattern()
		{
			return java.util.regex.Pattern
					.compile(

					/*
					 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:"
					 * (also matches a non-capture group
					 * - onFlags/offFlags are omitted)
					 * 
					 * group: onFlags (empty string if none)
					 * group + 1: offFlags
					 * (empty string if none; null, if omitted)
					 * 
					 * (2 groups)
					 */
					"\\Q(?\\E(\\w*+)(?:-(\\w*+))?[:\\)]|"

					/*
					 * matches a named capture group
					 * "(?<name>" (form 0)
					 * "(?'name'" (form 1)
					 * "(?P<name>" (form 2)
					 * 
					 * group: the name (3 groups)
					 */

					/* form 0 - 2 start */
					+ "\\Q(?\\E(?:"

					/* form 0 */
					+ "<(" + groupName
							+ ")>|"

							/* form 1 */
							+ "'("
							+ groupName
							+ ")'|"

							/* form 2 */
							+ "P<("
							+ groupName
							+ ")>"

							/* form 0 - 2 end */
							+ ")|"

							/*
							 * matches an unnamed capture group "(" - not
							 * followed by a "?"
							 * 
							 * group: everything (1 group)
							 */
							+ "(\\((?!\\?))|"

							/*
							 * matches a back reference (by number)
							 * "\n" (form 0)
							 * "\gn" (form 1)
							 * "\g{n}" or "\g{-n}" (form 2)
							 * 
							 * group: the number
							 * last group: the next character (if a digit)
							 * (4 groups)
							 */

							/* form 0 - 2 start */
							+ "(?:"

							/* form 0 */
							+ "\\\\(\\d++)|"

							/* form 1 */
							+ "\\\\g(-?\\d++)|"

							/* form 2 */
							+ "\\Q\\g{\\E(-?\\d++)}"

							/* form 0 - 2 end */
							+ ")(\\d?)|"

							/*
							 * matches a back reference (by name)
							 * "\g{name}" (form 0),
							 * "\k<name>" (form 1),
							 * "\k'name'" (form 2),
							 * "\k{name}" (form 3),
							 * "(?P=name)" (form 4)
							 * 
							 * group: everything after the first symbol
							 * group + 1: the name
							 * group + 2: the occurrence (if specified)
							 * last group: the next character (if a digit)
							 * (16 groups)
							 */

							/* form 0 - 4 start */
							+ "(?:"

							/* form 0 */
							+ "\\Q\\g{\\E("
							+ fullGroupName
							+ "\\}?)|"

							/* form 1 - 3 start */
							+ "\\\\k(?:"

							/* form 1 */
							+ "<("
							+ fullGroupName
							+ ">?)|"

							/* form 2 */
							+ "'("
							+ fullGroupName
							+ "'?)|"

							/* form 3 */
							+ "\\{("
							+ fullGroupName
							+ "\\}?)"

							/* form 1 - 3 end */
							+ ")|"

							/* form 4 */
							+ "\\Q(?P=\\E("
							+ fullGroupName
							+ "\\)?)"

							/* form 0 - 4 end */
							+ ")(\\d?)|"

							/*
							 * matches an assert condition
							 * "(?(?=)", "(?(?!)", "(?(?<=)", or "(?(?<!)"
							 * 
							 * group: the assert part (inside the parentheses)
							 * (1 group)
							 */
							+ "\\Q(?(\\E(\\?<?[!=])|"

							/*
							 * matches a conditional pattern (by number)
							 * "(?(n)" or "(?(-n)" (form 0)
							 * 
							 * group: the number
							 * (1 group)
							 */
							+ "\\Q(?(\\E(-?\\d++)\\)|"

							/*
							 * matches a named reference condition
							 * "(?(<name>)" (form 0),
							 * "(?('name')" (form 1), or
							 * "(?(name)" (form 2)
							 * 
							 * group: the name
							 * group + 1: the occurrence (if specified)
							 * (6 groups)
							 */

							/* form 0 - 2 start */
							+ "\\Q(?(\\E(?:"

							/* form 0 */
							+ "<"
							+ fullGroupName
							+ ">|"

							/* form 1 */
							+ "'"
							+ fullGroupName
							+ "'|"

							/* form 2 */
							+ fullGroupName

							/* form 0 - 2 end */
							+ ")\\)|"

							/*
							 * matches a "branch reset" subpattern "(?|"
							 * 
							 * group: everything (1 group)
							 */
							+ "(\\Q(?|\\E)|"

							/*
							 * matches a numeric range
							 * "(?Z[start..end])" or "(?NZ[start..end])"
							 * 
							 * group: "Z" or "NZ" (optional base and L/U)
							 * //group + 1: "r" for raw mode, or <null>
							 * group + 1: start
							 * group + 2: end
							 * (3 groups)
							 */
							// "\\Q(?\\E(N?Z(r)?(?:\\d++[LU]?)?)\\[(?:(-?\\d++)\\.\\.(-?\\d++)\\])?\\)?|"
							+ "\\Q(?\\E(N?Z(?:\\d++[LU]?)?)\\[(?:(-?[0-9a-zA-Z]++)\\.\\.(-?[0-9a-zA-Z]++)\\]?)?\\)?|"

							/* an open parenthesis */
							+ "\\(|"

							/* a closed parenthesis */
							+ "\\)|"

							/* a pike (|) */
							+ "\\||"

							/* an open square bracket - starts a character class */
							+ "\\[|"

							/* a closed square bracket - ends a character class */
							+ "\\]|"

							/*
							 * a "#" - starts comments when COMMENTS flag is
							 * enabled
							 */
							+ "#|"

							/* matches an escaped character */
							+ "\\\\.");
		}
	}

	/**
	 * Class used to refactor a regular expression with "advanced" patternSyntax
	 * to an equivalent form usable in Java and other RegEx engines.
	 * 
	 * <p>
	 * Depending on the flags set when compiling the pattern, additional
	 * simplifications may be performed. Note these additional simplifications
	 * are not required to use the pattern in Java. Rather, they are intended to
	 * refactor the regular expression to a form usable by "any" RegEx engine
	 * (including "basic" ones found in text editors, renamers, and other
	 * software).
	 * </p>
	 */
	private class Refactor
	{
		/** The regular expression to refactor. */
		private final String regex;

		/*
		 * These values are used during the refactoring process, and were made
		 * class members to reduce the need to pass them as parameters to the
		 * different functions called during the refactoring process.
		 * 
		 * Note: these values may change. The value is based on the current step
		 * of the refactoring process and how much the string has been parsed
		 * during that step.
		 */

		/** The text for the matcher in the refactoring step **/
		private String text;

		/**
		 * The flags
		 */
		@SuppressWarnings("hiding")
		private int flags = flags();

		/** Used to match the different parts to refactor. */
		private java.util.regex.Matcher matcher;

		/**
		 * Contains the current result of the refactoring process.
		 * 
		 * <p><b>Note</b>: this is the StringBuffer used when calling
		 * {@link java.util.regex.Matcher#appendReplacement(StringBuffer, String)}
		 * and {@link java.util.regex.Matcher#appendTail(StringBuffer)}</p>
		 */
		private StringBuffer result;

		/**
		 * Stores the matching String for <tt>matcher</tt> - the return value of
		 * {@link java.util.regex.Matcher#group()}
		 */
		private String match;

		/** The number of unclosed opening parenthesis. */
		private int parenthesisDepth;

		/** The number of unclosed opening square brackets. */
		private int charClassDepth;

		/**
		 * The total number of capture groups (includes capturing groups added
		 * as part of the refactoring).
		 */
		private int totalGroups;

		/**
		 * Total number of unnamed groups
		 */
		private int unnamedGroupCount;

		/**
		 * The number of capture groups (excludes capture groups added as part
		 * of the refactoring).
		 */
		private int currentGroup;

		/**
		 * The current named group (starts at 1)
		 */
		private int namedGroup;

		/**
		 * The current unnamed group (starts at 1)
		 */
		private int unnamedGroup;

		/** The index for the first non-null group. */
		private int group;

		/**
		 * A list with each index the mapping name for the repective perl group.
		 * 
		 * <p>Index 0 in the list refers to group 1</p>
		 * (only used if {@link Pattern#DOTNET_NUMBERING} is set).
		 */
		private HashMap<Integer, String> perlGroupMapping;

		/**
		 * A map with mappings from the mapping name of a capture group to the
		 * group index of the empty capture group used to test whether the group
		 * matched or not.
		 */
		private Map<String, Integer> testConditionGroups = new HashMap<String, Integer>(
				2);

		/**
		 * Set of group names that have an "any group" back reference
		 */
		private Set<String> anyGroupReferences = new HashSet<String>(2);

		/** The capture groups that require a testing group. */
		private TreeSet<String> requiresTestingGroup = new TreeSet<String>();

		/**
		 * A stack of states used to track when a testing group should be added
		 * to a capture group.
		 */
		private Stack<AddTestGroupState> addTestGroup = new Stack<AddTestGroupState>();

		/**
		 * A stack of states used to track the else branch of a conditional
		 * subpattern.
		 */
		private Stack<MatchState> handleElseBranch = new Stack<MatchState>();

		/**
		 * A stack of states used to track the end of the assertion in an assert
		 * conditional
		 */
		private Stack<MatchState> handleEndAssertCond = new Stack<MatchState>();

		/**
		 * A stack of states used to track the branches in a branch reset
		 * subpattern.
		 */
		private Stack<BranchResetState> branchReset = new Stack<BranchResetState>();

		/**
		 * A stack used to store the current flags.
		 * 
		 * <p>
		 * When entering a new parenthesis grouping, the current flags are
		 * saved. This value is later restored upon existing the parenthesis
		 * grouping.
		 * </p>
		 */
		private Stack<Integer> flagsStack = new Stack<Integer>();

		/**
		 * A map with mappings from group name to the number of occurrences for
		 * that group name.
		 */
		@SuppressWarnings("hiding")
		private Map<String, Integer> groupNameOccurrences = new HashMap<String, Integer>(
				2);

		/**
		 * The differences (insertions, deletions, and replacements) made to the
		 * original regular expression during the latest refactoring step.
		 * 
		 * <p>After each step, these differences are added to the list of
		 * changes.</p>
		 */
		Differences differences = new Differences();

		/**
		 * The differences (insertions, deletions, and replacements) made to the
		 * original regular expression during the refactoring process.
		 * 
		 * <p>If the refactored regular expression throws a
		 * {@link PatternSyntaxException}, this field is used to map the index
		 * for the exception to its respective index in the original regular
		 * expression.</p>
		 */
		Differences changes = new Differences();

		/**
		 * List of changes performed during the pre-refactoring step
		 */
		private Differences preRefactoringChanges;

		/**
		 * Mapping from integer (incremental) to index for error
		 */
		private Map<Integer, Integer> errorTrace;

		/**
		 * Indicates if the current VM is Java 1.5
		 */
		private boolean isJava1_5;

		/**
		 * Resets the necessary values before performing the next step of the
		 * refactoring.
		 */
		void reset()
		{
			flags = flags();
			parenthesisDepth = 0;
			charClassDepth = 0;
			currentGroup = 0;
			totalGroups = 0;
			namedGroup = 0;
			unnamedGroup = 0;

			result = new StringBuffer();

			addTestGroup.clear();
			handleElseBranch.clear();
			branchReset.clear();
			groupNameOccurrences.clear();
			flagsStack.clear();

			changes.addAll(differences);
			differences = new Differences();
		}

		/**
		 * Internal method used for handling all patternSyntax errors. The
		 * pattern is
		 * displayed with a pointer to aid in locating the patternSyntax error.
		 * 
		 * @param errorMessage
		 *            the <code>PatternErrorMessage</code> for the error
		 * @param index
		 *            The approximate index in the pattern of the error, or -1
		 *            if the index is not known
		 * @return a <code>PatternSyntaxException</code> with the given error
		 *         message, index, and using the original regex
		 */
		// private PatternSyntaxException error(PatternErrorMessage
		// errorMessage,
		// int index)
		private PatternSyntaxException error(String errorMessage, int index)
		{
			int originalIndex = changes.getOriginalIndex(index);

			return new PatternSyntaxException(errorMessage, regex,
					originalIndex);
		}

		/**
		 * Creates a new Refactor object and refactors the given regular
		 * expression so that it can be compiled by the original Pattern class.
		 * 
		 * @param regex
		 *            the regular expression to refactor
		 * @param patternSyntax
		 *            the <code>PatternSyntax</code>
		 */
		Refactor(String regex)
		{
			this.regex = regex;

			if (has(LITERAL)) {
				this.result = new StringBuffer(regex);
				return;
			}

			String javaVersion = System.getProperty("java.version");
			isJava1_5 = naturalCompareTo(javaVersion, "1.5.0") >= 0 &&
					naturalCompareTo(javaVersion, "1.6.0") < 0;

			this.result = new StringBuffer();

			preRefactor();
			preRefactoringChanges = differences;

			refactor();
			afterRefactor();
		}

		/**
		 * Returns the current result of the refactoring.
		 */
		@Override
		public String toString()
		{
			return result.toString();
		}

		/**
		 * Indicates whether a particular flag is set or not.
		 * 
		 * @param f
		 *            the flag to test
		 * @return <code>true</code> if the particular flag is set
		 */
		boolean has(int f)
		{
			return (flags & f) != 0;
		}

		/**
		 * <p>
		 * Used in the loop that matches parts for the current refactoring step.
		 * </p>
		 * 
		 * <p>Sets {@link #match} to the matched string, and sets {@link #group}
		 * to the index for the first non-null capture group.</p>
		 * 
		 * @return true if the current loop should be skipped
		 */
		private boolean loopSetup()
		{
			match = matcher.group();
			group = getUsedGroup(matcher);

			return false;
		}

		/**
		 * Steps taken to prepare the regular expression to be refactored
		 */
		private void preRefactor()
		{
			text = regex.toString();
			matcher = preRefactor.matcher(regex);

			// add a map from group 0 to group 0
			// TODO: add occurrence??
			addUnnamedGroup("[0][1]", 0);
			setOccurrences("[0]", 1);

			while (matcher.find()) {
				if (loopSetup())
					continue;

				if (group == 1) {
					/*
					 * matches
					 * "(?onFlags-offFlags)" or "(?onFlags-offFlags:"
					 * (also matches a non-capture group
					 * - onFlags/offFlags are omitted)
					 * 
					 * group: onFlags (empty string if none)
					 * group + 1: offFlags
					 * (empty string if none; null, if omitted)
					 * 
					 * (2 groups)
					 */

					if (!inCharClass())
						preRefactorFlags();
				} else if (group >= 3 && group <= 8) {
					/*
					 * matches a named capture group
					 * "(?<name>" (form 0)
					 * "(?'name'" (form 1)
					 * "(?P<name>" (form 2)
					 * 
					 * group: everything after first symbol
					 * group + 1: the name
					 * (6 groups)
					 */

					if (!inCharClass()) {
						int form = (group - 3) / 2;
						preRefactorCaptureGroup(FOR_NAME, form);
					}
				} else if (group == 9) {
					/*
					 * matches an unnamed capture group "("
					 * - not followed by a "?"
					 * 
					 * group: everything (1 group)
					 */

					if (!inCharClass()) {
						int form = group - 9;
						preRefactorCaptureGroup(!FOR_NAME, form);
					}
				} else if (group >= 10 && group <= 14) {
					/*
					 * matches a back reference (by name)
					 * "\g{name}" (form 0)
					 * "\k<name>" (form 1)
					 * "\k'name'" (form 2)
					 * "\k{name}" (form 3)
					 * "(?P=name)" (form 4)
					 * 
					 * group : the name
					 * (<name> can only be an "any group" (e.g. groupName[0]))
					 * (5 groups)
					 */

					if (!inCharClass())
						preRefactorBackreference();
				} else if (group == 15) {
					/*
					 * matches an assert condition
					 * "(?(?=)", "(?(?!)", "(?(?<=)", or "(?(?<!)"
					 * 
					 * group: the assert part (inside the parentheses)
					 * (1 group)
					 */

					if (!inCharClass())
						preRefactorAssertCondition();
				} else if (group == 16) {
					/*
					 * matches a reference condition (by number) matches
					 * "(?(n)" or "(?(-n)"
					 * 
					 * group: the number (1 group)
					 */

					if (!inCharClass()) {
						int form = group - 16;
						preRefactorConditionalPattern(!FOR_NAME, form);
					}
				} else if (group >= 17 && group <= 25) {
					/*
					 * matches a reference condition (by name)
					 * "(?(<name>)" (form 0),
					 * "(?('name')" (form 1), or
					 * "(?(name)" (form 2)
					 * 
					 * group: everything after first symbol (excluding ")")
					 * group + 1: the name
					 * group + 2: the occurrence (if specified)
					 * (9 groups)
					 */

					if (!inCharClass()) {
						int form = (group - 17) / 3;
						preRefactorConditionalPattern(FOR_NAME, form);
					}
				} else if (group == 26) {
					/*
					 * matches comment group
					 * "(?#comment) - comment cannot contain ")"
					 * 
					 * group: everything
					 * (1 group)
					 */

					if (!inCharClass()) {
						if (!match.endsWith(")"))
							throw error(UNCLOSED_COMMENT, matcher.end());

						// remove (in internal pattern)
						replaceWith("");
					}
				} else if (group == 27) {
					/*
					 * matches a "branch reset" subpattern "(?|"
					 * 
					 * group: everything
					 * (1 group)
					 */

					if (!inCharClass())
						preRefactorBranchReset();
				} else {
					preRefactorOthers();
				}
			}

			unnamedGroupCount = unnamedGroup;
			setCapturingGroupCount(currentGroup);
			setOccurrences("", currentGroup);

			if (has(DOTNET_NUMBERING)) {
				// for each named group, mark the occurrences as 1
				// for its respective number group
				// (doesn't affect named groups in "branch reset" patterns)

				for (int i = 1; i <= namedGroup; i++) {
					int groupIndex = unnamedGroupCount + i;
					setOccurrences("[" + groupIndex + "]", 1);
				}

				int currentNamedGroup = 0;

				for (Entry<Integer, String> entry : perlGroupMapping()
						.entrySet()) {
					String mappingName = entry.getValue();

					if (mappingName.charAt(0) != '[') {
						// a named group - using mapping name of unnamed group

						currentNamedGroup++;
						String groupName = wrapIndex(unnamedGroupCount
								+ currentNamedGroup);

						mappingName = getMappingName(groupName, 0);
						entry.setValue(mappingName);
					}
				}
			}

			setGroupNameOccurrences(new HashMap<String, Integer>(
					groupNameOccurrences));

			for (String groupName : anyGroupReferences) {
				if (groupCount(groupName) != 1)
					requiresTestingGroup.add(getMappingName(groupName, 0));
			}

			matcher.appendTail(result);
		}

		/**
		 * Refactors the regular expression
		 */
		private void refactor()
		{
			text = result.toString();
			matcher = getRefactorPattern().matcher(result);
			reset();

			while (matcher.find()) {
				if (loopSetup())
					continue;

				if (group == 1) {
					/*
					 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:"
					 * (also matches a non-capture group
					 * - onFlags/offFlags are omitted)
					 * 
					 * group: onFlags (empty string if none)
					 * group + 1: offFlags
					 * (empty string if none; null, if omitted)
					 * (2 groups)
					 */

					if (!inCharClass())
						refactorFlags();
				} else if (group >= 3 && group <= 5) {
					/*
					 * matches a named capture group
					 * "(?<name>" (form 0)
					 * "(?'name'" (form 1)
					 * "(?P<name>" (form 2)
					 * 
					 * group: the name
					 * (3 groups)
					 */

					if (!inCharClass())
						refactorCaptureGroup(FOR_NAME);
				} else if (group == 6) {
					/*
					 * matches an unnamed capture group
					 * "(" - not followed by a "?"
					 * 
					 * group: everything
					 * (1 group)
					 */

					if (!inCharClass())
						refactorCaptureGroup(!FOR_NAME);
				} else if (group >= 7 && group <= 10) {
					/*
					 * matches a back reference (by number)
					 * "\n" (form 0)
					 * "\gn" (form 1)
					 * "\g{n}" or "\g{-n}" (form 2)
					 * 
					 * group: the number
					 * last group: the next character (if a digit)
					 * (4 groups)
					 */

					if (!inCharClass() || group == 7) {
						int form = group - 7;
						int digitGroup = 10;
						refactorBackreference(!FOR_NAME, form, digitGroup);
					}
				} else if (group >= 11 && group <= 26) {
					/*
					 * matches a back reference (by name)
					 * "\g{name}" (form 0)
					 * "\k<name>" (form 1)
					 * "\k'name'" (form 2)
					 * "\k{name}" (form 3)
					 * "(?P=name)" (form 4)
					 * 
					 * group: everything after the first symbol
					 * group + 1: the name
					 * group + 2: the occurrence (if specified)
					 * last group: the next character (if a digit)
					 * (16 groups)
					 */

					if (!inCharClass()) {
						int form = (group - 11) / 3;
						int digitGroup = 26;
						refactorBackreference(FOR_NAME, form, digitGroup);
					}
				} else if (group == 27) {
					/*
					 * matches an assert condition
					 * "(?(?=)", "(?(?!)", "(?(?<=)", or "(?(?<!)"
					 * 
					 * group: the assert part (inside the parentheses)
					 * (1 group)
					 */

					if (!inCharClass())
						refactorAssertCondition();
				} else if (group == 28) {
					/*
					 * matches a conditional pattern (by number)
					 * "(?(n)" or "(?(-n)" (form 0)
					 * 
					 * group: the number
					 * (1 group)
					 */

					if (!inCharClass())
						refactorConditionalPattern(!FOR_NAME);
				} else if (group >= 29 && group <= 34) {
					/*
					 * matches a named reference condition
					 * "(?(<name>)" (form 0),
					 * "(?('name')" (form 1), or
					 * "(?(name)" (form 2)
					 * 
					 * group: the name
					 * group + 1: the occurrence (if specified)
					 * (6 groups)
					 */

					if (!inCharClass())
						refactorConditionalPattern(FOR_NAME);
				} else if (group == 35) {
					/*
					 * matches a "branch reset" subpattern "(?|"
					 * 
					 * group: everything
					 * (1 group)
					 */

					if (!inCharClass()) {
						refactorBranchReset();
					}
				} else if (group == 36) {
					/*
					 * matches a numeric range
					 * "(?Z[start..end])" or "(?NZ[start..end])"
					 * 
					 * group: "Z" or "NZ" (optional base and L/U)
					 * //group + 1: "r" for raw mode, or <null>
					 * group + 1: start
					 * group + 2: end
					 * (3 groups)
					 */

					if (!inCharClass())
						refactorNumericRange();
				} else {
					refactorOthers();
				}
			}

			if (parenthesisDepth > 0)
				throw error(UNCLOSED_GROUP, text.length());

			matcher.appendTail(result);
		}

		/**
		 * Steps taken after the regular expression is refactored
		 */
		private void afterRefactor()
		{
			text = result.toString();
			matcher = afterRefactor.matcher(result);
			reset();

			while (matcher.find()) {
				if (loopSetup())
					continue;

				if (group == 1) {
					/*
					 * matches:
					 * "\g{##group-mappingName}"
					 * "\g{##branchGroup-mappingName}"
					 * "\g{##test-mappingName}"
					 * "\g{##testF-mappingName}"
					 * 
					 * group: the number - mapped to the position to show the
					 * error
					 * group + 1: type ("group", "test", or "testF")
					 * group + 2: mappingName
					 * (3 groups)
					 */
					if (!inCharClass()) {
						String groupType = matcher.group(group + 1);
						String mappingName = matcher.group(group + 2);

						if (groupType.equals("group")) {
							replaceWith(acceptGroup(mappingName));
						} else if (groupType.equals("branchGroup")) {
							replaceWith(acceptBranchReset(mappingName));
						} else if (groupType.equals("test")) {
							replaceWith(acceptTestingGroup(mappingName));
						} else if (groupType.equals("testF")) {
							replaceWith(failTestingGroup(mappingName));
						}
					}
				} else if (group == 4) {
					/*
					 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:"
					 * (also matches a non-capture group - onFlags/offFlags are
					 * omitted)
					 * 
					 * group: onFlags (empty string if none)
					 * group + 1: offFlags (empty string if none; null, if
					 * omitted)
					 * (2 groups)
					 */

					if (!inCharClass())
						afterRefactorFlags();
				} else if (group == 6) {
					/*
					 * matches "\x{hhh..} - a hex code"
					 * 
					 * group: the number
					 * (1 group)
					 */

					afterRefactorHexUnicode();
				} else if (group == 7) {
					/*
					 * matches "\xh" or "\xhh" - a hex code
					 * 
					 * group: the number
					 * (1 group)
					 */

					afterRefactorHexChar();
				} else if (group == 8) {
					/*
					 * matches a unicode character
					 * 
					 * group: the number
					 * (1 group)
					 */

					afterRefactorUnicode();
				} else if (group == 9) {
					/*
					 * matches a POSIX character class
					 * 
					 * group: "^" or "" (whether to negate or not)
					 * group + 1: the class name
					 * (2 group)
					 */
					afterRefactorPosixClass();
				} else if (group == 11) {
					/*
					 * matches a control character - \cA through \cZ
					 * 
					 * These are equivalent to \x01 through \x1A (26 decimal).
					 * 
					 * group: the control character's letter
					 * (either upper and lower case are allowed)
					 * (1 group)
					 */

					afterRefactorControlCharacter();
				} else if (group == 12) {
					/*
					 * matches an unnamed capture group "(" - not followed by a
					 * "?"
					 * 
					 * group: everything (1 group)
					 */

					if (!inCharClass())
						afterRefactorCaptureGroup();
				} else {
					afterRefactorOthers();
				}
			}

			matcher.appendTail(result);
		}

		/**
		 * <p>
		 * Returns whether the specified mapping name is an "any group".
		 * </p>
		 * 
		 * <p>
		 * <b>Note</b>: these are groups with an occurrence of 0, for example,
		 * "groupName[0]". However "[0]" is not an "any group", but instead
		 * refers to the match itself.
		 * </p>
		 * 
		 * <p>
		 * Any groups allow referring to the first matched group in the case of
		 * multiple groups with the same name. If there is only one group, the
		 * "any group" is the first group.
		 * </p>
		 * 
		 * @param mappingName
		 *            the mapping name for the group
		 * @return <code>true</code> if the given group is an "any group"
		 */
		private boolean isAnyGroup(String mappingName)
		{
			// greater than, because "[0]" is not an "any group"
			// (requires a group name)
			return mappingName.length() > "[0]".length()
					&& mappingName.endsWith("[0]");
		}

		/**
		 * Removes the trailing "[0]" for the inputted "any group".
		 * 
		 * @param mappingName
		 *            must be an "any group"
		 * @return the group name for this "any group"
		 */
		private String anyGroupName(String mappingName)
		{
			return mappingName.substring(0, mappingName.length()
					- "[0]".length());
		}

		/**
		 * Surrounds the passed string in a non-capture group.
		 * 
		 * <p><b>Note</b>: If the pattern is compiled with the
		 * {@link Pattern#ONLY_CAPTURING_GROUPS} flag, then the group will be a
		 * capture group, and the {@link #totalGroups} will increase by one to
		 * reflect this.</p>
		 * 
		 * @param str
		 *            the string to surround
		 * @return the RegEx for the given string surrounded by a non-capture
		 *         group.
		 */
		private String nonCaptureGroup(String str)
		{
			return startNonCaptureGroup() + str + ")";
		}

		/**
		 * Returns the string that represents the start of a non-capture group,
		 * "(?:".
		 * 
		 * @return the string that represents the start of a non-capture group
		 */
		private String startNonCaptureGroup()
		{
			// if (!supportedSyntax(NONCAPTURE_GROUPS)) {
			// totalGroups++;
			// return "(";
			// } else
			return "(?:";
		}

		/**
		 * Returns a regular expression that matches the capture group with the
		 * given mapping name.
		 * 
		 * @param mappingName
		 *            the mapping name for the group to accept
		 * @return a "raw" RegEx that matches the capture group with the given
		 *         mapping name
		 */
		private String acceptGroup(String mappingName)
		{
			String accept;

			if (isAnyGroup(mappingName))
				accept = anyGroup(mappingName);
			else {
				int mappedIndex = getMappedIndex(mappingName);

				if (invalidForwardReference(mappedIndex))
					throw invalidForwardReference();

				accept = "\\" + mappedIndex;
			}

			return accept;
		}

		/**
		 * Returns a regular expression that matches any one of the groups
		 * with the given group name.
		 * 
		 * @param mappingName
		 *            group name with an occurrence of 0, an "any group"
		 * @return a regular expression that matches any one of the groups with
		 *         the given group name
		 */
		private String anyGroup(String mappingName)
		{
			if (mappingName.charAt(0) == '[') {
				// ex. [1][0] - (i.e. an unnamed group)
				return acceptBranchReset(mappingName);
			}

			// remove trailing "[0]"
			String groupName = anyGroupName(mappingName);

			int groupCount = groupCount(groupName);
			StringBuilder acceptAny = new StringBuilder();
			StringBuilder previousGroups = new StringBuilder();

			for (int i = 1; i <= groupCount; i++) {
				String tmpMappingName = getMappingName(groupName, i);
				int testingGroup = getTestingGroup(tmpMappingName);
				int mappedIndex = getMappedIndex(tmpMappingName);

				if (invalidForwardReference(mappedIndex))
					continue;

				acceptAny.append(previousGroups).append("\\").append(
						mappedIndex).append('|');

				previousGroups.append(failTestingGroup(testingGroup));
			}

			if (acceptAny.length() == 0)
				throw invalidForwardReference();

			return acceptAny.deleteCharAt(acceptAny.length() - 1).toString();
		}

		/**
		 * Returns a regular expression that matches any one of the groups
		 * in a "branch reset" subpattern with the given group name.
		 * 
		 * @param mappingName
		 *            group name with an occurrence of 0, an "any group"
		 * @return a regular expression that matches any one of the groups in a
		 *         "brach reset" subpattern with the given group name
		 */
		private String acceptBranchReset(String mappingName)
		{
			// remove trailing "[0]"
			String groupName = anyGroupName(mappingName);

			int groupCount = groupCount(groupName);
			StringBuilder acceptAnyBranch = new StringBuilder();

			for (int i = 1; i <= groupCount; i++) {
				String tmpMappingName = getMappingName(groupName, i);
				int mappedIndex = getMappedIndex(tmpMappingName);

				if (invalidForwardReference(mappedIndex))
					continue;

				acceptAnyBranch.append("\\").append(mappedIndex).append('|');
			}

			if (acceptAnyBranch.length() == 0)
				throw invalidForwardReference();

			return acceptAnyBranch.deleteCharAt(acceptAnyBranch.length() - 1)
					.toString();
		}

		/**
		 * <p>
		 * Returns a regular expression that matches the given target group.
		 * </p>
		 */
		private String acceptTestingGroup(int targetGroup)
		{
			return "(?=\\" + targetGroup + ")";
		}

		/**
		 * Returns a regular expression which will match the testing group(s)
		 * associated with the specified mapping name.
		 */
		private String acceptTestingGroup(String mappingName)
		{
			String accept;

			if (isAnyGroup(mappingName))
				accept = anyCondition(mappingName);
			else {
				Integer testingGroup = getTestingGroup(mappingName);

				if (testingGroup == null)
					throw error(INTERNAL_ERROR, -1);

				if (invalidForwardReference(testingGroup))
					throw invalidForwardReference();

				accept = "\\" + testingGroup;
			}

			return "(?=" + accept + ")";
		}

		/**
		 * <p>
		 * Returns a regular expression which fails if the specified target
		 * group matches.
		 * </p>
		 */
		private String failTestingGroup(int targetGroup)
		{
			return "(?!\\" + targetGroup + ")";
		}

		/**
		 * Returns a regular expression which fails if the testing group(s)
		 * associated with the specified mapping name matches.
		 */
		private String failTestingGroup(String mappingName)
		{
			String fail;

			if (isAnyGroup(mappingName))
				fail = anyCondition(mappingName);
			else {
				int testingGroup = getTestingGroup(mappingName);

				if (invalidForwardReference(testingGroup))
					throw invalidForwardReference();

				fail = "\\" + testingGroup;
			}

			return "(?!" + fail + ")";
		}

		/**
		 * Returns a regular expression that matches any one of the
		 * "testing groups" associated with the given mapping name.
		 * 
		 * @param mappingName
		 *            group name with an occurrence of 0, an "any group"
		 * @return a RegEx that matches any one of the "testing groups"
		 *         associated with the given mapping name
		 */
		private String anyCondition(String mappingName)
		{
			// remove trailing "[0]"
			String groupName = anyGroupName(mappingName);

			int groupCount = groupCount(groupName);
			StringBuilder acceptAny = new StringBuilder();

			for (int i = 1; i <= groupCount; i++) {
				String tmpMappingName = getMappingName(groupName, i);
				int testingGroup = getTestingGroup(tmpMappingName);

				if (invalidForwardReference(testingGroup))
					continue;

				acceptAny.append("\\").append(testingGroup).append('|');
			}

			if (acceptAny.length() == 0)
				throw invalidForwardReference();

			return acceptAny.deleteCharAt(acceptAny.length() - 1).toString();
		}

		/**
		 * Returns a regular expression that always fails
		 * 
		 * @return a regular expression that always fails
		 */
		private String fail()
		{
			return "\\b\\B";
		}

		/**
		 * Adds a mapping for the specified unnamed group.
		 * 
		 * @param mappingName
		 *            the mapping name
		 * @param targetGroupIndex
		 *            the actual group number (in the internal pattern)
		 */
		private void addUnnamedGroup(String mappingName, int targetGroupIndex)
		{
			addGroup(mappingName, targetGroupIndex);
		}

		/**
		 * Adds a mapping for the specified named group.
		 * 
		 * @param mappingName
		 *            the mapping name
		 * @param targetGroupIndex
		 *            the actual group number (in the internal pattern)
		 */
		private void addNamedGroup(String mappingName, int targetGroupIndex)
		{
			addGroup(mappingName, targetGroupIndex);
		}

		/**
		 * Adds a mapping from <code>mappingName</code> to
		 * <code>targetGroupIndex</code> to the group mapping
		 * 
		 * @param mappingName
		 *            the mapping name
		 * @param targetGroupIndex
		 *            the actual group number (in the internal pattern)
		 */
		private void addGroup(String mappingName, int targetGroupIndex)
		{
			getGroupMapping().put(mappingName, targetGroupIndex);
		}

		/**
		 * Adds a new mapping to {@link #testConditionGroups}.
		 * 
		 * @param mappingName
		 *            the mapping name
		 * @param targetGroupIndex
		 *            the actual group number (in the internal pattern)
		 */
		private void addTestingGroup(String mappingName, int targetGroupIndex)
		{
			testConditionGroups.put(mappingName, targetGroupIndex);
		}

		/**
		 * <p>
		 * Returns the absolute group number associated with the match.
		 * </p>
		 * 
		 * <p>
		 * If the group number is relative, then it is converted to an absolute
		 * occurrence
		 * </p>
		 * 
		 * TODO: modify function name
		 * 
		 * @param occurrences
		 *            the total number of occurrences of the group name (used to
		 *            from relative index to absolute)
		 * @return the absolute group number associated with the match
		 */
		private String getAbsoluteGroup(String groupOccurrence, int occurrences)
		{
			// boolean hasPlus = groupOccurrence.charAt(0) == '+';
			// int groupIndex = Integer.parseInt(hasPlus
			// ? groupOccurrence.substring(1) : groupOccurrence);

			int groupIndex = Integer.parseInt(groupOccurrence);

			// if (hasPlus) {
			// // groupIndex is relative
			// // e.g. +1 with occurrences == 5 -> 6
			// groupIndex += occurrences;
			// } else
			if (groupOccurrence.charAt(0) == '-') {
				// groupIndex is relative
				// e.g. -1 with occurrences == 5 -> 5
				groupIndex += occurrences + 1;

				if (groupIndex <= 0)
					return neverUsedMappingName();

				if (has(DOTNET_NUMBERING))
					return getPerlGroup(groupIndex);
			}

			return getMappingName(groupIndex, 0);
		}

		/**
		 * Returns a String that is never used as a mapping name
		 * 
		 * @return
		 */
		private String neverUsedMappingName()
		{
			return "$neverUsed";
		}

		private String getAbsoluteNamedGroup(String groupName,
				String groupOccurrence)
		{
			int occurrences = getOccurrences(groupName);

			// boolean hasPlus = groupOccurrence.charAt(0) == '+';
			// int groupIndex = Integer.parseInt(hasPlus
			// ? groupOccurrence.substring(1) : groupOccurrence);

			int groupIndex = Integer.parseInt(groupOccurrence);

			// if (hasPlus) {
			// // groupIndex is relative
			// // e.g. +1 with occurrences == 5 -> 6
			// groupIndex += occurrences;
			// } else
			if (groupOccurrence.charAt(0) == '-') {
				// groupIndex is relative
				// e.g. -1 with occurrences == 5 -> 5
				groupIndex += occurrences + 1;

				if (groupIndex <= 0)
					return neverUsedMappingName();
			}

			return getMappingName(groupName, groupIndex);
		}

		/**
		 * TODO: modify function name
		 * 
		 * @param groupName
		 *            the group name whose group index is returned
		 * @return the group index
		 */
		private String getGroupIndex(String groupName, String groupOccurrence)
		{
			if (groupOccurrence == null) {
				// TODO: e.g. groupName
				return getMappingName(groupName, 0);
			} else if (groupName.length() == 0) {
				return getAbsoluteGroup(groupOccurrence, currentGroup);
			} else {
				return getAbsoluteNamedGroup(groupName, groupOccurrence);
			}
		}

		/**
		 * Returns the mapping from perl group numbers to actual group number
		 * 
		 * @return
		 */
		private HashMap<Integer, String> perlGroupMapping()
		{
			if (perlGroupMapping == null)
				perlGroupMapping = new HashMap<Integer, String>();

			return perlGroupMapping;
		}

		private String getPerlGroup(int groupIndex)
		{
			return perlGroupMapping().get(groupIndex);
		}

		/**
		 * Returns the group index for the testing group associated with the
		 * given mapping name.
		 * 
		 * @param mappingName
		 *            the mapping name
		 * @return the group index for the testing group associated with the
		 *         given mapping name
		 */
		private Integer getTestingGroup(String mappingName)
		{
			return testConditionGroups.get(mappingName);
		}

		/**
		 * Add a mapping from integer (incremental) to the position of the match
		 * as an "in case of error" trace
		 * 
		 * @param errorIndex
		 *            position to show error
		 * 
		 * @return the integer key for the added mapping
		 */
		private Integer addErrorTrace(int errorIndex)
		{
			if (errorTrace == null)
				errorTrace = new HashMap<Integer, Integer>(2);

			Integer key = errorTrace.size();
			errorTrace.put(errorTrace.size(), errorIndex);

			return key;
		}

		private PatternSyntaxException invalidForwardReference()
		{
			int index = errorTrace.get(Integer.valueOf(matcher.group(group)));

			index = preRefactoringChanges.getOriginalIndex(index);

			return new PatternSyntaxException(INVALID_FORWARD_REFERENCE, regex,
					index);
		}

		/**
		 * Indicates whether the given index is an invalid forward reference
		 * 
		 * @param mappedIndex
		 *            the index for the group in the internal
		 *            <code>Matcher</code>
		 * @return <code>true</code> if the given index is an invalid forward
		 *         reference
		 */
		private boolean invalidForwardReference(int mappedIndex)
		{
			if (isJava1_5 && mappedIndex > totalGroups + 1)
				return true;

			return mappedIndex > totalGroups && mappedIndex >= 10;
		}

		/**
		 * Returns the number of occurrences of the given group name.
		 * 
		 * @param groupName
		 *            the group name
		 * @return the number of occurrences of the given group name
		 */
		private int getOccurrences(String groupName)
		{
			// if (groupName.length() == 0)
			// return currentGroup;

			Integer occurrence = groupNameOccurrences.get(groupName);
			return (occurrence == null ? 0 : occurrence);
		}

		/**
		 * Sets the number of occurrences of the give group name
		 * 
		 * @param groupName
		 *            the group name
		 * @param occurrences
		 *            the number of occurrences
		 */
		private void setOccurrences(String groupName, int occurrences)
		{
			groupNameOccurrences.put(groupName, occurrences);
		}

		/**
		 * Increases the occurrence count for the given group name by one.
		 * 
		 * @param groupName
		 *            the group name whose occurrence count to increase
		 * 
		 * @return the (new) number of occurrences for the given group name
		 */
		private int nextOccurrence(String groupName)
		{
			@SuppressWarnings("hiding")
			int occurrence = getOccurrences(groupName) + 1;

			// store the new occurrence count
			setOccurrences(groupName, occurrence);

			return occurrence;
		}

		/**
		 * Returns whether the current parenthesis depth matches the parenthesis
		 * depth of the top-most match state
		 * 
		 * @param matchStates
		 *            a stack of states
		 * @return whether the current parenthesis depth matches the parenthesis
		 *         depth of the top-most match state
		 */
		private boolean atRightDepth(Stack<? extends State> matchStates)
		{
			if (matchStates.size() == 0)
				return false;

			return matchStates.peek().getParenthesisDepth() == parenthesisDepth;
		}

		/**
		 * Increases the parenthesis depth by one.
		 * 
		 * @param duringPreRefactor
		 *            whether this function call occurs during the pre-refactor
		 *            step
		 */
		private void increaseParenthesisDepth(boolean duringPreRefactor)
		{
			parenthesisDepth++;
			flagsStack.push(flags);
		}

		/**
		 * Increases the current group.
		 * 
		 * @param duringPreRefactor
		 *            whether this function call occurs during the pre-refactor
		 *            step
		 */
		private void increaseCurrentGroup(boolean duringPreRefactor)
		{
			currentGroup++;

			if (!duringPreRefactor) {
				// if (digitCount(currentGroup) != digitCount(currentGroup - 1))
				// matcher.usePattern(getRefactorPattern(currentGroup));

				totalGroups++;
			}
		}

		/**
		 * Steps to perform when encountering an close parenthesis.
		 * 
		 * @param duringPreRefactor
		 *            whether this function call occurs during the pre-refactor
		 *            step
		 */
		private void decreaseParenthesisDepth(boolean duringPreRefactor)
		{
			parenthesisDepth--;
			flags = flagsStack.pop();
		}

		/*
		 * The below functions contain the steps to refactor a specific part of
		 * the refactoring. The respective function is called during the
		 * different steps in the refactoring process and for each part in the
		 * refactoring of that step.
		 */

		/**
		 * Modify the {@link #flags} variable to account for a change in the
		 * flags (some
		 * of flags may be ignored).
		 * 
		 * @param onFlags
		 *            the flags that were turned on
		 * @param offFlags
		 *            the flags that were turned off
		 */
		private void setFlags(String onFlags, String offFlags)
		{
			if (onFlags.contains("x"))
				flags |= COMMENTS;

			if (onFlags.contains("d"))
				flags |= UNIX_LINES;

			if (offFlags != null) {
				if (offFlags.contains("x"))
					flags &= ~COMMENTS;

				if (offFlags.contains("d"))
					flags &= ~UNIX_LINES;
			}
		}

		private String replaceFlags(String onFlags, String offFlags)
		{
			boolean flagsChanged = false;
			StringBuilder newFlags = new StringBuilder(matcher.end()
					- matcher.start());

			if (onFlags.contains("J")) {
				onFlags = onFlags.replace("J", "");
				flags |= DUPLICATE_NAMES;
				flagsChanged = true;
			}

			if (onFlags.contains("n")) {
				onFlags = onFlags.replace("n", "");
				flags |= EXPLICIT_CAPTURE;
				flagsChanged = true;
			}

			newFlags.append(onFlags);

			if (offFlags != null) {
				if (offFlags.contains("J")) {
					offFlags = offFlags.replace("J", "");
					flags &= ~DUPLICATE_NAMES;
					flagsChanged = true;
				}

				if (offFlags.contains("n")) {
					offFlags = offFlags.replace("n", "");
					flags &= ~EXPLICIT_CAPTURE;
					flagsChanged = true;
				}

				if (offFlags.length() != 0)
					newFlags.append('-').append(offFlags);
			}

			return flagsChanged ? newFlags.toString() : null;
		}

		/**
		 * Refactors the flags during the pre-refactoring step
		 */
		private void preRefactorFlags()
		{
			/*
			 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:" (also
			 * matches a non-capture group - onFlags/offFlags are omitted)
			 * 
			 * group: onFlags (empty string if none) group + 1: offFlags (empty
			 * string if none; null, if omitted)
			 */

			String onFlags = matcher.group(group);
			String offFlags = matcher.group(group + 1);

			char ending = match.charAt(match.length() - 1);
			boolean isGroup = ending == ')';

			if (!isGroup) {
				increaseParenthesisDepth(DURING_PREREFACTOR);
			}

			setFlags(onFlags, offFlags);
			String newFlags = replaceFlags(onFlags, offFlags);

			if (newFlags == null) {
				// no change
				return;
			}

			if (newFlags.length() != 0 || !isGroup)
				replaceWith("(?" + newFlags + ending);
			else
				replaceWith("");
		}

		/**
		 * Refactors the flags during the refactoring step
		 */
		private void refactorFlags()
		{
			/*
			 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:" (also
			 * matches a non-capture group - onFlags/offFlags are omitted)
			 * 
			 * group: onFlags (empty string if none) group + 1: offFlags (empty
			 * string if none; null, if omitted)
			 */

			String onFlags = matcher.group(group);
			String offFlags = matcher.group(group + 1);

			char ending = match.charAt(match.length() - 1);
			boolean isGroup = ending == ')';

			if (!isGroup) {
				increaseParenthesisDepth(!DURING_PREREFACTOR);
			}

			setFlags(onFlags, offFlags);
			// StringBuilder newFlags = new StringBuilder(matcher.end()
			// - matcher.start());
			//
			// newFlags.append(onFlags);
			//
			// if (offFlags != null) {
			// // if (offFlags.length() != 0)
			// // - above condition handled in preRefactorFlags()
			// newFlags.append('-').append(offFlags);
			// }

			// if (!supportedSyntax(NONCAPTURE_GROUPS) && !isGroup) {
			// if (newFlags.length() == 0) {
			// // i.e. a non-capture group "(?:RegEx)"
			// // (convert to a capture group)
			// replaceWith(startNonCaptureGroup());
			// } else
			// replaceWith(startNonCaptureGroup() + "(?" + newFlags + ")");
			// }
		}

		/**
		 * Refactors the flags (after the refactoring step)
		 */
		private void afterRefactorFlags()
		{
			String onFlags = matcher.group(group);
			String offFlags = matcher.group(group + 1);

			char ending = match.charAt(match.length() - 1);
			boolean isGroup = ending == ')';

			if (!isGroup) {
				increaseParenthesisDepth(!DURING_PREREFACTOR);
			}

			setFlags(onFlags, offFlags);
		}

		/**
		 * Refactors a capturing group during the pre-refactoring step
		 * 
		 * @param isNamedGroup
		 *            whether the capture group is a named group
		 * @param form
		 *            the capture group's 0-based form
		 */
		private void preRefactorCaptureGroup(boolean isNamedGroup, int form)
		{
			increaseParenthesisDepth(DURING_PREREFACTOR);

			if (!isNamedGroup && has(EXPLICIT_CAPTURE)) {
				replaceWith(startNonCaptureGroup());
				return;
			}

			increaseCurrentGroup(DURING_PREREFACTOR);

			if (isNamedGroup) {
				/*
				 * matches a named capture group
				 * "(?<name>" (form 0)
				 * "(?'name'" (form 1)
				 * "(?P<name>" (form 2)
				 * 
				 * group: everything after first symbol
				 * group + 1: the name
				 * (6 groups)
				 */

				subpatternNameExpected();
				checkForMissingTerminator(">'>", form);

				String groupName = matcher.group(group + 1);
				int occurrence = nextOccurrence(groupName);

				if (occurrence != 1 && !has(DUPLICATE_NAMES)) {
					throw error(DUPLICATE_NAME, matcher.start(group));
				}

				String mappingName = getMappingName(groupName, occurrence);
				addNamedGroup(mappingName, TARGET_UNKNOWN);

				if (!inBranchReset()) {
					namedGroup++;
				} else {
					unnamedGroup++;
				}

				if (has(DOTNET_NUMBERING)) {
					if (!inBranchReset())
						perlGroupMapping().put(currentGroup, mappingName);
					else
						perlGroupMapping().put(currentGroup,
								getMappingName(currentGroup, 0));
				}
			} else {
				unnamedGroup++;

				if (has(DOTNET_NUMBERING)) {
					String mappingName = getMappingName(unnamedGroup, 0);
					perlGroupMapping().put(currentGroup, mappingName);
				}
			}

			if (!has(DOTNET_NUMBERING) || inBranchReset() || !isNamedGroup) {
				int groupIndex = has(DOTNET_NUMBERING) ? unnamedGroup
						: currentGroup;

				String groupName = wrapIndex(groupIndex);
				int occurrence = nextOccurrence(groupName);
				String mappingName = getMappingName(groupIndex, occurrence);

				// add mapping for group index
				addUnnamedGroup(mappingName, TARGET_UNKNOWN);
			}

			// TODO: what is named group in branch reset pattern??
		}

		/**
		 * Checks that the necessary terminating character is present.
		 * 
		 * <p>
		 * <code>Endings</code> is a string where each character is an ending.
		 * The first character (index 0) refers to the ending for form 0, the
		 * second character (index 1) for form 1, etc.
		 * </p>
		 * 
		 * <p>
		 * Forms that have no terminating character must occur after the
		 * forms that do. For example, if form 3 has no ending,
		 * <code>endings</code> would be of length 3, and characters 0 - 2 would
		 * have the endings for forms 0 - 2.
		 * </p>
		 * 
		 * <p>
		 * In this case, if <code>form</code> was equal to three, the below
		 * method returns successfully since there is no missing terminator -
		 * there is no terminator at all, so there is no way that it is missing.
		 * </p>
		 * 
		 * @param endings
		 *            the endings to test
		 * @param form
		 *            the 0-based form
		 * 
		 * @throws PatternSyntaxException
		 *             If the necessary terminator is missing
		 */
		private void checkForMissingTerminator(String endings, int form)
		{
			boolean missingTerminator = form < endings.length()
					&& !matcher.group(group).endsWith(
					endings.substring(form, form + 1));

			if (missingTerminator) {
				throw error(MISSING_TERMINATOR, matcher.end(group));
			}
		}

		/**
		 * Checks that a subpattern name is present.
		 * 
		 * @throws PatternSyntaxException
		 *             If the subpattern name is missing
		 */
		private void subpatternNameExpected()
		{
			// name is blank and [occurrence] is null
			boolean missingName = matcher.start(group + 1) ==
					matcher.end(group + 1) && matcher.start(group + 2) == -1;

			if (missingName) {
				throw error(SUBPATTERN_NAME_EXPECTED, matcher.start(group));
			}
		}

		/**
		 * Refactors a capturing group during the refactoring step
		 * 
		 * @param isNamedGroup
		 *            whether the group is a named group
		 */
		private void refactorCaptureGroup(boolean isNamedGroup)
		{
			increaseParenthesisDepth(!DURING_PREREFACTOR);
			increaseCurrentGroup(!DURING_PREREFACTOR);

			if (isNamedGroup && !inBranchReset()) {
				namedGroup++;
			} else
				unnamedGroup++;

			boolean usedInCondition;
			String namedMappingName;

			if (isNamedGroup) {
				String groupName = matcher.group(group);
				int occurrence = nextOccurrence(groupName);
				namedMappingName = getMappingName(groupName, occurrence);

				// add mapping for group name
				addNamedGroup(namedMappingName, totalGroups);

				usedInCondition = usedInCondition(namedMappingName)
						|| usedInCondition(getMappingName(groupName, 0));
			} else {
				usedInCondition = false;
				namedMappingName = null;
			}

			int groupIndex = getCurrentGroup(isNamedGroup && !inBranchReset());
			String groupName = wrapIndex(groupIndex);
			int occurrence = nextOccurrence(groupName);
			String mappingName = getMappingName(groupName, occurrence);

			// add mapping for group index
			addUnnamedGroup(mappingName, totalGroups);

			if (!usedInCondition) {
				usedInCondition = usedInCondition(mappingName)
						|| usedInCondition(getMappingName(groupName, 0));

				if (!usedInCondition) {
					if (isNamedGroup) {
						// remove name part
						replaceWith("(");
					}

					return;
				}
			}

			// remove name part (if applicable) and convert form
			// "(?<name>RegEx)" -> "(?:(RegEx)())"
			replaceWith(startNonCaptureGroup() + "(");
			increaseParenthesisDepth(!DURING_PREREFACTOR);

			// add a MatchState to track where to add
			// the necessary "()" at the end of the capture group
			addTestGroup.push(new AddTestGroupState(mappingName,
					parenthesisDepth,
					namedMappingName));
		}

		private void afterRefactorCaptureGroup()
		{
			increaseParenthesisDepth(!DURING_PREREFACTOR);
			increaseCurrentGroup(!DURING_PREREFACTOR);
		}

		private int getCurrentGroup(boolean isNamedGroup)
		{
			if (has(DOTNET_NUMBERING)) {
				if (isNamedGroup) {
					return unnamedGroupCount + namedGroup;
				} else {
					return unnamedGroup;
				}
			} else
				return currentGroup;
		}

		/**
		 * Returns whether the specified group is used as a condition
		 * 
		 * @param mappingName
		 *            the mapping name for the group
		 * 
		 * @return <code>true</code> if the specified group is used as a
		 *         condition; <code>false</code> otherwise.
		 */
		private boolean usedInCondition(String mappingName)
		{
			return requiresTestingGroup.contains(mappingName);
		}

		/**
		 * Refactors a conditional pattern during the pre-refactoring step
		 * 
		 * @param isNamedGroup
		 *            whether the conditional is a name or number
		 * @param form
		 *            the 0-based form for the conditional
		 */
		private void preRefactorConditionalPattern(boolean isNamedGroup,
				int form)
		{
			increaseParenthesisDepth(DURING_PREREFACTOR);
			String mappingName;

			if (isNamedGroup) {
				/*
				 * matches a named reference condition
				 * "(?(<name>)" (form 0),
				 * "(?('name')" (form 1), or
				 * "(?(name)" (form 2)
				 * 
				 * group: everything after first symbol (excluding ")")
				 * group + 1: the name
				 * (6 groups)
				 */

				// if nothing after "(?("
				if (matcher.start(group) == matcher.end(group)) {
					throw error(ASSERTION_EXPECTED, matcher.end(group));
				}

				checkForMissingTerminator(">'", form);

				if (!match.endsWith(")")) {
					throw error(UNCLOSED_GROUP, matcher.end(group));
				}

				String groupName = matcher.group(group + 1);

				if (groupName.charAt(0) == '[') {
					String tmpMappingName = getAbsoluteGroup(
							unwrapIndex(groupName), currentGroup);

					if (isAnyGroup(tmpMappingName)) {
						// reference is an unnamed group
						// (any occurrence is possible)
						groupName = anyGroupName(tmpMappingName);

						String groupOccurrence = matcher.group(group + 2);
						mappingName = getGroupIndex(groupName, groupOccurrence);
					} else {
						// reference is a named group
						// (occurrence is ignored)

						mappingName = tmpMappingName;
					}
				} else {
					// named group

					String groupOccurrence = matcher.group(group + 2);
					mappingName = getGroupIndex(groupName, groupOccurrence);
				}
			} else {
				mappingName = getAbsoluteGroup(matcher.group(group),
						currentGroup);
			}

			requiresTestingGroup.add(mappingName);

			// add a MatchState to handle the "else" branch
			handleElseBranch.push(new MatchState("", parenthesisDepth, -1));
		}

		/**
		 * Refactors a conditional pattern during the refactoring step
		 * 
		 * @param isNamedGroup
		 *            whether the condition is a name or number
		 */
		private void refactorConditionalPattern(boolean isNamedGroup)
		{
			increaseParenthesisDepth(!DURING_PREREFACTOR);
			String groupName = normalizeGroupName(isNamedGroup
					? matcher.group(group)
					: "[" + matcher.group(group) + "]");

			// start of groupName / number
			int start = matcher.start(group);

			if (groupName.equals("[0]"))
				throw error(INVALID_CONDITION0, start);

			String groupOccurrence = isNamedGroup ? matcher.group(group + 1)
					: null;

			String mappingName = getGroupIndex(groupName, groupOccurrence);

			Integer mappingIndexI = getMappedIndex(mappingName);
			Integer testConditionGroupI = getTestingGroup(mappingName);

			if (isAnyGroup(mappingName)) {
				int groupCount = groupCount(groupName);

				if (groupCount == 0) {
					// if (has(VERIFY_GROUPS))
					throw error(NONEXISTENT_SUBPATTERN, start);

					// the specified group doesn't exist
					// replaceWith(startNonCaptureGroup() + fail());
				} else if (!allDone(groupName, groupCount)) {

					// some groups occur later on
					replaceWith(startNonCaptureGroup() + "\\g{"
							+ addErrorTrace(start) + "test-" + mappingName
							+ "}");
					testConditionGroupI = TARGET_UNKNOWN;
				} else {
					// all groups have already occurred
					replaceWith(startNonCaptureGroup()
							+ acceptTestingGroup(mappingName));
					// TODO: need to rename condition group
					// testConditionGroupI = BRANCH_RESET;
					testConditionGroupI = 0;
				}

			} else if (mappingIndexI == null) {
				// if (has(VERIFY_GROUPS))
				throw error(NONEXISTENT_SUBPATTERN, start);

				// the specified group doesn't exist
				// replaceWith(startNonCaptureGroup() + fail());
			} else if (testConditionGroupI == null) {
				// the specified group exists, but occurs later
				replaceWith(startNonCaptureGroup() + "\\g{"
						+ addErrorTrace(start) + "test-" + mappingName + "}");
				testConditionGroupI = TARGET_UNKNOWN;
			} else {
				// the specified group has already occurred
				replaceWith(startNonCaptureGroup()
						+ acceptTestingGroup(testConditionGroupI));
			}

			// add a MatchState to handle the "else" branch
			handleElseBranch.push(new MatchState(mappingName, parenthesisDepth,
					testConditionGroupI));
		}

		/**
		 * Refactors a back reference during the pre-refactoring step
		 */
		private void preRefactorBackreference()
		{
			String groupName = matcher.group(group);

			if (!isUnnamedGroup(groupName))
				anyGroupReferences.add(groupName);
		}

		/**
		 * Refactors a back reference during the refactoring step
		 * 
		 * @param isNamedGroup
		 *            whether the back reference is by name or number
		 * @param form
		 *            the form for the back reference
		 * @param digitGroup
		 *            the index for the group which stores the digit (if any)
		 *            that follows the back reference
		 */
		private void refactorBackreference(boolean isNamedGroup, int form,
				int digitGroup)
		{
			String mappingName;
			String trailingDigits = "";

			// start of groupName / number
			int start;

			if (isNamedGroup) {
				/*
				 * matches a back reference (by name)
				 * "\g{name}" (form 0)
				 * "\k<name>" (form 1)
				 * "\k'name'" (form 2)
				 * "\k{name}" (form 3)
				 * "(?P=name)" (form 4)
				 * 
				 * group: everything after first symbol
				 * group + 1: the name
				 * (10 groups)
				 */

				subpatternNameExpected();
				checkForMissingTerminator("}>'})", form);

				start = matcher.start(group + 1);
				String groupName = normalizeGroupName(matcher.group(group + 1));
				String groupOccurrence = matcher.group(group + 2);
				mappingName = getGroupIndex(groupName, groupOccurrence);
			} else {
				/*
				 * matches a back reference (by number)
				 * "\n" (form 0)
				 * "\gn" (form 1)
				 * "\g{n}" or "\g{-n}" (form 2)
				 * 
				 * group: the number
				 * (3 groups)
				 */

				int groupIndex;
				start = matcher.start(group);

				if (form == 0) {
					java.util.regex.MatchResult backreference = getBackreference(
							matcher.group(group), start);

					if (backreference == null) {
						// not a back reference (i.e. an octal code)
						// (handled in above function call)
						return;
					}

					groupIndex = Integer.parseInt(backreference.group(1));

					// TODO: verify functionality with DOTNET_NUMBERING
					if (groupIndex > currentGroup && groupIndex >= 10) {
						trailingDigits = String.valueOf(groupIndex % 10);
						groupIndex /= 10;
					}

					trailingDigits += backreference.group(2);
					String groupName = wrapIndex(groupIndex);
					mappingName = getMappingName(groupName, 0);
				} else {
					mappingName = getAbsoluteGroup(matcher.group(group),
							currentGroup);
				}
			}

			trailingDigits += matcher.group(digitGroup);

			// retrieve the actual group index for the specified groupIndex
			Integer mappedIndexI;

			// replace back reference with back reference RegEx
			if (isAnyGroup(mappingName)) {
				String groupName = anyGroupName(mappingName);

				if (groupName.equals("[0]"))
					throw error(ZERO_REFERENCE, start);

				int groupCount = groupCount(groupName);

				if (groupCount == 0) {
					// form 0 is \n (for unnamed group) 
					if (isNamedGroup || form != 0 || has(VERIFY_GROUPS))
						throw error(NONEXISTENT_SUBPATTERN, start);

					replaceWith(fail() + trailingDigits);
				} else if (groupCount == 1) {
					String tmpMappingName = getMappingName(groupName, 1);
					trailingDigits = fixTrailing(trailingDigits);

					if (getOccurrences(groupName) == groupCount) {
						// group has already occurred
						replaceWith(acceptGroup(tmpMappingName)
								+ trailingDigits);
					} else {
						// group occurs later on
						replaceWith("\\g{" + addErrorTrace(start) + "group-"
								+ tmpMappingName + "}" + trailingDigits);
					}
				} else if (allDone(groupName, groupCount)) {
					// all groups have already occurred
					String acceptGroup = acceptGroup(mappingName);

					replaceWith(nonCaptureGroup(acceptGroup) + trailingDigits);
				} else {
					// some groups occur later on
					replaceWith(nonCaptureGroup("\\g{"
							+ addErrorTrace(start) + "group-"
							+ mappingName + "}") + trailingDigits);
				}
			} else if ((mappedIndexI = getMappedIndex(mappingName)) == null) {
//				if (has(VERIFY_GROUPS))
					throw error(NONEXISTENT_SUBPATTERN, start);

//				replaceWith(fail() + trailingDigits);
			} else {
				int mappedIndex = mappedIndexI;

				trailingDigits = fixTrailing(trailingDigits);

				if (mappedIndex == TARGET_UNKNOWN) {
					// group hasn't occurred yet

					replaceWith("\\g{" + addErrorTrace(start) + "group-"
							+ mappingName + "}" + trailingDigits);
				} else {
					// group already occurred
					replaceWith("\\" + mappedIndex + trailingDigits);
				}
			}
		}

		/**
		 * Indicates whether the specified group name is an unnamed group
		 * 
		 * @param groupName
		 *            the group name
		 * @return <code>true</code> if, and only if, <code>groupName</code> is
		 *         the name of an unnamed group (e.g. [1])
		 */
		private boolean isUnnamedGroup(String groupName)
		{
			return groupName.charAt(0) == '[';
		}

		/**
		 * Indicates whether all groups with the specified group name have
		 * already appeared
		 * 
		 * @param groupName
		 *            the group name
		 * @param groupCount
		 *            the total number of groups with the specified name
		 * @return <code>true</code> if, and only if, all groups with the given
		 *         name have already appeared
		 */
		private boolean allDone(String groupName, int groupCount)
		{
			if (isUnnamedGroup(groupName)) {
				// e.g. [1][0]
				return getOccurrences(groupName) == groupCount;
			} else {
				// e.g. groupName[0]
				return getTestingGroup(getMappingName(groupName, groupCount)) != null;
			}
		}

		/**
		 * Returns a regular expression that matches the given digits literally.
		 * 
		 * @param trailingDigits
		 *            the literal digits that follow a back reference
		 */
		private String fixTrailing(String trailingDigits)
		{
			if (trailingDigits.length() == 0)
				return "";

			return "[" + trailingDigits.charAt(0) + "]"
					+ trailingDigits.substring(1);
		}

		/**
		 * Returns a <code>MatchResult</code> containing data about the back
		 * reference.
		 * 
		 * @param backreference
		 *            a string (of numbers) that make up a back reference
		 * @param start
		 *            the start index for the back reference (used in any thrown
		 *            exceptions)
		 * @return a <code>MatchResult</code> contain data about the back
		 *         reference, or null if the <code>backreference</code> doesn't
		 *         refer to a back reference
		 */
		private java.util.regex.MatchResult getBackreference(
				String backreference, int start)
		{
			if (backreference.charAt(0) == '0') {
				String input = has(PERL_OCTAL) ? backreference
						: backreference.substring(1);

				@SuppressWarnings("hiding")
				java.util.regex.Matcher matcher = perl_octal.matcher(input);

				if (!matcher.matches()) {
					// +1 because leading '0'
					int errorLoc = start + 1;
					throw error(ILLEGAL_OCTAL_ESCAPE, errorLoc);
				}

				String octal = matcher.group(1);
				String trailing = matcher.group(2);

				int octalCode = Integer.parseInt(octal, 8);

				String hexCode = String.format(hexCodeFormat, octalCode);
				replaceWith(hexCode + trailing);

				return null;
			}

			if (inCharClass()) {
				if (has(PERL_OCTAL)) {
					@SuppressWarnings("hiding")
					java.util.regex.Matcher matcher = perl_octal
							.matcher(backreference);

					if (!matcher.matches())
						throw error(ILLEGAL_OCTAL_ESCAPE, start);

					String octal = matcher.group(1);
					String trailing = matcher.group(2);

					int octalCode = Integer.parseInt(octal, 8);

					String hexCode = String.format(hexCodeFormat, octalCode);
					replaceWith(hexCode + trailing);

					return null;
				} else {
					// ignore back reference in character class
					return null;
				}
			}

			int digitCount = digitCount(currentGroup);

			@SuppressWarnings("hiding")
			java.util.regex.Pattern pattern = getDigitCountPattern(digitCount);
			@SuppressWarnings("hiding")
			java.util.regex.Matcher matcher = pattern.matcher(backreference);

			matcher.matches();

			int groupIndex = Integer.parseInt(matcher.group(1));
			String trailing = matcher.group(2);

			if (has(PERL_OCTAL) && (trailing.length() != 0 || digitCount > 1
					&& groupIndex > currentGroup)) {
				// an octal escape

				matcher = perl_octal.matcher(backreference);

				if (!matcher.matches())
					throw error(ILLEGAL_OCTAL_ESCAPE, start);

				String octal = matcher.group(1);
				trailing = matcher.group(2);

				int octalCode = Integer.parseInt(octal, 8);
				String hexCode = String.format(hexCodeFormat, octalCode);
				replaceWith(hexCode + trailing);

				return null;
			}

			return matcher.toMatchResult();
		}

		/**
		 * Refactors an assert condition during the pre-refactoring step
		 */
		private void preRefactorAssertCondition()
		{
			/*
			 * matches an assert condition
			 * "(?(?=)", "(?(?!)", "(?(?<=)", or "(?(?<!)"
			 * group : the assert part (inside the parentheses)
			 * (1 group)
			 */

			increaseParenthesisDepth(DURING_PREREFACTOR);

			// add a MatchState to handle the "else" branch
			handleElseBranch.push(new MatchState("", parenthesisDepth, -1));

			// increase parenthesis depth to that of the assertion
			increaseParenthesisDepth(DURING_PREREFACTOR);
		}

		/**
		 * Refactors an assert condition during the refactoring step
		 */
		private void refactorAssertCondition()
		{
			increaseParenthesisDepth(!DURING_PREREFACTOR);

			// convert form (m is the test condition group for the assertion)
			// "(?(assert)then|else)" ->
			// "(?:(?:(assert)())?+(?:(?:\m)then|(?!\m)else))"

			// (conversion works in PCRE, but only partly in Java)
			// does not work during repetition
			// TODO: verify conversions work in PCRE and Java

			replaceWith(startNonCaptureGroup() + startNonCaptureGroup() + "("
					+ matcher.group(group));

			// increase parenthesis depth to that of the assertion
			increaseParenthesisDepth(!DURING_PREREFACTOR);
			increaseParenthesisDepth(!DURING_PREREFACTOR);

			handleEndAssertCond.add(new MatchState("", parenthesisDepth,
					TARGET_UNKNOWN));
		}

		/**
		 * Returns the mapping name associated with the event to end an assert
		 * condition
		 * 
		 * @return
		 */
		private String endAssertCondMappingName()
		{
			return "$endAssertCond";
		}

		/**
		 * Initializes a "branch reset" subpattern during the pre-refactoring
		 * step.
		 */
		private void preRefactorBranchReset()
		{
			increaseParenthesisDepth(DURING_PREREFACTOR);

			branchReset.push(new BranchResetState(currentGroup, unnamedGroup,
					parenthesisDepth));
		}

		/**
		 * Initializes a "branch reset" subpattern during the refactoring step.
		 */
		private void refactorBranchReset()
		{
			replaceWith(startNonCaptureGroup());
			increaseParenthesisDepth(!DURING_PREREFACTOR);
			branchReset.push(new BranchResetState(currentGroup, unnamedGroup,
					parenthesisDepth));
		}

		private void refactorNumericRange()
		{
			String mode = matcher.group(group);
			// boolean rawMode = matcher.group(group + 1) != null;
			String start = matcher.group(group + 1);
			String end = matcher.group(group + 2);
			int endRange = matcher.end(group + 2);

			if (start == null || end == null) {
				// error is set at character after "["
				throw error(NUMERIC_RANGE_EXPECTED, matcher.end(group) + 1);
			}

			if (endRange >= text.length() || text.charAt(endRange) != ']') {
				throw error(UNCLOSED_RANGE, endRange);
			}

			if (!match.endsWith(")")) {
				// error is set at character after "]"
				throw error(UNCLOSED_GROUP, endRange + 1);
			}

			try {
				String range = Range.range(start, end, mode);

				// if (!rawMode)
				range = nonCaptureGroup(range);

				replaceWith(range);
			} catch (PatternSyntaxException e) {
				String desc = e.getDescription();
				int index = e.getIndex();

				if (desc.equals(INVALID_DIGIT_START)) {
					int startIndex = matcher.start(group + 1);
					throw error(desc, startIndex + index);
				} else if (desc.equals(INVALID_DIGIT_END)) {
					int endIndex = matcher.start(group + 2);
					throw error(desc, endIndex + index);
				}

			} catch (Exception e) {
				String message = e.getMessage();

				if (message.equals(INVALID_BASE)) {
					int errorIndex = matcher.start(group) + mode.indexOf('Z')
							+ 1;
					throw error(message + " in numeric range", errorIndex);
				}
			}
		}

		/**
		 * Refactors the hex unicode during the after-refactoring step
		 */
		private void afterRefactorHexUnicode()
		{
			int hex = 16;
			int codePoint;

			try {
				codePoint = Integer.parseInt(matcher.group(group), hex);

				if (codePoint <= 0xFF)
					replaceWith(String.format(hexCodeFormat, codePoint));
				else {
					// char[] array = Character.toChars(hexCode);
					//					  
					// if (array.length == 1)
					// replaceWith(String.format("\\x{%1$04x}", (int)
					// array[0]));
					// else
					// replaceWith(String.format("\\x{%1$04x}\\x{%2$04x}",
					// (int) array[0], (int) array[1]));

					if (Character.charCount(codePoint) == 1) {
						replaceWith(String.format(unicodeFormat, codePoint));
					} else {
						replaceWith(new String(Character.toChars(codePoint)));
					}
				}
			} catch (Exception e) {
				throw error(INVALID_HEX_CODE, matcher.start());
			}
		}

		/**
		 * Refactors the hex char during the after-refactoring step
		 */
		private void afterRefactorHexChar()
		{
			String hexCode = matcher.group(group);

			if (hexCode.length() == 1) {
				// matched "\xh"

				// add leading 0 (necessary for java syntax)
				replaceWith("\\x0" + hexCode);
			}
		}

		private void afterRefactorUnicode()
		{
			String unicode = matcher.group(group);

			StringBuilder replacement = new StringBuilder();

			replacement.append("\\u");

			for (int i = unicode.length(); i < 4; i++) {
				replacement.append('0');
			}

			replaceWith(replacement.append(unicode).toString());
		}

		private void afterRefactorPosixClass()
		{
			// if (patternSyntax == JAVA) {
			if (!inCharClass())
				throw error(POSIX_OUTSIDE_CLASS, matcher.start());

			boolean negated = matcher.group(group).length() != 0;
			String posixClass = matcher.group(group + 1);

			if (posixClass.equals("word"))
				replaceWith(negated ? "\\W" : "\\w");
			else {
				String value = posixClasses.get(posixClass);

				if (value != null)
					replaceWith("\\" + (negated ? "P" : "p") + "{" + value
							+ "}");
				else
					throw error(UNKNOWN_POSIX_CLASS, matcher.start(group + 1));
			}
			// }
		}

		/**
		 * Refactors a control character during the after-refactoring step
		 */
		private void afterRefactorControlCharacter()
		{
			char controlCharacter = matcher.group(group).charAt(0);
			int offset;

			if (controlCharacter >= 'a' && controlCharacter <= 'z')
				offset = 'a' - 1;
			else
				offset = 'A' - 1;

			replaceWith(String.format(hexCodeFormat, controlCharacter - offset));
		}

		/**
		 * Refactors the remaining parts during the pre-refactoring step
		 */
		private void preRefactorOthers()
		{
			if (match.equals("(")) {
				if (!inCharClass())
					increaseParenthesisDepth(DURING_PREREFACTOR);
			} else if (match.equals(")")) {
				if (!inCharClass())
					preRefactorCloseParenthesis();
			} else if (match.equals("|")) {
				if (!inCharClass())
					preRefactorPike();
			} else if (match.equals("[")) {
				increaseCharClassDepth();
			} else if (match.equals("]")) {
				decreaseCharClassDepth();
			} else if (match.equals("#")) {
				if (has(COMMENTS) && !inCharClass())
					parsePastLine();
			} else if (match.startsWith("\\Q")) {
				// if (!supportedSyntax(QE_QUOTATION)) {
				if (isJava1_5) {
					int start = 2;
					int end = match.length() - (match.endsWith("\\E") ? 2 : 0);
					replaceWith(literal(match.substring(start, end)));
				}
			}
		}

		/**
		 * Refactors the remaining parts during the refactoring step
		 */
		private void refactorOthers()
		{
			if (match.equals("(")) {
				if (!inCharClass())
					increaseParenthesisDepth(!DURING_PREREFACTOR);
			} else if (match.equals(")")) {
				if (!inCharClass())
					refactorCloseParenthesis();
			} else if (match.equals("|")) {
				if (!inCharClass())
					refactorPike();
			} else if (match.equals("[")) {
				increaseCharClassDepth();
			} else if (match.equals("]")) {
				decreaseCharClassDepth();
			} else if (match.equals("#")) {
				if (has(COMMENTS) && !inCharClass())
					parsePastLine();
			} else if (match.equals("\\Q")) {
				skipQuoteBlock();
			}
		}

		/**
		 * Refactors the remaining parts (after the refactoring step)
		 */
		private void afterRefactorOthers()
		{
			if (match.equals("(")) {
				if (!inCharClass())
					increaseParenthesisDepth(!DURING_PREREFACTOR);
			} else if (match.equals(")")) {
				if (!inCharClass())
					afterRefactorCloseParenthesis();
			} else if (match.equals("[")) {
				increaseCharClassDepth();
			} else if (match.equals("]")) {
				decreaseCharClassDepth();
			} else if (match.equals("#")) {
				if (has(COMMENTS) && !inCharClass())
					parsePastLine();
			} else if (match.equals("\\Q")) {
				skipQuoteBlock();
			} else if (match.equals("\\X")) {
				replaceWith("(?>\\P{M}\\p{M}*)");
			}
		}

		/**
		 * Skips from <code>\Q</code> to <code>\E</code>. If there is no
		 * <code>\E</code>
		 * the rest of the string is skipped.
		 */
		private void skipQuoteBlock()
		{
			int endQuote = text.indexOf("\\E", matcher.end());

			if (endQuote != -1) {
				setMatcherPosition(endQuote);
			} else {
				setMatcherPosition(text.length());
			}
		}

		/**
		 * Returns a literal pattern <code>String</code> for the specified
		 * <code>String</code>.
		 * 
		 * <p>This method produces a <code>String</code> that can be used to
		 * create
		 * a <code>Pattern</code> that would match the string <code>s</code> as
		 * if
		 * it were a literal pattern.</p>
		 * 
		 * <p>Metacharacters or escape sequences in the input sequence
		 * will be
		 * given
		 * no special meaning.</p>
		 * 
		 * <p><b>Note</b>: this function escapes each metacharacter
		 * individually,
		 * whereas {@link #quote(String)} uses a <code>\Q..\E</code> block. This
		 * function is used when refactoring a <code>\Q..\E</code> block into a
		 * RegEx patternSyntax that doesn't support the functionality.</p>
		 * 
		 * @param s
		 *            The string to be literalized
		 * @return A literal string replacement
		 */
		public String literal(String s)
		{
			// if (supportedSyntax(QE_QUOTATION))
			// return quote(s);
			//		
			@SuppressWarnings("hiding")
			java.util.regex.Pattern pattern = inCharClass() ?
					escapeClassMetachars
					: escapeMetachars;

			return pattern.matcher(s).replaceAll("\\\\$0");
		}

		/**
		 * Refactors a close parenthesis during the pre-refactoring step
		 */
		private void preRefactorCloseParenthesis()
		{
			if (parenthesisDepth == 0)
				throw error(UNMATCHED_PARENTHESES, matcher.start());

			if (atRightDepth(handleElseBranch))
				handleElseBranch.pop();
			else if (atRightDepth(branchReset))
				endBranchReset();

			decreaseParenthesisDepth(DURING_PREREFACTOR);
		}

		/**
		 * Refactors a close parenthesis during the refactoring step
		 */
		private void refactorCloseParenthesis()
		{
			if (atRightDepth(addTestGroup)) {
				decreaseParenthesisDepth(!DURING_PREREFACTOR);
				replaceWith(")())");

				totalGroups++;

				String mappingName = addTestGroup.peek().mappingName;
				String namedMappingName = addTestGroup.peek().namedMappingName;

				// add a mapping from mapping name to its test condition group
				addTestingGroup(mappingName, totalGroups);

				if (namedMappingName != null) {
					addTestingGroup(namedMappingName, totalGroups);
				}

				addTestGroup.pop();

				// done last because inside pars is same depth
				decreaseParenthesisDepth(!DURING_PREREFACTOR);
			} else if (atRightDepth(handleElseBranch)) {
				// no else branch for condition (i.e. only a "then" branch)
				// e.g. (?('name')...)

				// add an empty else branch
				// if condition isn't true, matches the empty string

				Integer testConditionGroupI = handleElseBranch.peek().testConditionGroupI;

				if (testConditionGroupI == null) {
					// the specified group doesn't exist,
					// always use else branch
					replaceWith("|)");
				} else {
					int testConditionGroup = testConditionGroupI;

					if (testConditionGroup == TARGET_UNKNOWN) {
						String mappingName = handleElseBranch.peek().mappingName;

						// the specified group exists, but occurs later
						replaceWith("|\\g{"
								+ addErrorTrace(matcher.start(group))
								+ "testF-" + mappingName + "})");
						// } else if (testConditionGroup == BRANCH_RESET) {
						// String mappingName =
						// handleElseBranch.peek().mappingName;
						//
						// // all groups have already occurred
						// replaceWith("|" + failTestingGroup(mappingName) +
						// ")");
					} else {
						String mappingName = handleElseBranch.peek().mappingName;

						StringBuilder replacement = new StringBuilder();

						replacement.append('|');

						if (testConditionGroup == 0) {
							replacement.append(failTestingGroup(mappingName));
						} else {
							replacement
									.append(failTestingGroup(testConditionGroup));
						}

						replacement.append(')');

						if (mappingName.equals(endAssertCondMappingName())) {
							replacement.append(')');
							decreaseParenthesisDepth(!DURING_PREREFACTOR);
						}

						// the specified group has already occurred
						replaceWith(replacement.toString());
					}
				}

				// remove this pike state
				handleElseBranch.pop();

				// done last because inside pars is same depth
				decreaseParenthesisDepth(!DURING_PREREFACTOR);
			} else if (atRightDepth(branchReset)) {
				endBranchReset();

				// done last because inside pars is same depth
				decreaseParenthesisDepth(!DURING_PREREFACTOR);
			} else if (atRightDepth(handleEndAssertCond)) {
				String mappingName = handleEndAssertCond.peek().mappingName;

				if (mappingName.equals(endAssertCondMappingName())) {
					// the end of an assert condition

					replaceWith("))");

					// adjust parenthesis depth
					decreaseParenthesisDepth(!DURING_PREREFACTOR);
					decreaseParenthesisDepth(!DURING_PREREFACTOR);

					// remove the state
					handleEndAssertCond.pop();
				} else {
					totalGroups++;

					replaceWith(")())?+" + startNonCaptureGroup()
							+ acceptTestingGroup(totalGroups));

					// adjust parenthesis depth
					decreaseParenthesisDepth(!DURING_PREREFACTOR);
					decreaseParenthesisDepth(!DURING_PREREFACTOR);
					increaseParenthesisDepth(!DURING_PREREFACTOR);

					// add state to handle else branch
					handleElseBranch.add(new MatchState(
							endAssertCondMappingName(), parenthesisDepth,
							totalGroups));

					// remove the state
					handleEndAssertCond.pop();
				}
			} else {
				// done last because inside pars is same depth
				decreaseParenthesisDepth(!DURING_PREREFACTOR);
			}
		}

		/**
		 * Refactors a close parenthesis during the after-refactoring step
		 */
		private void afterRefactorCloseParenthesis()
		{
			decreaseParenthesisDepth(!DURING_PREREFACTOR);
		}

		/**
		 * Steps to perform at the end (closing parenthesis) of a "branch reset"
		 * subpattern.
		 */
		private void endBranchReset()
		{
			int endGroup = branchReset.peek().endGroup;
			int endUnnamedGroup = branchReset.peek().endUnnamedGroup;

			if (endGroup > currentGroup)
				currentGroup = endGroup;

			if (endUnnamedGroup > unnamedGroup)
				unnamedGroup = endUnnamedGroup;

			branchReset.pop();
		}

		/**
		 * Refactors a pike during the pre-refactoring step
		 */
		private void preRefactorPike()
		{
			if (atRightDepth(handleElseBranch)) {
				if (handleElseBranch.peek().testConditionGroupI == parenthesisDepth) {
					throw error(CONDITIONAL_BRANCHES, matcher.start(group));
				}

				handleElseBranch.peek().testConditionGroupI = parenthesisDepth;
			} else if (atRightDepth(branchReset))
				branchReset();
		}

		/**
		 * Refactors a pike during the factoring step
		 */
		private void refactorPike()
		{
			if (atRightDepth(handleElseBranch)) {
				Integer testConditionGroupI = handleElseBranch.peek().testConditionGroupI;
				String mappingName = handleElseBranch.peek().mappingName;

				if (testConditionGroupI != null) {
					int testConditionGroup = testConditionGroupI;

					if (testConditionGroup == TARGET_UNKNOWN) {
						// the specified group exists, but occurs later
						replaceWith("|\\g{"
								+ addErrorTrace(matcher.start(group))
								+ "testF-" + mappingName + "}");
						// } else if (testConditionGroup == BRANCH_RESET) {
						// // all groups have already occurred
						// replaceWith("|" + failTestingGroup(mappingName));
					} else if (testConditionGroup != 0) {
						// specific group

						replaceWith("|" + failTestingGroup(testConditionGroup));
					} else {
						// any group
						replaceWith("|" + failTestingGroup(mappingName));
					}
				}
				// else, the specified group doesn't exist
				// (i.e. always use else branch) - dealt with elsewhere

				handleElseBranch.pop();

				if (mappingName.equals(endAssertCondMappingName()))
					handleEndAssertCond.add(new MatchState(mappingName,
							parenthesisDepth, testConditionGroupI));
			} else if (atRightDepth(branchReset)) {
				branchReset();
			}
		}

		/**
		 * Actions to take when matching the start of a character class
		 */
		private void increaseCharClassDepth()
		{
			charClassDepth++;

			int end = matcher.end();
			int length = text.length();
			boolean squareBracket = end < length && text.charAt(end) == ']';

			boolean negSequareBracket = end < length - 2
					&& text.substring(end, end + 2).equals("^]");

			if (squareBracket || negSequareBracket) {
				// a "]" follows the "["

				// don't count the "]" as the end of the character class
				// increase the char class depth,
				// (it will be decreased upon hitting the "]")
				charClassDepth++;
			}
		}

		/**
		 * Actions to take when matching the end of a character class
		 */
		private void decreaseCharClassDepth()
		{
			// only decrease depth if actually in a character class
			// otherwise, treat the "]" as literal

			if (inCharClass())
				charClassDepth--;
		}

		private boolean inCharClass()
		{
			return charClassDepth != 0;
		}

		/**
		 * Sets the "cursor" in the matcher to the given position
		 * 
		 * @param position
		 *            index to start the next call to {@link Matcher#find()}
		 */
		private void setMatcherPosition(int position)
		{
			// store the current pattern (restored at end)
			java.util.regex.Pattern tmp = matcher.pattern();

			int end = matcher.end();

			// replace the match with itself - calls matcher.appendReplacement
			replaceWith(matcher.group());

			// append the text from the end to the current position
			result.append(text.substring(end, position));

			matcher.usePattern(EMPTY_PATTERN);

			// move the cursor to the given position
			matcher.find(position);

			/*
			 * set the last append position to <position>
			 * (don't store the text
			 * - since its from the beginning of the string to <position>
			 */
			matcher.appendReplacement(new StringBuffer(), "");

			// restore the used pattern
			matcher.usePattern(tmp);
		}

		private void parsePastLine()
		{
			int i = matcher.end();

			while (i < text.length() && !isLineSeparator(text.charAt(i))) {
				i++;
			}

			setMatcherPosition(i);
		}

		/**
		 * Determines if character is a line separator in the current mode
		 * 
		 * @return
		 */
		private boolean isLineSeparator(int ch)
		{
			if (has(UNIX_LINES)) {
				return ch == '\n';
			} else {
				return (ch == '\n' || ch == '\r' || (ch | 1) == '\u2029' || ch == '\u0085');
			}
		}

		/**
		 * Steps taken when entering a new branch in a "branch reset"
		 * subpattern.
		 */
		private void branchReset()
		{
			branchReset.peek().updateEndGroup(currentGroup, unnamedGroup);
			currentGroup = branchReset.peek().startGroup;
			unnamedGroup = branchReset.peek().unnamedGroup;
		}

		/**
		 * Indicates whether currently in a "branch reset" pattern
		 * 
		 * @return <code>true</code> if, and only if, currently in a
		 *         "branch reset" pattern
		 */
		private boolean inBranchReset()
		{
			return branchReset.size() != 0;
		}

		/**
		 * <p>Replace the matched string with the specified (literal)
		 * replacement, and adds a new state to {@link #differences}.
		 * </p>
		 */
		private java.util.regex.Matcher replaceWith(String replacement)
		{
			String quoteReplacement = Matcher.quoteReplacement(replacement);

			matcher.appendReplacement(result, quoteReplacement);

			// int length = matcher.end() - matcher.start();
			int start = result.length() - quoteReplacement.length();
			// int end = start + length;

			differences.replace0(start, match, replacement);

			return matcher;
		}

		/**
		 * <p>Appends <tt>result</tt> with the specified (literal) string, and
		 * adds a new state to {@link #differences}.</p>
		 */
		/*
		 * private StringBuffer appendWith(String str)
		 * {
		 * differences.insert(result.length(), str);
		 * result.append(str);
		 * 
		 * return result;
		 * }
		 */

		/**
		 * Normalizes the group name.
		 */
		String normalizeGroupName(String groupName)
		{
			if (groupName.startsWith("[0")) {
				// group name is number with leading zeros
				int groupIndex = Integer.parseInt(groupName.substring(1,
						groupName.length() - 1));

				return wrapIndex(groupIndex);
			} else if (groupName.startsWith("[-")) {
				// relative reference

				int groupIndex = Integer.parseInt(groupName.substring(1,
						groupName.length() - 1));

				// groupIndex is relative
				// e.g. -1 with currentGroup == 5 -> 5
				groupIndex += currentGroup + 1;

				if (groupIndex <= 0)
					return neverUsedMappingName();

				if (has(DOTNET_NUMBERING)) {
					String tmpGroupName = getPerlGroup(groupIndex);

					if (tmpGroupName.charAt(0) == '[') {
						return anyGroupName(tmpGroupName);
					}
				} else
					return wrapIndex(groupIndex);
			}

			return groupName;
		}
	}

	/**
	 * Returns the actual group number (in the internal pattern) for the given
	 * mapping name.
	 */
	Integer getMappedIndex(String mappingName)
	{
		return groupMapping.get(mappingName);
	}

	/**
	 * Returns the (internally) used string for mapping a group and occurrence
	 * (in the original pattern) to its group index (in the refactored pattern).
	 * 
	 * @param groupName
	 *            the group name
	 * @param occurrence
	 *            the occurrence
	 * @return groupName "[" + occurrence + "]"
	 */
	static String getMappingName(String groupName, int occurrence)
	{
		return groupName + "[" + occurrence + "]";
	}

	/**
	 * Returns the (internally) used string for mapping a group and occurrence
	 * (in the original pattern) to its group index (in the refactored pattern).
	 * 
	 * @param groupName
	 *            the group name
	 * @param occurrence
	 *            the occurrence
	 * @return groupName "[" + occurrence + "]"
	 */
	static String getMappingName(int groupIndex, int occurrence)
	{
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
	static String wrapIndex(int groupIndex)
	{
		return "[" + groupIndex + "]";
	}

	/**
	 * Returns the unwrapped group index.
	 * 
	 * <p>If <code>groupIndex</code> is surrounded by square brackets, they are
	 * removed,
	 * and the group index is returned. Otherwise, the given group index is
	 * returned unmodified.</p>
	 */
	static String unwrapIndex(String groupIndex)
	{
		if (groupIndex.charAt(0) == '['
				&& groupIndex.charAt(groupIndex.length() - 1) == ']') {
			return groupIndex.substring(1, groupIndex.length() - 1);
		}

		return groupIndex;
	}

	/**
	 * Returns the group index for the first non-null match (starting with group
	 * 1) in the specified matcher.
	 */
	static int getUsedGroup(java.util.regex.Matcher matcher)
	{
		for (int i = 1; i <= matcher.groupCount(); i++) {
			if ((matcher.start(i)) != -1) {
				return i;
			}
		}

		return 0;
	}

	/**
	 * Returns the given group name, adjusting the case based on
	 * the {@link #CASE_INSENSITIVE_NAMES} flag.
	 * 
	 * @param groupName
	 *            the group name
	 * @return the group name, adjusting the case based on the
	 *         {@link #CASE_INSENSITIVE_NAMES} flag
	 */
	// String handleCase(String groupName)
	// {
	// return hasCaseInsensitiveGroupNames()
	// ? groupName.toLowerCase(Locale.ENGLISH) : groupName;
	// }

	/**
	 * TODO: add javadoc comments.
	 */
	private static class MatchState implements State
	{

		/** The mapping name. */
		String mappingName;

		/** The parenthesis depth. */
		int parenthesisDepth;

		/** The test condition group i. */
		Integer testConditionGroupI;

		/**
		 * @param mappingName
		 *            the mapping name for the group
		 * @param parenthesisDepth
		 *            the parenthesis depth for the group
		 * @param testConditionGroup
		 *            the test condition group
		 */
		MatchState(String mappingName, int parenthesisDepth,
				Integer testConditionGroup)
		{
			this.mappingName = mappingName;
			this.parenthesisDepth = parenthesisDepth;
			this.testConditionGroupI = testConditionGroup;
		}

		/**
		 * {@inheritDoc}
		 */
		public int getParenthesisDepth()
		{
			return this.parenthesisDepth;
		}

		/**
		 * Returns a string useful for debugging
		 * 
		 * @return a string representation of this state
		 */
		@Override
		public String toString()
		{
			return mappingName + " -> (" + testConditionGroupI + "): "
					+ parenthesisDepth;
		}
	}

	/**
	 * The Class BranchResetState.
	 */
	private static class BranchResetState implements State
	{

		/** The start group. */
		int startGroup;

		/** Number of unnamed groups */
		int unnamedGroup;

		/** The parenthesis depth. */
		int parenthesisDepth;

		/** The end group. */
		int endGroup = -1;

		/** The end unnamed groups */
		int endUnnamedGroup = -1;

		/**
		 * @param startGroup
		 *            the start group for the "branch reset" subpattern
		 * @param namedGroups
		 *            the current number of unnamed groups
		 * @param parenthesisDepth
		 *            the parenthesis depth for the subpattern
		 */
		BranchResetState(int startGroup, int unnamedGroups, int parenthesisDepth)
		{
			this.startGroup = startGroup;
			this.unnamedGroup = unnamedGroups;
			this.parenthesisDepth = parenthesisDepth;
		}

		/**
		 * {@inheritDoc}
		 */
		public int getParenthesisDepth()
		{
			return this.parenthesisDepth;
		}

		/**
		 * Updates the end group of a "branch reset" subpattern
		 * 
		 * <p>The end group is only updated if the specified group is greater
		 * than the current end group.</p>
		 * 
		 * @param group
		 *            the new end group
		 */
		void updateEndGroup(int group, int unnamed)
		{
			if (group > endGroup)
				endGroup = group;

			if (unnamed > endUnnamedGroup)
				endUnnamedGroup = unnamed;
		}

		/**
		 * Returns a string useful for debugging
		 * 
		 * @return a string representation of this state
		 */
		@Override
		public String toString()
		{
			// TODO Auto-generated method stub
			return super.toString();
		}
	}

	private static class AddTestGroupState implements State
	{
		String mappingName;
		String namedMappingName;
		int parenthesisDepth;

		/**
		 * @param mappingName
		 * @param namedMappingName
		 * @param parenthesisDepth
		 */
		AddTestGroupState(String mappingName, int parenthesisDepth,
				String namedMappingName)
		{
			this.mappingName = mappingName;
			this.namedMappingName = namedMappingName;
			this.parenthesisDepth = parenthesisDepth;
		}

		public int getParenthesisDepth()
		{
			return parenthesisDepth;
		}
	}

	/**
	 * The Interface State.
	 */
	private static interface State
	{
		/**
		 * Gets the parenthesis depth.
		 * 
		 * @return the parenthesis depth
		 */
		public int getParenthesisDepth();
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
	 * @throws IllegalArgumentException
	 *             If <i>mode</i> is not in the correct form, as described above
	 */
	public static String range(int start, int end, String mode)
	{
		java.util.regex.Matcher rangeMode = Range.rangeModeRegEx.matcher(mode);

		if (!rangeMode.matches())
			throw new IllegalArgumentException("Illegal range mode");

		int base = rangeMode.group(2) == null ? 10 : Integer
				.parseInt(rangeMode.group(2));

		return "(?:"
				+ Range.range(Integer.toString(start, base), Integer.toString(
				end,
				base), mode) + ")";
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
	public static String range(String start, String end, String mode)
	{
		if (start == null)
			throw new NullPointerException("Start value cannot be null");

		if (start.length() == 0)
			throw new IllegalArgumentException(
					"Start value cannot be the empty string");

		if (end == null)
			throw new NullPointerException("End value cannot be null");

		if (end.length() == 0)
			throw new IllegalArgumentException(
					"End value cannot be the empty string");

		return "(?:" + Range.range(start, end, mode) + ")";
	}

	/**
	 * Functions used to match a range of values e.g. 1 or 001..999; 001..999;
	 * 1..999
	 */
	private static class Range
	{
		/**
		 * Pattern used to match a range mode.
		 * 
		 * <p><b>Format</b>: Mode[Base[BaseMode]]</p>
		 * 
		 * <p>Descriptions and valid values:</p>
		 * <ul>
		 * <li><b>Mode</b>: either "Z" (allows leading zeros) or "NZ" (no
		 * leading
		 * zeros)</li>
		 * 
		 * <li><b>Base</b>: the numeric base for start and end (valid bases, 2 -
		 * 36)</li>
		 * 
		 * <li><b>BaseMode</b>: whether to use lower ("L"), upper ("U"), or both
		 * upper and lower (omitted) case digits when specifying digits A-Z for
		 * digit values 10-36.
		 * 
		 * <p>If the result doesn't include "letter digits" or if the base is
		 * ten or less,
		 * <i>BaseMode</i> has no effect, but can be specified (for
		 * consistency).</p></li>
		 * </ul>
		 */
		// private static final java.util.regex.Pattern rangeModeRegEx =
		// java.util.regex.Pattern
		// .compile("^(N?Z)r?(?:(\\d++)([LU])?)?$");
		static final java.util.regex.Pattern rangeModeRegEx = java.util.regex.Pattern
				.compile("^(N?Z)(?:(\\d++)([LU])?)?$");

		/**
		 * 
		 * @param start
		 * @param end
		 * @param mode
		 * @return
		 * 
		 * @throws IllegalArgumentException
		 *             If the mode is invalid
		 */
		static String range(String start, String end, String mode)
		{
			java.util.regex.Matcher rangeMode = rangeModeRegEx.matcher(mode);

			if (!rangeMode.matches())
				throw new IllegalArgumentException("Illegal range mode");

			start = start.toLowerCase(Locale.ENGLISH);
			end = end.toLowerCase(Locale.ENGLISH);

			boolean allowLeadingZeros = rangeMode.group(1).equals("Z");
			int base = rangeMode.group(2) == null ? 10 : Integer
					.parseInt(rangeMode.group(2));

			if (base < Character.MIN_RADIX || base > Character.MAX_RADIX)
				throw new IllegalArgumentException(INVALID_BASE);

			java.util.regex.Pattern validDigits = java.util.regex.Pattern
					.compile("^-?" + digitRange(0, base - 1, "UL") + "*+");

			java.util.regex.Matcher validStart = validDigits.matcher(start);
			validStart.find();

			if (!validStart.hitEnd()) {
				throw new PatternSyntaxException(INVALID_DIGIT_START, start,
						validStart.end());
			}

			validStart.reset(end);
			validStart.find();

			if (!validStart.hitEnd()) {
				throw new PatternSyntaxException(INVALID_DIGIT_END, end,
						validStart.end());
			}

			// String baseMode = rangeMode.group(3) == null ? "LU" : rangeMode
			// .group(3).toUpperCase(Locale.ENGLISH);
			String baseMode = rangeMode.group(3) == null ? "LU" : rangeMode
					.group(3);

			if (compare(start, end) > 0) {
				// swap values

				String tmpStr = start;
				start = end;
				end = tmpStr;
			}

			boolean negStart = start.charAt(0) == '-';
			boolean negEnd = end.charAt(0) == '-';

			String negS = "", negE = "", posS = "", posE = "";

			if (negStart) {
				if (negEnd) {
					// both start and end are negative
					// e.g. -170 to -16 -> 16 to 170 (with a leading negative
					// sign)
					negS = end.substring(1);
					negE = start.substring(1);
				} else {
					// start is negative and end is non-negative
					negS = compare(start, "0") == 0 ? "0" : "1";
					negE = start.substring(1);

					posS = "0";
					posE = end;
				}
			} else {
				// both are non-negative

				if (negEnd) {
					// special case
					// start = 0 and end = -0
					negS = "0";
					negE = "0";

					posS = "0";
					posE = "0";
				} else {
					posS = start;
					posE = end;
				}
			}

			StringBuilder result = new StringBuilder();

			if (negS.length() != 0) {
				result.append(range_private("-", negS, negE,
						allowLeadingZeros, base, baseMode));
			}

			if (posS.length() != 0) {
				if (result.length() != 0)
					result.append('|');

				result.append(range_private("", posS, posE, allowLeadingZeros,
						base, baseMode));
			}

			return result.toString();
		}

		/*
		 * TODO modify javadoc comments
		 */

		/**
		 * <p>
		 * Returns a regular expression that will match a number in the
		 * specified range.
		 * </p>
		 * 
		 * <p>
		 * If <code>allowLeadingZeros</code> is <code>true</code>, the number of
		 * digits in a accepted match is between the number of digits in
		 * <code>startStr</code> and <code>endStr</code> and leading zeros are
		 * allowed.
		 * </p>
		 * 
		 * <p>
		 * If <code>allowLeadingZeros</code> is <code>false</code>, the returned
		 * regular expression will not allow leading zeros in an accepted match.
		 * </p>
		 * 
		 * <p>
		 * This internal method has the following constraints:
		 * </p>
		 * <ol>
		 * <li>startStr and endStr must contain a positive number</li>
		 * <li>startStr &lt;= endStr</li>
		 * </ol>
		 * 
		 * <b>Note</b>: there is no constraint on how large
		 * <code>startStr</code> and <code>endStr</code> may be. In other words,
		 * they can be any positive integer.
		 */
		private static String range_private(String lead, String startStr,
				String endStr, boolean allowLeadingZeros, int base,
				String baseMode)
		{
			if (allowLeadingZeros)
				return rangeZ_private(lead, startStr, endStr, base, baseMode);

			StringBuilder result = new StringBuilder();

			// remove leading zeros
			String start = removeLeadingZeros(startStr);
			String end = removeLeadingZeros(endStr);

			if (start.length() == end.length())
				return rangeZ_private(lead, start, end, base, baseMode);

			for (int i = start.length(); i <= end.length(); i++) {
				String tmpStart, tmpEnd;

				if (i == start.length()) {
					tmpStart = start;
					tmpEnd = repeat(Character.forDigit(base - 1, base), i);
				} else if (i == end.length()) {
					tmpStart = "1" + repeat('0', i - 1);
					tmpEnd = end;
				} else {
					tmpStart = "1" + repeat('0', i - 1);
					tmpEnd = repeat(Character.forDigit(base - 1, base), i);
				}

				if (result.length() != 0)
					result.insert(0, '|');

				result.insert(0, rangeZ_private(lead, tmpStart, tmpEnd, base,
						baseMode));
			}

			return result.toString();
		}

		private static String rangeZ_private(String lead, String start,
				String end, int base, String baseMode)
		{
			// TODO: dive into test cases for each branch
			if (start.length() == 1 && end.length() == 1) {
				int digit1 = Character.digit(start.charAt(0), base);
				int digit2 = Character.digit(end.charAt(0), base);

				if (digit1 == digit2)
					return lead + start;
				else
					return lead + digitRange(digit1, digit2, baseMode);
			} else {
				boolean optZero;
				int digit1, digit2;
				String newStart, newEnd;

				if (start.length() < end.length()) {
					// optional zero (e.g. 1-17)

					digit1 = 0;
					digit2 = Character.digit(end.charAt(0), base);

					newStart = start;
					newEnd = end.substring(1);

					optZero = true;
				} else if (end.length() < start.length()) {
					// optional zero (e.g. 01-7)

					digit1 = Character.digit(start.charAt(0), base);
					digit2 = 0;

					newStart = start.substring(1);
					newEnd = end;

					optZero = true;
				} else {
					digit1 = Character.digit(start.charAt(0), base);
					digit2 = Character.digit(end.charAt(0), base);

					newStart = start.substring(1);
					newEnd = end.substring(1);

					optZero = false;
				}

				if (digit1 == digit2) {
					String newLead = lead + forDigit(digit1, baseMode)
							+ (optZero ? "?" : "");

					return rangeZ_private(newLead, newStart, newEnd, base,
							baseMode);
				}

				StringBuilder result = new StringBuilder();

				if (!newStart.equals(repeat('0', newStart.length()))) {
					String newLead = lead + forDigit(digit1, baseMode);

					if (optZero && digit1 == 0)
						newLead += "?";

					result.insert(0, rangeZ_private(newLead, newStart,
							repeat(
							Character.forDigit(base - 1, base), newEnd
							.length()), base, baseMode));

					digit1++;
				}

				boolean needSecondGroup;

				if (!newEnd.equals(repeat(Character.forDigit(base - 1, base),
						newEnd.length()))) {
					needSecondGroup = true;
					digit2--;
				} else
					needSecondGroup = false;

				if (digit1 <= digit2) {
					if (result.length() != 0) {
						result.insert(0, '|');
					}

					String newLead = lead;

					if (digit1 == digit2)
						newLead += forDigit(digit1, baseMode);
					else
						newLead += digitRange(digit1, digit2, baseMode);

					int useLength;

					if (optZero && digit1 == 0) {
						newLead += "?";
						useLength = newStart.length();
					} else
						useLength = newEnd.length();

					// result.insert(0, rangeZ_private(newLead, repeat("0",
					// useLength), repeat(Character.digit(base-1, base),
					// newEnd.length())));

					/*
					 * refactored to decrease pattern length (uses "{M, N}"
					 * patternSyntax)
					 * 
					 * result.insert(0, rangeZ_private(newLead, repeat("0",
					 * useLength), repeat(Character.digit(base-1, base),
					 * newEnd.length())));
					 */

					int minNum = Math.min(useLength, newEnd.length());
					int maxNum = Math.max(useLength, newEnd.length());

					if (digit1 == 0 && digit2 == base - 1) {
						newLead = "";
						maxNum++;

						if (!optZero)
							minNum++;
					}

					if (minNum == maxNum) {
						if (minNum == 1) {
							// range(newLead, "0", "9") -> newLead + "[0-9]"
							result.insert(0, newLead
									+ digitRange(0, base - 1, baseMode));
						} else {
							// e.g. useLength == 2
							// range(newLead, "00", "99") -> newLead +
							// "[0-9]{2}"
							result.insert(0, newLead
									+ digitRange(0, base - 1, baseMode) + "{"
									+ minNum + "}");
						}
					} else {
						result.insert(0, newLead
								+ digitRange(0, base - 1, baseMode) + "{"
								+ minNum + ","
								+ maxNum + "}");
					}
				}

				if (needSecondGroup) {
					if (result.length() != 0) {
						result.insert(0, '|');
					}

					String newLead = lead + forDigit(digit2 + 1, baseMode);

					if (optZero && digit2 + 1 == 0)
						newLead += "?";

					result.insert(0, rangeZ_private(newLead, repeat('0',
							newEnd.length()), newEnd, base, baseMode));
				}

				return result.toString();
			}
		}

		private static int compare(String value1, String value2)
		{
			value1 = simplify(value1);
			value2 = simplify(value2);

			int sign = sign_fin(value1);

			if (sign != sign_fin(value2)) {
				return sign;
			}

			if (value1.length() != value2.length()) {
				// e.g. 321 < 1234
				// e.g. -321 > -1234

				return (value1.length() - value2.length()) * sign;
			} else // value1.length() == value2.length()
			{
				// e.g. 123 < 234
				// e.g. -123 > -234

				return value1.compareTo(value2) * sign;
			}
		}

		/**
		 * Checks if the given number is negative or position.
		 * 
		 * @param number
		 *            the (string) number to check
		 * 
		 * @return -1 if the number is negative (has a '-' as the first
		 *         character); 1 otherwise.
		 */
		private static int sign_fin(String number)
		{
			return number.charAt(0) == '-' ? -1 : 1;
		}

		/**
		 * Simplifies the sign, and removes leading zeros.
		 * 
		 * <p>Simplifies leading
		 * "+" and "-" sign(s) to a single "-" sign if negative, and no sign, if
		 * positive. Removes any leading zeros in the number.</p>
		 * 
		 * @param number
		 *            the (string) number to simplify
		 * @return the inputted number, simplifying the sign and removing
		 *         leading zeros.
		 */
		private static String simplify(String number)
		{
			int sign = 1, i = 0;

			// simplify leading "+" and "-"
			while (number.charAt(i) == '-' || number.charAt(i) == '+') {
				if (number.charAt(i) == '-') {
					sign *= -1;
				}

				i++;
			}

			// remove leading zeros (won't remove last zero, in case number = 0)
			while (i + 1 < number.length() && number.charAt(i) == '0') {
				i++;
			}

			String simplifiedNumber = number.substring(i);

			if (simplifiedNumber.equals("0")) {
				return "0";
			}

			return (sign == -1 ? "-" : "") + simplifiedNumber;
		}

		/**
		 * Removes leading zeros from the given (string) number.
		 * 
		 * <p><b>Implementation note</b>: this method also simplifies the
		 * leading "+" and "-" signs.</p>
		 * 
		 * @param number
		 *            the number whose leading zeros are removed
		 * 
		 * @return the given number with leading zeros removed
		 */
		private static String removeLeadingZeros(String number)
		{
			return simplify(number);
		}

		private static String forDigit(int digit, String baseMode)
		{
			StringBuilder forDigit = new StringBuilder();
			boolean containsL = baseMode.contains("L");

			if (containsL)
				forDigit.append(Character
						.forDigit(digit, Character.MAX_RADIX));

			if (baseMode.contains("U") && digit >= 10 || !containsL) {
				forDigit.append((char) (Character.forDigit(digit,
						Character.MAX_RADIX) - (digit >= 10 ? ' ' : 0)));
			}

			return forDigit.length() == 1 ? forDigit.toString() : "["
					+ forDigit + "]";
		}

		/**
		 * Return a regular expression that matches the given range of digits.
		 * 
		 * @param start
		 *            the start digit
		 * @param end
		 *            the end digit
		 * @param baseMode
		 *            the L/U switch
		 * @return a regular expression that matches the given range of digits
		 */
		private static String digitRange(int start, int end, String baseMode)
		{
			StringBuilder digitRange = new StringBuilder("[");

			if (start < 9) {
				char startDigit = Character
						.forDigit(start, Character.MAX_RADIX);
				char endDigit = Character.forDigit(Math.min(end, 9),
						Character.MAX_RADIX);

				if (startDigit == endDigit)
					digitRange.append(startDigit);
				else
					digitRange.append(startDigit).append('-').append(endDigit);
			}

			if (end >= 10) {
				char startDigit = Character.forDigit(Math.max(start, 10),
						Character.MAX_RADIX);
				char endDigit = Character.forDigit(end, Character.MAX_RADIX);

				if (baseMode.contains("L")) {
					if (startDigit == endDigit)
						digitRange.append(startDigit);
					else
						digitRange.append(startDigit).append('-').append(
								endDigit);
				}

				if (baseMode.contains("U")) {
					if (startDigit == endDigit)
						digitRange.append((char) (startDigit - ' '));
					else
						digitRange.append((char) (startDigit - ' '))
								.append('-').append((char) (endDigit - ' '));
				}
			}

			return digitRange.append(']').toString();
		}

		/**
		 * Returns a String with the specified character repeated
		 * 
		 * @param character
		 *            <code>char</code> to repeat
		 * @param count
		 *            number of times to repeat the character
		 * @return a string with the given character repeated <code>count</code>
		 *         times
		 */
		private static String repeat(char character, int count)
		{
			StringBuilder result = new StringBuilder();

			for (int i = 0; i < count; i++) {
				result.append(character);
			}

			return result.toString();
		}
	}

	/**
	 * Enumeration of error messages encountered when interacting with patterns.
	 */
	static class PatternErrorMessage
	{
		static final String CONDITIONAL_BRANCHES = "Conditional group contains more than two branches";

		static final String UNMATCHED_PARENTHESES = "Unmatched closing ')'";

		static final String UNKNOWN_POSIX_CLASS = "Unknown POSIX class name";

		static final String POSIX_OUTSIDE_CLASS = "POSIX class outside of character class";

		static final String INVALID_HEX_CODE = "Character value in \\x{...} sequence is too large";

		static final String UNCLOSED_GROUP = "Unclosed group";

		static final String ILLEGAL_OCTAL_ESCAPE = "Illegal octal escape sequence";

		static final String NUMERIC_RANGE_EXPECTED = "Numeric range expected";

		static final String UNCLOSED_RANGE = "Missing closing ']' after numeric range";

		static final String INVALID_BASE = "Invalid base";

		static final String INVALID_DIGIT_START = "Invalid digit in start of range";

		static final String INVALID_DIGIT_END = "Invalid digit in end of range";

		static final String NONEXISTENT_SUBPATTERN = "Reference to non-existent subpattern";

		static final String ZERO_REFERENCE = "A numbered reference is zero";

		static final String INVALID_CONDITION0 = "Invalid condition (?(0)";

		static final String ASSERTION_EXPECTED = "Assertion expected after (?(";

		static final String SUBPATTERN_NAME_EXPECTED = "Subpattern name expected";

		static final String MISSING_TERMINATOR = "Missing terminator for subpattern name";

		/**
		 * Uses error message from Java 1.5
		 */
		static final String INVALID_FORWARD_REFERENCE = "No such group yet exists at this point in the pattern";

		static final String INTERNAL_ERROR = "An unexpected internal error has occurred";

		static final String DUPLICATE_NAME = "Two named subpatterns have the same name";

		static final String UNCLOSED_COMMENT = "Missing closing ')' after comment";
	}
}