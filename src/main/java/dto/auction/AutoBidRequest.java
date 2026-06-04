package dto.auction;

public class AutoBidRequest {
    public Long itemId;
    public Double maxBidLimit;

    public AutoBidRequest(Long itemId, Double maxBidLimit) {
        this.itemId = itemId;
        this.maxBidLimit = maxBidLimit;
    }
}
