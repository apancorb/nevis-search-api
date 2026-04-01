package com.nevis.search.search;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Tag(name = "Search", description = "Unified search across clients and documents")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Search across clients and documents",
               description = "Searches clients by name/email/description (substring + fuzzy) " +
                       "and documents by semantic similarity. Returns a flat list sorted by relevance.")
    public SearchResponse search(
            @Parameter(description = "Search query", required = true, example = "address proof")
            @RequestParam("q") String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query 'q' must not be empty");
        }
        return searchService.search(query.trim());
    }
}
