package com.myshop.repository.mongo;

import com.myshop.model.document.UserActivityLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends MongoRepository<UserActivityLog, String> {
}
