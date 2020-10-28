package pojo;

public class Notification {
    private String timestamp,msg;
    private int index;

    public Notification(String timestamp, String msg,int index) {
        this.timestamp = timestamp;
        this.msg = msg;
        this.index = index;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMsg() {
        return msg;
    }

    public int getindex() {
        return index;
    }


}
