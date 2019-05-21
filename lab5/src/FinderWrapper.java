import java.io.Serializable;

public class FinderWrapper implements Serializable {

    private String dbNumber;
    private String price;
    public FinderWrapper(String dbNumber, String price)
    {
        this.dbNumber = dbNumber;
        this.price = price;
    }

    public String getDbNumber() {
        return dbNumber;
    }

    public String getPrice() {
        return price;
    }
}
