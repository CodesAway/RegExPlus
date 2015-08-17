package info.codesaway.util.regex;

import static info.codesaway.util.regex.Matcher.getAbsoluteGroupIndex;
import static info.codesaway.util.regex.Pattern.CASE_INSENSITIVE;
import static info.codesaway.util.regex.Pattern.COMMENTS;
import static info.codesaway.util.regex.Pattern.DOTALL;
import static info.codesaway.util.regex.Pattern.DOTNET_NUMBERING;
import static info.codesaway.util.regex.Pattern.DUPLICATE_NAMES;
import static info.codesaway.util.regex.Pattern.EXPLICIT_CAPTURE;
import static info.codesaway.util.regex.Pattern.MULTILINE;
import static info.codesaway.util.regex.Pattern.PERL_OCTAL;
import static info.codesaway.util.regex.Pattern.UNICODE_CASE;
import static info.codesaway.util.regex.Pattern.UNIX_LINES;
import static info.codesaway.util.regex.Pattern.VERIFY_GROUPS;
import static info.codesaway.util.regex.Pattern.getMappingName;
import static info.codesaway.util.regex.Pattern.naturalCompareTo;
import static info.codesaway.util.regex.Pattern.wrapIndex;
import static info.codesaway.util.regex.RefactorUtility.anyGroupName;
import static info.codesaway.util.regex.RefactorUtility.fail;
import static info.codesaway.util.regex.RefactorUtility.getDigitCountPattern;
import static info.codesaway.util.regex.RefactorUtility.hexCodeFormat;
import static info.codesaway.util.regex.RefactorUtility.isAnyGroup;
import static info.codesaway.util.regex.RefactorUtility.neverUsedMappingName;
import static info.codesaway.util.regex.RefactorUtility.nonCaptureGroup;
import static info.codesaway.util.regex.RefactorUtility.parseInt;
import static info.codesaway.util.regex.RefactorUtility.perl_octal;
import static info.codesaway.util.regex.RefactorUtility.posixClasses;
import static info.codesaway.util.regex.RefactorUtility.startNonCaptureGroup;
import static info.codesaway.util.regex.RefactorUtility.unicodeFormat;

import info.codesaway.util.Differences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

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
class Refactor
{
	/** The pattern. */
	private final Pattern pattern;

	/** The regular expression to refactor. */
	private final String regex;

	// Refactor parent object
	// (for cases like subroutines)
	// private final Refactor refactor;

	/** the subpattern */
	private final Subpattern subpattern;

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
	private int flags;

	/** Used to match the different parts to refactor. */
	private java.util.regex.Matcher matcher;

	/**
	 * Contains the current result of the refactoring process.
	 * 
	 * <p><b>Note</b>: this is the StringBuffer used when calling
	 * {@link java.util.regex.Matcher#appendReplacement(StringBuffer, String)} and
	 * {@link java.util.regex.Matcher#appendTail(StringBuffer)}</p>
	 */
	private StringBuffer result;

	/**
	 * Stores the matching String for <tt>matcher</tt> - the return value of {@link java.util.regex.Matcher#group()}
	 */
	private String match;

	/** The number of unclosed opening parenthesis. */
	private int parenthesisDepth;

	/** The number of unclosed opening square brackets. */
	private int charClassDepth;

	/**
	 * Used to track if currently in a curly brace '{' through '}'
	 * 
	 * <p>Use <code>null</code> to represent that no further tracking should be performed (such as if there are two open
	 * curly braces in a row, which is never allowed)
	 */
	private Boolean isInCurlyBrace = false;

	/**
	 * Used to track if currently in a comments block
	 * 
	 * <p>Starts with "#" when {@link Pattern#COMMENTS} is enabled</p>
	 */
	private boolean isInComments = false;

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
	private final Map<String, Integer> testConditionGroups = new HashMap<String, Integer>(
			2);

	/**
	 * Set of group names that have an "any group" back reference
	 */
	private final Set<String> anyGroupReferences = new HashSet<String>(2);

	/** The capture groups that require a testing group. */
	private final TreeSet<String> requiresTestingGroup = new TreeSet<String>();

	/**
	 * A stack of states used to track when a testing group should be added
	 * to a capture group.
	 */
	private final Stack<AddTestGroupState> addTestGroup = new Stack<AddTestGroupState>();

	/**
	 * A stack of states used to track the else branch of a conditional
	 * subpattern.
	 */
	private final Stack<MatchState> handleElseBranch = new Stack<MatchState>();

	/**
	 * A stack of states used to track the end of the assertion in an assert
	 * conditional
	 */
	private final Stack<MatchState> handleEndAssertCond = new Stack<MatchState>();

	/**
	 * A stack of states used to track the branches in a branch reset
	 * subpattern.
	 */
	private final Stack<BranchResetState> branchReset = new Stack<BranchResetState>();

	/**
	 * A stack used to store the current flags.
	 * 
	 * <p>
	 * When entering a new parenthesis grouping, the current flags are
	 * saved. This value is later restored upon existing the parenthesis
	 * grouping.
	 * </p>
	 */
	private final Stack<Integer> flagsStack = new Stack<Integer>();

	/**
	 * A map with mappings from group name to the group count for
	 * that group name.
	 */
	private Map<String, Integer> groupCounts = new HashMap<String, Integer>(2);

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
	 * <p>If the refactored regular expression throws a {@link PatternSyntaxException}, this field is used to map
	 * the index
	 * for the exception to its respective index in the original regular
	 * expression.</p>
	 */
	Differences changes = new Differences();

	/**
	 * List of changes performed during the pre-refactoring step
	 */
	private final Differences preRefactoringChanges;

	/**
	 * Mapping from integer (incremental) to index for error
	 */
	private Map<Integer, Integer> errorTrace;

	/**
	 * Indicates if the current VM is Java 1.5
	 */
	private final boolean isJava1_5;

	/** The subpatterns. */
	private final Map<String, Subpattern> subpatterns;

	/** The open subpatterns. */
	private final Map<Integer, Subpattern> openSubpatterns = new HashMap<Integer, Subpattern>();

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

	static final String newLine = System.getProperty("line.separator");

	/* Error messages */
	static final String ASSERTION_EXPECTED = "Assertion expected after (?(";

	static final String CONDITIONAL_BRANCHES = "Conditional group contains more than two branches";

	static final String DUPLICATE_NAME = "Two named subpatterns have the same name";

	static final String ILLEGAL_OCTAL_ESCAPE = "Illegal octal escape sequence";

	static final String INTERNAL_ERROR = "An unexpected internal error has occurred";

	static final String INVALID_BASE = "Invalid base";

	static final String INVALID_CONDITION0 = "Invalid condition (?(0)";

	static final String INVALID_DIGIT_END = "Invalid digit in end of range";

	static final String INVALID_DIGIT_START = "Invalid digit in start of range";

	/**
	 * Uses error message from Java 1.5
	 */
	static final String INVALID_FORWARD_REFERENCE = "No such group yet exists at this point in the pattern";

	static final String INVALID_HEX_CODE = "Character value in \\x{...} sequence is too large";

