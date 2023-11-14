package com.example.demo.device.api;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tracker")
public class TrackerController {
    private final TrackerService trackerService;

    public TrackerController(TrackerService trackerService) {
        this.trackerService = trackerService;
    }

    // Method to generate fake devices
    @PostMapping("/{id}")
    public DeviceTrackerData addNew(@PathVariable Long id) {
        return trackerService.addFakeDevice(id);
    }

    @PatchMapping()
    public DeviceTrackerData update(@RequestBody DeviceTrackerUpdate deviceTrackerUpdate) {
        return trackerService.update(deviceTrackerUpdate);
    }

    @GetMapping("/account/{accountId}")
    public boolean isAvailable(@PathVariable UUID accountId) {
        return trackerService.isDeviceAvailable(accountId);
    }

    // POST with the size of the batch you want to create
    @PostMapping("/batch/{batchSize}")
    public int[] batchUpdate(@PathVariable int batchSize) {
        var result = trackerService.batchUpdate(batchSize, 500_000);
        return result;
    }

    @PostMapping("/batch-upsert/{batchSize}")
    public int[] batchUpsert(@PathVariable int batchSize) {
        var result = trackerService.batchUpsert(batchSize, 500_000);
        return result;
    }

    @PostMapping("/batch-cte/{batchSize}")
    public int batchUpsertCte(@PathVariable int batchSize) {
        // TODO this doesn't work...
        var result = trackerService.batchUpsertCTE(batchSize, 500_000);
        return result;
    }

    @PostMapping("/purge")
    public int purge() {
        return trackerService.removeExpired(30);
    }

    @PostMapping("/expire")
    public int expireOld() {
        var result = trackerService.expireOld(30);
        return result;
    }
}
