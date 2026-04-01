package com.nevis.search.search;

import java.util.List;

public record SearchResponse(
        String query,
        List<SearchResult> results,
        SearchMeta meta
) {
    public record SearchMeta(
            int totalResults,
            int limit,
            long searchTimeMs
    ) {}
}
