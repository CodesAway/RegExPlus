package info.codesaway.util.regex;

import static info.codesaway.util.regex.Matcher.getAbsoluteGroupIndex;
import static info.codesaway.util.regex.Matcher.noGroup;
import static info.codesaway.util.regex.Matcher.noNamedGroup;
import static info.codesaway.util.regex.Pattern.wrapIndex;
import static info.codesaway.util.regex.RefactorUtility.parseInt;

import java.util.Map;
import java.util.Map.Entry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

final class MatcherHelper {
	private MatcherHelper() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the mapped index for the specified group.
	 *
	 * @throws IndexOutOfBoundsException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed; only thrown if {@link #groupCount(String)
	 *             groupCount("[" + group + "]")} >= 2.
	 */
	static Integer getGroupIndex(final MatchResult m, final java.util.regex.MatchResult usedMatcher, final int group) {
		try {
			String groupName = wrapIndex(getAbsoluteGroupIndex(group,
					m.groupCount()));

			return getGroupIndex0(m, usedMatcher, groupName, "[0]");
		} catch (IndexOutOfBoundsException e) {
			throw noGroup(group);
		} catch (IllegalArgumentException e) {
			throw noGroup(group);
		}
	}

	/**
	 * Returns the mapped index for the specified group.
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	static Integer getGroupIndex(final MatchResult m, final java.util.regex.MatchResult usedMatcher,
			final String group) {
		// java.util.regex.Matcher matcher = fullGroupName.matcher(group);
		//
		// if (!matcher.matches())
		// throw noNamedGroup(group);
		//
		// String part1 = matcher.group(1);
		// String part2 = matcher.group(2);

		String[] parts = parseGroup(group);
		String part1 = parts[0];
		String part2 = parts[1];

		String groupName;
		try {
			groupName = m.pattern().normalizeGroupName(part1);
		} catch (IllegalArgumentException e) {
			throw noNamedGroup(group);
		}
		// String groupName = handleCase(matcher.group(1));
		String groupOccurrence = part2;

		if (groupOccurrence != null) {
			int occurrence;
			try {
				occurrence = getAbsoluteGroupIndex(
						parseInt(groupOccurrence),
						m.groupCount(groupName));
			} catch (IndexOutOfBoundsException e) {
				throw noGroup(group);
			} catch (IllegalArgumentException e) {
				// Changed so that when not using matcher, if the occurrence isn't a number, the correct error is thrown
				throw noNamedGroup(group);
			}

			if (groupName.length() == 0) {
				return getGroupIndex(m, usedMatcher, occurrence);
			} else if (occurrence != 0) {
				return getGroupIndex(m, usedMatcher, groupName, occurrence);
			}
		}

		return getGroupIndex0(m, usedMatcher, groupName, groupOccurrence == null
				? ""
						: "[" + groupOccurrence + "]");
	}

	/**
	 * Returns the mapped index for the specified group.
	 *
	 * @throws IllegalArgumentException
	 *             If there is no capturing group in the pattern
	 *             of the given group
	 */
	static Integer getGroupIndex(final MatchResult m, final java.util.regex.MatchResult usedMatcher,
			String groupName, int occurrence) {
		// groupName = handleCase(groupName);
		try {
			groupName = m.pattern().normalizeGroupName(groupName);
		} catch (IllegalArgumentException e) {
			throw noGroup(groupName, occurrence);
		}

		if (groupName.length() == 0) {
			return getGroupIndex(m, usedMatcher, occurrence);
		} else if (occurrence == 0) {
			return getGroupIndex0(m, usedMatcher, groupName, "[0]");
		}

		int passedOccurrence = occurrence;

		try {
			occurrence = getAbsoluteGroupIndex(occurrence,
					m.groupCount(groupName));
		} catch (IndexOutOfBoundsException e) {
			throw noGroup(groupName, passedOccurrence);
		}

		Integer groupIndexI = m
				.pattern()
				.getMappedIndex(groupName, occurrence);

		if (groupIndexI == null) {
			if (groupName.charAt(0) == '[' && occurrence == 1) {
				// ex. [3][1] - treat like [3]

				Integer tmp = m.pattern().getGroupMapping().get(groupName);

				// System.out.println(groupName + "\t" + passedOccurrence);

				if (tmp == null) {
					throw noGroup(groupName, passedOccurrence);
				}

				/*
				 * if found, return group
				 *
				 * can't be a "branch reset" pattern (-2), because if it were
				 * then the first occurrence MUST exist, and groupIndexI would
				 * not be null
				 */
				return tmp;
			}

			throw noGroup(groupName, passedOccurrence);
		}

		return groupIndexI;
	}

	/**
	 * Returns the occurrence of the first <i>matched</i> group with the given
	 * name.
	 *
	 * <p><a href="Pattern.html#branchreset">Branch reset</a> patterns and the {@link Pattern#DUPLICATE_NAMES} flag
	 * allow multiple capture groups with the same group name to exist.
	 * This method offers a way to determine which occurrence matched.</p>
	 *
	 * @param groupName
	 *            the name of the group
	 *
	 * @return the occurrence of the first <i>matched</i> group with the given
	 *         name.
	 *
	 */
	static int occurrence(final MatchResult m, final java.util.regex.MatchResult usedMatcher, final String groupName) {
		int groupCount = m.groupCount(groupName);

		if (groupCount == 0) {
			throw noNamedGroup(groupName);
		}

		int occurrence = 0;
		Integer groupIndexI;

		while ((groupIndexI = m.pattern().getMappedIndex(groupName, ++occurrence)) != null) {
			// if matched group

			if (usedMatcher.start(groupIndexI) != -1) {
				return occurrence;
			}
		}

		// if no group matched anything (i.e. all null)
		return -1;
	}

