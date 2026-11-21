// Theory: knowledge/01-java-core/exceptions.md
// Theory: knowledge/01-java-core/resources.md
// Research: docs/plans/CORE-EXCEPTIONS-RESOURCES.md
import java.io.*;
import java.util.*;

public class ExceptionResourceProbes {
  static final class R implements AutoCloseable {
    final String name;
    final StringBuilder log;
    final boolean broken;
    R(String name, StringBuilder log, boolean broken) {
      this.name = name; this.log = log; this.broken = broken;
      log.append("open-").append(name).append(';');
    }
    @Override public void close() throws IOException {
      log.append("close-").append(name).append(';');
      if (broken) throw new IOException("close-" + name);
    }
  }
  static R failedInit(StringBuilder log) throws IOException {
    log.append("init-fail;");
    throw new IOException("init-B");
  }
  static int finallyValue() {
    int n=67;
    try { return n; }
    finally { n=68; }
  }
  static int finallyOverrideReturn() {
    try { return 67; }
    finally { return 68; }
  }
  static String finallyOverrideThrow() {
    try { throw new IOException("work"); }
    finally { throw new IllegalStateException("finally"); }
  }
  static void checkedOperation() throws IOException { throw new IOException("io"); }
  static void wrappedOperation() {
    try { checkedOperation(); }
    catch (IOException e) { throw new IllegalStateException("service",e); }
  }
  public static void main(String[] args) throws Exception {
    System.out.println("P01=" + (new IOException() instanceof Exception) + "," + (new AssertionError() instanceof Error));
    try { checkedOperation(); } catch(IOException e) { System.out.println("P02=" + e.getMessage()); }
    try { wrappedOperation(); } catch(IllegalStateException e) { System.out.println("P03=" + e.getMessage() + "," + e.getCause().getClass().getSimpleName() + "," + e.getCause().getMessage()); }
    System.out.println("P04=" + finallyValue());
    System.out.println("P05=" + finallyOverrideReturn());
    try { finallyOverrideThrow(); } catch (Exception e) { System.out.println("P06=" + e.getClass().getSimpleName() + "," + e.getMessage() + "," + e.getSuppressed().length); }
    try { throw new IllegalArgumentException("unchecked"); } catch (RuntimeException e) { System.out.println("P07="+e.getMessage()); }
    try { throw new IOException("checked"); } catch (IOException | IllegalStateException e) {System.out.println("P08="+e.getClass().getSimpleName()); }
    StringBuilder log = new StringBuilder();
    try(R a = new R("A",log,false); R b = new R("B",log,false)) {log.append("body;");}
    System.out.println("P09=" + log);
    log = new StringBuilder();
    try(R a = new R("A",log,true); R b = new R("B",log,true)) {
      log.append("body;"); throw new IOException("work");
    } catch(IOException e) {System.out.println("P10="+log+"primary-"+e.getMessage()+",supp-"+Arrays.toString(Arrays.stream(e.getSuppressed()).map(Throwable::getMessage).toArray())); }
    log = new StringBuilder();
    try(R a = new R("A",log,true); R b = new R("B",log,true)) {log.append("body;");}
    catch(IOException e) {System.out.println("P11=" +log+"primary-"+e.getMessage()+",supp-"+Arrays.toString(Arrays.stream(e.getSuppressed()).map(Throwable::getMessage).toArray()));}
    log = new StringBuilder();
    try(R a = new R("A",log,false); R b = failedInit(log)) {log.append("body;");}
    catch(IOException e) {System.out.println("P12="+log+"primary-"+e.getMessage()+",supp-"+e.getSuppressed().length);}
    log = new StringBuilder();
    try(R a = new R("A",log,true); R b = failedInit(log)) {log.append("body;");}
    catch(IOException e) {System.out.println("P13="+log+"primary-"+e.getMessage()+",supp-"+Arrays.toString(Arrays.stream(e.getSuppressed()).map(Throwable::getMessage).toArray()));}
    log = new StringBuilder();
    R none = null;
    try(none) {log.append("body;");}
    System.out.println("P14="+log);
    log = new StringBuilder();
    R existing = new R("E",log,false);
    try(existing) {log.append("body;");}
    System.out.println("P15="+log);
    log = new StringBuilder();
    try(R a = new R("A",log,false)) {log.append("body;");}
    finally {log.append("finally;");}
    System.out.println("P16="+log);
    try(R a = new R("X",new StringBuilder(),true)) {}
    catch (IOException e) { System.out.println("P17="+e.getMessage()+",supp-"+e.getSuppressed().length); }
    AutoCloseable general = () -> { throw new Exception("auto"); };
    try(general) { System.out.println("P18=body"); }
    catch(Exception e) { System.out.println("P19="+e.getMessage()); }
    try { throw new IOException("x"); }
    catch (IOException e) { e.addSuppressed(new IllegalArgumentException("other")); System.out.println("P20="+e.getSuppressed()[0].getMessage()); }
  }
}
