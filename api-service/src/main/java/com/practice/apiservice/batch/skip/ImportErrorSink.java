package com.practice.apiservice.batch.skip;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImportErrorSink {

    private final NamedParameterJdbcTemplate jdbc;

    public void save(UUID requestId, Long row, String externalId, String reason, String rawLine) {
        var sql = """
      INSERT INTO import_errors(processing_request_id, row_num, external_id, reason, raw_line)
      VALUES (:rid, :row, :ext, :reason, :raw)
      """;
        var p = new MapSqlParameterSource()
                .addValue("rid", requestId)
                .addValue("row", row)
                .addValue("ext", externalId)
                .addValue("reason", reason)
                .addValue("raw", rawLine);
        jdbc.update(sql, p);
    }
}
