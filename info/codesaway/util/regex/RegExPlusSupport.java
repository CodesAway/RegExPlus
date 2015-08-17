package info.codesaway.util.regex;

/*
 * Copyright 2003-2009 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import info.codesaway.util.regex.Matcher;

/**
 * Used to store the last regex match.
 * 
 * <p>RegExPlus counterpart for the Groovy class, RegexSupport (org.codehaus.groovy.runtime.RegexSupport), 
 * which stores the last java.util.regex.Matcher object.</p>
 */
public class RegExPlusSupport
{
	private static final ThreadLocal<Matcher> CURRENT_MATCHER = new ThreadLocal<Matcher>();

	public static Matcher getLastMatcher()
	{
		return CURRENT_MATCHER.get();
	}

	public static Matcher setLastMatcher(Matcher matcher)
	{
		CURRENT_MATCHER.set(matcher);
		return matcher;
	}
}