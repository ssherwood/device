package com.example.demo.device.api;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.example.demo.device.api.DeviceTrackerData.*;

public record DeviceTrackerBatchUpdate(UUID deviceId, String mediaId, UUID accountId, String status,
                                       OffsetDateTime updatedDate) {

    public static DeviceTrackerBatchUpdate newFake(Long id) {
        return new DeviceTrackerBatchUpdate(randomDevice(id), randomContent(id), randomAccount(id), randomStatus(), OffsetDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceTrackerBatchUpdate that = (DeviceTrackerBatchUpdate) o;

        if (!deviceId.equals(that.deviceId)) return false;
        return mediaId.equals(that.mediaId);
    }

    @Override
    public int hashCode() {
        int result = deviceId.hashCode();
        result = 31 * result + mediaId.hashCode();
        return result;
    }
}
