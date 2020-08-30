package info.codesaway.util;

import java.util.List;
import java.util.Stack;

import info.codesaway.util.lcs.LcsString;

/**
 * List of differences
 */
public final class Differences {
	/**
	 * List of {@link Differences}
	 */
	private final Stack<Difference> changes;

	/**
	 * Constructs a new differences object initialized with no changes.
	 */
	public Differences() {
		this.changes = new Stack<>();
	}

	/**
	 * Adds all the differences in the passed object to this
	 * <code>Differences</code> object
	 *
	 * @param differences
	 *            differences to add
	 * @return <code>true</code> if this collection changed as a result of the
	 *         call
	 */
	public boolean addAll(final Differences differences) {
		return this.changes.addAll(differences.changes);
	}

	/**
	 * Applies each change, in order, to the original string and outputs the
	 * result
	 *
	 * @param original
	 *            original string on which to apply the changes
	 */
	// public void applyDifferences(String original)
	// {
	// StringBuffer result = new StringBuffer(original);
	//
	// for (Iterator<Difference> iterator = changes.iterator(); iterator
	// .hasNext();) {
	// iterator.next().applyStep(result);
	// }
	// }

	// public StringBuffer applyLastStep(StringBuffer replacement)
	// {
	// return changes.getLast().applyStep(replacement);
	// }

	/**
	 * Returns the original index (before the list of changes were applied)
	 * for the specified index.
	 *
	 * @param newIndex
	 *            the current index
	 * @return original index (before changes were applied)
	 * @throws IllegalArgumentException
	 *             If the specified index was not in the original string
	 */
	public int getOriginalIndex(final int newIndex) {
		int currentIndex = newIndex;

		// go from the top of the stack, down (descending order)
		for (int i = this.changes.size() - 1; i >= 0; i--) {
			currentIndex = this.changes.get(i).getOriginalIndexStep(currentIndex);
		}

		return currentIndex;
	}

	/**
	 * Returns the string representation of the internal changes.
	 *
	 * @return the internal changes as a string
	 */
	// @Override
	// public String toString()
	// {
	// return changes.toString();
	// }

	/**
	 * Adds a new difference to the list of changes.
	 *
	 * @param index
	 *            index for the insertion
	 * @param string
	 *            the string to insert
	 */
	// public void insert(int index, String string)
	// {
	// if (string.length() == 0)
	// return;
	//
	// changes.add(new InsertDifference(index, string));
	// }

	/**
	 * Adds a new difference to the list of changes.
	 *
	 * @param index
	 *            index for the character to remove
	 */
	// public void deleteCharAt(int index)
	// {
	// delete(index, index + 1);
	// }

	/**
	 * Adds a new difference to the list of changes.
	 *
	 * <p>If <code>start</code> and <code>end</code> are equal, no difference
	 * will be added.
	 *
	 * @param start
	 *            the begin index, inclusive
	 * @param end
	 *            the end index, exclusive.
	 */
	// public void delete(int start, int end)
	// {
	// if (start == end)
	// return;
	//
	// changes.add(new DeleteDifference(start, end));
	// }

	/**
	 * Adds a new difference to the list of changes.
	 *
	 * <p><b>Implementation notes</b>:</p>
	 *
	 * <p>The replacement consists of the insertions and deletions required to
	 * change the original string to the replacement string.</p>
	 *
	 * <p>The differences are detected using the {@link LcsString#getDiff()} method.</p>
	 *
	 * @param start
	 *            initial position for the replacement
	 * @param original
	 *            the original string
	 * @param replacement
	 *            the replacement string
	 */
	// public void replace(int start, String original, String replacement)
	// {
	// if (original.equals(replacement))
	// return;
	//
	// int end = start + original.length();
	// LcsString seq = new LcsString(original, replacement);
	//
	// changes.add(new ReplaceDifferencePlus(start, end, replacement,
	// seq.getDiff()));
	// }

	/**
	 * Adds a new difference to the list of changes.
	 *
	 * <p><b>Implementation notes</b>:</p>
	 *
	 * <p>The replacement consists of the insertions and deletions required to
	 * change the original string to the replacement string.</p>
	 *
	 * <p>The differences are detected using the {@link LcsString#getDiff0()} method.</p>
	 *
	 * @param start
	 *            initial position for the replacement
	 * @param original
	 *            the original string
	 * @param replacement
	 *            the replacement string
	 */
	public void replace0(final int start, final String original, final String replacement) {
		if (original.equals(replacement)) {
			return;
		}

		LcsString seq = new LcsString(original, replacement);

		int end = start + original.length();

		this.changes.add(new ReplaceDifferencePlus(start, end, replacement, seq
				.getDiff0()));
	}

