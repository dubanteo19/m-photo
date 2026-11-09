package com.mphoto.user.dto.event;

public record UserEvent(
                UserEventType type,
                Object data) {
        public static UserEvent of(UserEventType type, Object data) {
                return new UserEvent(type, data);
        }

}