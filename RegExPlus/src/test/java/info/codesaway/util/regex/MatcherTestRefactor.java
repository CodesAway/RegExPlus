package info.codesaway.util.regex;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import info.codesaway.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Testing class - refactoring a replacement
 */
@SuppressWarnings("unused")
@RunWith(value = Parameterized.class)
public class MatcherTestRefactor
{
	/**
	 * The expected refactored replacement
	 */
	String expected;

	/**
	 * The group names for the pattern
	 */
	String groupNames;

	/**
	 * The replacement to refactor
	 */
	String replacement;

	/**
	 * Returns the list of test cases
	 * 
	 * @return the list of parameter tests
	 */
	// expected, pattern, replacement
	@Parameters
	public static Collection<Object[]> data()
	{
		return Arrays.asList(new Object[][] {
				{ "", "", "" },
				});
	}

	/**
	 * Constructs the test case
	 * 
	 * @param expected
	 *            the expected refactored replacement
	 * @param pattern
	 *            the regex to use
	 * @param replacement
	 *            the replacement string to refactor
	 */
	public MatcherTestRefactor(String expected, String pattern,
			String replacement)
	{
		this.expected = expected;
		this.groupNames = Pattern.compile(pattern).getGroupNames().toString();
		this.replacement = replacement;
	}

	/**
	 * Tests {@link Matcher#refactor(String, String)}
	 * 
	 * @throws Exception
	 *             if the test fails
	 */
	@Test
	public void refactorReplacement() throws Exception
	{
	}
}
