import java.io.Serializable;

public class Wrapper implements Serializable {

    private Type type;
    private String key;
    private Integer value;

    public Wrapper(Type type, String key, Integer value){
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public Wrapper(Type type, String key){
        this.type = type;
        this.key = key;
        this.value = null;
    }

    public Integer getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    public String getKey() {
        return key;
    }
}
