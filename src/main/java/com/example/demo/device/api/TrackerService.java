package com.example.demo.device.api;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Service
public class TrackerService {
    private final TrackerRepo trackerRepo;

    public TrackerService(TrackerRepo trackerRepo) {
        this.trackerRepo = trackerRepo;
    }

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
}
