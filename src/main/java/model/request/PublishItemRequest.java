package model.request;

public class PublishItemRequest {
    public String title;
    public String description;
    public Long endTime;
    public Double startingPrice;
    public Double buyItNowPrice;
    public Double bidIncrement;

    public PublishItemRequest(String title,
                              String description,
                              Long endTime,
                              Double startingPrice,
                              Double buyItNowPrice,
                              Double bidIncrement){
        this.title = title;
        this.description = description;
        this.endTime = endTime;
        this.startingPrice = startingPrice;
        this.buyItNowPrice = buyItNowPrice;
        this.bidIncrement = bidIncrement;
    }
}