	/**
	 * Returns the mapped index for the first matching occurrence of the
	 * specified group name, or the first occurrence if there are no
	 * matches.
	 *
	 * @param occurrence0
	 *            string appended after group name in exception, if thrown
	 * @throws IllegalArgumentException
	 *             if the specified group never occurs
	 * @throws IllegalStateException
	 *             If no match has yet been attempted, or if the previous match
	 *             operation failed; only thrown if {@link #groupCount(String)
	 *             groupCount(groupName)} >= 2.
	 */
	static Integer getGroupIndex0(final MatchResult m,
			final java.util.regex.MatchResult usedMatcher,
			String groupName, final String occurrence0) {
		int groupCount = m.groupCount(groupName);

		if (groupCount == 0) {
			try {
				// Check if numbered group
				int groupNumber = Integer.parseInt(groupName);

				groupCount = m.groupCount(groupNumber);

				if (groupCount == 0) {
					throw noGroup(groupName + occurrence0);
				}

				groupName = wrapIndex(groupNumber);
			} catch (RuntimeException e) {
				if (occurrence0.length() == 0) {
					throw noNamedGroup(groupName);
				} else {
					throw noGroup(groupName + occurrence0);
				}
			}
		}

		Integer firstGroupIndex = m.pattern().getMappedIndex(groupName, 1);

		if (groupCount == 1) {
			return firstGroupIndex;
		}

		int occurrence = 0;
		Integer groupIndexI;

		while ((groupIndexI = m
				.pattern()
				.getMappedIndex(groupName, ++occurrence)) != null) {
			// if matched group

			if (usedMatcher.start(groupIndexI) != -1) {
				return groupIndexI;
			}
		}

		// if no group matched anything (i.e. all null)

		// return the index for the first group
		return firstGroupIndex;
	}

	static String getGroupName(final MatchResult m, final int groupIndex) {
		Map<String, Integer> groupMapping = m.pattern().getGroupMapping();

		for (Entry<String, Integer> entry : groupMapping.entrySet()) {
			if (entry.getValue().equals(groupIndex)) {
				if (entry.getKey().charAt(0) != '[') {
					String mappingName = entry.getKey();
					String groupName = mappingName.substring(0, mappingName
							.indexOf("["));

					int groupCount = m.groupCount(groupName);

					return groupCount == 1 ? groupName : mappingName;
				}
			}
		}

		return null;
	}

	/**
	 * @throws IllegalArgumentException
	 *             If key is not a <code>CharSequence</code> or
	 *             <code>Number</code>
	 * @since 0.2
	 */
	@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
	static boolean containsKey(final MatchResult m, final java.util.regex.MatchResult usedMatcher, final Object key) {
		if (key instanceof CharSequence) {
			try {
				getGroupIndex(m, usedMatcher, key.toString());
				return true;
			} catch (IllegalArgumentException e) {
				return false;
			}
		} else if (key instanceof Number) {
			Number index = (Number) key;

			try {
				getAbsoluteGroupIndex(index.intValue(), m.groupCount());
				return true;
			} catch (IndexOutOfBoundsException e) {
				return false;
			}
		}

		throw new IllegalArgumentException("Requires a group name/index: " + key);
	}

	private static String[] parseGroup(final String group) {
		String groupName;
		String occurrence;

		int bracketIndex = group.indexOf('[');

		if (bracketIndex != -1) {
			// Has bracket
			// [1], [-1], [1][1], groupName[occurrence]

			int lastBracketIndex = group.lastIndexOf('[');

			if (bracketIndex == lastBracketIndex) {
				// Has only one bracket
				// [1], [-1], groupName[occurrence]

				if (bracketIndex == 0) {
					// Starts with bracket
					// [1], [-1]

					if (!group.endsWith("]")) {
						throw noNamedGroup(group);
					}

					groupName = group;
					occurrence = null;
				} else {
					// Has bracket, not a beginning
					// groupName[occurrence]

					if (!group.endsWith("]")) {
						throw noNamedGroup(group);
					}

					groupName = group.substring(0, bracketIndex);

					// Get part between brackets
					occurrence = group.substring(bracketIndex + 1, group.length() - 1);
				}
			} else {
				// Has two brackets
				// [1][1], [1][-1]

				if (bracketIndex != 0) {
					throw noNamedGroup(group);
				}

				int closeBracket = group.indexOf(']');

				if (closeBracket != lastBracketIndex - 1) {
					throw noNamedGroup(group);
				}

				if (!group.endsWith("]")) {
					throw noNamedGroup(group);
				}

				groupName = group.substring(0, lastBracketIndex);

				if (groupName.indexOf('[', 1) != -1) {
					// Has multiple opening brackets
					// [[1]
					throw noNamedGroup(group);
				}

				occurrence = group.substring(lastBracketIndex + 1, group.length() - 1);
			}
		} else {
			// Has no bracket, just a group name
			groupName = group;
			occurrence = null;
		}

		// TODO: is there a better way to handle this case?
		if (groupName.startsWith("-")) {
			// Not allowed; for example, -1 isn't a valid group name ('-' isn't allowed in group names)
			throw noNamedGroup(group);
		}

		return new String[] { groupName, occurrence };
	}
}
