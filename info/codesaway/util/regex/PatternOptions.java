package info.codesaway.util.regex;

import java.util.EnumSet;

/**
 * Superinterface of both {@link PatternFlag} and {@link PatternFlags}.
 * 
 * @author Me
 * @since 0.2
 */
public interface PatternOptions
{
	/**
	 * Returns the flags as an integer.
	 * 
	 * @return the bitmask of individual flags
	 */
	public int intValue();

	/**
	 * Gets a copy of the flags.
	 * 
	 * @return the flags
	 */
	public PatternFlags getFlags();

	/**
	 * Gets a copy of the flags as an <code>EnumSet</code>.
	 * 
	 * @return the flags
	 */
	public EnumSet<PatternFlag> asEnumSet();

	public PatternFlags plus(PatternOptions flags);

	public PatternFlags minus(PatternOptions flags);

	/* Bitwise operations */

	public PatternFlags or(PatternOptions flags);

	public PatternFlags and(PatternOptions flags);

	public PatternFlags xor(PatternOptions flags);

//	public PatternFlags leftShift(PatternFlagInterface flags);
//
//	public PatternFlags leftShift(Collection<PatternFlag> flags);

	public PatternFlags bitwiseNegate();

	// Work around, since Java doesn't allow static methods in an interface
	public static class StaticMethods
	{
		/**
		 * Never instantiate a utility class
		 * 
		 * @throws AssertionError
		 *             always
		 */
		private StaticMethods()
		{
			throw new AssertionError();
		}

		public static PatternFlags plus(PatternOptions flags1, PatternOptions flags2)
		{
			return new PatternFlags(flags1, flags2);
		}

		public static PatternFlags minus(PatternOptions flags1, PatternOptions flags2)
		{
			PatternFlags result = flags1.getFlags();
			result.removeAll(flags2.getFlags());
			return result;
		}

		/* Bitwise operations */

		public static PatternFlags or(PatternOptions flags1, PatternOptions flags2)
		{
			return plus(flags1, flags2);
		}

		public static PatternFlags and(PatternOptions flags1, PatternOptions flags2)
		{
			PatternFlags result = flags1.getFlags();
			result.retainAll(flags2.getFlags());
			return result;
		}

		public static PatternFlags xor(PatternOptions flags1, PatternOptions flags2)
		{
			return or(minus(flags1, flags2), minus(flags2, flags1));
		}

//		public static PatternFlags leftShift(PatternFlagInterface flags1, PatternFlagInterface flags2)
//		{
//			if (flags1 instanceof PatternFlags) {
//				PatternFlags flags = (PatternFlags) flags1;
//				flags.addAll(flags2.getFlags());
//				return flags;
//			} else
//				return plus(flags1, flags2);
//		}
//
//		public static PatternFlags leftShift(PatternFlagInterface flags1, Collection<PatternFlag> flags2)
//		{
//			if (flags1 instanceof PatternFlags) {
//				PatternFlags flags = (PatternFlags) flags1;								
//				flags.addAll(flags2);
//				return flags;
//			} else {
//				PatternFlags flags = new PatternFlags(flags2);
//				flags.addAll(flags1.getFlags());
//				return flags;
//			}
//		}

		public static PatternFlags bitwiseNegate(PatternOptions flags)
		{
			PatternFlags result = new PatternFlags(PatternFlag.values());
			result.removeAll(flags.getFlags());
			return result;
		}
	}
}
