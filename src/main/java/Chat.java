public class Chat {
    private String sender;
    private String msg;

    Chat(String sender,String msg){
        this.sender=sender;
        this.msg=msg;
    }


    public String getSender() {
        return sender;
    }

    public String getMsg() {
        return msg;
    }
}
