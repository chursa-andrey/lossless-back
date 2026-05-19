package fm.lossless.tracks.web.dto;

import fm.lossless.tracks.domain.TrackPurchaseLink;

public record TrackPurchaseLinkResponse(
        String url,
        int position
) {
    public static TrackPurchaseLinkResponse from(TrackPurchaseLink purchaseLink) {
        return new TrackPurchaseLinkResponse(purchaseLink.getUrl(), purchaseLink.getPosition());
    }
}
