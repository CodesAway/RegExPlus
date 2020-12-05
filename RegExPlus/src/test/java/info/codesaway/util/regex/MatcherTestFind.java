package info.codesaway.util.regex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Me
 *
 */
@RunWith(value = Parameterized.class)
public class MatcherTestFind {
	/**
	 * The expected match
	 */
	private final String match;

	/**
	 * The character sequence to be matched
	 */
	private final String input;

	/**
	 * The regular expression
	 */
	private final String regex;

	// No longer required since RegExPlus already caches Pattern objects
	//	/**
	//	 * The cache of compiled <code>Pattern</code><code>s</code>
	//	 */
	//	private static TreeMap<String, Pattern> patterns = new TreeMap<String, Pattern>();

	/**
	 * Returns the list of test cases
	 *
	 * @return the list of parameter tests
	 */
	// match, input, regex
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { { null, "ab", "(a)?a(?(1)b|c)" }, { "aab", "aab", "(a)?a(?(1)b|c)" },
			{ "ac", "ac", "(a)?a(?(1)b|c)" }, { "abcabc11", "abcabc11", "(?<test>abc)()()()()()()()()()\\111" },
			{ "abcabc11", "abcabc11", "(?<test>abc)()()()()()()()()()\\g{test}11" },
			//				{ null, "abcabc11",
			//				"(?<test>abc)()()()()()()()()()\\g{11}1" },
		});
	}

	/**
	 * @param match The expected match
	 * @param input The character sequence to be matched
	 * @param regex The regular expression
	 */
	public MatcherTestFind(final String match, final String input, final String regex) {
		this.match = match;
		this.input = input;
		this.regex = regex;
	}

	/**
	 * Tests {@link Matcher#find()}
	 *
	 * @throws Exception
	 *             if the test fails
	 */
	@Test
	public void find() throws Exception {
		Pattern p = Pattern.compile(this.regex);
		Matcher m = p.matcher(this.input);
		String actual = (m.find() ? m.group() : null);

		MatchResult r = m.toMatchResult();

		assertEquals(this.match, actual);

		if (m.matched()) {
			assertThat(r.group()).isEqualTo(this.match);
		}
	}

	//	/**
	//	 * Compiles the pattern and caches the compiled <code>Pattern</code>
	//	 * @return the compiled pattern
	//	 */
	//	private Pattern compile()
	//	{
	//		if (patterns.containsKey(regex))
	//			return patterns.get(regex);
	//		else {
	//			Pattern pattern = Pattern.compile(regex);
	//			patterns.put(regex, pattern);
	//			// System.out.println(pattern.internalPattern());
	//			return pattern;
	//		}
	//	}
}
