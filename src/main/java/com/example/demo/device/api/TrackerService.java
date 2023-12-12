package com.example.demo.device.api;

import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * NOTE: using @Retryable at this level usually corresponds with a @Transactional rollback/retry.
 * If not using a transactional layer here, it may make more sense to put the @Retryable on the
 * Repository layer.
 */
@Service
public class TrackerService {
    private final TrackerRepo trackerRepo;

    public TrackerService(TrackerRepo trackerRepo) {
        this.trackerRepo = trackerRepo;
    }

    @Transactional
    @Retryable(interceptor = "ysqlRetryInterceptor")
    public DeviceTrackerData addFakeDevice(Long uniqueId) {
        return trackerRepo.saveNew(DeviceTrackerData.newFake(uniqueId));
    }

    @Transactional
    @Retryable(interceptor = "ysqlRetryInterceptor")
    public DeviceTrackerData update(DeviceTrackerUpdate deviceTrackerUpdate) {
        var rows = trackerRepo.findByDeviceId(deviceTrackerUpdate.deviceId());
        // to test: place breakpoint below.  Run concurrent transaction while paused and then resume.
        return trackerRepo.updateStatusEvent(deviceTrackerUpdate);
    }

    @Transactional
    @Retryable(interceptor = "ysqlRetryInterceptor")
    public boolean isDeviceAvailable(UUID accountId) {
        //sneakyThrow(new SQLException("Something happened", "40P01"));
        var total = trackerRepo.countActiveDevices(accountId);
        return total < 6;
    }

    @Transactional
    @Retryable(interceptor = "ysqlRetryInterceptor")
    public int[] batchUpdate(int batchSize, long numDevices) {
        Set<DeviceTrackerBatchUpdate> batchUpdate = new HashSet<>();
        while (batchUpdate.size() <= batchSize) {
            // only add unique devices
            batchUpdate.add(DeviceTrackerBatchUpdate.newFake(numDevices));
        }
        var updateList = List.copyOf(batchUpdate);
        var results = trackerRepo.batchUpdateStatusEvent(updateList);
        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0) {
                System.out.println("Failed? " + updateList.get(i));
            }
        }

        return results;
    }

    @Transactional
    @Retryable(interceptor = "ysqlRetryInterceptor")
    public int[] batchUpsert(int batchSize, long numDevices) {
        List<DeviceTrackerBatchUpdate> batchUpdate = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            batchUpdate.add(DeviceTrackerBatchUpdate.newFake(numDevices));
        }

        var results = trackerRepo.batchUpsertStatusEvent(batchUpdate);
        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0) {
                System.out.println("Failed? " + batchUpdate.get(i));
            }
        }

        return results;
    }

    @Transactional
    @Retryable(interceptor = "ysqlRetryInterceptor")
    public int batchUpsertCTE(int batchSize, long numDevices) {
        Set<DeviceTrackerBatchUpdate> batchUpdate = new HashSet<>();
        while (batchUpdate.size() <= batchSize) {
            // only add unique devices
            batchUpdate.add(DeviceTrackerBatchUpdate.newFake(numDevices));
        }
        var updateList = List.copyOf(batchUpdate);
        var results = trackerRepo.batchUpsertStatusEventCTE(updateList);
        return results;
    }

    /**
     * This expiring logic uses a special index technique on updated_date.  The
     * index is forced to split into a specific number of buckets that, when
     * coupled with yb_hash_code() and a modulus function, neatly organize the
     * indexed updated_date in descending ordered fashion:
     * <p>
     * <i>create index nonconcurrently on yb_device_tracker ((yb_hash_code(updated_date) % 16) asc, updated_date desc) split at values( (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13), (14) );</i>
     * <p>
     * This insures a good distribution of the index data across the cluster
     * and is still usable if just ordering updated_date.  For a cleanup
     * operation such as this, just iterating on the number of buckets can be
     * good enough to run the purge across all the ordered dates efficiently.
     * <p>
     * This operation can also be parallelized (see above operation that does
     * not use an index).
     *
     * @param daysOld number of days old an entry needs to be to qualify for deletion
     * @return count of total deleted rows
     */
    @Transactional
    @Retryable(interceptor = "ysqlRetryInterceptor")
    public int expireOld(int daysOld) {
        var total = 0;
        var expireTime = OffsetDateTime.now().minusDays(daysOld);

        // naive sequential... could be done in parallel
        for (int i = 0; i < 16; i++) {
            total += trackerRepo.deleteExpiredEntries(expireTime, i);
        }

        return total;
    }

    /**
     * This expiring logic avoids using an index entirely and takes advantage
     * of yb_hash_code() directly to parallelize the work.  Eliminating the
     * index can improve the write path throughput.
     *
     * @param numberOfTablets to determine the number of parallel jobs to execute
     * @return count of total deleted rows
     */
    @Transactional
    @Retryable(interceptor = "ysqlRetryInterceptor")
    public int removeExpired(int numberOfTablets) {
        if (numberOfTablets < 1) {
            throw new IllegalArgumentException("Splits should be greater than 0");
        }

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        var expiredDate = OffsetDateTime.now().minusDays(10);

        var partitionEnd = 0;
        for (int i = 0; i < numberOfTablets; i++) {
            var partitionBegin = partitionEnd;
            partitionEnd = (i + 1) * (0xFFFF + 1) / numberOfTablets;
            // System.out.printf("hash_split: [0x%04x,0x%04x)\n", pStart, pEnd);
            futures.add(trackerRepo.deleteExpiredEntriesNoIndex(expiredDate, partitionBegin, partitionEnd));
        }

        // NOTE this is still limited by the number of threads in the default job pool
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        // TODO instead of returning just an int, it would be nice to have the return type
        // reflect the yb_hash_code() range as well... then response could be "0-1024":10
        var deletedRows = futures.stream().map(future -> {
            try {
                return future.get();
            } catch (CompletionException e1) {
                System.out.println("Exception when joining");
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Exception when getting");
            }
            return 0;
        }).mapToInt(Integer::intValue).sum();

        return deletedRows;
    }

    // used sometimes for testing retryable
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
