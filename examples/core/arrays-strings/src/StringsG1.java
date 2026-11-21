// Theory: knowledge/01-java-core/arrays.md
// Theory: knowledge/01-java-core/strings.md
// Research: docs/plans/CORE-ARRAYS-STRINGS.md (accepted G1 section 6.2)
import java.util.*;

public class StringsG1 {
    static void show(String id, Object value) { System.out.println(id + "=" + value); }
    static String fails(Runnable action) {
        try { action.run(); return "NO_ERROR"; }
        catch (RuntimeException ex) { return ex.getClass().getSimpleName(); }
    }
    public static void main(String[] args) {
        String immutable = "a";
        String joined = immutable.concat("b");
        show("S01", immutable + "," + joined);
        show("S02", "Java".equals(new String("Java")) + "," + ("Java" == new String("Java")));
        show("S03", (("Ja" + "va") == "Java") + "," + (new String("Java").intern() == "Java"));
        show("S04", (67 + 1 + "x") + "," + ("x" + 67 + 1));
        String emoji = new String(Character.toChars(0x1F600));
        String mixed = "A" + emoji + "Z";
        show("S05", mixed.length() + "," + mixed.codePointCount(0,mixed.length()));
        show("S06", Character.isHighSurrogate(mixed.charAt(1)) + "," + mixed.codePointAt(1));
        show("S07", mixed.offsetByCodePoints(0,2) + "," + mixed.substring(1,3).equals(emoji));
        show("S08", mixed.codePoints().count());
        String spaced = "\u2003X\u2003";
        show("S09", spaced.trim().length() + "," + spaced.strip().length());
        show("S10", "\u2003".isBlank() + "," + "".isEmpty() + "," + " ".isEmpty());
        show("S11", Arrays.toString("a.b".split("\\.")));
        show("S12", Arrays.toString("a,b,,".split(",")) + "," + Arrays.toString("a,b,,".split(",",-1)));
        show("S13", String.join("|", "a", null));
        show("S14", String.valueOf((Object)null));
        show("S15", fails(() -> String.valueOf((char[])null)));
        show("S16", "a.c".replace(".", "!") + "," + "a.c".replaceAll(".", "!"));
        show("S17", "\u00E9".equals("e\u0301"));
        StringBuilder b = new StringBuilder("ab"); b.append(67).insert(0, "[").append(']');
        show("S18", b + "," + b.length());
        int oldLen = b.length(); b.ensureCapacity(30);
        show("S19", (b.length() == oldLen) + "," + (b.capacity() >= 30));
        show("S20", new StringBuilder("a").equals(new StringBuilder("a")) + "," + "a".contentEquals(new StringBuilder("a")));
        show("S21", new StringBuilder().append((String)null).toString());
        show("S22", fails(() -> new StringBuilder().append((char[])null)));
        StringBuffer buff = new StringBuffer("x"); buff.append("y");
        show("S23", buff.toString());
        StringBuilder padded = new StringBuilder("x"); padded.setLength(3);
        show("S24", padded.length() + "," + ((int)padded.charAt(1)) + "," + ((int)padded.charAt(2)));
        show("S25", String.format(Locale.ROOT, "%d", 67));
        show("S26", "abc".substring(1, 3));
        show("S27", fails(() -> "abc".charAt(3)));
        show("S28", fails(() -> "a".substring(2)));
    }
}
