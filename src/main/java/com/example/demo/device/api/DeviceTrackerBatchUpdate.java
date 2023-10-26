package com.example.demo.device.api;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record DeviceTrackerBatchUpdate(UUID deviceId, String mediaId, UUID accountId, String status,
                                       OffsetDateTime updatedDate) {
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
