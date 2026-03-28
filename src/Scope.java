import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final Scope parent;
    private final Map<String, Object> values = new HashMap<>();

    public Scope(Scope parent) {
        this.parent = parent;
    }

    public Scope getParent() {
        return parent;
    }

    private String normalize(String name) {
        return name == null ? null : name.toLowerCase();
    }

    public void define(String name, Object value) {
        values.put(normalize(name), value);
    }

    public boolean containsLocal(String name) {
        return values.containsKey(normalize(name));
    }

    public boolean canResolve(String name) {
        String key = normalize(name);
        Scope current = this;
        while (current != null) {
            if (current.values.containsKey(key)) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    public Object resolve(String name) {
        String key = normalize(name);
        Scope current = this;
        while (current != null) {
            if (current.values.containsKey(key)) {
                return current.values.get(key);
            }
            current = current.parent;
        }
        throw new RuntimeException("Undefined name: " + name);
    }

    public void assign(String name, Object value) {
        String key = normalize(name);
        Scope current = this;
        while (current != null) {
            if (current.values.containsKey(key)) {
                current.values.put(key, value);
                return;
            }
            current = current.parent;
        }
        throw new RuntimeException("Undefined variable: " + name);
    }
}
