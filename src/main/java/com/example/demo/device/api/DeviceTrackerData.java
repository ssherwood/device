package com.example.demo.device.api;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

public record DeviceTrackerData(UUID deviceId, String mediaId, UUID accountId, String status,
                                OffsetDateTime createdDate,
                                OffsetDateTime updatedDate) {

    // utils for generating fake data
    private static final Random random = new Random();

    public static DeviceTrackerData newFake(Long id) {
        return new DeviceTrackerData(randomDevice(id), randomContent(id), randomAccount(id), randomStatus(), OffsetDateTime.now(), OffsetDateTime.now());
    }

    public static Long randomDeviceId(Long randSize) {
        return random.nextLong(randSize);
    }

    public static UUID randomDevice(Long devId) {
        return UUID.fromString(String.format("cdd7cacd-8e0a-4372-8ceb-%012d", devId));
    }

    public static String randomContent(Long devId) {
        return String.format("48d1c2c2-0d83-43d9-%04d-%012d", random.nextInt(7), devId);
    }

    public static UUID randomAccount(Long devId) {
        return UUID.fromString(String.format("ff710c59-1e6d-47f9-a775-%012d", devId));
    }

    public static String randomStatus() {
        return String.format("STATUS-000-%d", random.nextInt(5));
    }
}