	/**
	 * Adds a new difference to the list of changes.
	 *
	 * @param start
	 *            the begin index, inclusive
	 * @param end
	 *            the end index, exclusive.
	 * @param replacement
	 *            the replacement string
	 */
	// public void replace(int start, int end, String replacement)
	// {
	// changes.add(new ReplaceDifference(start, end, replacement));
	// }

	/**
	 * Class to handle an insertion
	 */
	private static class InsertDifference extends Difference {
		/**
		 * Constructs a new <code>InsertDifference</code>
		 *
		 * @param index
		 *            index for the insertion
		 * @param insertion
		 *            the string to insert
		 */
		InsertDifference(final int index, final String insertion) {
			// super(DifferenceOperation.INSERT, index,
			// index + insertion.length(), insertion);
			super(DifferenceOperation.INSERT, index, index + insertion.length());
		}

		/**
		 * {@inheritDoc}
		 */
		// @Override
		// public String toString()
		// {
		// return "Insert " + getReplacement() + " at " + getStart();
		// }

		// @Override
		// protected StringBuffer applyStep(StringBuffer input)
		// {
		// return input.insert(getStart(), getStr());
		// }

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected boolean inOriginal(final int currentIndex) {
			int start = this.getStart();
			int end = this.getEnd();

			return currentIndex < start || currentIndex > end;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected int getOriginalIndexStep_Private(final int currentIndex) {
			int newIndex = currentIndex;

			if (currentIndex > this.getStart()) {
				newIndex -= this.getLength();
			}

			return newIndex;
		}
	}

	/**
	 * Class to handle a deletion
	 */
	private static class DeleteDifference extends Difference {
		/**
		 * Constructs a new <code>DeleteDifference</code>
		 *
		 * @param start
		 *            the begin index, inclusive
		 * @param end
		 *            the end index, exclusive.
		 */
		DeleteDifference(final int start, final int end) {
			// super(DifferenceOperation.DELETE, start, end, "");
			super(DifferenceOperation.DELETE, start, end);
		}

		/**
		 * {@inheritDoc}
		 */
		// @Override
		// public String toString()
		// {
		// return "Delete " + getStart() + " to " + getEnd();
		// }

		// @Override
		// protected StringBuffer applyStep(StringBuffer input)
		// {
		// return input.delete(getStart(), getEnd());
		// }

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected int getOriginalIndexStep_Private(final int currentIndex) {
			int newIndex = currentIndex;

			if (currentIndex >= this.getStart()) {
				newIndex += this.getLength();
			}

			return newIndex;
		}
	}

	/**
	 * Class to handle a replacement
	 */
	/*
	 * <p>
	 * Unlike {@link ReplaceDifference}, the replacement is the insertions and
	 * deletions to change the original string to the replacement string.
	 * </p>
	 */
	private static class ReplaceDifferencePlus extends Difference {
		/**
		 * List of differences in this replacement
		 */
		private final Stack<Difference> differences;

		/**
		 * Constructs a new <code>ReplaceDifferencePlus</code>
		 *
		 * @param start
		 *            the begin index, inclusive
		 * @param end
		 *            the end index, exclusive.
		 * @param replacement
		 *            the replacement string
		 * @param diff
		 *            the list of differences
		 */
		ReplaceDifferencePlus(final int start, final int end, final String replacement,
				final List<String> diff) {
			// super(DifferenceOperation.REPLACE, start, end, replacement);
			super(DifferenceOperation.REPLACE, start, end);

			this.differences = new Stack<>();

			int itemStart = start;

			for (String string : diff) {
				// -1 because the leading character ("+", "-", or " ")
				int length = string.length() - 1;

				if (string.charAt(0) == '+') {
					// insert

					this.differences.add(new InsertDifference(start, string
							.substring(1)));
				} else if (string.charAt(0) == '-') {
					// remove

					int itemEnd = start + length;
					this.differences.add(new DeleteDifference(itemStart, itemEnd));
				}
			}
		}

		/**
		 * {@inheritDoc}
		 */
		// @Override
		// public String toString()
		// {
		// return "Replace " + getStart() + " to " + getEnd() + " with "
		// + getReplacement() + "\t" + differences;
		// }

		// @Override
		// protected StringBuffer applyStep(StringBuffer input)
		// {
		// for (Difference difference : differences) {
		// difference.applyStep(input);
		// }
		//
		// return input;
		// }

		/**
		 * {@inheritDoc}
		 *
		 * @throws IllegalArgumentException {@inheritDoc}
		 */
		@Override
		protected int getOriginalIndexStep_Private(final int currentIndex) {
			int newIndex = currentIndex;

			// go from the top of the stack, down (descending order)
			for (int i = this.differences.size() - 1; i >= 0; i--) {
				Difference difference = this.differences.get(i);

				newIndex = difference.getOriginalIndexStep(newIndex);
			}

			return newIndex;
		}
	}

