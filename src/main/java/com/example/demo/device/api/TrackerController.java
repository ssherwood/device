package com.example.demo.device.api;

import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/tracker")
public class TrackerController {
    private final TrackerRepo trackerRepo;
    private final TrackerService trackerService;
    private final Random random = new Random();

    public TrackerController(TrackerRepo trackerRepo, TrackerService trackerService) {
        this.trackerRepo = trackerRepo;
        this.trackerService = trackerService;
    }

    // Faux method to generate new data
    @PostMapping("/{id}")
    public DeviceTrackerData addNew(@PathVariable Long id) {
        return trackerRepo.saveNew(DeviceTrackerData.newFake(id));
    }

    @PatchMapping()
    public DeviceTrackerData update(@RequestBody DeviceTrackerUpdate deviceTrackerUpdate) {
        return trackerRepo.updateStatusEvent(deviceTrackerUpdate);
    }

    @PostMapping("/expire")
    public int expireOld() {
        var total = 0;
        var expireTime = OffsetDateTime.now().minusDays(30);
        // naive sequential... could be in parallel
        for (int i = 0; i < 16; i++) {
            total += trackerRepo.deleteExpiredEntries(expireTime, i);
        }
        return total;
    }


    // POST with the size of the batch you want to create
    @PostMapping("/batch/{batchSize}")
    public int[] batchUpdate(@PathVariable int batchSize) {
        Set<DeviceTrackerBatchUpdate> batchUpdate = new HashSet<>();
        while (batchUpdate.size() <= batchSize) {
            // only add unique devices
            batchUpdate.add(generateDeviceUpdate(500_000));
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

    @PostMapping("/batch-upsert/{batchSize}")
    public int[] batchUpsert(@PathVariable int batchSize) {
        List<DeviceTrackerBatchUpdate> batchUpdate = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            batchUpdate.add(generateDeviceUpdate(500_000));
        }

        var results = trackerRepo.batchUpsertStatusEvent(batchUpdate);
        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0) {
                System.out.println("Failed? " + batchUpdate.get(i));
            }
        }

        return results;
    }

    @PostMapping("/batch-cte/{batchSize}")
    public int batchUpsertCte(@PathVariable int batchSize) {
        Set<DeviceTrackerBatchUpdate> batchUpdate = new HashSet<>();
        while (batchUpdate.size() <= batchSize) {
            // only add unique devices
            batchUpdate.add(generateDeviceUpdate(500_000));
        }
        var updateList = List.copyOf(batchUpdate);
        var results = trackerRepo.batchUpsertStatusEventCTE(updateList);
        return results;
    }

    @PostMapping("/purge")
    public int purge() {
        return trackerService.removeExpired(30);
    }

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
        return String.format("M06-%d", random.nextInt(5));
    }
}
