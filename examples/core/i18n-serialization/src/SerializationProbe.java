// Theory: knowledge/01-java-core/serialization.md
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serial;
import java.io.Serializable;

public class SerializationProbe {
    static class Node implements Serializable {
        @Serial private static final long serialVersionUID = 67L;
        static String category = "before";
        final String name;
        transient String session;
        Node next;
        Node(String name, String session) { this.name = name; this.session = session; }
        @Serial private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            if (name == null || name.isBlank()) throw new InvalidObjectException("invalid name");
        }
    }
    record Receipt(String label, int value) implements Serializable {
        static int canonicalCalls;
        Receipt { canonicalCalls++; if (label == null) throw new IllegalArgumentException("label"); }
    }
    enum Kind { ACTIVE }
    static byte[] write(Object... objects) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            for (Object object : objects) out.writeObject(object);
            out.flush();
            return bytes.toByteArray();
        }
    }
    public static void main(String[] args) throws Exception {
        Node root = new Node("root", "local-session");
        Node child = new Node("child", "another-session");
        root.next = child;
        child.next = root;
        byte[] bytes = write(root, root);
        System.out.println("S01=" + (bytes.length > 0));
        System.out.println("S02=" + ObjectStreamClass.lookup(Node.class).getSerialVersionUID());
        Node.category = "after";
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            in.setObjectInputFilter(info -> info.depth() > 10 || info.references() > 30 || info.streamBytes() > 4096
                    ? ObjectInputFilter.Status.REJECTED : ObjectInputFilter.Status.UNDECIDED);
            Node restored = (Node) in.readObject();
            Node second = (Node) in.readObject();
            System.out.println("S03=" + restored.name);
            System.out.println("S04=" + (restored.session == null));
            System.out.println("S05=" + Node.category);
            System.out.println("S06=" + restored.next.name);
            System.out.println("S07=" + (restored.next.next == restored));
            System.out.println("S08=" + (restored == second));
            System.out.println("S09=" + (restored != root));
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            in.setObjectInputFilter(info -> info.serialClass() == Node.class
                    ? ObjectInputFilter.Status.REJECTED : ObjectInputFilter.Status.UNDECIDED);
            try { in.readObject(); }
            catch (InvalidClassException ex) { System.out.println("S10=InvalidClassException:filter"); }
        }
        try { write(new Object()); }
        catch (NotSerializableException ex) { System.out.println("S11=NotSerializableException"); }
        byte[] invalid = write(new Node("", "temporary"));
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(invalid))) {
            try { in.readObject(); }
            catch (InvalidObjectException ex) { System.out.println("S12=InvalidObjectException:validation"); }
        }
        Receipt receipt = new Receipt("invoice", 67);
        System.out.println("S13=" + Receipt.canonicalCalls);
        byte[] rec = write(receipt);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(rec))) {
            Receipt restored = (Receipt) in.readObject();
            System.out.println("S14=" + Receipt.canonicalCalls);
            System.out.println("S15=" + restored.label() + ":" + restored.value());
            System.out.println("S16=" + (restored != receipt));
        }
        byte[] en = write(Kind.ACTIVE);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(en))) {
            System.out.println("S17=" + (in.readObject() == Kind.ACTIVE));
        }
        System.out.println("S18=" + Serializable.class.isAssignableFrom(Node.class));
        System.out.println("S19=" + ObjectStreamClass.lookup(Receipt.class).getSerialVersionUID());
        System.out.println("S20=" + root.session);
    }
}
