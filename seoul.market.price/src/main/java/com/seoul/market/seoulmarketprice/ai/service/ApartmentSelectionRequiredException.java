package com.seoul.market.seoulmarketprice.ai.service;

import com.seoul.market.seoulmarketprice.ai.dto.NaturalApartmentCandidate;
import java.util.List;

public class ApartmentSelectionRequiredException extends RuntimeException {
    private final List<NaturalApartmentCandidate> candidates;
    public ApartmentSelectionRequiredException(String message, List<NaturalApartmentCandidate> candidates) {
        super(message); this.candidates = candidates;
    }
    public List<NaturalApartmentCandidate> candidates() { return candidates; }
}
