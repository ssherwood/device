package com.example.demo.device.api;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Repository
public class TrackerRepo {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TrackerRepo(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DeviceTrackerData saveNew(DeviceTrackerData deviceTrackerData) {
        var params = Map.of(
                "deviceId", deviceTrackerData.deviceId(),
                "mediaId", deviceTrackerData.mediaId(),
                "accountId", deviceTrackerData.accountId(),
                "status", deviceTrackerData.status(),
                "createdDate", deviceTrackerData.createdDate(),
                "updatedDate", deviceTrackerData.updatedDate());

        var rows = jdbcTemplate.update("INSERT INTO yb_device_tracker2(device_id, media_id, account_id, status, created_date, updated_date) VALUES(:deviceId, :mediaId, :accountId, :status, :createdDate, :updatedDate);", params);
        return jdbcTemplate.queryForObject("SELECT * FROM yb_device_tracker2 where device_id = :deviceId", Map.of("deviceId", deviceTrackerData.deviceId()), new DataClassRowMapper<>(DeviceTrackerData.class));
    }

    public DeviceTrackerData updateStatusEvent(DeviceTrackerUpdate deviceTrackerData) {
        var params = Map.of(
                "deviceId", deviceTrackerData.deviceId(),
                "mediaId", deviceTrackerData.mediaId(),
                "newStatus", deviceTrackerData.status(),
                "now", OffsetDateTime.now());

        var rows = jdbcTemplate.update("UPDATE yb_device_tracker2 SET status = :newStatus, updated_date = :now WHERE device_id = :deviceId AND media_id = :mediaId", params);
        return jdbcTemplate.queryForObject("SELECT * FROM yb_device_tracker2 where device_id = :deviceId and media_id = :mediaId",
                Map.of("deviceId", deviceTrackerData.deviceId(), "mediaId", deviceTrackerData.mediaId()), new DataClassRowMapper<>(DeviceTrackerData.class));
    }

    //@Transactional //causes tx conflict errors, bigger batches == more conflicts
    public int[] batchUpdateStatusEvent(List<DeviceTrackerBatchUpdate> deviceTrackerData) {
        SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(deviceTrackerData);
        return jdbcTemplate.batchUpdate("UPDATE yb_device_tracker2 SET status = :status, updated_date = clock_timestamp() WHERE device_id = :deviceId AND media_id = :mediaId", batch);
    }

    public int[] batchUpsertStatusEvent(List<DeviceTrackerBatchUpdate> deviceTrackerData) {
        SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(deviceTrackerData);
        return jdbcTemplate.batchUpdate(
                """
                        INSERT INTO yb_device_tracker2(device_id, media_id, account_id, status, created_date, updated_date)
                        VALUES(:deviceId, :mediaId, :accountId, :status, clock_timestamp(), clock_timestamp())
                        ON CONFLICT (device_id, media_id)
                        DO UPDATE SET status = EXCLUDED.status,
                        updated_date = EXCLUDED.updated_date
                            """, batch);
    }

    public int batchUpsertStatusEventCTE(List<DeviceTrackerBatchUpdate> deviceTrackerData) {

        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("data", deviceTrackerData);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        return jdbcTemplate.update("""
                        
                        WITH upsert(device_id, media_id, account_id, status) AS (VALUES :data),
                updated AS (
                  UPDATE yb_device_tracker2 dt
                     SET status = upsert.status, updated_date = clock_timestamp()
                    FROM upsert
                   WHERE upsert.device_id = dt.device_id  AND upsert.media_id = dt.media_id
                  RETURNING dt.device_id, dt.media_id
                )
                INSERT INTO yb_device_tracker2(device_id, media_id, account_id, status, created_date, updated_date)
                SELECT upsert.device_id, upsert.media_id, upsert.account_id, upsert.status, clock_timestamp(), clock_timestamp()
                FROM upsert
                WHERE (upsert.device_id, upsert.media_id) NOT IN (SELECT updated.device_id, updated.media_id FROM updated);
                """, p, keyHolder);

    }

    public int deleteExpiredEntries(OffsetDateTime expireDate, int splitPoint) {
        var params = Map.of("expireDate", expireDate, "splitPoint", splitPoint);
        return jdbcTemplate.update("delete from yb_device_tracker2 where updated_date < :expireDate and ((yb_hash_code(updated_date)%16) = :splitPoint", params);
    }

    @Async
    public CompletableFuture<Integer> deleteExpiredEntriesNoIndex(OffsetDateTime expireDate, int low, int high) {
        var params = Map.of("expireDate", expireDate, "low", low, "high", high);
        return CompletableFuture.completedFuture(jdbcTemplate.update("delete from yb_device_tracker2 where updated_date < :expireDate and (yb_hash_code(device_id) >= :low and yb_hash_code(device_id) < :high)", params));
    }
}
