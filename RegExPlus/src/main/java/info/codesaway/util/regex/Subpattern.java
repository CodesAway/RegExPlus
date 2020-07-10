package info.codesaway.util.regex;

import static info.codesaway.util.regex.Refactor.CIRCULAR_SUBROUTINE;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The Class Subpattern.
 * 
 * <p>Used to store necessary values for subroutines
 */
class Subpattern
{
	/** The mapping name. */
	// private String mappingName;

	/** The start. */
	private int start = -1;

	/** The end. */
	private int end = -1;

	/** The flags. */
	private int flags;

	/** The subpattern. */
	private String subpattern;

	/** The refactor. */
	private Refactor refactor;

	private Set<String> dependsOnSubpatterns = new HashSet<String>();

	/* Details the current state for the subpattern */

	/** Whether this subpattern is in progress of being refactored */
	private boolean inProgress = false;

	/** Whether this subpattern has already been refactored */
	private boolean isRefactored = false;

	/**
	 * Instantiates a new subpattern.
	 * 
	 * @param refactor
	 *            the refactor
	 */
	// Subpattern(String mappingName)
	// {
	// this.mappingName = mappingName;
	// }

	/**
	 * Instantiates a new subpattern.
	 * 
	 * @param refactor
	 *            the refactor
	 */
	Subpattern(Refactor refactor)
	{
		this.refactor = refactor;
	}

	/**
	 * Gets the start.
	 * 
	 * @return the start
	 */
	public int getStart()
	{
		return start;
	}

	/**
	 * Sets the start.
	 * 
	 * @param start
	 *            the new start
	 */
	public void setStart(int start)
	{
		this.start = start;
	}

	/**
	 * Gets the end.
	 * 
	 * @return the end
	 */
	public int getEnd()
	{
		return end;
	}

	/**
	 * Sets the end.
	 * 
	 * @param end
	 *            the new end
	 */
	public void setEnd(int end)
	{
		this.end = end;
	}

	/**
	 * Gets the flags.
	 * 
	 * @return the flags
	 */
	public int getFlags()
	{
		return flags;
	}

	/**
	 * Sets the flags.
	 * 
	 * @param flags
	 *            the flags
	 */
	public void setFlags(int flags)
	{
		this.flags = flags;
	}

	/**
	 * Gets the pattern.
	 * 
	 * @param flags
	 *            the flags
	 * 
	 * @return the pattern
	 * @throws PatternSyntaxException
	 *             If the subpattern is circular (has a subroutine which depends on itself)
	 */
	public String getPattern(@SuppressWarnings("hiding") int flags)
	{
		if (!isRefactored) {
			if (inProgress)
				throw new PatternSyntaxException(CIRCULAR_SUBROUTINE, subpattern, -1);

			inProgress = true;
			subpattern = new Refactor(this, subpattern).toString();
			isRefactored = true;
			inProgress = false;
		}

		if (this.flags == flags)
			return subpattern;

		// One or more flags changed
		// Use inline modifiers to ensure subpattern's flags match original pattern's flags

		StringBuilder onFlags = new StringBuilder();
		StringBuilder offFlags = new StringBuilder();

		for (PatternFlag flag : PatternFlag.values())
		{
			if (!flag.hasInlineFlag())
				continue;

			boolean hasFlagBefore = (this.flags & flag.intValue()) != 0;
			boolean hasFlagAfter = (flags & flag.intValue()) != 0;

			if (hasFlagBefore != hasFlagAfter)
			{
				// Flag changed

				if (hasFlagBefore)
				{
					// Has flag before, but not after
					// (add to "on" flags)
					onFlags.append(flag.getInlineFlag());
				} else {
					// Has flag after, but not before
					// (add to "off" flags)
					offFlags.append(flag.getInlineFlag());
				}
			}
		}

		StringBuilder newFlags = new StringBuilder();

		newFlags.append(onFlags);

		if (offFlags.length() != 0)
		{
			newFlags.append('-').append(offFlags);
		}

		if (newFlags.length() != 0)
		{
			newFlags.insert(0, "(?").append(')');
		}

		// System.out.println(this.flags + " -> " + flags);
		// System.out.println(newFlags);
		// System.out.println(subpattern);

		return newFlags + subpattern;
		// return subpattern;
	}

	/**
	 * Sets the pattern.
	 * 
	 * @param pattern
	 *            the new pattern
	 */
	public void setPattern(String pattern)
	{
		this.subpattern = pattern;
	}

	/**
	 * Gets the pattern and whether the refactoring is done or in progress.
	 */
	@Override
	public String toString()
	{
		String state;

		if (isRefactored)
			state = "refactored";
		else if (inProgress)
			state = "refactoring";
		else
			state = "not refactored";

		return state + ": " + subpattern;
	}

	/**
	 * Gets the group counts.
	 * 
	 * @return the group counts
	 */
	public Map<String, Integer> getGroupCounts()
	{
		return refactor.getGroupCounts();
	}

	/**
	 * Gets the parent pattern.
	 * 
	 * @return the parent pattern
	 */
	public Pattern getParentPattern()
	{
		return refactor.getPattern();
	}

	/**
	 * Gets the parent subpatterns.
	 * 
	 * @return the parent subpatterns
	 */
	public Map<String, Subpattern> getParentSubpatterns()
	{
		return refactor.getSubpatterns();
	}

	/**
	 * Adds the subpattern dependency.
	 * 
	 * @param mappingName
	 *            the mapping name
	 * @return true, if successful
	 */
	public boolean addSubpatternDependency(String mappingName)
	{
		// System.out.println("Depends on subpatterns: " + dependsOnSubpatterns);
		return dependsOnSubpatterns.add(mappingName);
	}
}