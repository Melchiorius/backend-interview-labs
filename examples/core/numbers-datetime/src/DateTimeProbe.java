// Theory: knowledge/01-java-core/datetime.md
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.zone.ZoneRules;
import java.util.Locale;

public class DateTimeProbe {
    public static void main(String[] args) {
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        Instant instant = Instant.parse("2024-03-30T12:34:56Z");
        Clock fixed = Clock.fixed(instant, ZoneOffset.UTC);
        System.out.println("DT01=" + Instant.now(fixed));
        System.out.println("DT02=" + LocalDate.now(fixed));
        System.out.println("DT03=" + LocalTime.now(fixed));
        System.out.println("DT04=" + LocalDateTime.now(fixed));
        ZonedDateTime berlinTime = instant.atZone(berlin);
        System.out.println("DT05=" + berlinTime);
        System.out.println("DT06=" + instant.atZone(tokyo));
        System.out.println("DT07=" + berlinTime.toInstant().equals(instant));
        System.out.println("DT08=" + OffsetDateTime.ofInstant(instant, berlin));
        LocalDateTime wall = LocalDateTime.of(2024,3,31,2,30);
        ZoneRules rules = berlin.getRules();
        System.out.println("DT09=" + wall);
        System.out.println("DT10=" + rules.getValidOffsets(wall).size());
        System.out.println("DT11=" + wall.atZone(berlin));
        try { ZonedDateTime.ofStrict(wall, ZoneOffset.ofHours(1), berlin); }
        catch (DateTimeException e) { System.out.println("DT12=DateTimeException:gap"); }
        LocalDateTime repeated = LocalDateTime.of(2024,10,27,2,30);
        System.out.println("DT13=" + rules.getValidOffsets(repeated));
        ZonedDateTime first = repeated.atZone(berlin);
        System.out.println("DT14=" + first);
        ZonedDateTime second = first.withLaterOffsetAtOverlap();
        System.out.println("DT15=" + second);
        System.out.println("DT16=" + Duration.between(first.toInstant(),second.toInstant()).toMinutes());
        System.out.println("DT17=" + berlinTime.withZoneSameInstant(tokyo));
        System.out.println("DT18=" + berlinTime.withZoneSameLocal(tokyo));
        System.out.println("DT19=" + berlinTime.withZoneSameInstant(tokyo).toInstant().equals(instant));
        System.out.println("DT20=" + berlinTime.withZoneSameLocal(tokyo).toInstant().equals(instant));
        ZonedDateTime before = LocalDateTime.of(2024,3,30,12,0).atZone(berlin);
        ZonedDateTime plusDay = before.plusDays(1);
        ZonedDateTime plusHours = before.plusHours(24);
        System.out.println("DT21=" + before);
        System.out.println("DT22=" + plusDay);
        System.out.println("DT23=" + plusHours);
        System.out.println("DT24=" + Duration.between(before.toInstant(),plusDay.toInstant()).toHours());
        System.out.println("DT25=" + Period.between(before.toLocalDate(),plusDay.toLocalDate()));
        System.out.println("DT26=" + ChronoUnit.DAYS.between(before.toLocalDate(),plusDay.toLocalDate()));
        System.out.println("DT27=" + Duration.between(before.toInstant(),plusHours.toInstant()).toHours());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm XXX", Locale.ROOT).withZone(berlin);
        System.out.println("DT28=" + formatter.format(instant));
        System.out.println("DT29=" + Instant.parse("2024-03-30T12:34:56Z"));
        try { LocalDate.parse("2024-02-30"); }
        catch (DateTimeParseException e) { System.out.println("DT30=DateTimeParseException"); }
        try { Instant.from(LocalDate.of(2024,3,30)); }
        catch (DateTimeException e) { System.out.println("DT31=DateTimeException:missingInstant"); }
        try { LocalDate.of(2024,3,30).getLong(java.time.temporal.ChronoField.HOUR_OF_DAY); }
        catch (UnsupportedTemporalTypeException e) { System.out.println("DT32=UnsupportedTemporalTypeException"); }
        System.out.println("DT33=" + LocalDate.of(2024,3,30).plusDays(1));
        System.out.println("DT34=" + (fixed.getZone().equals(ZoneOffset.UTC)));
        System.out.println("DT35=" + berlin.getId());
        System.out.println("DT36=" + instant.getEpochSecond());
        System.out.println("DT37=" + LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
        System.out.println("DT38=" + berlinTime.toLocalDateTime());
    }
}
