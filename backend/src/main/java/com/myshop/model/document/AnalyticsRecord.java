package com.myshop.model.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@Document(collection = "analytics")
public class AnalyticsRecord {

    @Id
    private String id;

    private String eventType;
    private String userId;

    private Map<String, Object> data;

    @CreatedDate
    private Instant createdAt;
}
