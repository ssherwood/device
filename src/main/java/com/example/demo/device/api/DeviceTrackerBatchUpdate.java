package com.example.demo.device.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceTrackerBatchUpdate(UUID deviceId, String mediaId, String status, OffsetDateTime updatedDate) {
}
