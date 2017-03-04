package Requests;

import java.io.Serializable;

/**
 * Created by Cip on 28-Feb-17.
 */
public class Command implements Serializable {
    private String type;
    private String parameter;

    public Command(String type, String parameter) {
        this.type = type;
        this.parameter = parameter;
    }

    public String getType() {
        return type;
    }

    public String getParameter() {
        return parameter;
    }
}
