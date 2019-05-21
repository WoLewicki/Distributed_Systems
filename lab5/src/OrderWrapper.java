import java.io.Serializable;

public class OrderWrapper implements Serializable {
    private String orderDone;

    public OrderWrapper(String orderDone)
    {
        this.orderDone = orderDone;
    }

    public String getOrderDone() {
        return orderDone;
    }
}
