package info.codesaway.util.regex;

import java.io.Serializable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

// Implemented to ensure type-erasure isn't a problem when implementing methods
// Specifically, in Groovy, allows overloading operator using PatternFlags class
public class PatternFlags implements PatternOptions, Set<PatternFlag>, Cloneable, Serializable
{
	private final EnumSet<PatternFlag> flagsSet = EnumSet.noneOf(PatternFlag.class);

	public PatternFlags(PatternOptions... flags)
	{
		for (PatternOptions flag : flags) {
			addAll(flag.getFlags());
		}
	}

	public PatternFlags(Collection<PatternFlag> flags)
	{
		for (PatternFlag flag : flags) {
			add(flag);
		}
	}

	public PatternFlags(int flags)
	{
		for (PatternFlag flag : PatternFlag.values()) {
			int bitmask = flag.intValue();

			if ((flags & bitmask) != 0)
				add(flag);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int intValue()
	{
		return intValue(this);
	}

	public static int intValue(Collection<PatternFlag> flags)
	{
		int flagsBitmask = 0;

		for (PatternFlag flag : flags) {
			flagsBitmask |= flag.intValue();
		}

		return flagsBitmask;
	}

	/**
	 * {@inheritDoc}
	 */
	public EnumSet<PatternFlag> asEnumSet()
	{
		return flagsSet.clone();
	}

	/**
	 * {@inheritDoc}
	 */
	public PatternFlags getFlags()
	{
		return clone();
	}

	@Override
	public PatternFlags clone()
	{
		return new PatternFlags(flagsSet);
	}

	@Override
	public String toString()
	{
		return flagsSet.toString();
	}

	/* Groovy methods - makes RegExPlus groovier */

	/* Arithmetic operations */

	/**
	 * {@inheritDoc}
	 */
	public PatternFlags plus(PatternOptions flags)
	{
		return PatternOptions.StaticMethods.plus(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	public PatternFlags minus(PatternOptions flags)
	{
		return PatternOptions.StaticMethods.minus(this, flags);
	}

	/* Bitwise operations */

	/**
	 * {@inheritDoc}
	 */
	public PatternFlags or(PatternOptions flags)
	{
		return PatternOptions.StaticMethods.or(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	public PatternFlags and(PatternOptions flags)
	{
		return PatternOptions.StaticMethods.and(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	public PatternFlags xor(PatternOptions flags)
	{
		return PatternOptions.StaticMethods.xor(this, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	// public PatternFlags leftShift(PatternFlagInterface flags)
	// {
	// return PatternFlagInterface.StaticMethods.leftShift(this, flags);
	// }

	/**
	 * {@inheritDoc}
	 */
	// public PatternFlags leftShift(Collection<PatternFlag> flags)
	// {
	// return PatternFlagInterface.StaticMethods.leftShift(this, flags);
	// }

	public PatternFlags leftShift(PatternOptions flags)
	{
		addAll(flags.getFlags());
		return this;
	}

	public PatternFlags leftShift(Collection<PatternFlag> flags)
	{
		addAll(flags);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public PatternFlags bitwiseNegate()
	{
		return PatternOptions.StaticMethods.bitwiseNegate(this);
	}

	/* Set interface methods - delegated to <flags> */

	/**
	 * {@inheritDoc}
	 */
	public int size()
	{
		return flagsSet.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isEmpty()
	{
		return flagsSet.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean contains(Object o)
	{
		return flagsSet.contains(o);
	}

	/**
	 * {@inheritDoc}
	 */
	public Iterator<PatternFlag> iterator()
	{
		return flagsSet.iterator();
	}

	/**
	 * {@inheritDoc}
	 */
	public Object[] toArray()
	{
		return flagsSet.toArray();
	}

	/**
	 * {@inheritDoc}
	 */
	public <T> T[] toArray(T[] a)
	{
		return flagsSet.toArray(a);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean add(PatternFlag e)
	{
		return flagsSet.add(e);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean remove(Object o)
	{
		return flagsSet.remove(o);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean containsAll(Collection<?> c)
	{
		return flagsSet.containsAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean addAll(Collection<? extends PatternFlag> c)
	{
		return flagsSet.addAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean retainAll(Collection<?> c)
	{
		return flagsSet.retainAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean removeAll(Collection<?> c)
	{
		return flagsSet.removeAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	public void clear()
	{
		flagsSet.clear();
	}
}
