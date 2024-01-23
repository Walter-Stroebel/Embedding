/*
 *  Copyright (c) 2017 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.ffmpeg;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.swing.JTree;

public class Utils {

    /**
     * Just compareNatural with dutch as language.
     */
    public static final Comparator<String> COMPARE_DUTCH_WITH_NUMBERS = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return compareNatural(dutch, o1, o2);
        }
    };
    /**
     * Simple implementation to sort natural names like (title givenname or
     * initials surname) as surname, (rest).
     */
    public static final Comparator<String> COMPARE_LAST_FIRST = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return compareLastFirst(o1, o2);
        }
    };
    /**
     * Simple implementation to sort, ignoring case, natural names like (title
     * givenname or initials surname) as surname, (rest).
     */
    public static final Comparator<String> COMPARE_LAST_FIRST_IGNORE_CASE = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return compareLastFirstIgnoreCase(o1, o2);
        }
    };
    /**
     * <p>
     * A string comparator that does case insensitive comparisons and handles
     * embedded numbers correctly.
     * </p>
     * <p>
     * <b>Do not use</b> if your app might ever run on any locale that uses more
     * than 7-bit ascii characters.
     * </p>
     */
    public static final Comparator<String> IGNORE_CASE_NATURAL_COMPARATOR_ASCII = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return compareNaturalIgnoreCaseAscii(o1, o2);
        }
    };
    /**
     * <p>
     * A string comparator that does case sensitive comparisons and handles
     * embedded numbers correctly.
     * </p>
     * <p>
     * <b>Do not use</b> if your app might ever run on any locale that uses more
     * than 7-bit ascii characters.
     * </p>
     */
    public static final Comparator<String> NATURAL_COMPARATOR_ASCII = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return compareNaturalAscii(o1, o2);
        }
    };
    /**
     * Actually French but does not matter for sorting words and numbers
     */
    private static Collator dutch = Collator.getInstance(Locale.FRANCE);
    /**
     * Because we keep defining this for no reason.
     */
    public static final Random random = new Random();
    private static final TreeSet<String> localTokens = new TreeSet<>();

    public static String intToRoman(int num) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] romanLetters = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder roman = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                num = num - values[i];
                roman.append(romanLetters[i]);
            }
        }
        return roman.toString();
    }

    public static JTree expandAllNodes(JTree tree) {
        int j = tree.getRowCount();
        int i = 0;
        while (i < j) {
            tree.expandRow(i);
            i += 1;
            j = tree.getRowCount();
        }
        return tree;
    }

    /**
     * Return the most recent file that matches the pattern.
     *
     * @param dir Where the file should live.
     * @param glob Pattern to match.
     * @return null or the most recent file.
     */
    public static File getMostRecent(File dir, String glob) {
        final String globPattern = "glob:**" + File.separatorChar + glob;

        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(globPattern);

        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return pathMatcher.matches(Paths.get(name));
            }
        });

        if (files == null || files.length == 0) {
            return null;
        }

        File mostRecentFile = files[0];
        for (File file : files) {
            if (file.lastModified() > mostRecentFile.lastModified()) {
                mostRecentFile = file;
            }
        }

        return mostRecentFile;
    }

    private static <Obj> void permutations(int k, List<List<Obj>> ret, List<Obj> of) {
        if (1 == k) {
            ret.add(new ArrayList<>(of));
        } else {
            permutations(k - 1, ret, of);
            for (int i = 0; i < k - 1; i++) {
                if (0 == (k & 1)) {
                    // swap of[i],of[k-1]
                    Obj tmp = of.get(i);
                    of.set(i, of.get(k - 1));
                    of.set(k - 1, tmp);
                } else {
                    // swap of[0],of[k-1]
                    Obj tmp = of.get(0);
                    of.set(0, of.get(k - 1));
                    of.set(k - 1, tmp);
                }
                permutations(k - 1, ret, of);
            }
        }
    }

    /**
     * Return all permutations of the passed list.
     *
     * Be aware this is a factorial!
     *
     * @param <Obj> Element type of the lists.
     * @param of Input list, for instance A,B,C.
     * @return List of lists, for example [A,B,C],[A,C,B],[B,A,C],...,[C,B,A].
     * Note that the permutations are not returned in any particular order.
     */
    public static <Obj> List<List<Obj>> permutations(List<Obj> of) {
        ArrayList<List<Obj>> ret = new ArrayList<>();
        if (!of.isEmpty()) {
            permutations(of.size(), ret, of);
        }
        return ret;
    }

    public static List<String> BaosToList(ByteArrayOutputStream baos) {
        return BaosToList(baos, StandardCharsets.UTF_8);
    }

    public static List<String> BaosToList(ByteArrayOutputStream baos, Charset cs) {
        return BytesToList(baos.toByteArray(), cs);
    }

    public static List<String> BytesToList(byte[] bytes) {
        return BytesToList(bytes, StandardCharsets.UTF_8);
    }

    public static List<String> BytesToList(byte[] bytes, Charset cs) {
        return CharsToList(new String(bytes, cs).toCharArray());

    }

    public static List<String> CharsToList(final char[] buf) {
        ArrayList<String> ret = new ArrayList<>();
        try (BufferedReader bfr = new BufferedReader(new Reader() {
            int bufOfs = 0;
            int bufLen = buf.length;

            @Override
            public void close() throws IOException {
                // will never be called
            }

            @Override
            public int read(char[] chars, int off, int _len) throws IOException {
                if (0 == _len) {
                    return 0;
                }
                int i;
                for (i = 0; i < _len && bufOfs + i < bufLen; i++) {
                    chars[off + i] = buf[bufOfs + i];
                }
                bufOfs += i;
                return i == 0 ? -1 : i;
            }
        })) {
            for (String s = bfr.readLine(); null != s; s = bfr.readLine()) {
                ret.add(s);
            }
        } catch (IOException ex) {
            // will not happen
        }
        return ret;

    }

    /**
     * CompareIgnoreCase that can handle null values.
     *
     * @param s1 String or null one
     * @param s2 String or null two
     * @return -1, 0, 1 where null &lt; something and null == null
     */
    public static int compareIgnoreCaseNull(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 0;
        }
        if (s1 == null && s2 != null) {
            return -1;
        }
        if (s1 != null && s2 == null) {
            return 1;
        }
        return s1.compareToIgnoreCase(s2);
    }

    /**
     * Simple implementation to sort natural names (title given name or initials
     * surname) as surname, (rest).
     *
     * @param o1 First name
     * @param o2 Second name
     * @return Ordering based on the last word, then any prior words.
     */
    public static int compareLastFirst(String o1, String o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null && o2 != null) {
            return -1;
        }
        if (o1 != null && o2 == null) {
            return 1;
        }
        if (o1.isEmpty() && o2.isEmpty()) {
            return 0;
        }
        if (o1.isEmpty() && !o2.isEmpty()) {
            return -1;
        }
        if (!o1.isEmpty() && o2.isEmpty()) {
            return 1;
        }
        int l1 = 0;
        int l2 = 0;
        if (o1.contains(" ")) {
            l1 = o1.lastIndexOf(' ');
        }
        if (o2.contains(" ")) {
            l2 = o2.lastIndexOf(' ');
        }
        int c = compareNatural(dutch, o1.substring(l1), o2.substring(l2));
        if (c != 0) {
            return c;
        }
        if (l1 == 0 && l2 == 0) {
            return 0;
        }
        if (l1 == 0 && l2 != 0) {
            return -1;
        }
        if (l1 != 0 && l2 == 0) {
            return 1;
        }
        return compareNatural(dutch, o1, o2);
    }

    /**
     * Simple implementation, ignoring case, to sort natural names (title given
     * name or initials surname) as surname, (rest).
     *
     * @param o1 First name
     * @param o2 Second name
     * @return Ordering based on the last word ignoring case, then any prior
     * words.
     */
    public static int compareLastFirstIgnoreCase(String o1, String o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null && o2 != null) {
            return -1;
        }
        if (o1 != null && o2 == null) {
            return 1;
        }
        if (o1.isEmpty() && o2.isEmpty()) {
            return 0;
        }
        if (o1.isEmpty() && !o2.isEmpty()) {
            return -1;
        }
        if (!o1.isEmpty() && o2.isEmpty()) {
            return 1;
        }
        return compareLastFirst(o1.toLowerCase(), o2.toLowerCase());
    }

    /**
     * <p>
     * Compares two strings using the given collator and comparing contained
     * numbers based on their numeric values.
     * </p>
     *
     * @param collator Which one to use.
     * @param s first string
     * @param t second string
     * @return zero if <code>s</code> and <code>t</code> are equal, a value less
     * than zero if <code>s</code> lexicographically precedes <code>t</code> and
     * a value larger than zero if <code>s</code> lexicographically follows
     * <code>t</code>
     */
    public static int compareNatural(Collator collator, String s, String t) {
        return compareNatural(s, t, true, collator);
    }

    /**
     * <p>
     * Compares two strings using the current locale's rules and comparing
     * contained numbers based on their numeric values.
     * </p>
     * <p>
     * This is probably the best default comparison to use.
     * </p>
     * <p>
     * If you know that the texts to be compared are in a certain language that
     * differs from the default locale's langage, then get a collator for the
     * desired locale ({@link java.text.Collator#getInstance(java.util.Locale)})
     * and pass it to
     * {@link #compareNatural(java.text.Collator, String, String)}
     * </p>
     *
     * @param s first string
     * @param t second string
     * @return zero if <code>s</code> and <code>t</code> are equal, a value less
     * than zero if <code>s</code> lexicographically precedes <code>t</code> and
     * a value larger than zero if <code>s</code> lexicographically follows
     * <code>t</code>
     */
    public static int compareNatural(String s, String t) {
        return compareNatural(s, t, false, Collator.getInstance());
    }

    /**
     * @param s first string
     * @param t second string
     * @param caseSensitive treat characters differing in case only as equal -
     * will be ignored if a collator is given
     * @param collator used to compare subwords that aren't numbers - if null,
     * characters will be compared individually based on their Unicode value
     * @return zero if <code>s</code> and <code>t</code> are equal, a value less
     * than zero if <code>s</code> lexicographically precedes <code>t</code> and
     * a value larger than zero if <code>s</code> lexicographically follows
     * <code>t</code>
     */
    private static int compareNatural(String s, String t, boolean caseSensitive, Collator collator) {
        int sIndex = 0;
        int tIndex = 0;
        int sLength = s.length();
        int tLength = t.length();
        while (true) {
            // both character indices are after a subword (or at zero)
            // Check if one string is at end
            if (sIndex == sLength && tIndex == tLength) {
                return 0;
            }
            if (sIndex == sLength) {
                return -1;
            }
            if (tIndex == tLength) {
                return 1;
            }
            // Compare sub word
            char sChar = s.charAt(sIndex);
            char tChar = t.charAt(tIndex);
            boolean sCharIsDigit = Character.isDigit(sChar);
            boolean tCharIsDigit = Character.isDigit(tChar);
            if (sCharIsDigit && tCharIsDigit) {
                // Compare numbers
                // skip leading 0s
                int sLeadingZeroCount = 0;
                while (sChar == '0') {
                    ++sLeadingZeroCount;
                    ++sIndex;
                    if (sIndex == sLength) {
                        break;
                    }
                    sChar = s.charAt(sIndex);
                }
                int tLeadingZeroCount = 0;
                while (tChar == '0') {
                    ++tLeadingZeroCount;
                    ++tIndex;
                    if (tIndex == tLength) {
                        break;
                    }
                    tChar = t.charAt(tIndex);
                }
                boolean sAllZero = sIndex == sLength || !Character.isDigit(sChar);
                boolean tAllZero = tIndex == tLength || !Character.isDigit(tChar);
                if (sAllZero && tAllZero) {
                    continue;
                }
                if (sAllZero && !tAllZero) {
                    return -1;
                }
                if (tAllZero) {
                    return 1;
                }
                int diff = 0;
                do {
                    if (diff == 0) {
                        diff = sChar - tChar;
                    }
                    ++sIndex;
                    ++tIndex;
                    if (sIndex == sLength && tIndex == tLength) {
                        return diff != 0 ? diff : sLeadingZeroCount - tLeadingZeroCount;
                    }
                    if (sIndex == sLength) {
                        if (diff == 0) {
                            return -1;
                        }
                        return Character.isDigit(t.charAt(tIndex)) ? -1 : diff;
                    }
                    if (tIndex == tLength) {
                        if (diff == 0) {
                            return 1;
                        }
                        return Character.isDigit(s.charAt(sIndex)) ? 1 : diff;
                    }
                    sChar = s.charAt(sIndex);
                    tChar = t.charAt(tIndex);
                    sCharIsDigit = Character.isDigit(sChar);
                    tCharIsDigit = Character.isDigit(tChar);
                    if (!sCharIsDigit && !tCharIsDigit) {
                        // both number sub words have the same length
                        if (diff != 0) {
                            return diff;
                        }
                        break;
                    }
                    if (!sCharIsDigit) {
                        return -1;
                    }
                    if (!tCharIsDigit) {
                        return 1;
                    }
                } while (true);
            } else {
                // Compare words
                if (collator != null) {
                    // To use the collator the whole subwords have to be compared - character-by-character comparision
                    // is not possible. So find the two subwords first
                    int aw = sIndex;
                    int bw = tIndex;
                    do {
                        ++sIndex;
                    } while (sIndex < sLength && !Character.isDigit(s.charAt(sIndex)));
                    do {
                        ++tIndex;
                    } while (tIndex < tLength && !Character.isDigit(t.charAt(tIndex)));
                    String as = s.substring(aw, sIndex);
                    String bs = t.substring(bw, tIndex);
                    int subwordResult = collator.compare(as, bs);
                    if (subwordResult != 0) {
                        return subwordResult;
                    }
                } else {
                    // No collator specified. All characters should be ascii only. Compare character-by-character.
                    do {
                        if (sChar != tChar) {
                            if (caseSensitive) {
                                return sChar - tChar;
                            }
                            sChar = Character.toUpperCase(sChar);
                            tChar = Character.toUpperCase(tChar);
                            if (sChar != tChar) {
                                sChar = Character.toLowerCase(sChar);
                                tChar = Character.toLowerCase(tChar);
                                if (sChar != tChar) {
                                    return sChar - tChar;
                                }
                            }
                        }
                        ++sIndex;
                        ++tIndex;
                        if (sIndex == sLength && tIndex == tLength) {
                            return 0;
                        }
                        if (sIndex == sLength) {
                            return -1;
                        }
                        if (tIndex == tLength) {
                            return 1;
                        }
                        sChar = s.charAt(sIndex);
                        tChar = t.charAt(tIndex);
                        sCharIsDigit = Character.isDigit(sChar);
                        tCharIsDigit = Character.isDigit(tChar);
                    } while (!sCharIsDigit && !tCharIsDigit);
                }
            }
        }
    }

    /**
     * <p>
     * Compares two strings using each character's Unicode value for non-digit
     * characters and the numeric values off any contained numbers.
     * </p>
     * <p>
     * (This will probably make sense only for strings containing 7-bit ascii
     * characters only.)
     * </p>
     *
     * @param s String one
     * @param t String two
     * @return zero if <code>s</code> and <code>t</code> are equal, a value less
     * than zero if <code>s</code> lexicographically precedes <code>t</code> and
     * a value larger than zero if <code>s</code> lexicographically follows
     * <code>t</code>
     */
    public static int compareNaturalAscii(String s, String t) {
        return compareNatural(s, t, true, null);
    }

    /**
     * <p>
     * Compares two strings using each character's Unicode value - ignoring
     * upper/lower case - for non-digit characters and the numeric values of any
     * contained numbers.
     * </p>
     * <p>
     * (This will probably make sense only for strings containing 7-bit ascii
     * characters only.)
     * </p>
     *
     * @param s String one
     * @param t String two
     * @return zero if <code>s</code> and <code>t</code> are equal, a value less
     * than zero if <code>s</code> lexicographically precedes <code>t</code> and
     * a value larger than zero if <code>s</code> lexicographically follows
     * <code>t</code>
     */
    public static int compareNaturalIgnoreCaseAscii(String s, String t) {
        return compareNatural(s, t, false, null);
    }

    public static String enc(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            // no fail
        }
        return value;
    }

    /**
     * Returns a comparator that compares contained numbers based on their
     * numeric values and compares other parts using the current locale's order
     * rules.
     * <p>
     * For example in German locale this will be a comparator that handles
     * umlauts correctly and ignores upper/lower case differences.
     * </p>
     *
     * @return
     * <p>
     * A string comparator that uses the current locale's order rules and
     * handles embedded numbers correctly.
     * </p>
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<String> getNaturalComparator() {
        Collator collator = Collator.getInstance();
        return getNaturalComparator(collator);
    }

    /**
     * Returns a comparator that compares contained numbers based on their
     * numeric values and compares other parts using the given collator.
     *
     * @param collator used for locale specific comparison of text (non-number)
     * subwords - must not be null
     * @return
     * <p>
     * A string comparator that uses the given Collator to compare subwords and
     * handles embedded numbers correctly.
     * </p>
     * @see #getNaturalComparator()
     */
    public static Comparator<String> getNaturalComparator(final Collator collator) {
        if (collator == null) {
            // it's important to explicitly handle this here - else the bug will manifest anytime later in possibly
            // unrelated code that tries to use the comparator
            throw new NullPointerException("collator must not be null");
        }
        return new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return compareNatural(collator, o1, o2);
            }
        };
    }

    /**
     * Returns a comparator that compares contained numbers based on their
     * numeric values and compares other parts based on each character's Unicode
     * value.
     *
     * @return
     * <p>
     * a string comparator that does case sensitive comparisons on pure ascii
     * strings and handles embedded numbers correctly.
     * </p>
     * <b>Do not use</b> if your app might ever run on any locale that uses more
     * than 7-bit ascii characters.
     * @see #getNaturalComparator()
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<String> getNaturalComparatorAscii() {
        return NATURAL_COMPARATOR_ASCII;
    }

    /**
     * Returns a comparator that compares contained numbers based on their
     * numeric values and compares other parts based on each character's Unicode
     * value while ignore upper/lower case differences. <b>Do not use</b> if
     * your app might ever run on any locale that uses more than 7-bit ascii
     * characters.
     *
     * @return
     * <p>
     * a string comparator that does case insensitive comparisons on pure ascii
     * strings and handles embedded numbers correctly.
     * </p>
     * @see #getNaturalComparator()
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<String> getNaturalComparatorIgnoreCaseAscii() {
        return IGNORE_CASE_NATURAL_COMPARATOR_ASCII;
    }

    /**
     * Utility function.
     *
     * @param out PrintWriter.
     * @param text Text with potential &lt;,&gt;, " or &amp; to encode.
     * @throws IOException If PrintWriter does.
     */
    public static void html(PrintWriter out, String text) throws IOException {
        out.print(html(text));
    }

    /**
     * Converts a string to a HTML entity encoded string, preserving any
     * existing entities. This method properly encodes a string like
     * &lt;&amp;EURO;&gt; to &amp;lt;&amp;EURO;&amp;gt;.
     *
     * @param text Text with potential &lt;,&gt;, " or &amp; to encode.
     * @return The text with any &lt;,&gt;, " or &amp; converted to &amp;lt;,
     * &amp;gt;, &amp;quot; and &amp;amp; while preserving any occurrences of
     * &amp;any;.
     */
    public static String html(String text) {
        if (text == null) {
            return "";
        }
        int amp = text.indexOf('&');
        if (amp >= 0) {
            int semi = text.indexOf(';', amp);
            if (semi > amp && semi - amp < 7) { // seems a valid html entity
                StringBuilder sb = new StringBuilder();
                if (amp > 0) {
                    sb.append(html(text.substring(0, amp)));
                }
                sb.append(text.substring(amp, semi));
                if (semi < text.length() - 1) {
                    sb.append(html(text.substring(semi + 1)));
                }
                return sb.toString();
            }
        }
        StringBuilder ret = new StringBuilder();
        for (char c : text.toCharArray()) {
            ret.append(htmlChar(c));
        }
        return ret.toString();
    }

    /**
     * Translates needed characters to entities.
     *
     * @param c Possibly dangerous character.
     * @return The character as a safe string.
     */
    public static String htmlChar(char c) {
        switch (c) {
            case '"':
                return ("&quot;");
            case '&':
                return ("&amp;");
            case '<':
                return ("&lt;");
            case '>':
                return ("&gt;");
            case '€':
                return ("&euro;");
            default:
                return (Character.toString(c));
        }
    }

    public static String listToString(List<String> list) {
        return listToString(list, System.lineSeparator(), true);
    }

    public static String listToString(List<String> list, String sep, boolean addBlank) {
        StringBuilder ret = new StringBuilder();
        for (String s : list) {
            if (addBlank || !s.isBlank()) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }
                ret.append(s);
            }
        }
        return ret.toString();
    }

    public static String nbsp(int count) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < count; i++) {
            ret.append("&nbsp;");
        }
        return ret.toString();
    }

    /**
     * Replaces any ASCII double quotes with &amp;quot;.
     *
     * @param text text to replace quotes in.
     * @return text with any quotes replaced.
     */
    public static String quote(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\"", "&quot;");
    }

    public static Object randomCollectionMember(final Collection col) {
        if (null == col) {
            return null;
        }
        int s = random.nextInt(col.size());
        int i = 0;
        for (Object r : col) {
            if (i == s) {
                return r;
            }
            i++;
        }
        return null;
    }

    /**
     * Random token.
     *
     * @param numChars Length for the token.
     * @return A random string.
     */
    public static String randomToken(int numChars) {
        StringBuilder ret = new StringBuilder("" + (char) (65 + random.nextInt(26)));
        for (int i = 1; i < numChars; i++) {
            ret.append((char) (97 + random.nextInt(26)));
        }
        return ret.toString();
    }

    /**
     * Random 6 char token.
     *
     * @return A random string.
     */
    public static String randomToken() {
        return randomToken(6);
    }

    /**
     * Next token or random 6 char token.
     *
     * @param toker StringTokenizer that should have a token.
     * @return The token or a random string.
     */
    public static String randomToken(StringTokenizer toker) {
        if (toker.hasMoreTokens()) {
            return toker.nextToken();
        }
        return randomToken();
    }

    /**
     * Given token or random 6 char token if the given one is null or blank.
     *
     * @param token Given token, can be null or blank..
     * @return Given token or random 6 char token if the given one is null or
     * blank.
     */
    public static String randomToken(String token) {
        if (null == token || token.isBlank()) {
            return randomToken();
        }
        return token;
    }

    /**
     * Given token or random 6 char token if the given one is null, blank or not
     * unique.
     *
     * If the given token is from an external source, there is a 1/308,915,776 (
     * about 0.00000032 % ) chance of a duplicate token.
     *
     * @param tokens Set of previously defined tokens.
     * @param token Given token, can be null or blank..
     * @return Given token or random 6 char token if the given one is null or
     * blank.
     */
    public static String uniqueToken(final TreeSet<String> tokens, String token) {
        synchronized (tokens) {
            if (null == token || token.isBlank()) {
                return uniqueToken(tokens, randomToken());
            }
            if (!tokens.add(token)) {
                return uniqueToken(tokens, randomToken());
            }
            return token;
        }
    }

    /**
     * Given token or random 6 char token if the given one is null, blank or not
     * unique.
     *
     * If the given token is from an external source, there is a 1/308,915,776 (
     * about 0.00000032 % ) chance of a duplicate token.
     *
     * @param token Given token, can be null or blank..
     * @return Given token or random 6 char token if the given one is null or
     * blank.
     */
    public static String uniqueToken(String token) {
        return uniqueToken(localTokens, token);
    }

    /**
     * A random 6 char token that is locally unique.
     *
     * @return A random 6 char token that is locally unique.
     */
    public static String uniqueToken() {
        return uniqueToken(localTokens, null);
    }

    /**
     * Basic String-to-tokens parse.
     *
     * @param string Input, will be made lowercase and trimmed.
     * @return any tokens in the input.
     */
    public static List<String> split(String string) {
        LinkedList<String> tokens = new LinkedList<>();
        if (null != string) {
            string = string.toLowerCase().trim();
            if (!string.isBlank()) {
                StringTokenizer toker = new StringTokenizer(string);
                while (toker.hasMoreTokens()) {
                    tokens.add(toker.nextToken());
                }
            }
        }
        return tokens;
    }

    public static List<String> wordsTokenizer(String source) {
        return wordsTokenizer(source, false);
    }

    /**
     * Simple breaks a (big) text up in lines. This is the default version which
     * trims leading and trailing space for each line and omits blank lines.
     * Only the meat.
     *
     * @param s The text.
     * @return A list of lines.
     */
    public static List<String> stringToLines(String s) {
        return stringToLines(s, true, false);
    }

    /**
     * Simple breaks a (big) text up in lines.
     *
     * @param s The text.
     * @param trim trim leading and trailing spaces on each line.
     * @param keepBlank keep empty lines (if not trimming, lines with only
     * spaces will still be kept).
     * @return A list of lines.
     */
    public static List<String> stringToLines(String s, boolean trim, boolean keepBlank) {
        StringTokenizer toker = new StringTokenizer(s, "\n\r\f");
        LinkedList<String> ret = new LinkedList<>();
        while (toker.hasMoreTokens()) {
            if (trim) {
                if (!keepBlank) {
                    String tok = toker.nextToken().trim();
                    if (!tok.isEmpty()) {
                        ret.add(tok);
                    }
                } else {
                    ret.add(toker.nextToken().trim());
                }
            } else {
                if (!keepBlank) {
                    String tok = toker.nextToken();
                    if (!tok.isEmpty()) {
                        ret.add(tok);
                    }
                } else {
                    ret.add(toker.nextToken());
                }
            }
        }
        return ret;
    }

    public static List<String> wordsTokenizer(String source, boolean alphaNum) {
        List<String> ret = new ArrayList<>();
        boolean inWord = false;
        StringBuilder word = new StringBuilder();
        for (char ch : source.toCharArray()) {
            if (inWord) {
                if (alphaNum) {
                    if (Character.isLetterOrDigit(ch)) {
                        word.append(ch);
                    } else {
                        ret.add(word.toString());
                        word.setLength(0);
                        inWord = false;
                    }
                } else {
                    if (Character.isLetter(ch)) {
                        word.append(ch);
                    } else {
                        ret.add(word.toString());
                        word.setLength(0);
                        inWord = false;
                    }
                }
            } else {
                if (alphaNum) {
                    if (Character.isLetterOrDigit(ch)) {
                        inWord = true;
                        word.append(ch);
                    }
                } else {
                    if (Character.isLetter(ch)) {
                        inWord = true;
                        word.append(ch);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Map a value to the specified range.
     *
     * @param srcMin Source minimum.
     * @param sourceMax Source maximum.
     * @param targetMin Target range start.
     * @param targetMax Target range end.
     * @param value Value to map.
     * @return Mapped value.
     */
    public static int iMap(final double srcMin, final double sourceMax, final double targetMin, final double targetMax, final double value) {
        double xf = (sourceMax - srcMin) / (targetMax - targetMin);
        int ret = (int) Math.round(((value - srcMin) / xf) + targetMin);
        if (ret > targetMax) {
            ret = (int) Math.round(targetMax);
        }
        if (ret < targetMin) {
            ret = (int) Math.round(targetMin);
        }
        return ret;
    }

    /**
     * Map a value to the specified range.
     *
     * @param srcMin Source minimum.
     * @param sourceMax Source maximum.
     * @param targetMin Target range start.
     * @param targetMax Target range end.
     * @param value Value to map.
     * @return Mapped value.
     */
    public static double map(final double srcMin, final double sourceMax, final double targetMin, final double targetMax, final double value) {
        double xf = (sourceMax - srcMin) / (targetMax - targetMin);
        return ((value - srcMin) / xf) + targetMin;
    }
}
