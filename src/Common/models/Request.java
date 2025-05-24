package Common.models;

public class Request {
    private String type;
    private String command;

    public Request setType(String type) {
        this.type = type;
        return this;
    }

    public Request setCommand(String command) {
        this.command = command;
        return this;
    }

    public String getType() {
        return type;
    }

    public String getCommand() {
        return command;
    }
}
