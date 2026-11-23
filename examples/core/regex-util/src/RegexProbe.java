// Theory: knowledge/01-java-core/regex.md
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexProbe {
    public static void main(String[] args) {
        String regex = "\\d+";
        System.out.println("RG01=" + regex);
        System.out.println("RG02=" + Pattern.compile(regex).matcher("ID67").find());
        System.out.println("RG03=" + "ID67".matches(regex));
        System.out.println("RG04=" + Pattern.compile(regex).matcher("67").matches());
        System.out.println("RG05=" + Pattern.compile(regex).matcher("67x").lookingAt());
        Matcher digits = Pattern.compile(regex).matcher("a67b8");
        List<String> locations = new ArrayList<>();
        while (digits.find()) locations.add(digits.group() + "@" + digits.start() + ":" + digits.end());
        System.out.println("RG06=" + locations);
        try { Pattern.compile("(a)").matcher("a").group(); }
        catch (IllegalStateException ex) { System.out.println("RG07=IllegalStateException"); }
        Matcher named = Pattern.compile("(?<word>[a-z]+)-(?<n>\\d+)").matcher("abc-67");
        System.out.println("RG08=" + named.matches() + ":" + named.group("word") + ":" + named.group(2));
        System.out.println("RG09=" + Pattern.compile("(?:ab)+").matcher("abab").groupCount());
        Matcher greedy = Pattern.compile("a.+b").matcher("a1b2b"); greedy.find();
        Matcher reluctant = Pattern.compile("a.+?b").matcher("a1b2b"); reluctant.find();
        System.out.println("RG10=" + greedy.group() + ":" + reluctant.group());
        System.out.println("RG11=" + Pattern.compile("a.++b").matcher("a1b2b").find());
        Matcher zero = Pattern.compile("(?=a)").matcher("aa");
        List<Integer> zeros = new ArrayList<>(); while (zero.find()) zeros.add(zero.start());
        System.out.println("RG12=" + zeros);
        System.out.println("RG13=" + Pattern.compile("^cat$").matcher("dog\ncat").find());
        System.out.println("RG14=" + Pattern.compile("^cat$", Pattern.MULTILINE).matcher("dog\ncat").find());
        System.out.println("RG15=" + Pattern.compile("a.b").matcher("a\nb").matches());
        System.out.println("RG16=" + Pattern.compile("a.b", Pattern.DOTALL).matcher("a\nb").matches());
        System.out.println("RG17=" + Pattern.compile("abc", Pattern.CASE_INSENSITIVE).matcher("ABC").matches());
        System.out.println("RG18=" + Pattern.compile(Pattern.quote("a.b")).matcher("a.b").matches());
        System.out.println("RG19=" + Pattern.compile("a.b").matcher("axb").matches());
        System.out.println("RG20=" + "user-67".replaceAll("([a-z]+)-(\\d+)", "$2:$1"));
        System.out.println("RG21=" + "67".replaceAll("\\d+", Matcher.quoteReplacement("$\\literal")));
        try { Pattern.compile("["); } catch (PatternSyntaxException ex) { System.out.println("RG22=PatternSyntaxException:" + ex.getIndex()); }
        try { "a".replaceAll("(a)", "$9"); }
        catch (RuntimeException ex) { System.out.println("RG23=" + ex.getClass().getSimpleName()); }
        System.out.println("RG24=" + "a67b".replaceFirst("\\d+", "X"));
        System.out.println("RG25=" + Pattern.compile("\\d").matcher("٣").matches());
        System.out.println("RG26=" + Pattern.compile("\\d", Pattern.UNICODE_CHARACTER_CLASS).matcher("٣").matches());
        Matcher reset = Pattern.compile("a").matcher("aba"); reset.find(); int start = reset.start(); reset.reset(); reset.find();
        System.out.println("RG27=" + start + ":" + reset.start());
        Matcher opaque = Pattern.compile("\\bword").matcher("xxword").region(2,6);
        boolean before = opaque.find(); opaque.reset().region(2,6).useTransparentBounds(true); boolean after = opaque.find();
        System.out.println("RG28=" + before + ":" + after);
        System.out.println("RG29=" + Pattern.compile("a.b", Pattern.LITERAL).matcher("a.b").matches());
        System.out.println("RG30=" + Pattern.compile("a #comment\nb", Pattern.COMMENTS).matcher("ab").matches());
        System.out.println("RG31=" + "a,,b,".split(",").length + ":" + "a,,b,".split(",", -1).length);
        Matcher emoji = Pattern.compile(".").matcher("😀"); emoji.find();
        System.out.println("RG32=" + emoji.group().length() + ":" + emoji.end());
        System.out.println("RG33=" + Pattern.compile("(?=\\d{2})\\d").matcher("67").find());
    }
}
