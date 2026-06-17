package com.smartincident.incidentbackend.rank.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.enums.AgencyType;
import com.smartincident.incidentbackend.rank.entity.AgencyRank;
import com.smartincident.incidentbackend.rank.repository.AgencyRankRepository;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ranks")
@RequiredArgsConstructor
public class AgencyRankController {

    private final AgencyRankRepository rankRepository;

    /** Returns all active ranks for the specified agency type (POLICE or FIRE), ordered by seniority. */
    @Authenticated
    @GetMapping
    public ResponseList<AgencyRank> getRanks(@RequestParam AgencyType agencyType) {
        return new ResponseList<>(
                rankRepository.findByAgencyTypeAndIsActiveTrueOrderByRankOrderAsc(agencyType));
    }
}
