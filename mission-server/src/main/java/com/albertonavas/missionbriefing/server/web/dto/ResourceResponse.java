package com.albertonavas.missionbriefing.server.web.dto;

import com.albertonavas.missionbriefing.model.Resource;
import com.albertonavas.missionbriefing.model.ResourceType;

public record ResourceResponse(Long id, String name, ResourceType type, String callSign, boolean available) {

    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(), resource.getName(), resource.getType(), resource.getCallSign(), resource.isAvailable());
    }
}
