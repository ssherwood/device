package com.example.demo.device.api;

import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracker")
public class TrackerController {
    private final TrackerRepo trackerRepo;
    private final Random random = new Random();

    public TrackerController(TrackerRepo trackerRepo) {
        this.trackerRepo = trackerRepo;
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

    // POST with the size of the batch you want to create
    @PostMapping("/batch/{batchSize}")
    public int[] batchUpdate(@PathVariable int batchSize) {
        List<DeviceTrackerBatchUpdate> batchUpdate = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            batchUpdate.add(generateDeviceUpdate());
        }

        var results = trackerRepo.batchUpdateStatusEvent(batchUpdate);
        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0) {
                System.out.println("Failed? " + batchUpdate.get(i));
            }
        }
        return results;
    }

    private DeviceTrackerBatchUpdate generateDeviceUpdate() {
        var devId = random.nextInt(11000);
        return new DeviceTrackerBatchUpdate(randomDevice(devId), randomContent(devId), randomStatus(), OffsetDateTime.now());
    }

    private String randomStatus() {
        return String.format("M05-%d", random.nextInt(99));
    }

    private String randomContent(int devId) {
        return String.format("48d1c2c2-0d83-43d9-%04d-%012d", random.nextInt(60), devId);
    }

    private UUID randomDevice(int devId) {
        return UUID.fromString(String.format("cdd7cacd-8e0a-4372-8ceb-%012d", devId));
    }
}
