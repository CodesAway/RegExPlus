Version 0.4 - December 21, 2013
Escape '}' in regular expressions (required for Android, but not for Java, but still honored in Java)
Added Pattern.getGroupCounts public method to return unmodifiable map of group name to group counts
Improved synchronization of pattern cache when compiling a pattern

Fix for bug using library in Android 
with quote block (\Q...\E) and including comments (lines that start with "#") with has comments flag (?x)

Version 0.3 - Sep 22, 2013
Optimized Matcher.matched / group methods and Pattern.literal

Version 0.2 - May 16, 2010

TODO: check for other methods to pass to MatchResult class
TODO: add checks to Matcher code - if MatchResult do something different
    for some methods, prevent execution (return immediately) ?? - Groovy can access them
    
[Add]
Patterns can now be lazily compiled. Previously, only serialized Patterns were lazily compiled (to mimic Java's Pattern class).
The default is to not lazily compile Patterns (to mimic Java)

Patterns are now cached when compiled or when converted from a java.util.regex.Pattern 
    to an info.codesaway.util.regex.Pattern, via the Pattern.valueOf method.
    
Introduced embedded flags for VERIFY_GROUPS (?v) and PERL_OCTAL (?o)
 
Integer group indexes can now be passed as Strings where a group or group name is taken. 
    - If a group is named the same as the number (e.g. "(?<1>group 1)"), it is treated as a group name
    - Otherwise, it is treated as a group index
    
Added basic subroutine support - both named/unnamed groups
Added (?(DEFINE)...) condition from PCRE
Added (*FAIL) and (*F) verbs from PCRE - always fail    

[Added Methods]
Pattern.literal - returns regex to match literal text; like quote, but escapes each character (so regex can
be used in a regex tool which doesn't support \Q..\E blocks)

MatchResult (and Matcher) implement Iterable<MatchResult>
MatchResult now contains pattern() method (from Matcher)

matchResult.isMatchResult instance method, to allow detecting whether an MatchResult is a Matcher 
    or a MatchResult (which although has the class name of Matcher, is immutable, 
    since it has no mutator methods and is a static copy of a Matcher)

Added methods to make using RegExPlus that make RegExPlus much more Groovy, and behave like Java regexes do in Groovy
    - getAt(int) has been implemented to mimic the groovy functionality (gets the nth match). 
        For a Matcher, this differs from Groovy, which returns a List; however, you can still use the double array syntax, due to how the overloading was implemented - differently for Matcher and MatchResult
        For a matchResult, getAt is an alias for group(int); this differs from 
    - getAt(String) is an alias for group(String), and always returns the specified group, for both Matchers and MatchResults
    
Added methods hasGroup to test whether a group exists in a pattern / matcher
Overloaded "matched" methods to allow testing if a group matched without throwing an error if the group doesn't exist
    - can be used to test if a group matched, even if the group doesn't exist in the pattern 
    (useful for dynamically built patterns which may not always have the same capture groups)
    
Added getReplacement method - able to replace capture groups in replacement string with their group value
    
[Fix]
Can now correctly compile the empty string. Due to an optimization, the empty string was incorrectly compiled. For example, Pattern.compile("").groupCount(""); would return a NullPointerException. It now (correctly) returns 0, since there are zero groups.
The website now mentions the version of the library, which is also specified in this change log. 

In Java 5, inside a character class escapes ampersands '&', when placed in a \Q..\E block, fixes Java bug (work on wording)

Some numeric ranges were incorrect, for example, (?Z[100..199]) would incorrectly match 0 to 99 - the leading 1 was dropped

Better organized code, moved inner classes to separate files when possible.

Added Pattern.getNaturalComparator, which sorts using Pattern.naturalCompareTo (natural string comparator)

Fixed bug with Matcher.keySet and entrySet methods

Version 0.1
Initial release.