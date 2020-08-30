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
import static info.codesaway.util.regex.Pattern.UNICODE_CHARACTER_CLASS;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import info.codesaway.util.Differences;

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
class Refactor {
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
	private final Map<String, Integer> testConditionGroups = new HashMap<>(2);

	/**
	 * Set of group names that have an "any group" back reference
	 */
	private final Set<String> anyGroupReferences = new HashSet<>(2);

	/** The capture groups that require a testing group. */
	private final TreeSet<String> requiresTestingGroup = new TreeSet<>();

	/**
	 * A stack of states used to track when a testing group should be added
	 * to a capture group.
	 */
	private final Stack<AddTestGroupState> addTestGroup = new Stack<>();

	/**
	 * A stack of states used to track the else branch of a conditional
	 * subpattern.
	 */
	private final Stack<MatchState> handleElseBranch = new Stack<>();

	/**
	 * A stack of states used to track the end of the assertion in an assert
	 * conditional
	 */
	private final Stack<MatchState> handleEndAssertCond = new Stack<>();

	/**
	 * A stack of states used to track the branches in a branch reset
	 * subpattern.
	 */
	private final Stack<BranchResetState> branchReset = new Stack<>();

	/**
	 * A stack used to store the current flags.
	 *
	 * <p>
	 * When entering a new parenthesis grouping, the current flags are
	 * saved. This value is later restored upon existing the parenthesis
	 * grouping.
	 * </p>
	 */
	private final Stack<Integer> flagsStack = new Stack<>();

