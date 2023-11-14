package com.example.demo.device.api;

import java.util.UUID;

import static com.example.demo.device.api.DeviceTrackerData.*;

public record DeviceTrackerUpdate(UUID deviceId, String mediaId, String status) {
    public static DeviceTrackerUpdate newFake(Long id) {
        return new DeviceTrackerUpdate(randomDevice(id), randomContent(id), randomStatus());
    }
}
