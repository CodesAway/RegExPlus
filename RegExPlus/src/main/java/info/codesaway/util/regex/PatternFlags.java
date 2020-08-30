package info.codesaway.util.regex;

import java.io.Serializable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

// Implemented to ensure type-erasure isn't a problem when implementing methods
// Specifically, in Groovy, allows overloading operator using PatternFlags class
public final class PatternFlags implements PatternOptions, Set<PatternFlag>, Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	private final EnumSet<PatternFlag> flagsSet = EnumSet.noneOf(PatternFlag.class);

	public PatternFlags(final PatternOptions... flags) {
		for (PatternOptions flag : flags) {
			this.addAll(flag.getFlags());
		}
	}

	public PatternFlags(final Collection<PatternFlag> flags) {
		for (PatternFlag flag : flags) {
			this.add(flag);
		}
	}

	public PatternFlags(final int flags) {
		for (PatternFlag flag : PatternFlag.values()) {
			int bitmask = flag.intValue();

			if ((flags & bitmask) != 0) {
				this.add(flag);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int intValue() {
		return intValue(this);
	}

	public static int intValue(final Collection<PatternFlag> flags) {
		int flagsBitmask = 0;

		for (PatternFlag flag : flags) {
			flagsBitmask |= flag.intValue();
		}

		return flagsBitmask;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EnumSet<PatternFlag> asEnumSet() {
		return this.flagsSet.clone();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags getFlags() {
		return this.clone();
	}

	@Override
	public PatternFlags clone() {
		try {
			PatternFlags clone = (PatternFlags) super.clone();
			clone.addAll(this.flagsSet);
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	@Override
	public String toString() {
		return this.flagsSet.toString();
	}

	/* Groovy methods - makes RegExPlus groovier */

	/* Arithmetic operations */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags plus(final PatternOptions flags) {
		return PatternOptions.plus(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags minus(final PatternOptions flags) {
		return PatternOptions.minus(this, flags);
	}

	/* Bitwise operations */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags or(final PatternOptions flags) {
		return PatternOptions.or(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags and(final PatternOptions flags) {
		return PatternOptions.and(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags xor(final PatternOptions flags) {
		return PatternOptions.xor(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	// public PatternFlags leftShift(PatternFlagInterface flags)
	// {
	// return PatternFlagInterface.leftShift(this, flags);
	// }

	/**
	 * {@inheritDoc}
	 */
	// public PatternFlags leftShift(Collection<PatternFlag> flags)
	// {
	// return PatternFlagInterface.leftShift(this, flags);
	// }

	public PatternFlags leftShift(final PatternOptions flags) {
		this.addAll(flags.getFlags());
		return this;
	}

	public PatternFlags leftShift(final Collection<PatternFlag> flags) {
		this.addAll(flags);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PatternFlags bitwiseNegate() {
		return PatternOptions.bitwiseNegate(this);
	}

	/* Set interface methods - delegated to <flags> */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return this.flagsSet.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return this.flagsSet.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(final Object o) {
		return this.flagsSet.contains(o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<PatternFlag> iterator() {
		return this.flagsSet.iterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] toArray() {
		return this.flagsSet.toArray();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T[] toArray(final T[] a) {
		return this.flagsSet.toArray(a);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(final PatternFlag e) {
		return this.flagsSet.add(e);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(final Object o) {
		return this.flagsSet.remove(o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(final Collection<?> c) {
		return this.flagsSet.containsAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(final Collection<? extends PatternFlag> c) {
		return this.flagsSet.addAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(final Collection<?> c) {
		return this.flagsSet.retainAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(final Collection<?> c) {
		return this.flagsSet.removeAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		this.flagsSet.clear();
	}
}