	static final String MISSING_TERMINATOR = "Missing terminator for subpattern name";

	static final String NONEXISTENT_SUBPATTERN = "Reference to non-existent subpattern";

	static final String NUMERIC_RANGE_EXPECTED = "Numeric range expected";

	static final String POSIX_OUTSIDE_CLASS = "POSIX class outside of character class";

	static final String SUBPATTERN_NAME_EXPECTED = "Subpattern name expected";

	static final String UNCLOSED_COMMENT = "Missing closing ')' after comment";

	static final String UNCLOSED_GROUP = "Unclosed group";

	static final String UNCLOSED_RANGE = "Missing closing ']' after numeric range";

	static final String UNKNOWN_POSIX_CLASS = "Unknown POSIX class name";

	static final String UNMATCHED_PARENTHESES = "Unmatched closing ')'";

	static final String ZERO_REFERENCE = "A numbered reference is zero";

	static final String INVALID_SUBROUTINE = "Subroutine contains unsupported syntax for a subroutine";

	static final String CIRCULAR_SUBROUTINE = "Subroutine cannot be circular (have a subroutine which depends on itself)";

	/**
	 * Resets the necessary values before performing the next step of the
	 * refactoring.
	 */
	void reset()
	{
		// flags = pattern.flags();
		flags = initializeFlags();
		parenthesisDepth = 0;
		charClassDepth = 0;
		currentGroup = 0;
		totalGroups = 0;
		namedGroup = 0;
		unnamedGroup = 0;
		isInComments = false;

		result = new StringBuffer();

		addTestGroup.clear();
		handleElseBranch.clear();
		branchReset.clear();
		flagsStack.clear();

		if (inSubroutine()) {
			groupCounts = subpattern.getGroupCounts();
		} else {
			groupCounts.clear();
		}

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
		return error(errorMessage, index, null);
	}

