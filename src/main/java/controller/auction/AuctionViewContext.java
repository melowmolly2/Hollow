package controller.auction;

import dto.auction.ItemResponse;

// Note: Small hand-off object for opening detail pages from list cards.
public class AuctionViewContext {
    public enum Mode {
        BIDDER,
        SELLER
    }

    public static ItemResponse selectedItem;
    public static Mode selectedMode = Mode.BIDDER;

    private AuctionViewContext() {
    }
}
