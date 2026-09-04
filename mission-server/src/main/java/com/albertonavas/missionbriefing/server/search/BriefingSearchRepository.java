package com.albertonavas.missionbriefing.server.search;

import java.util.List;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BriefingSearchRepository extends ElasticsearchRepository<BriefingDocument, String> {

    @Query("""
            {"match": {"summary": "?0"}}
            """)
    List<BriefingDocument> searchBySummary(String text);
}
