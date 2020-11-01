package pojo;

public class Schedule {
    private String date,time,description;

    public Schedule(String date, String time, String description) {
        this.date = date;
        this.time = time;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }
}
