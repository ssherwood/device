package com.example.demo.device.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceTrackerData(UUID deviceId, String mediaId, UUID accountId, String status, OffsetDateTime createdDate,
                                OffsetDateTime updatedDate) {
    public static DeviceTrackerData newFake(Long id) {
        return new DeviceTrackerData(UUID.fromString(String.format("cdd7cacd-8e0a-4372-8ceb-%012d", id)),
                UUID.fromString(String.format("48d1c2c2-0d83-43d9-bc17-%012d", id)).toString(),
                UUID.fromString(String.format("ff710c59-1e6d-47f9-a775-%012d", id)),
                "ACTIVE", OffsetDateTime.now(), OffsetDateTime.now());
    }
}