	/**
	 * Internal method used for handling all patternSyntax errors. The
	 * pattern is displayed with a pointer to aid in locating the patternSyntax error.
	 * 
	 * @param errorMessage
	 *            the <code>PatternErrorMessage</code> for the error
	 * @param index
	 *            The approximate index in the pattern of the error, or -1
	 *            if the index is not known
	 * @param additionalDetails
	 *            the additional details (or <code>null</code> if there are none)
	 * @return a <code>PatternSyntaxException</code> with the given error
	 *         message, index, and using the original regex
	 */
	private PatternSyntaxException error(String errorMessage, int index, String additionalDetails)
	{
		int originalIndex;

		try {
			originalIndex = changes.getOriginalIndex(index);
		} catch (IllegalArgumentException e) {
			originalIndex = -1;
		}

		return new PatternSyntaxException(errorMessage, regex, originalIndex, additionalDetails);
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
	// Refactor(String regex)
	Refactor(Pattern pattern)
	{
		this(pattern, pattern.pattern(), null);
	}

	Refactor(Subpattern subpattern, String regex)
	{
		this(subpattern.getParentPattern(), regex, subpattern);
	}

	private Refactor(Pattern pattern, String regex, Subpattern subpattern)
	{
		this.pattern = pattern;
		this.regex = regex;
		this.subpattern = subpattern;
		// this.refactor = null;

		// this.flags = pattern.flags();

		this.flags = initializeFlags();

		// System.out.println("Flags: " + new PatternFlags(flags));

		this.subpatterns = subpattern != null ? subpattern.getParentSubpatterns() : new HashMap<String, Subpattern>();

		// if (has(LITERAL)) {
		// this.result = new StringBuffer(regex);
		// initializeForZeroGroups();
		// return;
		// }

		String javaVersion = System.getProperty("java.version");
		isJava1_5 = naturalCompareTo(javaVersion, "1.5.0") >= 0 &&
				naturalCompareTo(javaVersion, "1.6.0") < 0;

		// if (!inSubroutine()) {
		this.result = new StringBuffer();
		preRefactor();
		preRefactoringChanges = differences;
		// } else
		// this.result = new StringBuffer(regex);

		refactor();
		afterRefactor();
	}

	private int initializeFlags()
	{
		return subpattern != null ? subpattern.getFlags() : pattern.flags();
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
	 * Gets the subpatterns.
	 * 
	 * @return the subpatterns
	 */
	Map<String, Subpattern> getSubpatterns()
	{
		return subpatterns;
	}

	/**
	 * Indicates whether the refactoring is being done within a subroutine
	 * 
	 * @return <code>true</code> if currently refactoring a subroutine
	 */
	private boolean inSubroutine()
	{
		return subpattern != null;
	}

	/**
	 * Gets the pattern.
	 * 
	 * @return the pattern
	 */
	Pattern getPattern()
	{
		return pattern;
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
	 * Returns the group index for the first non-null group.
	 * 
	 * @param matcher
	 *            the matcher
	 * @return the group index for the first non-null group
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
	 * <p>
	 * Used in the loop that matches parts for the current refactoring step.
	 * </p>
	 * 
	 * <p>Sets {@link #match} to the matched string, and sets {@link #group} to the index for the first non-null
	 * capture group.</p>
	 * 
	 * @return true if the current loop should be skipped
	 */
	private boolean loopSetup()
	{
		match = matcher.group();
		group = getUsedGroup(matcher);

		if (isInComments)
		{
			if (match.startsWith("(?#") && !inCharClass())
			{
				// Comment block
				return false;
			}
			else if (match.startsWith("\\Q"))
			{
				// Quote block
				return false;
			}
			else if (match.equals("\n") || match.equals("\r") || match.equals("\u0085")
					|| match.equals("\u2028") || match.equals("\u2029"))
			{
				// Line terminator [\n\r\u0085\u2028\u2029]
				return false;
			}
			else
			{
				// Other syntax should be ignored
				return true;
			}
		}

		return false;
	}

	/**
	 * Sets the number of capture groups.
	 * 
	 * @param capturingGroupCount
	 *            the number of capture groups
	 */
	void setCapturingGroupCount(int capturingGroupCount)
	{
		if (!inSubroutine())
			pattern.setCapturingGroupCount(capturingGroupCount);
	}

	/**
	 * @param addedGroups
	 * @since 0.2
	 */
	void setAddedGroups(boolean addedGroups)
	{
		if (!inSubroutine())
			pattern.setAddedGroups(addedGroups);
	}

	/**
	 * Sets the group name counts.
	 * 
	 * @param groupNameCounts
	 *            the group name counts
	 */
	void setGroupNameCounts(Map<String, Integer> groupNameCounts)
	{
		if (!inSubroutine())
			pattern.setGroupCounts(groupNameCounts);
	}

	/**
	 * Steps taken to prepare the regular expression to be refactored
	 */
	private void preRefactor()
	{
		text = regex.toString();
		matcher = RefactorUtility.preRefactor.matcher(regex);

		// add a map from group 0 to group 0
		// TODO: add occurrence??
		addUnnamedGroup("[0][1]", 0);
		setGroupCount("[0]", 1);

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
				 * - not followed by a "?" (or a '*', used by verbs)
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

					if (isInComments)
					{
						checkForLineTerminator();
					}

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
			} else if (group == 28) {
				/*
				 * FAIL verb (from PCRE) - always fails
				 * (*FAIL) or (*F) - case sensitive
				 * 
				 * synonym for (?!)
				 */

				if (!inCharClass())
					replaceWith(fail());
			} else {
				preRefactorOthers();
			}
		}

		unnamedGroupCount = unnamedGroup;
		setCapturingGroupCount(currentGroup);
		setGroupCount("", currentGroup);
		setAddedGroups(totalGroups != currentGroup);

		if (has(DOTNET_NUMBERING)) {
			// for each named group, mark the group count as 1
			// for its respective number group
			// (doesn't affect named groups in "branch reset" patterns)

			for (int i = 1; i <= namedGroup; i++) {
				int groupIndex = unnamedGroupCount + i;
				setGroupCount("[" + groupIndex + "]", 1);
			}

			int currentNamedGroup = 0;

			for (Entry<Integer, String> entry : perlGroupMapping()
					.entrySet()) {
				String mappingName = entry.getValue();

				if (mappingName.charAt(0) != '[') {
					// a named group - using mapping name of unnamed group

					currentNamedGroup++;
					String groupName = wrapIndex(unnamedGroupCount +
							currentNamedGroup);

					mappingName = getMappingName(groupName, 0);
					entry.setValue(mappingName);
				}
			}
		}

		setGroupNameCounts(new HashMap<String, Integer>(
				groupCounts));

		for (String groupName : anyGroupReferences) {
			// if (groupCount(groupName) != 1)
			// System.out.println("getGroupCount: " + groupName);

			if (getGroupCount(groupName) != 1)
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
		// matcher = getRefactorPattern().matcher(result);
		matcher = RefactorUtility.refactor.matcher(result);
		reset();

		while (matcher.find()) {
			if (loopSetup())
				continue;

			// System.out.println("group: " + group + "\t" + matcher.group());
			if (group == 1) {
				/*
				 * matches an unnamed subroutine reference
				 * "(?[-+]n)" (form 0)
				 * 
				 * group: number (including [-+] to make it relative)
				 * (1 group)
				 */

				// System.out.println("Subroutine: " + matcher.group(group));

				if (!inCharClass()) {
					refactorSubroutine(!FOR_NAME);
				}
			} else if (group == 2) {
				/*
				 * matches a named subroutine reference
				 * "(?&group)" (form 0)
				 * "(?P>group)" (form 1)
				 * 
				 * group: group name
				 * group + 1: occurrence
				 * (2 group)
				 */

				if (!inCharClass()) {
					refactorSubroutine(FOR_NAME);
				}
			} else if (group == 4) {
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
			} else if (group >= 6 && group <= 8) {
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
			} else if (group == 9) {
				/*
				 * matches an unnamed capture group
				 * "(" - not followed by a "?"
				 * 
				 * group: everything
				 * (1 group)
				 */

				if (!inCharClass())
					refactorCaptureGroup(!FOR_NAME);
			} else if (group >= 10 && group <= 13) {
				int startGroup = 10;
				int groupCount = 4;
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

				if (!inCharClass() || group == startGroup) {
					int form = group - startGroup;
					int digitGroup = startGroup + groupCount - 1;
					refactorBackreference(!FOR_NAME, form, digitGroup);
				}
			} else if (group >= 14 && group <= 29) {
				int startGroup = 14;
				int groupCount = 16;
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
					int form = (group - startGroup) / 3;
					int digitGroup = startGroup + groupCount - 1;
					refactorBackreference(FOR_NAME, form, digitGroup);
				}
			} else if (group == 30) {
				/*
				 * matches an assert condition
				 * "(?(?=)", "(?(?!)", "(?(?<=)", or "(?(?<!)"
				 * 
				 * group: the assert part (inside the parentheses)
				 * (1 group)
				 */

				if (!inCharClass())
					refactorAssertCondition();
			} else if (group == 31) {
				/*
				 * matches a conditional pattern (by number)
				 * "(?(n)" or "(?(-n)" (form 0)
				 * 
				 * group: the number
				 * (1 group)
				 */

				if (!inCharClass())
					refactorConditionalPattern(!FOR_NAME);
			} else if (group >= 32 && group <= 37) {
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
			} else if (group == 38) {
				/*
				 * matches a "branch reset" subpattern "(?|"
				 * 
				 * group: everything
				 * (1 group)
				 */

				if (!inCharClass()) {
					refactorBranchReset();
				}
			} else if (group == 39) {
				/*
				 * matches an unbounded numeric range
				 * such as "(?Z[<1.234])"
				 * 
				 * group: "Z" or "NZ"
				 * group + 1: comparison (such as "<")
				 * group + 2: value (such as "1.234")
				 * (3 groups)
				 */

				if (!inCharClass()) {
					String mode = matcher.group(group);
					String operator = matcher.group(group + 1);
					Comparison comparison = Comparison.valueOf(operator
							.contains("<"), operator.contains("="));
					replaceWith(nonCaptureGroup(PatternRange.unboundedRange(
							comparison, matcher.group(group + 2), new RangeMode(mode))));
				}
			} else if (group == 42) {
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
		matcher = RefactorUtility.afterRefactor.matcher(result);
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
			int mappedIndex = pattern.getMappedIndex(mappingName);

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

		// int groupCount = groupCount(groupName);
		int groupCount = getGroupCount(groupName);
		StringBuilder acceptAny = new StringBuilder();
		StringBuilder previousGroups = new StringBuilder();

		for (int i = 1; i <= groupCount; i++) {
			String tmpMappingName = getMappingName(groupName, i);
			int testingGroup = getTestingGroup(tmpMappingName);
			int mappedIndex = pattern.getMappedIndex(tmpMappingName);

			if (invalidForwardReference(mappedIndex))
				continue;

			acceptAny.append(previousGroups).append("\\").append(
					mappedIndex).append('|');

			previousGroups.append(RefactorUtility
					.failTestingGroup(testingGroup));
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

		// int groupCount = groupCount(groupName);
		int groupCount = getGroupCount(groupName);
		StringBuilder acceptAnyBranch = new StringBuilder();

		for (int i = 1; i <= groupCount; i++) {
			int mappedIndex = pattern.getMappedIndex(groupName, i);

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

		// int groupCount = groupCount(groupName);
		int groupCount = pattern.getGroupCount(groupName);
		StringBuilder acceptAny = new StringBuilder();

		// System.out.println(groupName + "\t" + groupCount);

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
		if (!inSubroutine())
			pattern.getGroupMapping().put(mappingName, targetGroupIndex);
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
		// TODO: verify this is correct
		if (!inSubroutine())
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
	 * @param index
	 *            the index
	 * @param groupCount
	 *            the group count
	 * @return the absolute group number associated with the match
	 */
	private String getAbsoluteGroup(String index, int groupCount)
	{
		boolean startsWithPlus = index.charAt(0) == '+';

		int groupIndex = parseInt(startsWithPlus ? index.substring(1) : index);

		if (startsWithPlus) {
			groupIndex += groupCount;
		} else if (groupIndex < 0) {
			groupIndex = getAbsoluteGroupIndex(groupIndex,
					groupCount);

			if (groupIndex == -1)
				return RefactorUtility.neverUsedMappingName();

			if (has(DOTNET_NUMBERING))
				return getPerlGroup(groupIndex);
		}

		// System.out.println(index + ":" + groupCount + " -> " + groupIndex);

		return getMappingName(groupIndex, 0);
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
		if (groupName.startsWith("[") && groupName.endsWith("]")) {
			// Remove "[" and "]"
			groupName = groupName.substring(1, groupName.length() - 1);

			boolean startsWithPlus = groupName.startsWith("+");
			int groupIndex = parseInt(startsWithPlus ? groupName.substring(1) : groupName);

			if (startsWithPlus) {
				groupIndex += currentGroup;
			} else if (groupIndex < 0) {
				groupIndex = getAbsoluteGroupIndex(groupIndex,
						currentGroup);

				if (groupIndex == -1)
					return neverUsedMappingName();

				if (has(DOTNET_NUMBERING)) {
					String tmpGroupName = getPerlGroup(groupIndex);

					if (tmpGroupName.charAt(0) == '[') {
						return anyGroupName(tmpGroupName);
					} else
						return groupName;
					// TODO: what should "else" case return?
					// previously groupName was returned (sounds incorrect)
				}
			}

			return wrapIndex(groupIndex);
		}

		return groupName;
	}

	private String getAbsoluteNamedGroup(String groupName,
			String groupOccurrence)
	{
		try {
			int index = parseInt(groupOccurrence);
			int groupCount = getGroupCount(groupName);

			// System.out.println("get abs name: " + groupCount + "\t" + index);
			int occurrence = getAbsoluteGroupIndex(index, groupCount);
			return getMappingName(groupName, occurrence);
		} catch (IndexOutOfBoundsException e) {
			return RefactorUtility.neverUsedMappingName();
		}

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
	 * @param groupName
	 *            the group name whose group index is returned
	 * @param groupOccurrence
	 *            the group occurrence
	 * @return the group index
	 */
	private String getAbsoluteGroup(String groupName, String groupOccurrence)
	{
		if (groupOccurrence == null) {
			// e.g. groupName
			return getMappingName(groupName, 0);
		} else if (groupName.length() == 0) {
			return getAbsoluteGroup(groupOccurrence, currentGroup);
		} else {
			return getAbsoluteNamedGroup(groupName, groupOccurrence);
		}
	}

	/**
	 * Gets the displayed name for the group.
	 * 
	 * @param groupName
	 *            the group name
	 * @param groupOccurrence
	 *            the group occurrence
	 * @return the displayed name for the group
	 */
	private String getDisplayName(String groupName, String groupOccurrence)
	{
		return groupOccurrence == null ? groupName : groupName + "[" + groupOccurrence + "]";
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

		try {
			index = preRefactoringChanges.getOriginalIndex(index);
		} catch (IllegalArgumentException e) {
			index = -1;
		}

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
	 * Gets the group counts.
	 * 
	 * @return the group counts
	 */
	Map<String, Integer> getGroupCounts()
	{
		return groupCounts;
	}

	/**
	 * Returns the group count for the given group name.
	 * 
	 * @param groupName
	 *            the group name
	 * @return the group count for the given group name
	 */
	private int getGroupCount(String groupName)
	{
		// if (groupName.length() == 0)
		// return currentGroup;

		// System.out.println("groupCounts: " + groupCounts);

		Integer groupCount = groupCounts.get(groupName);
		return (groupCount == null ? 0 : groupCount);
	}

	/**
	 * Sets the group count for the given group name.
	 * 
	 * @param groupName
	 *            the group name
	 * @param groupCount
	 *            the group count
	 */
	private void setGroupCount(String groupName, int groupCount)
	{
		if (!inSubroutine())
			groupCounts.put(groupName, groupCount);
	}

	/**
	 * Increases the group count for the given group name by one.
	 * 
	 * @param groupName
	 *            the group name
	 * 
	 * @return the (new) group count for the given group name
	 */
	private int increaseGroupCount(String groupName)
	{
		int groupCount = getGroupCount(groupName) + 1;

		// store the new group count
		setGroupCount(groupName, groupCount);

		return groupCount;
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
		if (matchStates.isEmpty())
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

	private void addSubpattern(String mappingName, Subpattern subpattern)
	{
		// +1 is because the group hasn't been added yet
		// int occurrence = getGroupCount(wrapIndex(currentGroup)) + 1;
		// String mappingName = getMappingName(currentGroup, occurrence);

		// System.out.println("Group (" + mappingName + "): " + parenthesisDepth);

		// Subpattern subpattern = new Subpattern(mappingName);

		// Set to subpattern start
		// (after the captured text, which is the group name / symbols)
		// subpattern.setStart(matcher.end(group));

		if (!inSubroutine()) {
			// subpattern.addSubpatternDependency(mappingName);
			getSubpatterns().put(mappingName, subpattern);
		}
		// openSubpatterns.put(parenthesisDepth, subpattern);
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
		if (duringPreRefactor && !inSubroutine()) {
			// System.out.println("Close: " + parenthesisDepth);

			Subpattern subpattern = openSubpatterns.get(parenthesisDepth);

			if (subpattern != null) {
				openSubpatterns.remove(parenthesisDepth);

				// Don't include end parenthesis
				subpattern.setEnd(matcher.start());
				String subregex = regex.substring(subpattern.getStart(), subpattern.getEnd());
				// System.out.println("Subregex: " + subregex);
				subpattern.setPattern(subregex);
				// subpattern.setPattern(new Refactor(subpattern, subregex).toString());
				// System.out.println("Refactored subpattern: " + subpattern.getPattern());

				/*
				 * Subroutines
				 * 
				 * 0) Run like a subroutine in code, must behave as if the regex jumped to that point in the pattern to
				 * match; if that's not possible in Java, throw an error
				 * 
				 * 1) Relative references should be made absolute
				 */

				// System.out.println("Subpattern: " + subpattern.getPattern());
			}

			// Subpattern subpattern = pattern.getSubpatterns().get();
		}

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
	 * Refactors the subroutine during the refactoring step.
	 * 
	 * @param isNamedGroup
	 *            whether the subroutine is a named group
	 */
	private void refactorSubroutine(boolean isNamedGroup)
	{
		int errorPosition = matcher.start(group);

		String displayName;
		String mappingName;

		// TODO: Test whether group occurrences are supported
		// TODO: Test whether relative group occurrences are supported within subpatterns

		if (isNamedGroup) {
			String groupName = normalizeGroupName(matcher.group(group));
			String groupOccurrence = matcher.group(group + 1);
			int groupOccurenceInt = groupOccurrence == null ? 0 : Integer.parseInt(groupOccurrence);

			// System.out.println("Group name: " + groupName);
			// System.out.println("Group occurrence: " + groupOccurrence);

			displayName = getDisplayName(matcher.group(group), matcher.group(group + 1));
			// mappingName = getAbsoluteGroup(groupName, groupOccurrence);
			mappingName = getMappingName(groupName, groupOccurenceInt);
		} else {
			displayName = getDisplayName(matcher.group(group), null);
			mappingName = getAbsoluteGroup(matcher.group(group), currentGroup);
		}

		if (mappingName.startsWith("[0]"))
			throw error(ZERO_REFERENCE, errorPosition);

		// TODO: mimics PCRE, by using first occurrence
		if (isAnyGroup(mappingName))
			mappingName = anyGroupName(mappingName) + "[1]";

		// System.out.println("Subroutine: " + mappingName);
		// System.out.println("Mapped index: " + pattern.getMappedIndex(mappingName));
		// System.out.println("Pattern: " + pattern);
		// System.out.println("Group mapping: " + pattern.getGroupMapping());

		if (pattern.getMappedIndex(mappingName) == null)
			throw error(NONEXISTENT_SUBPATTERN, errorPosition);

		@SuppressWarnings("hiding")
		Subpattern subpattern = getSubpatterns().get(mappingName);

		if (subpattern == null)
			throw error(INVALID_SUBROUTINE, errorPosition);

		// System.out.println("Subpattern pattern: " + subpattern.getPattern());

		// if (!subpattern.addSubpatternDependency(mappingName))
		// throw error(CIRCULAR_SUBROUTINE, matcher.start(group));

		subpattern.addSubpatternDependency(mappingName);

		try {
			// Uses an atomic group, same as PCRE
			replaceWith("(?>" + subpattern.getPattern(flags) + ")");
		} catch (PatternSyntaxException e) {
			// TODO: use mapping name, if contains relative reference
			PatternSyntaxException error = error(e.getDescription(), errorPosition,
					"Subroutine: \"" + displayName + "\"" + newLine + e.getMessage());

			error.setStackTrace(e.getStackTrace());

			throw error;
		}

	}

	/**
	 * Modify the {@link #flags} variable to account for a change in the
	 * flags (some of flags may be ignored).
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

		if (onFlags.contains("o"))
			flags |= PERL_OCTAL;

		if (onFlags.contains("v"))
			flags |= VERIFY_GROUPS;

		// Added to keep track of all inline flags (required for subroutines)
		if (onFlags.contains("i"))
			flags |= CASE_INSENSITIVE;

		if (onFlags.contains("s"))
			flags |= DOTALL;

		if (onFlags.contains("J"))
			flags |= DUPLICATE_NAMES;

		if (onFlags.contains("n"))
			flags |= EXPLICIT_CAPTURE;

		if (onFlags.contains("m"))
			flags |= MULTILINE;

		if (onFlags.contains("u"))
			flags |= UNICODE_CASE;

		if (offFlags != null) {
			if (offFlags.contains("x"))
				flags &= ~COMMENTS;

			if (offFlags.contains("d"))
				flags &= ~UNIX_LINES;

			if (offFlags.contains("o"))
				flags &= ~PERL_OCTAL;

			if (offFlags.contains("v"))
				flags &= ~VERIFY_GROUPS;

			// Added to keep track of all inline flags (required for subroutines)
			if (offFlags.contains("i"))
				flags &= ~CASE_INSENSITIVE;

			if (offFlags.contains("s"))
				flags &= ~DOTALL;

			if (offFlags.contains("J"))
				flags &= ~DUPLICATE_NAMES;

			if (offFlags.contains("n"))
				flags &= ~EXPLICIT_CAPTURE;

			if (offFlags.contains("m"))
				flags &= ~MULTILINE;

			if (offFlags.contains("u"))
				flags &= ~UNICODE_CASE;
		}
	}

	private String replaceFlags(String onFlags, String offFlags)
	{
		boolean flagsChanged = false;
		StringBuilder newFlags = new StringBuilder(matcher.end() -
				matcher.start());

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

		// System.out.println("replace: " + onFlags + "-" + offFlags + " >> " + newFlags);

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

		// Ensure that all illegal flags are removed
		boolean flagsChanged = false;
		StringBuilder newFlags = new StringBuilder(matcher.end() -
				matcher.start());

		if (onFlags.contains("o")) {
			onFlags = onFlags.replace("o", "");
			flags |= PERL_OCTAL;
			flagsChanged = true;
		}

		if (onFlags.contains("v")) {
			onFlags = onFlags.replace("v", "");
			flags |= VERIFY_GROUPS;
			flagsChanged = true;
		}

		newFlags.append(onFlags);

		if (offFlags != null) {
			if (offFlags.contains("o")) {
				offFlags = offFlags.replace("o", "");
				flags &= ~PERL_OCTAL;
				flagsChanged = true;
			}

			if (offFlags.contains("v")) {
				offFlags = offFlags.replace("v", "");
				flags &= ~VERIFY_GROUPS;
				flagsChanged = true;
			}

			if (offFlags.length() != 0)
				newFlags.append('-').append(offFlags);
		}

		newFlags = flagsChanged ? newFlags : null;

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
	 * Refactors a capturing group during the pre-refactoring step
	 * 
	 * @param isNamedGroup
	 *            whether the capture group is a named group
	 * @param form
	 *            the capture group's 0-based form
	 */
	private void preRefactorCaptureGroup(boolean isNamedGroup, int form)
	{
		// TODO: verify this works fully

		// if (inSubroutine())
		// throw error("Subroutines do not support capture groups", matcher.start(group));

		increaseParenthesisDepth(DURING_PREREFACTOR);

		if (!isNamedGroup && has(EXPLICIT_CAPTURE)) {
			replaceWith(startNonCaptureGroup());
			return;
		}

		if (inSubroutine())
			return;

		increaseCurrentGroup(DURING_PREREFACTOR);

		Subpattern subpattern;

		if (!inSubroutine()) {
			// Initialize subpattern
			subpattern = new Subpattern(this);
			subpattern.setStart(matcher.end(group));

			// System.out.println("Total groups: " + totalGroups);

			// TODO: include flags and prepend subpattern with them
			subpattern.setFlags(flags);

			// flags are maintained
			openSubpatterns.put(parenthesisDepth, subpattern);
		} else
			subpattern = null;

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
			int occurrence = increaseGroupCount(groupName);

			if (occurrence != 1 && !has(DUPLICATE_NAMES)) {
				throw error(DUPLICATE_NAME, matcher.start(group));
			}

			String mappingName = getMappingName(groupName, occurrence);
			addNamedGroup(mappingName, TARGET_UNKNOWN);

			addSubpattern(mappingName, subpattern);

			if (!inBranchReset()) {
				namedGroup++;
			} else {
				unnamedGroup++;
			}

			// TODO: verify this is correct
			if (!inSubroutine() && has(DOTNET_NUMBERING)) {
				if (!inBranchReset())
					perlGroupMapping().put(currentGroup, mappingName);
				else
					perlGroupMapping().put(currentGroup,
							getMappingName(currentGroup, 0));
			}
		} else {
			unnamedGroup++;

			// TODO: verify this is correct
			if (!inSubroutine() && has(DOTNET_NUMBERING)) {
				String mappingName = getMappingName(unnamedGroup, 0);
				perlGroupMapping().put(currentGroup, mappingName);
			}
		}

		if (!has(DOTNET_NUMBERING) || inBranchReset() || !isNamedGroup) {
			int groupIndex = has(DOTNET_NUMBERING) ? unnamedGroup : currentGroup;

			String groupName = wrapIndex(groupIndex);
			int occurrence = increaseGroupCount(groupName);
			String mappingName = getMappingName(groupIndex, occurrence);

			// add mapping for group index
			addUnnamedGroup(mappingName, TARGET_UNKNOWN);
			addSubpattern(mappingName, subpattern);
		}

		// TODO: what is named group in branch reset pattern??

		/*
		 * TODO: how to add numeric subpattern for case of
		 * 1) has(DOTNET_NUMBERING)
		 * 2) !inBranchReset()
		 * 3) isNamedGroup
		 */

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
		boolean missingTerminator = form < endings.length() &&
				!matcher.group(group).endsWith(
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
		boolean missingName = matcher.start(group + 1) == matcher.end(group + 1) &&
				matcher.start(group + 2) == -1;

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
			int occurrence = increaseGroupCount(groupName);
			namedMappingName = getMappingName(groupName, occurrence);

			// add mapping for group name
			addNamedGroup(namedMappingName, totalGroups);

			usedInCondition = usedInCondition(namedMappingName) ||
					usedInCondition(getMappingName(groupName, 0));
		} else {
			usedInCondition = false;
			namedMappingName = null;
		}

		int groupIndex = getCurrentGroup(isNamedGroup && !inBranchReset());
		String groupName = wrapIndex(groupIndex);
		int occurrence = increaseGroupCount(groupName);
		String mappingName = getMappingName(groupName, occurrence);

		// add mapping for group index
		addUnnamedGroup(mappingName, totalGroups);

		// TODO: uncomment to use for debugging
		// System.out.printf("%s (%s): %s%n", mappingName, totalGroups, getSubpatterns().get(mappingName));

		if (!usedInCondition) {
			usedInCondition = usedInCondition(mappingName) ||
					usedInCondition(getMappingName(groupName, 0));

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
				parenthesisDepth, namedMappingName));
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
	 * Returns the unwrapped group index.
	 * 
	 * <p>If <code>groupIndex</code> is surrounded by square brackets, they are
	 * removed, and the group index is returned. Otherwise, the given group
	 * index is
	 * returned unmodified.</p>
	 * 
	 * @param groupIndex
	 *            the group index
	 * @return the unwrapped index
	 */
	static String unwrapIndex(String groupIndex)
	{
		if (groupIndex.charAt(0) == '[' &&
				groupIndex.charAt(groupIndex.length() - 1) == ']') {
			return groupIndex.substring(1, groupIndex.length() - 1);
		}

		return groupIndex;
	}

	/**
	 * Refactors a conditional pattern during the pre-refactoring step
	 * 
	 * @param isNamedGroup
	 *            whether the conditional is a name or number
	 * @param form
	 *            the 0-based form for the conditional
	 */
	private void preRefactorConditionalPattern(boolean isNamedGroup, int form)
	{
		if (inSubroutine())
			throw error("Subroutines do not support named/unnamed conditionals", matcher.start(group));

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
					mappingName = getAbsoluteGroup(groupName,
							groupOccurrence);
				} else {
					// reference is a named group
					// (occurrence is ignored)

					mappingName = tmpMappingName;
				}
			} else {
				// named group

				String groupOccurrence = matcher.group(group + 2);
				mappingName = getAbsoluteGroup(groupName, groupOccurrence);
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
		String groupName = normalizeGroupName(isNamedGroup ? matcher
				.group(group) : "[" + matcher.group(group) + "]");

		// start of groupName / number
		int start = matcher.start(group);

		if (groupName.equals("[0]"))
			throw error(INVALID_CONDITION0, start);

		String groupOccurrence = isNamedGroup ? matcher.group(group + 1)
				: null;

		String mappingName = getAbsoluteGroup(groupName, groupOccurrence);

		Integer mappingIndexI = pattern.getMappedIndex(mappingName);
		Integer testConditionGroupI = getTestingGroup(mappingName);

		// System.out.println("refactorConditionalPattern: " + mappingName);

		if (isAnyGroup(mappingName)) {
			// int groupCount = groupCount(groupName);
			// int groupCount = getGroupCount(groupName);
			int groupCount = pattern.getGroupCount(groupName);

			// TODO: verify this change is valid
			// modified to add support for +x on conditionals (can be used in subroutines)

			if (groupCount == 0) {
				if (groupName.equals("DEFINE")) {
					// (?(DEFINE)...) is a special condition, which should always be false
					// (allows defining subpatterns, without them being matched at that point)
					// TODO: is it possible to completely remove DEFINE group from internal pattern ??
					// TODO: ensure DEFINE group has no alternations - otherwise, throw error
					replaceWith(startNonCaptureGroup() + fail());
				} else {
					// if (has(VERIFY_GROUPS))
					throw error(NONEXISTENT_SUBPATTERN, start);
				}

				// the specified group doesn't exist
				// replaceWith(startNonCaptureGroup() + fail());
			} else if (!allDone(groupName, groupCount)) {
				// System.out.println("Some groups occur later on: " + mappingName);

				// some groups occur later on
				replaceWith(startNonCaptureGroup() + "\\g{" +
						addErrorTrace(start) + "test-" + mappingName + "}");
				testConditionGroupI = TARGET_UNKNOWN;
			} else {
				// System.out.println("All groups have already occurred: " + mappingName);

				// all groups have already occurred
				replaceWith(startNonCaptureGroup() +
						acceptTestingGroup(mappingName));
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
			replaceWith(startNonCaptureGroup() + "\\g{" +
					addErrorTrace(start) + "test-" + mappingName + "}");
			testConditionGroupI = TARGET_UNKNOWN;
		} else {
			// the specified group has already occurred
			replaceWith(startNonCaptureGroup() +
					RefactorUtility.acceptTestingGroup(testConditionGroupI));
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

		if (!RefactorUtility.isUnnamedGroup(groupName))
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
			mappingName = getAbsoluteGroup(groupName, groupOccurrence);
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

				// TODO: uncomment when debugging
				// if (backreference == null)
				// System.out.println("Null reference: " + matcher.group(group));

				if (backreference == null) {
					// not a back reference (i.e. an octal code)
					// (handled in above function call)
					return;
				}

				groupIndex = Integer.parseInt(backreference.group(1));

				// TODO: verify functionality with DOTNET_NUMBERING
				// Uses if, not while, because getBackreference method checks backreference
				// uses smallest digit for # of digits in current group
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

		// Checked after octal check
		// TODO: verify this works fully
		// if (inSubroutine())
		// throw error("Subroutines do not support backreferences", matcher.start(group));

		trailingDigits += matcher.group(digitGroup);

		// retrieve the actual group index for the specified groupIndex
		Integer mappedIndexI;

		// replace back reference with back reference RegEx
		if (isAnyGroup(mappingName)) {
			String groupName = anyGroupName(mappingName);

			if (groupName.equals("[0]"))
				throw error(ZERO_REFERENCE, start);

			// int groupCount = groupCount(groupName);
			int groupCount = getGroupCount(groupName);

			if (groupCount == 0) {
				// form 0 is \n (for unnamed group)
				if (isNamedGroup || form != 0 || has(VERIFY_GROUPS))
					throw error(NONEXISTENT_SUBPATTERN, start);

				// Used to mimic java functionality
				// Not needed
				// (required because otherwise group could point to a valid capture group, due to added groups)
				// !isNamedGroup, form == 0 ('\n' syntax), and !has(VERIFY_GROUPS)
				replaceWith(fail() + trailingDigits);
			} else if (groupCount == 1) {
				String tmpMappingName = getMappingName(groupName, 1);
				trailingDigits = RefactorUtility
						.fixTrailing(trailingDigits);

				if (getGroupCount(groupName) == groupCount) {
					// group has already occurred
					replaceWith(acceptGroup(tmpMappingName) +
							trailingDigits);
				} else {
					// group occurs later on
					replaceWith("\\g{" + addErrorTrace(start) + "group-" +
							tmpMappingName + "}" + trailingDigits);
				}
			} else if (allDone(groupName, groupCount)) {
				// all groups have already occurred
				String acceptGroup = acceptGroup(mappingName);

				replaceWith(nonCaptureGroup(acceptGroup) + trailingDigits);
			} else {
				// some groups occur later on
				replaceWith(nonCaptureGroup("\\g{" + addErrorTrace(start) +
						"group-" + mappingName + "}") +
						trailingDigits);
			}
		} else if ((mappedIndexI = pattern.getMappedIndex(mappingName)) == null) {
			// if (has(VERIFY_GROUPS))
			throw error(NONEXISTENT_SUBPATTERN, start);

			// replaceWith(fail() + trailingDigits);
		} else {
			int mappedIndex = mappedIndexI;

			trailingDigits = RefactorUtility.fixTrailing(trailingDigits);

			if (mappedIndex == TARGET_UNKNOWN) {
				// group hasn't occurred yet

				replaceWith("\\g{" + addErrorTrace(start) + "group-" +
						mappingName + "}" + trailingDigits);
			} else {
				// group already occurred
				replaceWith("\\" + mappedIndex + trailingDigits);
			}
		}
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
		if (RefactorUtility.isUnnamedGroup(groupName)) {
			// e.g. [1][0]
			return getGroupCount(groupName) == groupCount;
		} else {
			// e.g. groupName[0]
			return getTestingGroup(getMappingName(groupName, groupCount)) != null;
		}
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
			String input = has(PERL_OCTAL) ? backreference : backreference
					.substring(1);

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

		int digitCount = RefactorUtility.digitCount(currentGroup);

		@SuppressWarnings("hiding")
		java.util.regex.Pattern pattern = getDigitCountPattern(digitCount);
		@SuppressWarnings("hiding")
		java.util.regex.Matcher matcher = pattern.matcher(backreference);

		matcher.matches();

		int groupIndex = Integer.parseInt(matcher.group(1));
		String trailing = matcher.group(2);

		if (has(PERL_OCTAL) &&
				(trailing.length() != 0 || digitCount > 1 &&
						groupIndex > currentGroup)) {
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

		replaceWith(startNonCaptureGroup() + startNonCaptureGroup() + "(" +
				matcher.group(group));

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

	/**
	 * Refactors a numeric range during the refactoring step
	 */
	private void refactorNumericRange()
	{
		String mode = matcher.group(group);
		// boolean rawMode = matcher.group(group + 1) != null;
		boolean inclusiveStart = matcher.start(group + 1) == -1;
		String start = matcher.group(group + 2);
		boolean inclusiveEnd = matcher.start(group + 3) == -1;
		String end = matcher.group(group + 4);
		int endRange = matcher.end(group + 4);

		// System.out.println("Numeric range: " + start + "\t" + end);

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

		// TODO: add bug testing

		String range = PatternRange.boundedRange(start, inclusiveStart, end, inclusiveEnd, new RangeMode(mode));
		range = nonCaptureGroup(range);
		replaceWith(range);

		// if (true)
		// return;
		//
		// try {
		// String range = Range.range(start, end, mode);
		//
		// // if (!rawMode)
		// range = nonCaptureGroup(range);
		//
		// replaceWith(range);
		// } catch (PatternSyntaxException e) {
		// String desc = e.getDescription();
		// int index = e.getIndex();
		//
		// if (desc.equals(INVALID_DIGIT_START)) {
		// int startIndex = matcher.start(group + 1);
		// throw error(desc, startIndex + index);
		// } else if (desc.equals(INVALID_DIGIT_END)) {
		// int endIndex = matcher.start(group + 2);
		// throw error(desc, endIndex + index);
		// }
		//
		// } catch (Exception e) {
		// String message = e.getMessage();
		//
		// if (message.equals(INVALID_BASE)) {
		// int errorIndex = matcher.start(group) + mode.indexOf('Z') +
		// 1;
		// throw error(message + " in numeric range", errorIndex);
		// }
		// }
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

	/**
	 * Refactors a unicode character during the after-refactoring step
	 */
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

	/**
	 * Refactors a posix class during the after-refactoring step
	 */
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
				replaceWith("\\" + (negated ? "P" : "p") + "{" + value +
						"}");
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
		} else if (match.equals("{")) {
			// Tracked to prevent Android issue, where '}' must be escaped, but not in Java
			// Only track if not in character class (Android allows '}' in character group)
			if (!inCharClass())
			{
				if (Boolean.FALSE.equals(isInCurlyBrace))
				{
					// If not currently in a curly brace, mark as in one
					isInCurlyBrace = true;
				}
				else
				{
					// If already in a curly brace, the pattern has an error,
					// since shouldn't ever have two open curly braces without a close between them
					// If flag was already null, does nothing
					isInCurlyBrace = null;
				}
			}
		} else if (match.equals("}")) {
			// Tracked to prevent Android issue, where '}' must be escaped, but not in Java
			// Only track if not in character class (Android allows '}' in character group)
			if (!inCharClass())
			{
				if (Boolean.TRUE.equals(isInCurlyBrace))
				{
					// An open curly brace, followed at some point by a close one
					isInCurlyBrace = false;
				}
				else if (Boolean.FALSE.equals(isInCurlyBrace))
				{
					// A closed curly brace by itself (escape it in the internal pattern)
					// (allows compiling regex when doing Android development, which doesn't allow '}' to be unescaped)
					replaceWith("\\}");
				}
			}
		} else if (match.equals("#")) {
			if (has(COMMENTS) && !inCharClass())
				handleStartComments();
			// parsePastLine();
		} else if (match.startsWith("\\Q")) {
			// if (!supportedSyntax(QE_QUOTATION)) {
			if (isJava1_5) {
				int start = 2;
				int end = match.length() - (match.endsWith("\\E") ? 2 : 0);
				replaceWith(literal(match.substring(start, end)));
			}

			if (isInComments)
			{
				checkForLineTerminator();
			}
		} else if (isInComments)
		{
			checkForLineTerminator();
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
				handleStartComments();
			// parsePastLine();
			// } else if (match.equals("\\Q")) {
			// skipQuoteBlock();
			// }
		} else if (match.startsWith("\\Q")) {
			// Skip quote block

			if (isInComments)
			{
				checkForLineTerminator();
			}
		} else if (isInComments) {
			checkForLineTerminator();
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
				handleStartComments();
			// parsePastLine();
			// } else if (match.equals("\\Q")) {
			// skipQuoteBlock();
		} else if (match.startsWith("\\Q")) {
			// Skip quote block

			if (isInComments)
			{
				checkForLineTerminator();
			}
		} else if (match.equals("\\X")) {
			replaceWith("(?>\\P{M}\\p{M}*)");
		} else if (isInComments)
		{
			checkForLineTerminator();
		}
	}

	/**
	 * Pattern to match the end of a quote block (either a "\E" or the end of the regex)
	 */
	private static final java.util.regex.Pattern endQuoteBlockPattern = java.util.regex.Pattern.compile("\\\\E|$");

	// /**
	// * Skips from <code>\Q</code> to <code>\E</code>. If there is no <code>\E</code> the rest of the string is
	// skipped.
	// */
	// private void skipQuoteBlock()
	// {
	// // Store the current pattern (restored at end)
	// java.util.regex.Pattern currentPattern = matcher.pattern();
	//
	// // Find the end of the quote block (thus skipping it)
	// matcher.usePattern(endQuoteBlockPattern).find();
	//
	// // Replace the previous pattern
	// matcher.usePattern(currentPattern);
	// }

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
	 * whereas {@link Pattern#quote(String)} uses a <code>\Q..\E</code> block. This
	 * function is used when refactoring a <code>\Q..\E</code> block into a
	 * RegEx patternSyntax that doesn't support the functionality.</p>
	 * 
	 * @param s
	 *            The string to be literalized
	 * @return A literal string replacement
	 */
	public String literal(String s)
	{
		return Pattern.literal(s, inCharClass()
				? Pattern.REGEX_CHAR_CLASS_METACHARACTERS
				: Pattern.REGEX_METACHARACTERS);
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
					replaceWith("|\\g{" +
							addErrorTrace(matcher.start(group)) + "testF-" +
							mappingName + "})");
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
						replacement.append(RefactorUtility
								.failTestingGroup(testConditionGroup));
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

				replaceWith(")())?+" + startNonCaptureGroup() +
						RefactorUtility.acceptTestingGroup(totalGroups));

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
					replaceWith("|\\g{" +
							addErrorTrace(matcher.start(group)) + "testF-" +
							mappingName + "}");
					// } else if (testConditionGroup == BRANCH_RESET) {
					// // all groups have already occurred
					// replaceWith("|" + failTestingGroup(mappingName));
				} else if (testConditionGroup != 0) {
					// specific group

					replaceWith("|" +
							RefactorUtility
									.failTestingGroup(testConditionGroup));
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

		boolean negSequareBracket = end < length - 2 &&
				text.substring(end, end + 2).equals("^]");

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

	/**
	 * Indicates whether currently in a character class.
	 * 
	 * @return <code>true</code>, if in a character class
	 */
	private boolean inCharClass()
	{
		return charClassDepth != 0;
	}

	/**
	 * Pattern to match a unix line separator (or the end of the pattern)
	 */
	private static final java.util.regex.Pattern unixLineSeparatorPattern = java.util.regex.Pattern.compile("\\n++|$");

	/**
	 * Pattern to match a line separator (based on line separators specified at
	 * http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#lt); also matches the end of the pattern
	 */
	private static final java.util.regex.Pattern lineSeparatorPattern = java.util.regex.Pattern
			.compile("[\\n\\r\u0085\u2028\u2029]++|$");

	/**
	 * Sets the parser "cursor" at the end of the current line.
	 */
	private void parsePastLine()
	{
		// Save the current pattern (restored at end)
		java.util.regex.Pattern currentPattern = matcher.pattern();

		// Causes issues in Android (for some unknown reason)
		// (causes garbage collection to run wild if using comments
		// if comments contain named capture group, incorrectly thinks there's two)
		// (XXX: something is really up)
		if (has(UNIX_LINES))
		{
			matcher.usePattern(unixLineSeparatorPattern);
		}
		else
		{
			matcher.usePattern(lineSeparatorPattern);
		}

		// Skip past the current line
		matcher.find();

		// Restore the previous pattern
		matcher.usePattern(currentPattern);
	}

	/**
	 * Handle the start comments "#" when {@link Pattern#COMMENTS} is enabled
	 */
	private void handleStartComments()
	{
		isInComments = true;
	}

	/**
	 * Check for a line terminator and if one is found, end the comment block
	 */
	private void checkForLineTerminator()
	{
		if (has(UNIX_LINES))
		{
			if (match.contains("\n"))
			{
				isInComments = false;
			}
		}
		else if (match.contains("\n") || match.contains("\r") || match.contains("\u0085")
				|| match.contains("\u2028") || match.contains("\u2029"))
		{
			// Line terminator [\n\r\u0085\u2028\u2029]
			isInComments = false;
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
		return !branchReset.isEmpty();
	}

	/**
	 * <p>Replace the matched string with the specified (literal)
	 * replacement, and adds a new state to {@link #differences}.
	 * </p>
	 * 
	 * @param replacement
	 *            the replacement
	 */
	private void replaceWith(String replacement)
	{
		String quoteReplacement = Matcher.quoteReplacement(replacement);

		matcher.appendReplacement(result, quoteReplacement);

		// int length = matcher.end() - matcher.start();
		int start = result.length() - quoteReplacement.length();
		// int end = start + length;

		differences.replace0(start, match, replacement);

		// java.util.regex.Matcher
		// return matcher;
	}

	/**
	 * add javadoc comments.
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
		@Override
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
			return mappingName + " -> (" + testConditionGroupI + "): " +
					parenthesisDepth;
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
		 * @param unnamedGroups
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
		@Override
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
		 * @param unnamed
		 *            the new unnamed end group
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
			return super.toString();
		}
	}

	private static class AddTestGroupState implements State
	{
		/** The mapping name. */
		String mappingName;

		/** The named mapping name. */
		String namedMappingName;

		/** The parenthesis depth. */
		int parenthesisDepth;

		/**
		 * Instantiates a new AddTestGroupState.
		 * 
		 * @param mappingName
		 *            the mapping name
		 * @param parenthesisDepth
		 *            the parenthesis depth
		 * @param namedMappingName
		 *            the named mapping name
		 */
		AddTestGroupState(String mappingName, int parenthesisDepth,
				String namedMappingName)
		{
			this.mappingName = mappingName;
			this.namedMappingName = namedMappingName;
			this.parenthesisDepth = parenthesisDepth;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
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
}