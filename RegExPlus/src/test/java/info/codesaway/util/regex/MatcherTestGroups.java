package info.codesaway.util.regex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Testing class - capture groups
 */
@RunWith(value = Parameterized.class)
public class MatcherTestGroups {
	/**
	 * Matcher to use when matching
	 */
	private final Matcher matcher;

	/**
	 * Match result of the matcher
	 *
	 * <p>This is used to verify that everything works as a MatchResult
	 * the same as the Matcher itself</p>
	 */
	private final MatchResult matchResult;

	/**
	 * the test case index
	 */
	private final int index;

	/**
	 * Pattern to check
	 */
	private static Pattern pattern;

	/**
	 * Expected capture for group 1
	 */
	private static String[] group1;

	/**
	 * Expected mapped index for group 1
	 */
	private static int[] group1_index;

	/**
	 * Expected capture for group 2
	 */
	private static String[] group2;

	/**
	 * Expected mapped index for group 2
	 */
	private static int[] group2_index;

	/**
	 * Expected capture for group 3
	 */
	private static String[] group3;

	/**
	 * Expected mapped index for group 3
	 */
	private static int[] group3_index;

	/**
	 * Expected capture for group 4
	 */
	private static String[] group4;

	/**
	 * Expected mapped index for group 4
	 */
	private static int[] group4_index;

	/**
	 * Expected capture for group 5
	 */
	private static String[] group5;

	/**
	 * Expected mapped index for group 5
	 */
	private static int[] group5_index;

	/**
	 * Expected capture for group 6
	 */
	private static String[] group6;

	/**
	 * Expected mapped index for group 6
	 */
	private static int[] group6_index;

	/**
	 * Expected capture for the named group, test
	 */
	private static String[] groupTest;

	/**
	 * Expected mapped index for the named group, test
	 */
	private static int[] groupTest_index;

	/**
	 * Expected capture for the named group, test2
	 */
	private static String[] groupTest2;

	/**
	 * Expected mapped index for the named group, test2
	 */
	private static int[] groupTest2_index;

