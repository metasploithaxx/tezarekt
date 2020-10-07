public class Chat {
    private String sender;
    private String msg;
    private String date;
    private String time;

    Chat(String sender,String msg,String date,String time){
        this.sender=sender;
        this.msg=msg;
        this.date=date;
        this.time=time;
    }


    public String getSender() {
        return sender;
    }

    public String getMsg() {
        return msg;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }
}
