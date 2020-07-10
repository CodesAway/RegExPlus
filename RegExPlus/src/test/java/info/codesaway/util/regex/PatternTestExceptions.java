package info.codesaway.util.regex;

import static info.codesaway.util.regex.Pattern.VERIFY_GROUPS;
import static info.codesaway.util.regex.Refactor.CONDITIONAL_BRANCHES;
import static info.codesaway.util.regex.Refactor.DUPLICATE_NAME;
import static info.codesaway.util.regex.Refactor.INVALID_CONDITION0;
import static info.codesaway.util.regex.Refactor.MISSING_TERMINATOR;
import static info.codesaway.util.regex.Refactor.NONEXISTENT_SUBPATTERN;
import static info.codesaway.util.regex.Refactor.NUMERIC_RANGE_EXPECTED;
import static info.codesaway.util.regex.Refactor.SUBPATTERN_NAME_EXPECTED;
import static info.codesaway.util.regex.Refactor.UNCLOSED_GROUP;
import static info.codesaway.util.regex.Refactor.UNMATCHED_PARENTHESES;
import static info.codesaway.util.regex.Refactor.UNCLOSED_RANGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Class to test patterns that shouldn't compile
 */
@RunWith(value = Parameterized.class)
public class PatternTestExceptions
{
	/**
	 * The expected error message
	 */
	private String errorMessage;

	/**
	 * The expected index for the error
	 */
	private int errorOffset;

	/**
	 * The pattern to test
	 */
	private String pattern;

	/**
	 * The flags used when compiling the pattern
	 */
	private int flags;

	/**
	 * Returns the list of test cases
	 * 
	 * @return the list of parameter tests
	 */
	// errorOffset, pattern, flags
	@Parameters
	public static Collection<Object[]> data()
	{
		return Arrays.asList(new Object[][] {
				{ SUBPATTERN_NAME_EXPECTED, 4, "(?P<", 0 },
				{ SUBPATTERN_NAME_EXPECTED, 3, "(?')", 0 },
				{ SUBPATTERN_NAME_EXPECTED, 3, "(?'')", 0 },
				{ MISSING_TERMINATOR, 4, "(?'a", 0 },
				{ DUPLICATE_NAME, 12, "(?<test>)(?<test>)", 0 },
				{ DUPLICATE_NAME, 26, "(?<test>)(?J:(?<test>))(?<test>)", 0 },
				{ NONEXISTENT_SUBPATTERN, 6, "abc\\g{1}", VERIFY_GROUPS },
				{ NONEXISTENT_SUBPATTERN, 6, "abc\\g{abc}", VERIFY_GROUPS },
				{ NONEXISTENT_SUBPATTERN, 6, "abc(?(1))", VERIFY_GROUPS },
				{ NONEXISTENT_SUBPATTERN, 6, "abc(?(abc))", VERIFY_GROUPS },
				{ MISSING_TERMINATOR, 4, "(?<a", 0 },
				{ MISSING_TERMINATOR, 4, "(?'a?", 0 },
				{ SUBPATTERN_NAME_EXPECTED, 3, "\\g{", 0 },
				{ SUBPATTERN_NAME_EXPECTED, 4, "(?P=", 0 },
				{ MISSING_TERMINATOR, 4, "\\g{a", 0 },
				{ UNCLOSED_RANGE, 7, "(?Z[1..", 0 },
//				{ NUMERIC_RANGE_EXPECTED, 4, "(?Z[1..", 0 },
				{ UNCLOSED_GROUP, 11, "(?Z[12..24]", 0 },
				{ INVALID_CONDITION0, 6, "abc(?(0)1|2)", 0 },
				{ UNMATCHED_PARENTHESES, 3, "abc)", 0 },
				{ UNCLOSED_GROUP, 4, "abc(", 0 },
				{ CONDITIONAL_BRANCHES, 10, "(?(?=a)b|c|d)", 0 },
				{ CONDITIONAL_BRANCHES, 10, "()(?(1)b|c|d)", 0 },
				{ DUPLICATE_NAME, 15, "(?<abc>(?J))(?<abc>)", 0 },
				{ DUPLICATE_NAME, 16, "(?J:(?<abc>))(?<abc>)", 0 },
				// 06/2020 - Moved from PatternTest based on 0.4 changes
				{ NONEXISTENT_SUBPATTERN, 3, "(?(1)2|3)", 0 },
				{ UNCLOSED_RANGE, 7, "(?Z[0...2])", 0},
				{ UNCLOSED_RANGE, 7, "(?Z[0....2])", 0},
				{ UNCLOSED_RANGE, 7, "(?Z[0.....2])", 0},
				{ UNCLOSED_RANGE, 6, "(?Z[<2", 0},
				{ UNCLOSED_RANGE, 8, "(?Z[0..2", 0},
				{ UNCLOSED_GROUP, 7, "(?Z[<2]", 0},
				{ UNCLOSED_GROUP, 9, "(?Z[0..2]", 0},
				{ UNCLOSED_RANGE, 7, "(?Z[0...2]", 0},
				{ UNCLOSED_RANGE, 7, "(?Z[0....2]", 0},
				{ UNCLOSED_RANGE, 7, "(?Z[0.....2]", 0},
				{ NONEXISTENT_SUBPATTERN, 24, "(?|(a)(b)|(c)|(d)(e))\\g{2[0]}", 0},
				{ NONEXISTENT_SUBPATTERN, 3, "(?(test[0])1|2)", 0},
				});
	}

	/**
	 * Constructs the test case
	 * 
	 * @param errorMessage
	 *            The expected <code>PatternErrorMessage</code>
	 * @param errorOffset
	 *            The expected index for the error
	 * @param pattern
	 *            The pattern to compile
	 * @param flags
	 *            The flags used when compiling the pattern
	 */
	public PatternTestExceptions(String errorMessage,
			int errorOffset,
			String pattern, int flags)
	{
		this.errorMessage = errorMessage;
		this.errorOffset = errorOffset;
		this.pattern = pattern;
		this.flags = flags;
	}

	/**
	 * Verifies that the pattern doesn't compile, and compares the error message
	 * with the expected error message.
	 * 
	 * @throws Exception
	 *             if the test fails
	 */
	@Test
	public void checkPattern() throws Exception
	{
		try {
			Pattern.compile(pattern, flags);
			fail("Pattern shouldn't have compiled");
		} catch (PatternSyntaxException e) {
			assertEquals("Wrong error:", errorMessage, e.getDescription());
			assertEquals("Wrong offset:", errorOffset, e.getIndex());
		}
	}
}
