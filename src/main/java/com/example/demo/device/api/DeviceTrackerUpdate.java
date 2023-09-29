package com.example.demo.device.api;

import java.util.UUID;

public record DeviceTrackerUpdate(UUID deviceId, String mediaId, String status) {
}
