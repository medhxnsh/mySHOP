package com.myshop.repository.mongo;

import com.myshop.model.document.AnalyticsRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyticsRepository extends MongoRepository<AnalyticsRecord, String> {
}
