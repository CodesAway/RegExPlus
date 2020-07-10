package info.codesaway.util.regex;

import static info.codesaway.util.regex.Pattern.DUPLICATE_NAMES;
import static info.codesaway.util.regex.Pattern.EXPLICIT_CAPTURE;
import static info.codesaway.util.regex.Pattern.LITERAL;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Class to test patterns which should compile.
 */
@RunWith(value = Parameterized.class)
public class PatternTest
{
	/**
	 * The expected refactored pattern
	 */
	String expected;

	/**
	 * The pattern to compile
	 */
	String pattern;

	/**
	 * The flags for the pattern
	 */
	int flags;

	/**
	 * Returns a list of test cases
	 * 
	 * @return the list of parameter tests
	 */
	// expected, pattern, flags
	@Parameters
	public static Collection<Object[]> data()
	{
		// TODO: descibe what each test tests
		return Arrays
				.asList(new Object[][] {
						{ "(?ii:test)", "(?iJiJ:test)", 0 },
						{ "(?ii)", "(?iJiJ)", 0 },
						{ "", "(?JJ)", 0 },
						{ "(?:test)", "(?J:test)", 0 },
						{ "(?:test)", "(?:test)", 0 },
						{ "(?-i:test)", "(?J-i:test)", 0 },
						{ "(?:test)", "(?J-:test)", 0 },

						{ "()()", "(?<test>)(?<test>)", DUPLICATE_NAMES },
						{ "()(?:())", "(?<test>)(?J:(?<test>))", 0 },
						{ "()()", "(?<test>)(?<Test>)", 0 },

						// 10
						{
								"(?:(1|2)())(?:(?=\\2|\\4)(?:3|4)|(?!\\2|\\4))(?:(5|6)())",
								"(?J)(?<test>1|2)(?(test[0])(?:3|4))(?<test>5|6)",
								0
						},
						// 11
						{
								"(?:(?:(abc)())|(?:(def)()))(?:(?=\\2|\\4)1|(?!\\2|\\4)2)",
								"(?J)(?:(?<test>abc)|(?<test>def))(?(test[000])1|2)",
								0
						},
						// 12 
						// Moved to PatternTestExceptions based on 0.4 changes
						// TODO: find another example
						{
//								"(?:\\b\\B2|3)", "(?(1)2|3)", 0
							"", "", 0
						},
						// 13
						{
								"(?:(?=\\2)2|(?!\\2)3)(?:(abc)())",
								"(?(1)2|3)(?<test>abc)", 0
						},
						// 14
						{ "()(?:())", "(?<test>)(?J:(?<test>))",
								EXPLICIT_CAPTURE },
						// 15
						{
								"(?:(1|2)())(?:(3|4)())(?:(5|6)())(?:(?=\\4)(?:7|8)|(?!\\4)(?:9|10))(?:\\1|(?!\\2)\\5)",
								"(?<test>1|2)(?<test2>3|4)(?'test'5|6)(?(test2)(?:7|8)|(?:9|10))\\k{test[0]}",
								DUPLICATE_NAMES },
						// 16
						{
								"(?:(1|2)())(?:(3|4)())(?:(5|6)())(?:(?=\\4)(?:7|8)|(?!\\4)(?:9|10))(?:\\1|(?!\\2)\\5)",
								"(?J)(?<test>1|2)(?<test2>3|4)(?'test'5|6)(?(test2)(?:7|8)|(?:9|10))\\g{test}",
								0 },
						// 17
						{
								"(?:(1|2)())\\1(?:(3|4)())",
								"(?J)(?<test>1|2)\\k<test[0]>(?<test>3|4)", 0 },
						// 18
						{
								"(1|2)\\1[3]", "(1|2)\\13", 0
						},
						// 19
						{
								"(?!)3", "\\13", 0
						},
						// 20
						{
								"(?!)3(?:((?:(?=\\2)2|(?!\\2)3))())(abc)",
								"\\23((?(1)2|3))(?<test>abc)", 0
						},
						// 21
						// Moved to PatternTestExceptions based on 0.4 changes
						// TODO: find another example
						{
								"", "", 0
//								"(?:(a)(b)|(c)|(d)(e))\\b\\B",
//								"(?|(a)(b)|(c)|(d)(e))\\g{2[0]}", 0
						},
						// 22
						// Moved to PatternTestExceptions based on 0.4 changes
						// TODO: find another example
						{
								"", "", 0
//								"(?:\\b\\B1|2)", "(?(test[0])1|2)", 0
						},
						// 23
												// assertion condition
						{
								"(?:(?:(?=[^a-z]*[a-z])())?+(?:(?=\\1)\\d{2}-[a-z]{3}-\\d{2}|(?!\\1)\\d{2}-\\d{2}-\\d{2}))",
								"(?(?=[^a-z]*[a-z])\\d{2}-[a-z]{3}-\\d{2}|\\d{2}-\\d{2}-\\d{2})",
								0
						}

				});
	}

	/**
	 * Consructs a test case
	 * 
	 * @param expected
	 *            the expected refactored pattern
	 * @param pattern
	 *            the pattern to compile
	 * @param flags
	 *            the flags for the pattern
	 */
	public PatternTest(String expected, String pattern, int flags)
	{
		this.expected = expected;
		this.pattern = pattern;
		this.flags = flags;
	}

	/**
	 * Tests {@link Pattern#compile(String, int)}
	 * 
	 * @throws Exception
	 *             if the test fails
	 */
	@Test
	public void checkPattern() throws Exception
	{
		Pattern p = Pattern.compile(pattern, flags);
		assertEquals(expected, p.internalPattern());
	}

	/**
	 * Tests {@link Pattern#LITERAL}
	 * 
	 * @throws Exception
	 *             if the test fails
	 */
	@Test
	public void literalPattern() throws Exception
	{
		Pattern p = Pattern.compile(pattern, LITERAL);
		assertEquals(pattern, p.internalPattern());
	}
}