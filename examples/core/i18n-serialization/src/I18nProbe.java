// Theory: knowledge/01-java-core/internationalization.md
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18nProbe {
    public static class Messages_en extends ListResourceBundle {
        @Override protected Object[][] getContents() { return new Object[][]{{"welcome", "Welcome"}}; }
    }
    public static class Messages_de extends ListResourceBundle {
        @Override protected Object[][] getContents() { return new Object[][]{{"welcome", "Willkommen"}}; }
    }
    private static String cps(String text) {
        return text.codePoints().mapToObj(cp -> String.format(Locale.ROOT, "U+%04X", cp))
                .reduce((a,b) -> a+","+b).orElse("");
    }
    public static void main(String[] args) throws Exception {
        Locale us = Locale.US;
        Locale de = Locale.GERMANY;
        Locale tr = Locale.forLanguageTag("tr-TR");
        Locale script = Locale.forLanguageTag("sr-Latn-RS");
        System.out.println("L01=" + script.toLanguageTag());
        System.out.println("L02=" + script.getLanguage()+":"+script.getScript()+":"+script.getCountry());
        System.out.println("L03=" + us.toLanguageTag()+":"+de.toLanguageTag());
        NumberFormat numUS = NumberFormat.getNumberInstance(us);
        NumberFormat numDE = NumberFormat.getNumberInstance(de);
        numUS.setMinimumFractionDigits(2);
        numDE.setMinimumFractionDigits(2);
        System.out.println("L04=" + numUS.format(12345.67));
        System.out.println("L05=" + numDE.format(12345.67));
        DecimalFormatSymbols symbolsDE = DecimalFormatSymbols.getInstance(de);
        System.out.println("L06=" + cps(String.valueOf(symbolsDE.getDecimalSeparator())));
        System.out.println("L07=" + cps(String.valueOf(symbolsDE.getGroupingSeparator())));
        DecimalFormat exact = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(us));
        exact.setParseBigDecimal(true);
        BigDecimal parsed = (BigDecimal) exact.parse("67.25");
        System.out.println("L08=" + parsed.getClass().getSimpleName()+":"+parsed.toPlainString());
        ParsePosition pos = new ParsePosition(0);
        String rawNumber = "12,345.67junk";
        Number partial = numUS.parse(rawNumber, pos);
        System.out.println("L09=" + partial+":"+pos.getIndex()+":"+rawNumber.length());
        try { numUS.parse("not-a-number"); }
        catch (ParseException e) { System.out.println("L10=ParseException"); }
        DecimalFormat even = new DecimalFormat("0", DecimalFormatSymbols.getInstance(us));
        even.setRoundingMode(RoundingMode.HALF_EVEN);
        DecimalFormat up = new DecimalFormat("0", DecimalFormatSymbols.getInstance(us));
        up.setRoundingMode(RoundingMode.HALF_UP);
        System.out.println("L11=" + even.format(new BigDecimal("2.5")));
        System.out.println("L12=" + up.format(new BigDecimal("2.5")));
        System.out.println("L13=" + numUS.getMaximumFractionDigits());
        LocalDate date = LocalDate.of(2024, 3, 30);
        DateTimeFormatter longDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);
        System.out.println("L14=" + longDate.withLocale(us).format(date));
        System.out.println("L15=" + longDate.withLocale(de).format(date));
        System.out.println("L16=" + date);
        Instant instant = Instant.parse("2024-03-30T12:34:56Z");
        System.out.println("L17=" + DateTimeFormatter.ISO_INSTANT.withLocale(de).format(instant));
        System.out.println("L18=" + instant.atOffset(ZoneOffset.UTC).toInstant().equals(instant));
        System.out.println("L19=" + "TITLE".toLowerCase(Locale.ROOT));
        System.out.println("L20=" + "TITLE".toLowerCase(tr));
        System.out.println("L21=" + cps("TITLE".toLowerCase(tr)));
        System.out.println("L22=" + "é".getBytes(StandardCharsets.UTF_8).length);
        System.out.println("L23=" + "é".getBytes(StandardCharsets.UTF_16BE).length);
        System.out.println("L24=" + ResourceBundle.getBundle("I18nProbe$Messages", Locale.ENGLISH).getString("welcome"));
        System.out.println("L25=" + ResourceBundle.getBundle("I18nProbe$Messages", Locale.GERMAN).getString("welcome"));
        try { ResourceBundle.getBundle("I18nProbe$Messages", Locale.ENGLISH).getString("missing"); }
        catch (java.util.MissingResourceException e) { System.out.println("L26=MissingResourceException"); }
        System.out.println("L27=" + Locale.forLanguageTag("en-US-u-nu-latn").toLanguageTag());
        System.out.println("L28=" + NumberFormat.getIntegerInstance(us).format(67.8));
        System.out.println("L29=" + numDE.parse("12.345,67").doubleValue());
    }
}
