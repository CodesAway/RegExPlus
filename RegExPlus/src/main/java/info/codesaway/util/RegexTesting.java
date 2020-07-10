package info.codesaway.util;

import info.codesaway.util.regex.Pattern;

public class RegexTesting {
	public static void main(String[] args) {
//		String regex = "((?(1)2|3))";
		String regex = "(?Z[<2]";
		
		Pattern pattern = Pattern.compile(regex);
		System.out.println(pattern.internalPattern());
		
	}
}
