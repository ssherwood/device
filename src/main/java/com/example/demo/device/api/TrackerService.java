package com.example.demo.device.api;

import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Service
public class TrackerService {
    private static final Random random = new Random();

    private final TrackerRepo trackerRepo;

    public TrackerService(TrackerRepo trackerRepo) {
        this.trackerRepo = trackerRepo;
    }


    @Retryable(interceptor = "ysqlRetryInterceptor")
    //@Transactional
    public DeviceTrackerData addFakeDevice(Long uniqueId) {
        trackerRepo.countActiveDevices(randomAccount(Math.toIntExact(uniqueId)));
        return trackerRepo.saveNew(DeviceTrackerData.newFake(uniqueId));
    }

    //@Transactional
    @Retryable(interceptor = "ysqlRetryInterceptor")
    public DeviceTrackerData update(DeviceTrackerUpdate deviceTrackerUpdate) {
        var rows = trackerRepo.findByDeviceId(deviceTrackerUpdate.deviceId());
        // to test: place breakpoint below.  Run concurrent transaction while paused and then resume.
        return trackerRepo.updateStatusEvent(deviceTrackerUpdate);
    }

    @Retryable(interceptor = "ysqlRetryInterceptor")
    public boolean isDeviceAvailable(UUID accountId) {
        //sneakyThrow(new SQLException("Something happened", "40P01"));
        var total = trackerRepo.countActiveDevices(accountId);
        return total < 6;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    @Retryable(interceptor = "ysqlRetryInterceptor")
    public int[] batchUpdate(int batchSize, int numDevices) {
        Set<DeviceTrackerBatchUpdate> batchUpdate = new HashSet<>();
        while (batchUpdate.size() <= batchSize) {
            // only add unique devices
            batchUpdate.add(generateDeviceUpdate(numDevices));
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

    @Retryable(interceptor = "ysqlRetryInterceptor")
    public int[] batchUpsert(int batchSize, int numDevices) {
        List<DeviceTrackerBatchUpdate> batchUpdate = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            batchUpdate.add(generateDeviceUpdate(numDevices));
        }

        var results = trackerRepo.batchUpsertStatusEvent(batchUpdate);
        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0) {
                System.out.println("Failed? " + batchUpdate.get(i));
            }
        }

        return results;
    }

    @Retryable(interceptor = "ysqlRetryInterceptor")
    public int batchUpsertCTE(int batchSize, int numDevices) {
        Set<DeviceTrackerBatchUpdate> batchUpdate = new HashSet<>();
        while (batchUpdate.size() <= batchSize) {
            // only add unique devices
            batchUpdate.add(generateDeviceUpdate(numDevices));
        }
        var updateList = List.copyOf(batchUpdate);
        var results = trackerRepo.batchUpsertStatusEventCTE(updateList);
        return results;
    }

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

    @Retryable(interceptor = "ysqlRetryInterceptor")
    public int expireOld(int daysOld) {
        var total = 0;
        var expireTime = OffsetDateTime.now().minusDays(daysOld);
        // naive sequential... could be in parallel
        for (int i = 0; i < 16; i++) {
            total += trackerRepo.deleteExpiredEntries(expireTime, i);
        }
        return total;
    }


    // utils
    private DeviceTrackerBatchUpdate generateDeviceUpdate(int randSize) {
        var devId = random.nextInt(randSize);
        return new DeviceTrackerBatchUpdate(randomDevice(devId), randomContent(devId), randomAccount(devId), randomStatus(), OffsetDateTime.now());
    }

    private UUID randomDevice(int devId) {
        return UUID.fromString(String.format("cdd7cacd-8e0a-4372-8ceb-%012d", devId));
    }

    private String randomContent(int devId) {
        return String.format("48d1c2c2-0d83-43d9-%04d-%012d", random.nextInt(7), devId);
    }

    private UUID randomAccount(int devId) {
        return UUID.fromString(String.format("ff710c59-1e6d-47f9-a775-%012d", devId));
    }

    private String randomStatus() {
        return String.format("STATUS-000-%d", random.nextInt(5));
    }
}
