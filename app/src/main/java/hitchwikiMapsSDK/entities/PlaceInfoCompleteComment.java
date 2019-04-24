package hitchwikiMapsSDK.entities;

public class PlaceInfoCompleteComment {
    public PlaceInfoCompleteComment(String id, String comment, String datetime,
                                    String userId, String userName) {
        super();
        this.id = id;
        this.comment = comment;
        this.datetime = datetime;
        this.userId = userId;
        this.userName = userName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    private String id;
    private String comment;
    private String datetime;
    private String userId;
    private String userName;
}