	/**
	 * Initialize values
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		pattern = Pattern.compile("(?|(a)|(b)(c)(d)|(e)(f))(?J:(?<test>g)|(?<test>h))(?<test2>i)");

		group1 = new String[] { "a", "a", "b", "b", "e", "e" };
		group1_index = new int[] { 1, 1, 2, 2, 5, 5 };

		group2 = new String[] { null, null, "c", "c", "f", "f" };
		group2_index = new int[] { 3, 3, 3, 3, 6, 6 };

		group3 = new String[] { null, null, "d", "d", null, null };
		group3_index = new int[] { 4, 4, 4, 4, 4, 4 };

		group4 = new String[] { "g", null, "g", null, "g", null };
		group4_index = new int[] { 7, 7, 7, 7, 7, 7 };

		group5 = new String[] { null, "h", null, "h", null, "h" };
		group5_index = new int[] { 8, 8, 8, 8, 8, 8 };

		group6 = new String[] { "i", "i", "i", "i", "i", "i" };
		group6_index = new int[] { 9, 9, 9, 9, 9, 9 };

		groupTest = new String[] { "g", "h", "g", "h", "g", "h" };
		groupTest_index = new int[] { 7, 8, 7, 8, 7, 8 };

		groupTest2 = new String[] { "i", "i", "i", "i", "i", "i" };
		groupTest2_index = new int[] { 9, 9, 9, 9, 9, 9 };
	}

	/**
	 * Returns the list of test cases
	 *
	 * @return the list of parameter tests
	 */
	// input
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { { 0, "agi" }, { 1, "ahi" }, { 2, "bcdgi" }, { 3, "bcdhi" }, { 4, "efgi" },
			{ 5, "efhi" }, });
	}

	/**
	 * Constructs the test case
	 *
	 * @param index The test case index
	 * @param input The character sequence to be matched
	 */
	public MatcherTestGroups(final int index, final String input) {
		this.matcher = pattern.matcher(input);
		this.matcher.find();
		this.index = index;
		this.matchResult = this.matcher.toMatchResult();
	}

	/**
	 * Checks if the actual match was the expected match
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group1() throws Exception {
		this.matchGroup(group1, 1);
	}

	/**
	 * Checks if the actual mapped index was the expected index
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group1_index() throws Exception {
		this.matchGroupIndex(group1_index, 1);
	}

	/**
	 * Checks if the actual match was the expected match
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group2() throws Exception {
		this.matchGroup(group2, 2);
	}

	/**
	 * Checks if the actual mapped index was the expected index
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group2_index() throws Exception {
		this.matchGroupIndex(group2_index, 2);
	}

	/**
	 * Checks if the actual match was the expected match
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group3() throws Exception {
		this.matchGroup(group3, 3);
	}

	/**
	 * Checks if the actual mapped index was the expected index
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group3_index() throws Exception {
		this.matchGroupIndex(group3_index, 3);
	}

	/**
	 * Checks if the actual match was the expected match
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group4() throws Exception {
		this.matchGroup(group4, 4);
	}

	/**
	 * Checks if the actual mapped index was the expected index
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group4_index() throws Exception {
		this.matchGroupIndex(group4_index, 4);
	}

	/**
	 * Checks if the actual match was the expected match
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group5() throws Exception {
		this.matchGroup(group5, 5);
	}

	/**
	 * Checks if the actual mapped index was the expected index
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group5_index() throws Exception {
		this.matchGroupIndex(group5_index, 5);
	}

	/**
	 * Checks if the actual match was the expected match
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group6() throws Exception {
		this.matchGroup(group6, 6);
	}

	/**
	 * Checks if the actual mapped index was the expected index
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void group6_index() throws Exception {
		this.matchGroupIndex(group6_index, 6);
	}

	/**
	 * Checks if the actual match was the expected match
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void groupTest() throws Exception {
		this.matchGroup(groupTest, "test");
	}

	/**
	 * Checks if the actual mapped index was the expected index
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void groupTest_index() throws Exception {
		this.matchGroupIndex(groupTest_index, "test");
	}

	/**
	 * Checks if the actual match was the expected match
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void groupTest2() throws Exception {
		this.matchGroup(groupTest2, "test2");
	}

	/**
	 * Checks if the actual mapped index was the expected index
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void groupTest2_index() throws Exception {
		this.matchGroupIndex(groupTest2_index, "test2");
	}

	private void matchGroup(final String[] tests, final int group) throws Exception {
		String expected = tests[this.index];
		String actual = this.matcher.group(group);

		assertEquals(expected, actual);
		assertThat(this.matchResult.group(group)).isEqualTo(expected);
	}

	private void matchGroupIndex(final int[] tests, final int group) throws Exception {
		int expected = tests[this.index];
		int actual = this.matcher.getGroupIndex(group);

		assertEquals(expected, actual);
	}

	private void matchGroup(final String[] tests, final String group) throws Exception {
		String expected = tests[this.index];
		String actual = this.matcher.group(group);

		assertEquals(expected, actual);
		assertThat(this.matchResult.group(group)).isEqualTo(expected);
	}

	private void matchGroupIndex(final int[] tests, final String group) throws Exception {
		int expected = tests[this.index];
		int actual = this.matcher.getGroupIndex(group);

		assertEquals(expected, actual);
	}

	/**
	 * Tests that the specified group is not a group
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void notAGroup() throws Exception {
		assertThrows("No group with name <NotAGroup>", IllegalArgumentException.class, () -> this.matcher.group("NotAGroup"));
	}

	/**
	 * Tests that the specified group is not a group
	 *
	 * @throws Exception if the test fails
	 */
	@Test
	public void notAGroupAnyGroup() throws Exception {
		assertThrows("No group with name <NotAGroup[]>", IllegalArgumentException.class, () -> this.matcher.group("NotAGroup[]"));
	}

	/**
	 * Tests that the specified group is not a group
	 *
	 * @throws Exception if the test fails
	 */
	public void notAGroup0() throws Exception {
		assertThrows("No group NotAGroup[0]", IndexOutOfBoundsException.class, () -> this.matcher.group("NotAGroup", 0));
	}

	/**
	 * Tests that the specified group is not a group
	 *
	 * @throws Exception if the test fails
	 */
	public void notAGroup00() throws Exception {
		assertThrows("No group NotAGroup[00]", IndexOutOfBoundsException.class, () -> this.matcher.group("NotAGroup[00]"));
	}

	/**
	 * Tests that the specified group is an invalid group
	 *
	 * @throws Exception if the test fails
	 */
	public void invalidGroup() throws Exception {
		assertThrows("No group [test][0]", IndexOutOfBoundsException.class, () -> this.matcher.group("[test]", 0));
	}
}