	/**
	 * Class to handle a replacement
	 */
	// private static class ReplaceDifference extends Difference
	// {
	// /**
	// * Constructs a new <code>ReplaceDifferencePlus</code>
	// *
	// * @param start
	// * the begin index, inclusive
	// * @param end
	// * the end index, exclusive.
	// * @param replacement
	// * the replacement string
	// */
	// protected ReplaceDifference(int start, int end, String replacement)
	// {
	// super(DifferenceOperation.REPLACE, start, end, replacement);
	// }
	//
	// /**
	// * {@inheritDoc}
	// */
	// // @Override
	// // public String toString()
	// // {
	// // return "Replace " + getStart() + " to " + getEnd() + " with "
	// // + getReplacement();
	// // }
	//
	// // @Override
	// // protected StringBuffer applyStep(StringBuffer input)
	// // {
	// // return input.replace(getStart(), getEnd(), getStr());
	// // }
	//
	// /**
	// * {@inheritDoc}
	// */
	// @Override
	// protected boolean inOriginal(int currentIndex)
	// {
	// int start = getStart();
	// int end = start + getReplacement().length();
	//
	// return currentIndex < start || currentIndex > end;
	// }
	//
	// /**
	// * {@inheritDoc}
	// */
	// @Override
	// protected int getOriginalIndexStep_Private(int currentIndex)
	// {
	// int newIndex = currentIndex;
	//
	// if (currentIndex > getStart())
	// newIndex += getLength() - getReplacement().length();
	//
	// return newIndex;
	// }
	// }

	/**
	 * Abstract class for a difference
	 */
	private static abstract class Difference {
		/**
		 * The difference operation for this difference
		 */
		private final DifferenceOperation diffOp;

		/**
		 * The start index for the difference
		 */
		private final int start;

		/**
		 * The end index for the difference
		 */
		private final int end;

		/**
		 * The inserted string or replacement string
		 */
		// private final String replacement;

		/**
		 * Constructor for an insertion
		 *
		 * @param diffOp
		 *            the difference operation for this difference
		 * @param start
		 *            the start index for the difference
		 * @param end
		 *            the end index for the difference
		 */
		// Difference(DifferenceOperation diffOp, int start, int end,
		// String replacement)
		/*
		 * @param replacement
		 * the inserted string or replacement string
		 */
		Difference(final DifferenceOperation diffOp, final int start, final int end) {
			this.diffOp = diffOp;
			this.start = start;
			this.end = end;
			// this.replacement = replacement;
		}

		/**
		 * Returns the difference operation for this difference.
		 *
		 * @return the difference operation for this difference
		 */
		@SuppressWarnings("unused")
		public DifferenceOperation getDiffOp() {
			return this.diffOp;
		}

		/**
		 * Returns the start index for the difference.
		 *
		 * @return the start index for the difference
		 */
		public int getStart() {
			return this.start;
		}

		/**
		 * Returns the start index for the difference.
		 *
		 * @return the start index for the difference
		 */
		public int getEnd() {
			return this.end;
		}

		/**
		 * Returns the length of the replacement
		 *
		 * @return the length of the replacement
		 */
		public int getLength() {
			return this.getEnd() - this.getStart();
		}

		/**
		 * Returns the inserted / replacement string
		 *
		 * @return the inserted / replacement string
		 */
		// public String getReplacement()
		// {
		// return this.replacement;
		// }

		/**
		 * Returns a string representation of this difference.
		 *
		 * @return a string representation of this difference
		 */
		// @Override
		// public abstract String toString();

		// protected abstract StringBuffer applyStep(StringBuffer input);

		/**
		 * Returns whether the given index is in the original value.
		 *
		 * @param currentIndex
		 *            the current index
		 *
		 * @return whether the given index is in the original value
		 */
		protected boolean inOriginal(final int currentIndex) {
			return true;
		}

		/**
		 * Returns the index prior to performing this difference.
		 *
		 * @param currentIndex
		 *            the current index
		 * @return the offset by which this Difference affects the original
		 *         index
		 * @throws IllegalArgumentException
		 *             If the specified index was not in the original string
		 */
		final int getOriginalIndexStep(final int currentIndex) {
			if (!this.inOriginal(currentIndex)) {
				throw new IllegalArgumentException(
						"The specified index was not in the original string");
			}

			return this.getOriginalIndexStep_Private(currentIndex);
		}

		/**
		 * Returns the index prior to performing this difference.
		 *
		 * @param currentIndex
		 *            the current index
		 * @return the offset by which this <code>Difference</code> affects the
		 *         original index
		 * @throws IllegalArgumentException
		 *             If the specified index was not in the original string
		 */
		protected abstract int getOriginalIndexStep_Private(int currentIndex);
	}

	/**
	 * Enumeration of difference operations
	 */
	private static enum DifferenceOperation {

		/**
		 * An insertion operation
		 */
		INSERT,

		/**
		 * A deletion operation
		 */
		DELETE,

		/**
		 * A replacement operation
		 */
		REPLACE
	}
}