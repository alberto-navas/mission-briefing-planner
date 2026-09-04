package com.albertonavas.missionbriefing.server.repository;

import com.albertonavas.missionbriefing.model.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, Long> {
}