	/**
	 * A map with mappings from group name to the group count for
	 * that group name.
	 */
	private Map<String, Integer> groupCounts = new HashMap<>(2);

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
	private final Map<Integer, Subpattern> openSubpatterns = new HashMap<>();

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
	void reset() {
		// flags = pattern.flags();
		this.flags = this.initializeFlags();
		this.parenthesisDepth = 0;
		this.charClassDepth = 0;
		this.currentGroup = 0;
		this.totalGroups = 0;
		this.namedGroup = 0;
		this.unnamedGroup = 0;
		this.isInComments = false;

		this.result = new StringBuffer();

		this.addTestGroup.clear();
		this.handleElseBranch.clear();
		this.branchReset.clear();
		this.flagsStack.clear();

		if (this.inSubroutine()) {
			this.groupCounts = this.subpattern.getGroupCounts();
		} else {
			this.groupCounts.clear();
		}

		this.changes.addAll(this.differences);
		this.differences = new Differences();
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
	private PatternSyntaxException error(final String errorMessage, final int index) {
		return this.error(errorMessage, index, null);
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
	private PatternSyntaxException error(final String errorMessage, final int index, final String additionalDetails) {
		int originalIndex;

		try {
			originalIndex = this.changes.getOriginalIndex(index);
		} catch (IllegalArgumentException e) {
			originalIndex = -1;
		}

		return new PatternSyntaxException(errorMessage, this.regex, originalIndex, additionalDetails);
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
	Refactor(final Pattern pattern) {
		this(pattern, pattern.pattern(), null);
	}

	Refactor(final Subpattern subpattern, final String regex) {
		this(subpattern.getParentPattern(), regex, subpattern);
	}

	private Refactor(final Pattern pattern, final String regex, final Subpattern subpattern) {
		this.pattern = pattern;
		this.regex = regex;
		this.subpattern = subpattern;
		// this.refactor = null;

		// this.flags = pattern.flags();

		this.flags = this.initializeFlags();

		// System.out.println("Flags: " + new PatternFlags(flags));

		this.subpatterns = subpattern != null ? subpattern.getParentSubpatterns() : new HashMap<>();

		// if (has(LITERAL)) {
		// this.result = new StringBuffer(regex);
		// initializeForZeroGroups();
		// return;
		// }

		String javaVersion = System.getProperty("java.version");
		this.isJava1_5 = naturalCompareTo(javaVersion, "1.5.0") >= 0 && naturalCompareTo(javaVersion, "1.6.0") < 0;

		// if (!inSubroutine()) {
		this.result = new StringBuffer();
		this.preRefactor();
		this.preRefactoringChanges = this.differences;
		// } else
		// this.result = new StringBuffer(regex);

		this.refactor();
		this.afterRefactor();
	}

	private int initializeFlags() {
		return this.subpattern != null ? this.subpattern.getFlags() : this.pattern.flags();
	}

	/**
	 * Returns the current result of the refactoring.
	 */
	@Override
	public String toString() {
		return this.result.toString();
	}

	/**
	 * Gets the subpatterns.
	 *
	 * @return the subpatterns
	 */
	Map<String, Subpattern> getSubpatterns() {
		return this.subpatterns;
	}

	/**
	 * Indicates whether the refactoring is being done within a subroutine
	 *
	 * @return <code>true</code> if currently refactoring a subroutine
	 */
	private boolean inSubroutine() {
		return this.subpattern != null;
	}

	/**
	 * Gets the pattern.
	 *
	 * @return the pattern
	 */
	Pattern getPattern() {
		return this.pattern;
	}

	/**
	 * Indicates whether a particular flag is set or not.
	 *
	 * @param f
	 *            the flag to test
	 * @return <code>true</code> if the particular flag is set
	 */
	boolean has(final int f) {
		return (this.flags & f) != 0;
	}

	/**
	 * Returns the group index for the first non-null group.
	 *
	 * @param matcher
	 *            the matcher
	 * @return the group index for the first non-null group
	 */
	static int getUsedGroup(final java.util.regex.Matcher matcher) {
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
	private boolean loopSetup() {
		this.match = this.matcher.group();
		this.group = getUsedGroup(this.matcher);

		if (this.isInComments) {
			if (this.match.startsWith("(?#") && !this.inCharClass()) {
				// Comment block
				return false;
			} else if (this.match.startsWith("\\Q")) {
				// Quote block
				return false;
			} else if (this.match.equals("\n") || this.match.equals("\r") || this.match.equals("\u0085")
					|| this.match.equals("\u2028") || this.match.equals("\u2029")) {
				// Line terminator [\n\r\u0085\u2028\u2029]
				return false;
			} else {
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
	void setCapturingGroupCount(final int capturingGroupCount) {
		if (!this.inSubroutine()) {
			this.pattern.setCapturingGroupCount(capturingGroupCount);
		}
	}

	/**
	 * @param addedGroups
	 * @since 0.2
	 */
	void setAddedGroups(final boolean addedGroups) {
		if (!this.inSubroutine()) {
			this.pattern.setAddedGroups(addedGroups);
		}
	}

	/**
	 * Sets the group name counts.
	 *
	 * @param groupNameCounts
	 *            the group name counts
	 */
	void setGroupNameCounts(final Map<String, Integer> groupNameCounts) {
		if (!this.inSubroutine()) {
			this.pattern.setGroupCounts(groupNameCounts);
		}
	}

	/**
	 * Steps taken to prepare the regular expression to be refactored
	 */
	private void preRefactor() {
		this.text = this.regex;
		this.matcher = RefactorUtility.preRefactor.matcher(this.regex);

		// add a map from group 0 to group 0
		// TODO: add occurrence??
		this.addUnnamedGroup("[0][1]", 0);
		this.setGroupCount("[0]", 1);

		while (this.matcher.find()) {
			if (this.loopSetup()) {
				continue;
			}

			if (this.group == 1) {
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

				if (!this.inCharClass()) {
					this.preRefactorFlags();
				}
			} else if (this.group >= 3 && this.group <= 8) {
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

				if (!this.inCharClass()) {
					int form = (this.group - 3) / 2;
					this.preRefactorCaptureGroup(FOR_NAME, form);
				}
			} else if (this.group == 9) {
				/*
				 * matches an unnamed capture group "("
				 * - not followed by a "?" (or a '*', used by verbs)
				 *
				 * group: everything (1 group)
				 */

				if (!this.inCharClass()) {
					int form = this.group - 9;
					this.preRefactorCaptureGroup(!FOR_NAME, form);
				}
			} else if (this.group >= 10 && this.group <= 14) {
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

				if (!this.inCharClass()) {
					this.preRefactorBackreference();
				}
			} else if (this.group == 15) {
				/*
				 * matches an assert condition
				 * "(?(?=)", "(?(?!)", "(?(?<=)", or "(?(?<!)"
				 *
				 * group: the assert part (inside the parentheses)
				 * (1 group)
				 */

				if (!this.inCharClass()) {
					this.preRefactorAssertCondition();
				}
			} else if (this.group == 16) {
				/*
				 * matches a reference condition (by number) matches
				 * "(?(n)" or "(?(-n)"
				 *
				 * group: the number (1 group)
				 */

				if (!this.inCharClass()) {
					int form = this.group - 16;
					this.preRefactorConditionalPattern(!FOR_NAME, form);
				}
			} else if (this.group >= 17 && this.group <= 25) {
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

				if (!this.inCharClass()) {
					int form = (this.group - 17) / 3;
					this.preRefactorConditionalPattern(FOR_NAME, form);
				}
			} else if (this.group == 26) {
				/*
				 * matches comment group
				 * "(?#comment) - comment cannot contain ")"
				 *
				 * group: everything
				 * (1 group)
				 */

				if (!this.inCharClass()) {
					if (!this.match.endsWith(")")) {
						throw this.error(UNCLOSED_COMMENT, this.matcher.end());
					}

					if (this.isInComments) {
						this.checkForLineTerminator();
					}

					// remove (in internal pattern)
					this.replaceWith("");
				}
			} else if (this.group == 27) {
				/*
				 * matches a "branch reset" subpattern "(?|"
				 *
				 * group: everything
				 * (1 group)
				 */

				if (!this.inCharClass()) {
					this.preRefactorBranchReset();
				}
			} else if (this.group == 28) {
				/*
				 * FAIL verb (from PCRE) - always fails
				 * (*FAIL) or (*F) - case sensitive
				 *
				 * synonym for (?!)
				 */

				if (!this.inCharClass()) {
					this.replaceWith(fail());
				}
			} else {
				this.preRefactorOthers();
			}
		}

		this.unnamedGroupCount = this.unnamedGroup;
		this.setCapturingGroupCount(this.currentGroup);
		this.setGroupCount("", this.currentGroup);
		this.setAddedGroups(this.totalGroups != this.currentGroup);

		if (this.has(DOTNET_NUMBERING)) {
			// for each named group, mark the group count as 1
			// for its respective number group
			// (doesn't affect named groups in "branch reset" patterns)

			for (int i = 1; i <= this.namedGroup; i++) {
				int groupIndex = this.unnamedGroupCount + i;
				this.setGroupCount("[" + groupIndex + "]", 1);
			}

			int currentNamedGroup = 0;

			for (Entry<Integer, String> entry : this.perlGroupMapping().entrySet()) {
				String mappingName = entry.getValue();

				if (mappingName.charAt(0) != '[') {
					// a named group - using mapping name of unnamed group

					currentNamedGroup++;
					String groupName = wrapIndex(this.unnamedGroupCount + currentNamedGroup);

					mappingName = getMappingName(groupName, 0);
					entry.setValue(mappingName);
				}
			}
		}

		this.setGroupNameCounts(new HashMap<>(this.groupCounts));

		for (String groupName : this.anyGroupReferences) {
			// if (groupCount(groupName) != 1)
			// System.out.println("getGroupCount: " + groupName);

			if (this.getGroupCount(groupName) != 1) {
				this.requiresTestingGroup.add(getMappingName(groupName, 0));
			}
		}

		this.matcher.appendTail(this.result);
	}

	/**
	 * Refactors the regular expression
	 */
	private void refactor() {
		this.text = this.result.toString();
		// matcher = getRefactorPattern().matcher(result);
		this.matcher = RefactorUtility.refactor.matcher(this.result);
		this.reset();

		while (this.matcher.find()) {
			if (this.loopSetup()) {
				continue;
			}

			// System.out.println("group: " + group + "\t" + matcher.group());
			if (this.group == 1) {
				/*
				 * matches an unnamed subroutine reference
				 * "(?[-+]n)" (form 0)
				 *
				 * group: number (including [-+] to make it relative)
				 * (1 group)
				 */

				// System.out.println("Subroutine: " + matcher.group(group));

				if (!this.inCharClass()) {
					this.refactorSubroutine(!FOR_NAME);
				}
			} else if (this.group == 2) {
				/*
				 * matches a named subroutine reference
				 * "(?&group)" (form 0)
				 * "(?P>group)" (form 1)
				 *
				 * group: group name
				 * group + 1: occurrence
				 * (2 group)
				 */

				if (!this.inCharClass()) {
					this.refactorSubroutine(FOR_NAME);
				}
			} else if (this.group == 4) {
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

				if (!this.inCharClass()) {
					this.refactorFlags();
				}
			} else if (this.group >= 6 && this.group <= 8) {
				/*
				 * matches a named capture group
				 * "(?<name>" (form 0)
				 * "(?'name'" (form 1)
				 * "(?P<name>" (form 2)
				 *
				 * group: the name
				 * (3 groups)
				 */

				if (!this.inCharClass()) {
					this.refactorCaptureGroup(FOR_NAME);
				}
			} else if (this.group == 9) {
				/*
				 * matches an unnamed capture group
				 * "(" - not followed by a "?"
				 *
				 * group: everything
				 * (1 group)
				 */

				if (!this.inCharClass()) {
					this.refactorCaptureGroup(!FOR_NAME);
				}
			} else if (this.group >= 10 && this.group <= 13) {
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

				if (!this.inCharClass() || this.group == startGroup) {
					int form = this.group - startGroup;
					int digitGroup = startGroup + groupCount - 1;
					this.refactorBackreference(!FOR_NAME, form, digitGroup);
				}
			} else if (this.group >= 14 && this.group <= 29) {
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

				if (!this.inCharClass()) {
					int form = (this.group - startGroup) / 3;
					int digitGroup = startGroup + groupCount - 1;
					this.refactorBackreference(FOR_NAME, form, digitGroup);
				}
			} else if (this.group == 30) {
				/*
				 * matches an assert condition
				 * "(?(?=)", "(?(?!)", "(?(?<=)", or "(?(?<!)"
				 *
				 * group: the assert part (inside the parentheses)
				 * (1 group)
				 */

				if (!this.inCharClass()) {
					this.refactorAssertCondition();
				}
			} else if (this.group == 31) {
				/*
				 * matches a conditional pattern (by number)
				 * "(?(n)" or "(?(-n)" (form 0)
				 *
				 * group: the number
				 * (1 group)
				 */

				if (!this.inCharClass()) {
					this.refactorConditionalPattern(!FOR_NAME);
				}
			} else if (this.group >= 32 && this.group <= 37) {
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

				if (!this.inCharClass()) {
					this.refactorConditionalPattern(FOR_NAME);
				}
			} else if (this.group == 38) {
				/*
				 * matches a "branch reset" subpattern "(?|"
				 *
				 * group: everything
				 * (1 group)
				 */

				if (!this.inCharClass()) {
					this.refactorBranchReset();
				}
			} else if (this.group == 39) {
				/*
				 * matches an unbounded numeric range
				 * such as "(?Z[<1.234])"
				 *
				 * group: "Z" or "NZ"
				 * group + 1: comparison (such as "<")
				 * group + 2: value (such as "1.234")
				 * (3 groups)
				 */

				if (!this.inCharClass()) {
					String mode = this.matcher.group(this.group);
					String operator = this.matcher.group(this.group + 1);
					Comparison comparison = Comparison.valueOf(operator.contains("<"), operator.contains("="));

					int endRange = this.matcher.end(this.group + 2);

					if (endRange >= this.text.length() || this.text.charAt(endRange) != ']') {
						throw this.error(UNCLOSED_RANGE, endRange);
					}

					if (!this.match.endsWith(")")) {
						// error is set at character after "]"
						throw this.error(UNCLOSED_GROUP, endRange + 1);
					}

					this.replaceWith(nonCaptureGroup(PatternRange.unboundedRange(comparison,
							this.matcher.group(this.group + 2), new RangeMode(mode))));
				}
			} else if (this.group == 42) {
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

				if (!this.inCharClass()) {
					this.refactorNumericRange();
				}
			} else {
				this.refactorOthers();
			}
		}

		if (this.parenthesisDepth > 0) {
			throw this.error(UNCLOSED_GROUP, this.text.length());
		}

		this.matcher.appendTail(this.result);
	}

	/**
	 * Steps taken after the regular expression is refactored
	 */
	private void afterRefactor() {
		this.text = this.result.toString();
		this.matcher = RefactorUtility.afterRefactor.matcher(this.result);
		this.reset();

		while (this.matcher.find()) {
			if (this.loopSetup()) {
				continue;
			}

			if (this.group == 1) {
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
				if (!this.inCharClass()) {
					String groupType = this.matcher.group(this.group + 1);
					String mappingName = this.matcher.group(this.group + 2);

					if (groupType.equals("group")) {
						this.replaceWith(this.acceptGroup(mappingName));
					} else if (groupType.equals("branchGroup")) {
						this.replaceWith(this.acceptBranchReset(mappingName));
					} else if (groupType.equals("test")) {
						this.replaceWith(this.acceptTestingGroup(mappingName));
					} else if (groupType.equals("testF")) {
						this.replaceWith(this.failTestingGroup(mappingName));
					}
				}
			} else if (this.group == 4) {
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

				if (!this.inCharClass()) {
					this.afterRefactorFlags();
				}
			} else if (this.group == 6) {
				/*
				 * matches "\x{hhh..} - a hex code"
				 *
				 * group: the number
				 * (1 group)
				 */

				this.afterRefactorHexUnicode();
			} else if (this.group == 7) {
				/*
				 * matches "\xh" or "\xhh" - a hex code
				 *
				 * group: the number
				 * (1 group)
				 */

				this.afterRefactorHexChar();
			} else if (this.group == 8) {
				/*
				 * matches a unicode character
				 *
				 * group: the number
				 * (1 group)
				 */

				this.afterRefactorUnicode();
			} else if (this.group == 9) {
				/*
				 * matches a POSIX character class
				 *
				 * group: "^" or "" (whether to negate or not)
				 * group + 1: the class name
				 * (2 group)
				 */
				this.afterRefactorPosixClass();
			} else if (this.group == 11) {
				/*
				 * matches a control character - \cA through \cZ
				 *
				 * These are equivalent to \x01 through \x1A (26 decimal).
				 *
				 * group: the control character's letter
				 * (either upper and lower case are allowed)
				 * (1 group)
				 */

				this.afterRefactorControlCharacter();
			} else if (this.group == 12) {
				/*
				 * matches an unnamed capture group "(" - not followed by a
				 * "?"
				 *
				 * group: everything (1 group)
				 */

				if (!this.inCharClass()) {
					this.afterRefactorCaptureGroup();
				}
			} else {
				this.afterRefactorOthers();
			}
		}

		this.matcher.appendTail(this.result);
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
	private String acceptGroup(final String mappingName) {
		String accept;

		if (isAnyGroup(mappingName)) {
			accept = this.anyGroup(mappingName);
		} else {
			int mappedIndex = this.pattern.getMappedIndex(mappingName);

			if (this.invalidForwardReference(mappedIndex)) {
				throw this.invalidForwardReference();
			}

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
	private String anyGroup(final String mappingName) {
		if (mappingName.charAt(0) == '[') {
			// ex. [1][0] - (i.e. an unnamed group)
			return this.acceptBranchReset(mappingName);
		}

		// remove trailing "[0]"
		String groupName = anyGroupName(mappingName);

		// int groupCount = groupCount(groupName);
		int groupCount = this.getGroupCount(groupName);
		StringBuilder acceptAny = new StringBuilder();
		StringBuilder previousGroups = new StringBuilder();

		for (int i = 1; i <= groupCount; i++) {
			String tmpMappingName = getMappingName(groupName, i);
			int testingGroup = this.getTestingGroup(tmpMappingName);
			int mappedIndex = this.pattern.getMappedIndex(tmpMappingName);

			if (this.invalidForwardReference(mappedIndex)) {
				continue;
			}

			acceptAny.append(previousGroups).append("\\").append(mappedIndex).append('|');

			previousGroups.append(RefactorUtility.failTestingGroup(testingGroup));
		}

		if (acceptAny.length() == 0) {
			throw this.invalidForwardReference();
		}

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
	private String acceptBranchReset(final String mappingName) {
		// remove trailing "[0]"
		String groupName = anyGroupName(mappingName);

		// int groupCount = groupCount(groupName);
		int groupCount = this.getGroupCount(groupName);
		StringBuilder acceptAnyBranch = new StringBuilder();

		for (int i = 1; i <= groupCount; i++) {
			int mappedIndex = this.pattern.getMappedIndex(groupName, i);

			if (this.invalidForwardReference(mappedIndex)) {
				continue;
			}

			acceptAnyBranch.append("\\").append(mappedIndex).append('|');
		}

		if (acceptAnyBranch.length() == 0) {
			throw this.invalidForwardReference();
		}

		return acceptAnyBranch.deleteCharAt(acceptAnyBranch.length() - 1).toString();
	}

	/**
	 * Returns a regular expression which will match the testing group(s)
	 * associated with the specified mapping name.
	 */
	private String acceptTestingGroup(final String mappingName) {
		String accept;

		if (isAnyGroup(mappingName)) {
			accept = this.anyCondition(mappingName);
		} else {
			Integer testingGroup = this.getTestingGroup(mappingName);

			if (testingGroup == null) {
				throw this.error(INTERNAL_ERROR, -1);
			}

			if (this.invalidForwardReference(testingGroup)) {
				throw this.invalidForwardReference();
			}

			accept = "\\" + testingGroup;
		}

		return "(?=" + accept + ")";
	}

	/**
	 * Returns a regular expression which fails if the testing group(s)
	 * associated with the specified mapping name matches.
	 */
	private String failTestingGroup(final String mappingName) {
		String fail;

		if (isAnyGroup(mappingName)) {
			fail = this.anyCondition(mappingName);
		} else {
			int testingGroup = this.getTestingGroup(mappingName);

			if (this.invalidForwardReference(testingGroup)) {
				throw this.invalidForwardReference();
			}

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
	private String anyCondition(final String mappingName) {
		// remove trailing "[0]"
		String groupName = anyGroupName(mappingName);

		// int groupCount = groupCount(groupName);
		int groupCount = this.pattern.getGroupCount(groupName);
		StringBuilder acceptAny = new StringBuilder();

		// System.out.println(groupName + "\t" + groupCount);

		for (int i = 1; i <= groupCount; i++) {
			String tmpMappingName = getMappingName(groupName, i);

			int testingGroup = this.getTestingGroup(tmpMappingName);

			if (this.invalidForwardReference(testingGroup)) {
				continue;
			}

			acceptAny.append("\\").append(testingGroup).append('|');
		}

		if (acceptAny.length() == 0) {
			throw this.invalidForwardReference();
		}

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
	private void addUnnamedGroup(final String mappingName, final int targetGroupIndex) {
		this.addGroup(mappingName, targetGroupIndex);
	}

	/**
	 * Adds a mapping for the specified named group.
	 *
	 * @param mappingName
	 *            the mapping name
	 * @param targetGroupIndex
	 *            the actual group number (in the internal pattern)
	 */
	private void addNamedGroup(final String mappingName, final int targetGroupIndex) {
		this.addGroup(mappingName, targetGroupIndex);
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
	private void addGroup(final String mappingName, final int targetGroupIndex) {
		if (!this.inSubroutine()) {
			this.pattern.getGroupMapping().put(mappingName, targetGroupIndex);
		}
	}

	/**
	 * Adds a new mapping to {@link #testConditionGroups}.
	 *
	 * @param mappingName
	 *            the mapping name
	 * @param targetGroupIndex
	 *            the actual group number (in the internal pattern)
	 */
	private void addTestingGroup(final String mappingName, final int targetGroupIndex) {
		// TODO: verify this is correct
		if (!this.inSubroutine()) {
			this.testConditionGroups.put(mappingName, targetGroupIndex);
		}
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
	private String getAbsoluteGroup(final String index, final int groupCount) {
		boolean startsWithPlus = index.charAt(0) == '+';

		int groupIndex = parseInt(startsWithPlus ? index.substring(1) : index);

		if (startsWithPlus) {
			groupIndex += groupCount;
		} else if (groupIndex < 0) {
			groupIndex = getAbsoluteGroupIndex(groupIndex, groupCount);

			if (groupIndex == -1) {
				return RefactorUtility.neverUsedMappingName();
			}

			if (this.has(DOTNET_NUMBERING)) {
				return this.getPerlGroup(groupIndex);
			}
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
	String normalizeGroupName(String groupName) {
		if (groupName.startsWith("[") && groupName.endsWith("]")) {
			// Remove "[" and "]"
			groupName = groupName.substring(1, groupName.length() - 1);

			boolean startsWithPlus = groupName.startsWith("+");
			int groupIndex = parseInt(startsWithPlus ? groupName.substring(1) : groupName);

			if (startsWithPlus) {
				groupIndex += this.currentGroup;
			} else if (groupIndex < 0) {
				groupIndex = getAbsoluteGroupIndex(groupIndex, this.currentGroup);

				if (groupIndex == -1) {
					return neverUsedMappingName();
				}

				if (this.has(DOTNET_NUMBERING)) {
					String tmpGroupName = this.getPerlGroup(groupIndex);

					if (tmpGroupName.charAt(0) == '[') {
						return anyGroupName(tmpGroupName);
					} else {
						return groupName;
						// TODO: what should "else" case return?
						// previously groupName was returned (sounds incorrect)
					}
				}
			}

			return wrapIndex(groupIndex);
		}

		return groupName;
	}

	private String getAbsoluteNamedGroup(final String groupName, final String groupOccurrence) {
		try {
			int index = parseInt(groupOccurrence);
			int groupCount = this.getGroupCount(groupName);

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
	private String getAbsoluteGroup(final String groupName, final String groupOccurrence) {
		if (groupOccurrence == null) {
			// e.g. groupName
			return getMappingName(groupName, 0);
		} else if (groupName.length() == 0) {
			return this.getAbsoluteGroup(groupOccurrence, this.currentGroup);
		} else {
			return this.getAbsoluteNamedGroup(groupName, groupOccurrence);
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
	private String getDisplayName(final String groupName, final String groupOccurrence) {
		return groupOccurrence == null ? groupName : groupName + "[" + groupOccurrence + "]";
	}

	/**
	 * Returns the mapping from perl group numbers to actual group number
	 *
	 * @return
	 */
	private HashMap<Integer, String> perlGroupMapping() {
		if (this.perlGroupMapping == null) {
			this.perlGroupMapping = new HashMap<>();
		}

		return this.perlGroupMapping;
	}

	private String getPerlGroup(final int groupIndex) {
		return this.perlGroupMapping().get(groupIndex);
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
	private Integer getTestingGroup(final String mappingName) {
		return this.testConditionGroups.get(mappingName);
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
	private Integer addErrorTrace(final int errorIndex) {
		if (this.errorTrace == null) {
			this.errorTrace = new HashMap<>(2);
		}

		Integer key = this.errorTrace.size();
		this.errorTrace.put(this.errorTrace.size(), errorIndex);

		return key;
	}

	private PatternSyntaxException invalidForwardReference() {
		int index = this.errorTrace.get(Integer.valueOf(this.matcher.group(this.group)));

		try {
			index = this.preRefactoringChanges.getOriginalIndex(index);
		} catch (IllegalArgumentException e) {
			index = -1;
		}

		return new PatternSyntaxException(INVALID_FORWARD_REFERENCE, this.regex, index);
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
	private boolean invalidForwardReference(final int mappedIndex) {
		if (this.isJava1_5 && mappedIndex > this.totalGroups + 1) {
			return true;
		}

		return mappedIndex > this.totalGroups && mappedIndex >= 10;
	}

	/**
	 * Gets the group counts.
	 *
	 * @return the group counts
	 */
	Map<String, Integer> getGroupCounts() {
		return this.groupCounts;
	}

	/**
	 * Returns the group count for the given group name.
	 *
	 * @param groupName
	 *            the group name
	 * @return the group count for the given group name
	 */
	private int getGroupCount(final String groupName) {
		// if (groupName.length() == 0)
		// return currentGroup;

		// System.out.println("groupCounts: " + groupCounts);

		Integer groupCount = this.groupCounts.get(groupName);
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
	private void setGroupCount(final String groupName, final int groupCount) {
		if (!this.inSubroutine()) {
			this.groupCounts.put(groupName, groupCount);
		}
	}

	/**
	 * Increases the group count for the given group name by one.
	 *
	 * @param groupName
	 *            the group name
	 *
	 * @return the (new) group count for the given group name
	 */
	private int increaseGroupCount(final String groupName) {
		int groupCount = this.getGroupCount(groupName) + 1;

		// store the new group count
		this.setGroupCount(groupName, groupCount);

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
	private boolean atRightDepth(final Stack<? extends State> matchStates) {
		if (matchStates.isEmpty()) {
			return false;
		}

		return matchStates.peek().getParenthesisDepth() == this.parenthesisDepth;
	}

	/**
	 * Increases the parenthesis depth by one.
	 *
	 * @param duringPreRefactor
	 *            whether this function call occurs during the pre-refactor
	 *            step
	 */
	private void increaseParenthesisDepth(final boolean duringPreRefactor) {
		this.parenthesisDepth++;
		this.flagsStack.push(this.flags);
	}

	/**
	 * Increases the current group.
	 *
	 * @param duringPreRefactor
	 *            whether this function call occurs during the pre-refactor
	 *            step
	 */
	private void increaseCurrentGroup(final boolean duringPreRefactor) {
		this.currentGroup++;

		if (!duringPreRefactor) {
			// if (digitCount(currentGroup) != digitCount(currentGroup - 1))
			// matcher.usePattern(getRefactorPattern(currentGroup));

			this.totalGroups++;
		}
	}

	private void addSubpattern(final String mappingName, final Subpattern subpattern) {
		// +1 is because the group hasn't been added yet
		// int occurrence = getGroupCount(wrapIndex(currentGroup)) + 1;
		// String mappingName = getMappingName(currentGroup, occurrence);

		// System.out.println("Group (" + mappingName + "): " + parenthesisDepth);

		// Subpattern subpattern = new Subpattern(mappingName);

		// Set to subpattern start
		// (after the captured text, which is the group name / symbols)
		// subpattern.setStart(matcher.end(group));

		if (!this.inSubroutine()) {
			// subpattern.addSubpatternDependency(mappingName);
			this.getSubpatterns().put(mappingName, subpattern);
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
	private void decreaseParenthesisDepth(final boolean duringPreRefactor) {
		if (duringPreRefactor && !this.inSubroutine()) {
			// System.out.println("Close: " + parenthesisDepth);

			Subpattern subpattern = this.openSubpatterns.get(this.parenthesisDepth);

			if (subpattern != null) {
				this.openSubpatterns.remove(this.parenthesisDepth);

				// Don't include end parenthesis
				subpattern.setEnd(this.matcher.start());
				String subregex = this.regex.substring(subpattern.getStart(), subpattern.getEnd());
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

		this.parenthesisDepth--;
		this.flags = this.flagsStack.pop();
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
	private void refactorSubroutine(final boolean isNamedGroup) {
		int errorPosition = this.matcher.start(this.group);

		String displayName;
		String mappingName;

		// TODO: Test whether group occurrences are supported
		// TODO: Test whether relative group occurrences are supported within subpatterns

		if (isNamedGroup) {
			String groupName = this.normalizeGroupName(this.matcher.group(this.group));
			String groupOccurrence = this.matcher.group(this.group + 1);
			int groupOccurenceInt = groupOccurrence == null ? 0 : Integer.parseInt(groupOccurrence);

			// System.out.println("Group name: " + groupName);
			// System.out.println("Group occurrence: " + groupOccurrence);

			displayName = this.getDisplayName(this.matcher.group(this.group), this.matcher.group(this.group + 1));
			// mappingName = getAbsoluteGroup(groupName, groupOccurrence);
			mappingName = getMappingName(groupName, groupOccurenceInt);
		} else {
			displayName = this.getDisplayName(this.matcher.group(this.group), null);
			mappingName = this.getAbsoluteGroup(this.matcher.group(this.group), this.currentGroup);
		}

		if (mappingName.startsWith("[0]")) {
			throw this.error(ZERO_REFERENCE, errorPosition);
		}

		// TODO: mimics PCRE, by using first occurrence
		if (isAnyGroup(mappingName)) {
			mappingName = anyGroupName(mappingName) + "[1]";
		}

		// System.out.println("Subroutine: " + mappingName);
		// System.out.println("Mapped index: " + pattern.getMappedIndex(mappingName));
		// System.out.println("Pattern: " + pattern);
		// System.out.println("Group mapping: " + pattern.getGroupMapping());

		if (this.pattern.getMappedIndex(mappingName) == null) {
			throw this.error(NONEXISTENT_SUBPATTERN, errorPosition);
		}

		@SuppressWarnings("hiding")
		Subpattern subpattern = this.getSubpatterns().get(mappingName);

		if (subpattern == null) {
			throw this.error(INVALID_SUBROUTINE, errorPosition);
		}

		// System.out.println("Subpattern pattern: " + subpattern.getPattern());

		// if (!subpattern.addSubpatternDependency(mappingName))
		// throw error(CIRCULAR_SUBROUTINE, matcher.start(group));

		subpattern.addSubpatternDependency(mappingName);

		try {
			// Uses an atomic group, same as PCRE
			this.replaceWith("(?>" + subpattern.getPattern(this.flags) + ")");
		} catch (PatternSyntaxException e) {
			// TODO: use mapping name, if contains relative reference
			PatternSyntaxException error = this.error(e.getDescription(), errorPosition,
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
	private void setFlags(final String onFlags, final String offFlags) {
		if (onFlags.contains("x")) {
			this.flags |= COMMENTS;
		}

		if (onFlags.contains("d")) {
			this.flags |= UNIX_LINES;
		}

		if (onFlags.contains("o")) {
			this.flags |= PERL_OCTAL;
		}

		if (onFlags.contains("v")) {
			this.flags |= VERIFY_GROUPS;
		}

		// Added to keep track of all inline flags (required for subroutines)
		if (onFlags.contains("i")) {
			this.flags |= CASE_INSENSITIVE;
		}

		if (onFlags.contains("s")) {
			this.flags |= DOTALL;
		}

		if (onFlags.contains("J")) {
			this.flags |= DUPLICATE_NAMES;
		}

		if (onFlags.contains("n")) {
			this.flags |= EXPLICIT_CAPTURE;
		}

		if (onFlags.contains("m")) {
			this.flags |= MULTILINE;
		}

		if (onFlags.contains("u")) {
			this.flags |= UNICODE_CASE;
		}

		if (onFlags.contains("U")) {
			this.flags |= UNICODE_CHARACTER_CLASS;
		}

		if (offFlags != null) {
			if (offFlags.contains("x")) {
				this.flags &= ~COMMENTS;
			}

			if (offFlags.contains("d")) {
				this.flags &= ~UNIX_LINES;
			}

			if (offFlags.contains("o")) {
				this.flags &= ~PERL_OCTAL;
			}

			if (offFlags.contains("v")) {
				this.flags &= ~VERIFY_GROUPS;
			}

			// Added to keep track of all inline flags (required for subroutines)
			if (offFlags.contains("i")) {
				this.flags &= ~CASE_INSENSITIVE;
			}

			if (offFlags.contains("s")) {
				this.flags &= ~DOTALL;
			}

			if (offFlags.contains("J")) {
				this.flags &= ~DUPLICATE_NAMES;
			}

			if (offFlags.contains("n")) {
				this.flags &= ~EXPLICIT_CAPTURE;
			}

			if (offFlags.contains("m")) {
				this.flags &= ~MULTILINE;
			}

			if (offFlags.contains("u")) {
				this.flags &= ~UNICODE_CASE;
			}

			if (offFlags.contains("U")) {
				this.flags &= ~UNICODE_CHARACTER_CLASS;
			}
		}
	}

	private String replaceFlags(String onFlags, String offFlags) {
		boolean flagsChanged = false;
		StringBuilder newFlags = new StringBuilder(this.matcher.end() - this.matcher.start());

		if (onFlags.contains("J")) {
			onFlags = onFlags.replace("J", "");
			this.flags |= DUPLICATE_NAMES;
			flagsChanged = true;
		}

		if (onFlags.contains("n")) {
			onFlags = onFlags.replace("n", "");
			this.flags |= EXPLICIT_CAPTURE;
			flagsChanged = true;
		}

		newFlags.append(onFlags);

		if (offFlags != null) {
			if (offFlags.contains("J")) {
				offFlags = offFlags.replace("J", "");
				this.flags &= ~DUPLICATE_NAMES;
				flagsChanged = true;
			}

			if (offFlags.contains("n")) {
				offFlags = offFlags.replace("n", "");
				this.flags &= ~EXPLICIT_CAPTURE;
				flagsChanged = true;
			}

			if (offFlags.length() != 0) {
				newFlags.append('-').append(offFlags);
			}
		}

		// System.out.println("replace: " + onFlags + "-" + offFlags + " >> " + newFlags);

		return flagsChanged ? newFlags.toString() : null;
	}

	/**
	 * Refactors the flags during the pre-refactoring step
	 */
	private void preRefactorFlags() {
		/*
		 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:" (also
		 * matches a non-capture group - onFlags/offFlags are omitted)
		 *
		 * group: onFlags (empty string if none) group + 1: offFlags (empty
		 * string if none; null, if omitted)
		 */

		String onFlags = this.matcher.group(this.group);
		String offFlags = this.matcher.group(this.group + 1);

		char ending = this.match.charAt(this.match.length() - 1);
		boolean isGroup = ending == ')';

		if (!isGroup) {
			this.increaseParenthesisDepth(DURING_PREREFACTOR);
		}

		this.setFlags(onFlags, offFlags);
		String newFlags = this.replaceFlags(onFlags, offFlags);

		if (newFlags == null) {
			// no change
			return;
		}

		if (newFlags.length() != 0 || !isGroup) {
			this.replaceWith("(?" + newFlags + ending);
		} else {
			this.replaceWith("");
		}
	}

	/**
	 * Refactors the flags during the refactoring step
	 */
	private void refactorFlags() {
		/*
		 * matches "(?onFlags-offFlags)" or "(?onFlags-offFlags:" (also
		 * matches a non-capture group - onFlags/offFlags are omitted)
		 *
		 * group: onFlags (empty string if none) group + 1: offFlags (empty
		 * string if none; null, if omitted)
		 */

		String onFlags = this.matcher.group(this.group);
		String offFlags = this.matcher.group(this.group + 1);

		char ending = this.match.charAt(this.match.length() - 1);
		boolean isGroup = ending == ')';

		if (!isGroup) {
			this.increaseParenthesisDepth(!DURING_PREREFACTOR);
		}

		this.setFlags(onFlags, offFlags);
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
	private void afterRefactorFlags() {
		String onFlags = this.matcher.group(this.group);
		String offFlags = this.matcher.group(this.group + 1);

		char ending = this.match.charAt(this.match.length() - 1);
		boolean isGroup = ending == ')';

		if (!isGroup) {
			this.increaseParenthesisDepth(!DURING_PREREFACTOR);
		}

		this.setFlags(onFlags, offFlags);

		// Ensure that all illegal flags are removed
		boolean flagsChanged = false;
		StringBuilder newFlags = new StringBuilder(this.matcher.end() - this.matcher.start());

		if (onFlags.contains("o")) {
			onFlags = onFlags.replace("o", "");
			this.flags |= PERL_OCTAL;
			flagsChanged = true;
		}

		if (onFlags.contains("v")) {
			onFlags = onFlags.replace("v", "");
			this.flags |= VERIFY_GROUPS;
			flagsChanged = true;
		}

		newFlags.append(onFlags);

		if (offFlags != null) {
			if (offFlags.contains("o")) {
				offFlags = offFlags.replace("o", "");
				this.flags &= ~PERL_OCTAL;
				flagsChanged = true;
			}

			if (offFlags.contains("v")) {
				offFlags = offFlags.replace("v", "");
				this.flags &= ~VERIFY_GROUPS;
				flagsChanged = true;
			}

			if (offFlags.length() != 0) {
				newFlags.append('-').append(offFlags);
			}
		}

		newFlags = flagsChanged ? newFlags : null;

		if (newFlags == null) {
			// no change
			return;
		}

		if (newFlags.length() != 0 || !isGroup) {
			this.replaceWith("(?" + newFlags + ending);
		} else {
			this.replaceWith("");
		}
	}

	/**
	 * Refactors a capturing group during the pre-refactoring step
	 *
	 * @param isNamedGroup
	 *            whether the capture group is a named group
	 * @param form
	 *            the capture group's 0-based form
	 */
	private void preRefactorCaptureGroup(final boolean isNamedGroup, final int form) {
		// TODO: verify this works fully

		// if (inSubroutine())
		// throw error("Subroutines do not support capture groups", matcher.start(group));

		this.increaseParenthesisDepth(DURING_PREREFACTOR);

		if (!isNamedGroup && this.has(EXPLICIT_CAPTURE)) {
			this.replaceWith(startNonCaptureGroup());
			return;
		}

		if (this.inSubroutine()) {
			return;
		}

		this.increaseCurrentGroup(DURING_PREREFACTOR);

		Subpattern subpattern;

		if (!this.inSubroutine()) {
			// Initialize subpattern
			subpattern = new Subpattern(this);
			subpattern.setStart(this.matcher.end(this.group));

			// System.out.println("Total groups: " + totalGroups);

			// TODO: include flags and prepend subpattern with them
			subpattern.setFlags(this.flags);

			// flags are maintained
			this.openSubpatterns.put(this.parenthesisDepth, subpattern);
		} else {
			subpattern = null;
		}

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

			this.subpatternNameExpected();
			this.checkForMissingTerminator(">'>", form);

			String groupName = this.matcher.group(this.group + 1);
			int occurrence = this.increaseGroupCount(groupName);

			if (occurrence != 1 && !this.has(DUPLICATE_NAMES)) {
				throw this.error(DUPLICATE_NAME, this.matcher.start(this.group));
			}

			String mappingName = getMappingName(groupName, occurrence);
			this.addNamedGroup(mappingName, TARGET_UNKNOWN);

			this.addSubpattern(mappingName, subpattern);

			if (!this.inBranchReset()) {
				this.namedGroup++;
			} else {
				this.unnamedGroup++;
			}

			// TODO: verify this is correct
			if (!this.inSubroutine() && this.has(DOTNET_NUMBERING)) {
				if (!this.inBranchReset()) {
					this.perlGroupMapping().put(this.currentGroup, mappingName);
				} else {
					this.perlGroupMapping().put(this.currentGroup, getMappingName(this.currentGroup, 0));
				}
			}
		} else {
			this.unnamedGroup++;

			// TODO: verify this is correct
			if (!this.inSubroutine() && this.has(DOTNET_NUMBERING)) {
				String mappingName = getMappingName(this.unnamedGroup, 0);
				this.perlGroupMapping().put(this.currentGroup, mappingName);
			}
		}

		if (!this.has(DOTNET_NUMBERING) || this.inBranchReset() || !isNamedGroup) {
			int groupIndex = this.has(DOTNET_NUMBERING) ? this.unnamedGroup : this.currentGroup;

			String groupName = wrapIndex(groupIndex);
			int occurrence = this.increaseGroupCount(groupName);
			String mappingName = getMappingName(groupIndex, occurrence);

			// add mapping for group index
			this.addUnnamedGroup(mappingName, TARGET_UNKNOWN);
			this.addSubpattern(mappingName, subpattern);
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
	private void checkForMissingTerminator(final String endings, final int form) {
		boolean missingTerminator = form < endings.length()
				&& !this.matcher.group(this.group).endsWith(endings.substring(form, form + 1));

		if (missingTerminator) {
			throw this.error(MISSING_TERMINATOR, this.matcher.end(this.group));
		}
	}

	/**
	 * Checks that a subpattern name is present.
	 *
	 * @throws PatternSyntaxException
	 *             If the subpattern name is missing
	 */
	private void subpatternNameExpected() {
		// name is blank and [occurrence] is null
		boolean missingName = this.matcher.start(this.group + 1) == this.matcher.end(this.group + 1)
				&& this.matcher.start(this.group + 2) == -1;

		if (missingName) {
			throw this.error(SUBPATTERN_NAME_EXPECTED, this.matcher.start(this.group));
		}
	}

	/**
	 * Refactors a capturing group during the refactoring step
	 *
	 * @param isNamedGroup
	 *            whether the group is a named group
	 */
	private void refactorCaptureGroup(final boolean isNamedGroup) {
		this.increaseParenthesisDepth(!DURING_PREREFACTOR);
		this.increaseCurrentGroup(!DURING_PREREFACTOR);

		if (isNamedGroup && !this.inBranchReset()) {
			this.namedGroup++;
		} else {
			this.unnamedGroup++;
		}

		boolean usedInCondition;
		String namedMappingName;

		if (isNamedGroup) {
			String groupName = this.matcher.group(this.group);
			int occurrence = this.increaseGroupCount(groupName);
			namedMappingName = getMappingName(groupName, occurrence);

			// add mapping for group name
			this.addNamedGroup(namedMappingName, this.totalGroups);

			usedInCondition = this.usedInCondition(namedMappingName)
					|| this.usedInCondition(getMappingName(groupName, 0));
		} else {
			usedInCondition = false;
			namedMappingName = null;
		}

		int groupIndex = this.getCurrentGroup(isNamedGroup && !this.inBranchReset());
		String groupName = wrapIndex(groupIndex);
		int occurrence = this.increaseGroupCount(groupName);
		String mappingName = getMappingName(groupName, occurrence);

		// add mapping for group index
		this.addUnnamedGroup(mappingName, this.totalGroups);

		// TODO: uncomment to use for debugging
		// System.out.printf("%s (%s): %s%n", mappingName, totalGroups, getSubpatterns().get(mappingName));

		if (!usedInCondition) {
			usedInCondition = this.usedInCondition(mappingName) || this.usedInCondition(getMappingName(groupName, 0));

			if (!usedInCondition) {
				if (isNamedGroup) {
					// remove name part
					this.replaceWith("(");
				}

				return;
			}
		}

		// remove name part (if applicable) and convert form
		// "(?<name>RegEx)" -> "(?:(RegEx)())"
		this.replaceWith(startNonCaptureGroup() + "(");
		this.increaseParenthesisDepth(!DURING_PREREFACTOR);

		// add a MatchState to track where to add
		// the necessary "()" at the end of the capture group
		this.addTestGroup.push(new AddTestGroupState(mappingName, this.parenthesisDepth, namedMappingName));
	}

	private void afterRefactorCaptureGroup() {
		this.increaseParenthesisDepth(!DURING_PREREFACTOR);
		this.increaseCurrentGroup(!DURING_PREREFACTOR);
	}

	private int getCurrentGroup(final boolean isNamedGroup) {
		if (this.has(DOTNET_NUMBERING)) {
			if (isNamedGroup) {
				return this.unnamedGroupCount + this.namedGroup;
			} else {
				return this.unnamedGroup;
			}
		} else {
			return this.currentGroup;
		}
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
	private boolean usedInCondition(final String mappingName) {
		return this.requiresTestingGroup.contains(mappingName);
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
	static String unwrapIndex(final String groupIndex) {
		if (groupIndex.charAt(0) == '[' && groupIndex.charAt(groupIndex.length() - 1) == ']') {
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
	private void preRefactorConditionalPattern(final boolean isNamedGroup, final int form) {
		if (this.inSubroutine()) {
			throw this.error("Subroutines do not support named/unnamed conditionals", this.matcher.start(this.group));
		}

		this.increaseParenthesisDepth(DURING_PREREFACTOR);
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
			if (this.matcher.start(this.group) == this.matcher.end(this.group)) {
				throw this.error(ASSERTION_EXPECTED, this.matcher.end(this.group));
			}

			this.checkForMissingTerminator(">'", form);

			if (!this.match.endsWith(")")) {
				throw this.error(UNCLOSED_GROUP, this.matcher.end(this.group));
			}

			String groupName = this.matcher.group(this.group + 1);

			if (groupName.charAt(0) == '[') {
				String tmpMappingName = this.getAbsoluteGroup(unwrapIndex(groupName), this.currentGroup);

				if (isAnyGroup(tmpMappingName)) {
					// reference is an unnamed group
					// (any occurrence is possible)
					groupName = anyGroupName(tmpMappingName);

					String groupOccurrence = this.matcher.group(this.group + 2);
					mappingName = this.getAbsoluteGroup(groupName, groupOccurrence);
				} else {
					// reference is a named group
					// (occurrence is ignored)

					mappingName = tmpMappingName;
				}
			} else {
				// named group

				String groupOccurrence = this.matcher.group(this.group + 2);
				mappingName = this.getAbsoluteGroup(groupName, groupOccurrence);
			}
		} else {
			mappingName = this.getAbsoluteGroup(this.matcher.group(this.group), this.currentGroup);
		}

		this.requiresTestingGroup.add(mappingName);

		// add a MatchState to handle the "else" branch
		this.handleElseBranch.push(new MatchState("", this.parenthesisDepth, -1));
	}

	/**
	 * Refactors a conditional pattern during the refactoring step
	 *
	 * @param isNamedGroup
	 *            whether the condition is a name or number
	 */
	private void refactorConditionalPattern(final boolean isNamedGroup) {
		this.increaseParenthesisDepth(!DURING_PREREFACTOR);
		String groupName = this.normalizeGroupName(
				isNamedGroup ? this.matcher.group(this.group) : "[" + this.matcher.group(this.group) + "]");

		// start of groupName / number
		int start = this.matcher.start(this.group);

		if (groupName.equals("[0]")) {
			throw this.error(INVALID_CONDITION0, start);
		}

		String groupOccurrence = isNamedGroup ? this.matcher.group(this.group + 1) : null;

		String mappingName = this.getAbsoluteGroup(groupName, groupOccurrence);

		Integer mappingIndexI = this.pattern.getMappedIndex(mappingName);
		Integer testConditionGroupI = this.getTestingGroup(mappingName);

		// System.out.println("refactorConditionalPattern: " + mappingName);

		if (isAnyGroup(mappingName)) {
			// int groupCount = groupCount(groupName);
			// int groupCount = getGroupCount(groupName);
			int groupCount = this.pattern.getGroupCount(groupName);

			// TODO: verify this change is valid
			// modified to add support for +x on conditionals (can be used in subroutines)

			if (groupCount == 0) {
				if (groupName.equals("DEFINE")) {
					// (?(DEFINE)...) is a special condition, which should always be false
					// (allows defining subpatterns, without them being matched at that point)
					// TODO: is it possible to completely remove DEFINE group from internal pattern ??
					// TODO: ensure DEFINE group has no alternations - otherwise, throw error
					this.replaceWith(startNonCaptureGroup() + fail());
				} else {
					// if (has(VERIFY_GROUPS))
					throw this.error(NONEXISTENT_SUBPATTERN, start);
				}

				// the specified group doesn't exist
				// replaceWith(startNonCaptureGroup() + fail());
			} else if (!this.allDone(groupName, groupCount)) {
				// System.out.println("Some groups occur later on: " + mappingName);

				// some groups occur later on
				this.replaceWith(
						startNonCaptureGroup() + "\\g{" + this.addErrorTrace(start) + "test-" + mappingName + "}");
				testConditionGroupI = TARGET_UNKNOWN;
			} else {
				// System.out.println("All groups have already occurred: " + mappingName);

				// all groups have already occurred
				this.replaceWith(startNonCaptureGroup() + this.acceptTestingGroup(mappingName));
				// TODO: need to rename condition group
				// testConditionGroupI = BRANCH_RESET;
				testConditionGroupI = 0;
			}

		} else if (mappingIndexI == null) {
			// if (has(VERIFY_GROUPS))
			throw this.error(NONEXISTENT_SUBPATTERN, start);

			// the specified group doesn't exist
			// replaceWith(startNonCaptureGroup() + fail());
		} else if (testConditionGroupI == null) {
			// the specified group exists, but occurs later
			this.replaceWith(startNonCaptureGroup() + "\\g{" + this.addErrorTrace(start) + "test-" + mappingName + "}");
			testConditionGroupI = TARGET_UNKNOWN;
		} else {
			// the specified group has already occurred
			this.replaceWith(startNonCaptureGroup() + RefactorUtility.acceptTestingGroup(testConditionGroupI));
		}

		// add a MatchState to handle the "else" branch
		this.handleElseBranch.push(new MatchState(mappingName, this.parenthesisDepth, testConditionGroupI));
	}

	/**
	 * Refactors a back reference during the pre-refactoring step
	 */
	private void preRefactorBackreference() {
		String groupName = this.matcher.group(this.group);

		if (!RefactorUtility.isUnnamedGroup(groupName)) {
			this.anyGroupReferences.add(groupName);
		}
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
	private void refactorBackreference(final boolean isNamedGroup, final int form, final int digitGroup) {
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

			this.subpatternNameExpected();
			this.checkForMissingTerminator("}>'})", form);

			start = this.matcher.start(this.group + 1);
			String groupName = this.normalizeGroupName(this.matcher.group(this.group + 1));
			String groupOccurrence = this.matcher.group(this.group + 2);
			mappingName = this.getAbsoluteGroup(groupName, groupOccurrence);
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
			start = this.matcher.start(this.group);

			if (form == 0) {
				java.util.regex.MatchResult backreference = this.getBackreference(this.matcher.group(this.group),
						start);

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
				if (groupIndex > this.currentGroup && groupIndex >= 10) {
					trailingDigits = String.valueOf(groupIndex % 10);
					groupIndex /= 10;
				}

				trailingDigits += backreference.group(2);
				String groupName = wrapIndex(groupIndex);
				mappingName = getMappingName(groupName, 0);
			} else {
				mappingName = this.getAbsoluteGroup(this.matcher.group(this.group), this.currentGroup);
			}
		}

		// Checked after octal check
		// TODO: verify this works fully
		// if (inSubroutine())
		// throw error("Subroutines do not support backreferences", matcher.start(group));

		trailingDigits += this.matcher.group(digitGroup);

		// retrieve the actual group index for the specified groupIndex
		Integer mappedIndexI;

		// replace back reference with back reference RegEx
		if (isAnyGroup(mappingName)) {
			String groupName = anyGroupName(mappingName);

			if (groupName.equals("[0]")) {
				throw this.error(ZERO_REFERENCE, start);
			}

			// int groupCount = groupCount(groupName);
			int groupCount = this.getGroupCount(groupName);

			if (groupCount == 0) {
				// form 0 is \n (for unnamed group)
				if (isNamedGroup || form != 0 || this.has(VERIFY_GROUPS)) {
					throw this.error(NONEXISTENT_SUBPATTERN, start);
				}

				// Used to mimic java functionality
				// Not needed
				// (required because otherwise group could point to a valid capture group, due to added groups)
				// !isNamedGroup, form == 0 ('\n' syntax), and !has(VERIFY_GROUPS)
				this.replaceWith(fail() + trailingDigits);
			} else if (groupCount == 1) {
				String tmpMappingName = getMappingName(groupName, 1);
				trailingDigits = RefactorUtility.fixTrailing(trailingDigits);

				if (this.getGroupCount(groupName) == groupCount) {
					// group has already occurred
					this.replaceWith(this.acceptGroup(tmpMappingName) + trailingDigits);
				} else {
					// group occurs later on
					this.replaceWith(
							"\\g{" + this.addErrorTrace(start) + "group-" + tmpMappingName + "}" + trailingDigits);
				}
			} else if (this.allDone(groupName, groupCount)) {
				// all groups have already occurred
				String acceptGroup = this.acceptGroup(mappingName);

				this.replaceWith(nonCaptureGroup(acceptGroup) + trailingDigits);
			} else {
				// some groups occur later on
				this.replaceWith(nonCaptureGroup("\\g{" + this.addErrorTrace(start) + "group-" + mappingName + "}")
						+ trailingDigits);
			}
		} else if ((mappedIndexI = this.pattern.getMappedIndex(mappingName)) == null) {
			// if (has(VERIFY_GROUPS))
			throw this.error(NONEXISTENT_SUBPATTERN, start);

			// replaceWith(fail() + trailingDigits);
		} else {
			int mappedIndex = mappedIndexI;

			trailingDigits = RefactorUtility.fixTrailing(trailingDigits);

			if (mappedIndex == TARGET_UNKNOWN) {
				// group hasn't occurred yet

				this.replaceWith("\\g{" + this.addErrorTrace(start) + "group-" + mappingName + "}" + trailingDigits);
			} else {
				// group already occurred
				this.replaceWith("\\" + mappedIndex + trailingDigits);
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
	private boolean allDone(final String groupName, final int groupCount) {
		return this.getTestingGroup(getMappingName(groupName, groupCount)) != null;
		//		if (RefactorUtility.isUnnamedGroup(groupName)) {
		//			// e.g. [1][0]
		//			return getGroupCount(groupName) == groupCount;
		//		} else {
		//			// e.g. groupName[0]
		//			return getTestingGroup(getMappingName(groupName, groupCount)) != null;
		//		}
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
	private java.util.regex.MatchResult getBackreference(final String backreference, final int start) {
		if (backreference.charAt(0) == '0') {
			String input = this.has(PERL_OCTAL) ? backreference : backreference.substring(1);

			@SuppressWarnings("hiding")
			java.util.regex.Matcher matcher = perl_octal.matcher(input);

			if (!matcher.matches()) {
				// +1 because leading '0'
				int errorLoc = start + 1;
				throw this.error(ILLEGAL_OCTAL_ESCAPE, errorLoc);
			}

			String octal = matcher.group(1);
			String trailing = matcher.group(2);

			int octalCode = Integer.parseInt(octal, 8);

			String hexCode = String.format(hexCodeFormat, octalCode);
			this.replaceWith(hexCode + trailing);

			return null;
		}

		if (this.inCharClass()) {
			if (this.has(PERL_OCTAL)) {
				@SuppressWarnings("hiding")
				java.util.regex.Matcher matcher = perl_octal.matcher(backreference);

				if (!matcher.matches()) {
					throw this.error(ILLEGAL_OCTAL_ESCAPE, start);
				}

				String octal = matcher.group(1);
				String trailing = matcher.group(2);

				int octalCode = Integer.parseInt(octal, 8);

				String hexCode = String.format(hexCodeFormat, octalCode);
				this.replaceWith(hexCode + trailing);

				return null;
			} else {
				// ignore back reference in character class
				return null;
			}
		}

		int digitCount = RefactorUtility.digitCount(this.currentGroup);

		@SuppressWarnings("hiding")
		java.util.regex.Pattern pattern = getDigitCountPattern(digitCount);
		@SuppressWarnings("hiding")
		java.util.regex.Matcher matcher = pattern.matcher(backreference);

		matcher.matches();

		int groupIndex = Integer.parseInt(matcher.group(1));
		String trailing = matcher.group(2);

		if (this.has(PERL_OCTAL) && (trailing.length() != 0 || digitCount > 1 && groupIndex > this.currentGroup)) {
			// an octal escape

			matcher = perl_octal.matcher(backreference);

			if (!matcher.matches()) {
				throw this.error(ILLEGAL_OCTAL_ESCAPE, start);
			}

			String octal = matcher.group(1);
			trailing = matcher.group(2);

			int octalCode = Integer.parseInt(octal, 8);
			String hexCode = String.format(hexCodeFormat, octalCode);
			this.replaceWith(hexCode + trailing);

			return null;
		}

		return matcher.toMatchResult();
	}

	/**
	 * Refactors an assert condition during the pre-refactoring step
	 */
	private void preRefactorAssertCondition() {
		/*
		 * matches an assert condition
		 * "(?(?=)", "(?(?!)", "(?(?<=)", or "(?(?<!)"
		 * group : the assert part (inside the parentheses)
		 * (1 group)
		 */

		this.increaseParenthesisDepth(DURING_PREREFACTOR);

		// add a MatchState to handle the "else" branch
		this.handleElseBranch.push(new MatchState("", this.parenthesisDepth, -1));

		// increase parenthesis depth to that of the assertion
		this.increaseParenthesisDepth(DURING_PREREFACTOR);
	}

	/**
	 * Refactors an assert condition during the refactoring step
	 */
	private void refactorAssertCondition() {
		this.increaseParenthesisDepth(!DURING_PREREFACTOR);

		// convert form (m is the test condition group for the assertion)
		// "(?(assert)then|else)" ->
		// "(?:(?:(assert)())?+(?:(?:\m)then|(?!\m)else))"

		// (conversion works in PCRE, but only partly in Java)
		// does not work during repetition
		// TODO: verify conversions work in PCRE and Java

		this.replaceWith(startNonCaptureGroup() + startNonCaptureGroup() + "(" + this.matcher.group(this.group));

		// increase parenthesis depth to that of the assertion
		this.increaseParenthesisDepth(!DURING_PREREFACTOR);
		this.increaseParenthesisDepth(!DURING_PREREFACTOR);

		this.handleEndAssertCond.add(new MatchState("", this.parenthesisDepth, TARGET_UNKNOWN));
	}

	/**
	 * Returns the mapping name associated with the event to end an assert
	 * condition
	 *
	 * @return
	 */
	private String endAssertCondMappingName() {
		return "$endAssertCond";
	}

	/**
	 * Initializes a "branch reset" subpattern during the pre-refactoring
	 * step.
	 */
	private void preRefactorBranchReset() {
		this.increaseParenthesisDepth(DURING_PREREFACTOR);

		this.branchReset.push(new BranchResetState(this.currentGroup, this.unnamedGroup, this.parenthesisDepth));
	}

	/**
	 * Initializes a "branch reset" subpattern during the refactoring step.
	 */
	private void refactorBranchReset() {
		this.replaceWith(startNonCaptureGroup());
		this.increaseParenthesisDepth(!DURING_PREREFACTOR);
		this.branchReset.push(new BranchResetState(this.currentGroup, this.unnamedGroup, this.parenthesisDepth));
	}

	/**
	 * Refactors a numeric range during the refactoring step
	 */
	private void refactorNumericRange() {
		String mode = this.matcher.group(this.group);
		// boolean rawMode = matcher.group(group + 1) != null;
		boolean inclusiveStart = this.matcher.start(this.group + 1) == -1;
		String start = this.matcher.group(this.group + 2);
		boolean inclusiveEnd = this.matcher.start(this.group + 3) == -1;
		String end = this.matcher.group(this.group + 4);
		int endRange = this.matcher.end(this.group + 4);

		// System.out.println("Numeric range: " + start + "\t" + end);

		if (start == null || end == null) {
			// error is set at character after "["
			throw this.error(NUMERIC_RANGE_EXPECTED, this.matcher.end(this.group) + 1);
		}

		if (endRange >= this.text.length() || this.text.charAt(endRange) != ']') {
			throw this.error(UNCLOSED_RANGE, endRange);
		}

		if (!this.match.endsWith(")")) {
			// error is set at character after "]"
			throw this.error(UNCLOSED_GROUP, endRange + 1);
		}

		// TODO: add bug testing

		String range = PatternRange.boundedRange(start, inclusiveStart, end, inclusiveEnd, new RangeMode(mode));
		range = nonCaptureGroup(range);
		this.replaceWith(range);

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
	private void afterRefactorHexUnicode() {
		int hex = 16;
		int codePoint;

		try {
			codePoint = Integer.parseInt(this.matcher.group(this.group), hex);

			if (codePoint <= 0xFF) {
				this.replaceWith(String.format(hexCodeFormat, codePoint));
			} else {
				// char[] array = Character.toChars(hexCode);
				//
				// if (array.length == 1)
				// replaceWith(String.format("\\x{%1$04x}", (int)
				// array[0]));
				// else
				// replaceWith(String.format("\\x{%1$04x}\\x{%2$04x}",
				// (int) array[0], (int) array[1]));

				if (Character.charCount(codePoint) == 1) {
					this.replaceWith(String.format(unicodeFormat, codePoint));
				} else {
					this.replaceWith(new String(Character.toChars(codePoint)));
				}
			}
		} catch (RuntimeException e) {
			throw this.error(INVALID_HEX_CODE, this.matcher.start());
		}
	}

	/**
	 * Refactors the hex char during the after-refactoring step
	 */
	private void afterRefactorHexChar() {
		String hexCode = this.matcher.group(this.group);

		if (hexCode.length() == 1) {
			// matched "\xh"

			// add leading 0 (necessary for java syntax)
			this.replaceWith("\\x0" + hexCode);
		}
	}

	/**
	 * Refactors a unicode character during the after-refactoring step
	 */
	private void afterRefactorUnicode() {
		String unicode = this.matcher.group(this.group);

		StringBuilder replacement = new StringBuilder();

		replacement.append("\\u");

		for (int i = unicode.length(); i < 4; i++) {
			replacement.append('0');
		}

		this.replaceWith(replacement.append(unicode).toString());
	}

	/**
	 * Refactors a posix class during the after-refactoring step
	 */
	private void afterRefactorPosixClass() {
		// if (patternSyntax == JAVA) {
		if (!this.inCharClass()) {
			throw this.error(POSIX_OUTSIDE_CLASS, this.matcher.start());
		}

		boolean negated = this.matcher.group(this.group).length() != 0;
		String posixClass = this.matcher.group(this.group + 1);

		if (posixClass.equals("word")) {
			this.replaceWith(negated ? "\\W" : "\\w");
		} else {
			String value = posixClasses.get(posixClass);

			if (value != null) {
				this.replaceWith("\\" + (negated ? "P" : "p") + "{" + value + "}");
			} else {
				throw this.error(UNKNOWN_POSIX_CLASS, this.matcher.start(this.group + 1));
			}
		}
		// }
	}

	/**
	 * Refactors a control character during the after-refactoring step
	 */
	private void afterRefactorControlCharacter() {
		char controlCharacter = this.matcher.group(this.group).charAt(0);
		int offset;

		if (controlCharacter >= 'a' && controlCharacter <= 'z') {
			offset = 'a' - 1;
		} else {
			offset = 'A' - 1;
		}

		this.replaceWith(String.format(hexCodeFormat, controlCharacter - offset));
	}

	/**
	 * Refactors the remaining parts during the pre-refactoring step
	 */
	private void preRefactorOthers() {
		if (this.match.equals("(")) {
			if (!this.inCharClass()) {
				this.increaseParenthesisDepth(DURING_PREREFACTOR);
			}
		} else if (this.match.equals(")")) {
			if (!this.inCharClass()) {
				this.preRefactorCloseParenthesis();
			}
		} else if (this.match.equals("|")) {
			if (!this.inCharClass()) {
				this.preRefactorPike();
			}
		} else if (this.match.equals("[")) {
			this.increaseCharClassDepth();
		} else if (this.match.equals("]")) {
			this.decreaseCharClassDepth();
		} else if (this.match.equals("{")) {
			// Tracked to prevent Android issue, where '}' must be escaped, but not in Java
			// Only track if not in character class (Android allows '}' in character group)
			if (!this.inCharClass()) {
				if (Boolean.FALSE.equals(this.isInCurlyBrace)) {
					// If not currently in a curly brace, mark as in one
					this.isInCurlyBrace = true;
				} else {
					// If already in a curly brace, the pattern has an error,
					// since shouldn't ever have two open curly braces without a close between them
					// If flag was already null, does nothing
					this.isInCurlyBrace = null;
				}
			}
		} else if (this.match.equals("}")) {
			// Tracked to prevent Android issue, where '}' must be escaped, but not in Java
			// Only track if not in character class (Android allows '}' in character group)
			if (!this.inCharClass()) {
				if (Boolean.TRUE.equals(this.isInCurlyBrace)) {
					// An open curly brace, followed at some point by a close one
					this.isInCurlyBrace = false;
				} else if (Boolean.FALSE.equals(this.isInCurlyBrace)) {
					// A closed curly brace by itself (escape it in the internal pattern)
					// (allows compiling regex when doing Android development, which doesn't allow '}' to be unescaped)
					this.replaceWith("\\}");
				}
			}
		} else if (this.match.equals("#")) {
			if (this.has(COMMENTS) && !this.inCharClass()) {
				this.handleStartComments();
				// parsePastLine();
			}
		} else if (this.match.startsWith("\\Q")) {
			// if (!supportedSyntax(QE_QUOTATION)) {
			if (this.isJava1_5) {
				int start = 2;
				int end = this.match.length() - (this.match.endsWith("\\E") ? 2 : 0);
				this.replaceWith(this.literal(this.match.substring(start, end)));
			}

			if (this.isInComments) {
				this.checkForLineTerminator();
			}
		} else if (this.isInComments) {
			this.checkForLineTerminator();
		}
	}

	/**
	 * Refactors the remaining parts during the refactoring step
	 */
	private void refactorOthers() {
		if (this.match.equals("(")) {
			if (!this.inCharClass()) {
				this.increaseParenthesisDepth(!DURING_PREREFACTOR);
			}
		} else if (this.match.equals(")")) {
			if (!this.inCharClass()) {
				this.refactorCloseParenthesis();
			}
		} else if (this.match.equals("|")) {
			if (!this.inCharClass()) {
				this.refactorPike();
			}
		} else if (this.match.equals("[")) {
			this.increaseCharClassDepth();
		} else if (this.match.equals("]")) {
			this.decreaseCharClassDepth();
		} else if (this.match.equals("#")) {
			if (this.has(COMMENTS) && !this.inCharClass()) {
				this.handleStartComments();
				// parsePastLine();
				// } else if (match.equals("\\Q")) {
				// skipQuoteBlock();
				// }
			}
			// Block isn't needed since duplicate of next block (FindBugs warning)
			//		} else if (this.match.startsWith("\\Q")) {
			//			// Skip quote block
			//
			//			if (this.isInComments) {
			//				this.checkForLineTerminator();
			//			}
		} else if (this.isInComments) {
			this.checkForLineTerminator();
		}
	}

	/**
	 * Refactors the remaining parts (after the refactoring step)
	 */
	private void afterRefactorOthers() {
		if (this.match.equals("(")) {
			if (!this.inCharClass()) {
				this.increaseParenthesisDepth(!DURING_PREREFACTOR);
			}
		} else if (this.match.equals(")")) {
			if (!this.inCharClass()) {
				this.afterRefactorCloseParenthesis();
			}
		} else if (this.match.equals("[")) {
			this.increaseCharClassDepth();
		} else if (this.match.equals("]")) {
			this.decreaseCharClassDepth();
		} else if (this.match.equals("#")) {
			if (this.has(COMMENTS) && !this.inCharClass()) {
				this.handleStartComments();
				// parsePastLine();
				// } else if (match.equals("\\Q")) {
				// skipQuoteBlock();
			}
		} else if (this.match.startsWith("\\Q")) {
			// Skip quote block

			if (this.isInComments) {
				this.checkForLineTerminator();
			}
		} else if (this.match.equals("\\X")) {
			this.replaceWith("(?>\\P{M}\\p{M}*)");
		} else if (this.isInComments) {
			this.checkForLineTerminator();
		}
	}

	/**
	 * Pattern to match the end of a quote block (either a "\E" or the end of the regex)
	 */
	//	private static final java.util.regex.Pattern endQuoteBlockPattern = java.util.regex.Pattern.compile("\\\\E|$");

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
	public String literal(final String s) {
		return Pattern.literal(s,
				this.inCharClass() ? Pattern.REGEX_CHAR_CLASS_METACHARACTERS : Pattern.REGEX_METACHARACTERS);
	}

	/**
	 * Refactors a close parenthesis during the pre-refactoring step
	 */
	private void preRefactorCloseParenthesis() {
		if (this.parenthesisDepth == 0) {
			throw this.error(UNMATCHED_PARENTHESES, this.matcher.start());
		}

		if (this.atRightDepth(this.handleElseBranch)) {
			this.handleElseBranch.pop();
		} else if (this.atRightDepth(this.branchReset)) {
			this.endBranchReset();
		}

		this.decreaseParenthesisDepth(DURING_PREREFACTOR);
	}

	/**
	 * Refactors a close parenthesis during the refactoring step
	 */
	private void refactorCloseParenthesis() {
		if (this.atRightDepth(this.addTestGroup)) {
			this.decreaseParenthesisDepth(!DURING_PREREFACTOR);
			this.replaceWith(")())");

			this.totalGroups++;

			String mappingName = this.addTestGroup.peek().mappingName;
			String namedMappingName = this.addTestGroup.peek().namedMappingName;

			// add a mapping from mapping name to its test condition group
			this.addTestingGroup(mappingName, this.totalGroups);

			if (namedMappingName != null) {
				this.addTestingGroup(namedMappingName, this.totalGroups);
			}

			this.addTestGroup.pop();

			// done last because inside pars is same depth
			this.decreaseParenthesisDepth(!DURING_PREREFACTOR);
		} else if (this.atRightDepth(this.handleElseBranch)) {
			// no else branch for condition (i.e. only a "then" branch)
			// e.g. (?('name')...)

			// add an empty else branch
			// if condition isn't true, matches the empty string

			Integer testConditionGroupI = this.handleElseBranch.peek().testConditionGroupI;

			if (testConditionGroupI == null) {
				// the specified group doesn't exist,
				// always use else branch
				this.replaceWith("|)");
			} else {
				int testConditionGroup = testConditionGroupI;

				if (testConditionGroup == TARGET_UNKNOWN) {
					String mappingName = this.handleElseBranch.peek().mappingName;

					// the specified group exists, but occurs later
					this.replaceWith("|\\g{" + this.addErrorTrace(this.matcher.start(this.group)) + "testF-"
							+ mappingName + "})");
					// } else if (testConditionGroup == BRANCH_RESET) {
					// String mappingName =
					// handleElseBranch.peek().mappingName;
					//
					// // all groups have already occurred
					// replaceWith("|" + failTestingGroup(mappingName) +
					// ")");
				} else {
					String mappingName = this.handleElseBranch.peek().mappingName;

					StringBuilder replacement = new StringBuilder();

					replacement.append('|');

					if (testConditionGroup == 0) {
						replacement.append(this.failTestingGroup(mappingName));
					} else {
						replacement.append(RefactorUtility.failTestingGroup(testConditionGroup));
					}

					replacement.append(')');

					if (mappingName.equals(this.endAssertCondMappingName())) {
						replacement.append(')');
						this.decreaseParenthesisDepth(!DURING_PREREFACTOR);
					}

					// the specified group has already occurred
					this.replaceWith(replacement.toString());
				}
			}

			// remove this pike state
			this.handleElseBranch.pop();

			// done last because inside pars is same depth
			this.decreaseParenthesisDepth(!DURING_PREREFACTOR);
		} else if (this.atRightDepth(this.branchReset)) {
			this.endBranchReset();

			// done last because inside pars is same depth
			this.decreaseParenthesisDepth(!DURING_PREREFACTOR);
		} else if (this.atRightDepth(this.handleEndAssertCond)) {
			String mappingName = this.handleEndAssertCond.peek().mappingName;

			if (mappingName.equals(this.endAssertCondMappingName())) {
				// the end of an assert condition

				this.replaceWith("))");

				// adjust parenthesis depth
				this.decreaseParenthesisDepth(!DURING_PREREFACTOR);
				this.decreaseParenthesisDepth(!DURING_PREREFACTOR);

				// remove the state
				this.handleEndAssertCond.pop();
			} else {
				this.totalGroups++;

				this.replaceWith(
						")())?+" + startNonCaptureGroup() + RefactorUtility.acceptTestingGroup(this.totalGroups));

				// adjust parenthesis depth
				this.decreaseParenthesisDepth(!DURING_PREREFACTOR);
				this.decreaseParenthesisDepth(!DURING_PREREFACTOR);
				this.increaseParenthesisDepth(!DURING_PREREFACTOR);

				// add state to handle else branch
				this.handleElseBranch
						.add(new MatchState(this.endAssertCondMappingName(), this.parenthesisDepth, this.totalGroups));

				// remove the state
				this.handleEndAssertCond.pop();
			}
		} else {
			// done last because inside pars is same depth
			this.decreaseParenthesisDepth(!DURING_PREREFACTOR);
		}
	}

	/**
	 * Refactors a close parenthesis during the after-refactoring step
	 */
	private void afterRefactorCloseParenthesis() {
		this.decreaseParenthesisDepth(!DURING_PREREFACTOR);
	}

	/**
	 * Steps to perform at the end (closing parenthesis) of a "branch reset"
	 * subpattern.
	 */
	private void endBranchReset() {
		int endGroup = this.branchReset.peek().endGroup;
		int endUnnamedGroup = this.branchReset.peek().endUnnamedGroup;

		if (endGroup > this.currentGroup) {
			this.currentGroup = endGroup;
		}

		if (endUnnamedGroup > this.unnamedGroup) {
			this.unnamedGroup = endUnnamedGroup;
		}

		this.branchReset.pop();
	}

	/**
	 * Refactors a pike during the pre-refactoring step
	 */
	private void preRefactorPike() {
		if (this.atRightDepth(this.handleElseBranch)) {
			if (this.handleElseBranch.peek().testConditionGroupI == this.parenthesisDepth) {
				throw this.error(CONDITIONAL_BRANCHES, this.matcher.start(this.group));
			}

			this.handleElseBranch.peek().testConditionGroupI = this.parenthesisDepth;
		} else if (this.atRightDepth(this.branchReset)) {
			this.branchReset();
		}
	}

	/**
	 * Refactors a pike during the factoring step
	 */
	private void refactorPike() {
		if (this.atRightDepth(this.handleElseBranch)) {
			Integer testConditionGroupI = this.handleElseBranch.peek().testConditionGroupI;
			String mappingName = this.handleElseBranch.peek().mappingName;

			if (testConditionGroupI != null) {
				int testConditionGroup = testConditionGroupI;

				if (testConditionGroup == TARGET_UNKNOWN) {
					// the specified group exists, but occurs later
					this.replaceWith("|\\g{" + this.addErrorTrace(this.matcher.start(this.group)) + "testF-"
							+ mappingName + "}");
					// } else if (testConditionGroup == BRANCH_RESET) {
					// // all groups have already occurred
					// replaceWith("|" + failTestingGroup(mappingName));
				} else if (testConditionGroup != 0) {
					// specific group

					this.replaceWith("|" + RefactorUtility.failTestingGroup(testConditionGroup));
				} else {
					// any group
					this.replaceWith("|" + this.failTestingGroup(mappingName));
				}
			}
			// else, the specified group doesn't exist
			// (i.e. always use else branch) - dealt with elsewhere

			this.handleElseBranch.pop();

			if (mappingName.equals(this.endAssertCondMappingName())) {
				this.handleEndAssertCond.add(new MatchState(mappingName, this.parenthesisDepth, testConditionGroupI));
			}
		} else if (this.atRightDepth(this.branchReset)) {
			this.branchReset();
		}
	}

	/**
	 * Actions to take when matching the start of a character class
	 */
	private void increaseCharClassDepth() {
		this.charClassDepth++;

		int end = this.matcher.end();
		int length = this.text.length();
		boolean squareBracket = end < length && this.text.charAt(end) == ']';

		boolean negSequareBracket = end < length - 2 && this.text.substring(end, end + 2).equals("^]");

		if (squareBracket || negSequareBracket) {
			// a "]" follows the "["

			// don't count the "]" as the end of the character class
			// increase the char class depth,
			// (it will be decreased upon hitting the "]")
			this.charClassDepth++;
		}
	}

	/**
	 * Actions to take when matching the end of a character class
	 */
	private void decreaseCharClassDepth() {
		// only decrease depth if actually in a character class
		// otherwise, treat the "]" as literal

		if (this.inCharClass()) {
			this.charClassDepth--;
		}
	}

	/**
	 * Indicates whether currently in a character class.
	 *
	 * @return <code>true</code>, if in a character class
	 */
	private boolean inCharClass() {
		return this.charClassDepth != 0;
	}

	//	/**
	//	 * Pattern to match a unix line separator (or the end of the pattern)
	//	 */
	//	private static final java.util.regex.Pattern unixLineSeparatorPattern = java.util.regex.Pattern.compile("\\n++|$");

	//	/**
	//	 * Pattern to match a line separator (based on line separators specified at
	//	 * http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#lt); also matches the end of the pattern
	//	 */
	//	private static final java.util.regex.Pattern lineSeparatorPattern = java.util.regex.Pattern
	//			.compile("[\\n\\r\u0085\u2028\u2029]++|$");

	/**
	 * Sets the parser "cursor" at the end of the current line.
	 */
	//	private void parsePastLine()
	//	{
	//		// Save the current pattern (restored at end)
	//		java.util.regex.Pattern currentPattern = matcher.pattern();
	//
	//		// Causes issues in Android (for some unknown reason)
	//		// (causes garbage collection to run wild if using comments
	//		// if comments contain named capture group, incorrectly thinks there's two)
	//		// (XXX: something is really up)
	//		if (has(UNIX_LINES))
	//		{
	//			matcher.usePattern(unixLineSeparatorPattern);
	//		}
	//		else
	//		{
	//			matcher.usePattern(lineSeparatorPattern);
	//		}
	//
	//		// Skip past the current line
	//		matcher.find();
	//
	//		// Restore the previous pattern
	//		matcher.usePattern(currentPattern);
	//	}

	/**
	 * Handle the start comments "#" when {@link Pattern#COMMENTS} is enabled
	 */
	private void handleStartComments() {
		this.isInComments = true;
	}

	/**
	 * Check for a line terminator and if one is found, end the comment block
	 */
	private void checkForLineTerminator() {
		if (this.has(UNIX_LINES)) {
			if (this.match.contains("\n")) {
				this.isInComments = false;
			}
		} else if (this.match.contains("\n") || this.match.contains("\r") || this.match.contains("\u0085")
				|| this.match.contains("\u2028") || this.match.contains("\u2029")) {
			// Line terminator [\n\r\u0085\u2028\u2029]
			this.isInComments = false;
		}
	}

	/**
	 * Steps taken when entering a new branch in a "branch reset"
	 * subpattern.
	 */
	private void branchReset() {
		this.branchReset.peek().updateEndGroup(this.currentGroup, this.unnamedGroup);
		this.currentGroup = this.branchReset.peek().startGroup;
		this.unnamedGroup = this.branchReset.peek().unnamedGroup;
	}

	/**
	 * Indicates whether currently in a "branch reset" pattern
	 *
	 * @return <code>true</code> if, and only if, currently in a
	 *         "branch reset" pattern
	 */
	private boolean inBranchReset() {
		return !this.branchReset.isEmpty();
	}

	/**
	 * <p>Replace the matched string with the specified (literal)
	 * replacement, and adds a new state to {@link #differences}.
	 * </p>
	 *
	 * @param replacement
	 *            the replacement
	 */
	private void replaceWith(final String replacement) {
		String quoteReplacement = Matcher.quoteReplacement(replacement);

		this.matcher.appendReplacement(this.result, quoteReplacement);

		// int length = matcher.end() - matcher.start();
		int start = this.result.length() - quoteReplacement.length();
		// int end = start + length;

		this.differences.replace0(start, this.match, replacement);

		// java.util.regex.Matcher
		// return matcher;
	}

	/**
	 * add javadoc comments.
	 */
	private static class MatchState implements State {
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
		MatchState(final String mappingName, final int parenthesisDepth, final Integer testConditionGroup) {
			this.mappingName = mappingName;
			this.parenthesisDepth = parenthesisDepth;
			this.testConditionGroupI = testConditionGroup;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getParenthesisDepth() {
			return this.parenthesisDepth;
		}

		/**
		 * Returns a string useful for debugging
		 *
		 * @return a string representation of this state
		 */
		@Override
		public String toString() {
			return this.mappingName + " -> (" + this.testConditionGroupI + "): " + this.parenthesisDepth;
		}
	}

	/**
	 * The Class BranchResetState.
	 */
	private static class BranchResetState implements State {
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
		BranchResetState(final int startGroup, final int unnamedGroups, final int parenthesisDepth) {
			this.startGroup = startGroup;
			this.unnamedGroup = unnamedGroups;
			this.parenthesisDepth = parenthesisDepth;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getParenthesisDepth() {
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
		void updateEndGroup(final int group, final int unnamed) {
			if (group > this.endGroup) {
				this.endGroup = group;
			}

			if (unnamed > this.endUnnamedGroup) {
				this.endUnnamedGroup = unnamed;
			}
		}

		/**
		 * Returns a string useful for debugging
		 *
		 * @return a string representation of this state
		 */
		@Override
		public String toString() {
			return super.toString();
		}
	}

	private static class AddTestGroupState implements State {
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
		AddTestGroupState(final String mappingName, final int parenthesisDepth, final String namedMappingName) {
			this.mappingName = mappingName;
			this.namedMappingName = namedMappingName;
			this.parenthesisDepth = parenthesisDepth;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getParenthesisDepth() {
			return this.parenthesisDepth;
		}
	}

	/**
	 * The Interface State.
	 */
	private static interface State {
		/**
		 * Gets the parenthesis depth.
		 *
		 * @return the parenthesis depth
		 */
		public int getParenthesisDepth();
	}
}