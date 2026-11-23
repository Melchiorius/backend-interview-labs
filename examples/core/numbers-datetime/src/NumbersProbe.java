// Theory: knowledge/01-java-core/numbers.md
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.TreeSet;

public class NumbersProbe {
    public static void main(String[] args) {
        int max = Integer.MAX_VALUE;
        System.out.println("NM01=" + (max + 1));
        try { Math.addExact(max, 1); } catch (ArithmeticException e) { System.out.println("NM02=ArithmeticException:addExact"); }
        try { Math.multiplyExact(Long.MAX_VALUE, 2L); } catch (ArithmeticException e) { System.out.println("NM03=ArithmeticException:multiplyExact"); }
        System.out.println("NM04=" + (5 / 2) + ":" + (5.0 / 2));
        try { int zero = 0; System.out.println(5 / zero); }
        catch (ArithmeticException e) { System.out.println("NM05=ArithmeticException:divisionByZero"); }
        byte small = 67;
        System.out.println("NM06=" + ((Object) (small + 1)).getClass().getSimpleName());
        System.out.println("NM07=" + (0.1 + 0.2));
        System.out.println("NM08=" + Double.isNaN(Double.NaN) + ":" + (Double.NaN == Double.NaN));
        System.out.println("NM09=" + (Double.POSITIVE_INFINITY > 67.0) + ":" + (1.0 / 0.0));
        System.out.println("NM10=" + (0.0 == -0.0) + ":" + Double.compare(-0.0, 0.0));
        System.out.println("NM11=" + (Double.doubleToRawLongBits(-0.0) == Double.doubleToRawLongBits(0.0)));
        System.out.println("NM12=" + Double.isInfinite(67.0 / 0.0));
        BigDecimal text = new BigDecimal("0.1");
        BigDecimal binary = new BigDecimal(0.1);
        BigDecimal viaValueOf = BigDecimal.valueOf(0.1);
        System.out.println("NM13=" + text);
        System.out.println("NM14=" + binary);
        System.out.println("NM15=" + viaValueOf);
        System.out.println("NM16=" + text.equals(viaValueOf) + ":" + text.equals(binary));
        BigDecimal a = new BigDecimal("2.0");
        BigDecimal b = new BigDecimal("2.00");
        System.out.println("NM17=" + a.scale() + ":" + b.scale());
        System.out.println("NM18=" + a.precision() + ":" + b.precision());
        System.out.println("NM19=" + a.equals(b) + ":" + a.compareTo(b));
        System.out.println("NM20=" + new HashSet<>(java.util.List.of(a,b)).size() + ":" + new TreeSet<>(java.util.List.of(a,b)).size());
        System.out.println("NM21=" + a.unscaledValue());
        System.out.println("NM22=" + new BigDecimal("1.00").stripTrailingZeros().toPlainString());
        try { BigDecimal.ONE.divide(new BigDecimal("3")); }
        catch (ArithmeticException e) { System.out.println("NM23=ArithmeticException:nonterminating"); }
        System.out.println("NM24=" + BigDecimal.ONE.divide(new BigDecimal("3"), 2, RoundingMode.HALF_EVEN));
        System.out.println("NM25=" + BigDecimal.ONE.divide(new BigDecimal("3"), new MathContext(4, RoundingMode.HALF_UP)));
        System.out.println("NM26=" + new BigDecimal("2.5").setScale(0, RoundingMode.HALF_EVEN));
        System.out.println("NM27=" + new BigDecimal("2.5").setScale(0, RoundingMode.HALF_UP));
        System.out.println("NM28=" + new BigDecimal("3.5").setScale(0, RoundingMode.HALF_EVEN));
        try { new BigDecimal("2.55").setScale(1, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { System.out.println("NM29=ArithmeticException:roundingRequired"); }
        System.out.println("NM30=" + BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
        System.out.println("NM31=" + new BigDecimal("67.00").movePointRight(2).toPlainString());
        System.out.println("NM32=" + new BigDecimal("1.0").divide(new BigDecimal("8")));
        System.out.println("NM33=" + (Math.fma(2.0,3.0,1.0)));
    }
}
