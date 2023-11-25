package com.feiniaojin.ddd.eventsourcing.infrastructure.persistence.jdbc.repository;

import com.feiniaojin.ddd.eventsourcing.infrastructure.persistence.jdbc.data.Snapshot;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * 表名称：t_snapshot自动生成的Repository
 * 表注释：快照表
 * NOTICE:本文件由代码生成器code-generator生成，不要在本文件手工追加任何内容，因为随时可能重新生成替换
 * github：https://github.com/feiniaojin/code-generator
 */
public interface SnapshotJdbcRepository extends CrudRepository<Snapshot, Long> {

    @Query("select * from t_snapshot where entity_id=:entityId")
    Snapshot queryOneByEntityId(String entityId);
}
