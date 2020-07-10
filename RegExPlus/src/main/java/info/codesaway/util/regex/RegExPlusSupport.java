package info.codesaway.util.regex;

/**
 * Used to store the last regex match.
 *
 * <p>RegExPlus counterpart for the Groovy class, RegexSupport (org.codehaus.groovy.runtime.RegexSupport),
 * which stores the last java.util.regex.Matcher object.</p>
 */
public class RegExPlusSupport {
	private static final ThreadLocal<Matcher> CURRENT_MATCHER = new ThreadLocal<>();

	public static Matcher getLastMatcher() {
		return CURRENT_MATCHER.get();
	}

	public static Matcher setLastMatcher(final Matcher matcher) {
		CURRENT_MATCHER.set(matcher);
		return matcher;
	}
}