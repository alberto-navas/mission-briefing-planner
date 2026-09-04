package com.albertonavas.missionbriefing.server.repository;

import com.albertonavas.missionbriefing.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
