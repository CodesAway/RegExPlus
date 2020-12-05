package info.codesaway.util.regex;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MatchResultTest {
	// Issue #10
	@Test
	public void testMatchResult() {
		Pattern p = Pattern.compile("");
		Matcher m = p.matcher();

		assertThat(m.toMatchResult().text()).isEmpty();
	}
}
