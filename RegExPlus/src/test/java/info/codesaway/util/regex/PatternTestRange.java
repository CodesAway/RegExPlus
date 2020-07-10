package info.codesaway.util.regex;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test class - numeric ranges ({@link Pattern#range(String, String, boolean)})
 */
@RunWith(value = Parameterized.class)
public class PatternTestRange
{
	/* 
	 * expected, value
	 * 
	 * value = "Z[start..end]" or "NZ[start..end]"
	 */

	/**
	 * Returns the list of test cases
	 * 
	 * @return the list of parameter tests
	 */
	@Parameters
	public static Collection<Object[]> data()
	{
		return Arrays.asList(new Object[][] {
				{ "25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}", "Z[000..255]" },
				{ "25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}", "Z[0..255]" },
				{ "12[0-7]|1[0-1][0-9]|0?[0-9]{1,2}", "Z[0..127]" },
				{ "[1-9][0-9]{2}|[1-9][0-9]|[0-9]", "NZ[0..999]" },
				{ "[0-9]{3}", "Z[000..999]" }, { "[0-9]{1,3}", "Z[0..999]" },
				{ "[1-9][0-9]{2}|[1-9][0-9]|[1-9]", "NZ[1..999]" },
				{ "[1-9][0-9]{2}|0[1-9][0-9]|00[1-9]", "Z[001..999]" },
				{ "[1-9][0-9]{2}|0?[1-9][0-9]|0?0?[1-9]", "Z[1..999]" },
				{ "[0-5]?[0-9]", "Z[0..59]" },
				{ "36[0-6]|3[0-5][0-9]|[0-2]?[0-9]{1,2}", "Z[0..366]" } });
	}

	/**
	 * The expected numeric range
	 */
	private String expected;
	
	/**
	 * The compiled pattern
	 */
	private Pattern pattern;

	/**
	 * @param expected the expected refactored regular expression
	 * @param range the numeric range to test
	 */
	public PatternTestRange(String expected, String range)
	{
		this.expected = "(?:" + expected + ")";
		this.pattern = Pattern.compile("(?" + range + ")");
	}

	/**
	 * Tests {@link Pattern#range(String, String, boolean)}
	 * @throws Exception if test failed
	 */
	@Test
	public void testRange() throws Exception
	{
		assertEquals(expected, pattern.internalPattern());
	}
}
