package com.mphoto.event;

import java.util.List;

public record PhotoPurgedEvent(List<Long> ids) {}
