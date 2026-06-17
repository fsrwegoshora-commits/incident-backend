package com.smartincident.incidentbackend.operational.service;

import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.PostStatus;
import com.smartincident.incidentbackend.enums.PostType;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.medical.entity.Hospital;
import com.smartincident.incidentbackend.medical.repository.HospitalRepository;
import com.smartincident.incidentbackend.operational.dto.OperationalPostDto;
import com.smartincident.incidentbackend.operational.entity.OperationalPost;
import com.smartincident.incidentbackend.operational.repository.OperationalPostRepository;
import com.smartincident.incidentbackend.police.entity.Location;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.utils.LoggedUser;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OperationalPostService {

    private final OperationalPostRepository repository;
    private final AgencyRepository agencyRepository;
    private final PoliceStationRepository policeStationRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;
    private final HospitalRepository hospitalRepository;

    private static final Set<PostType> POLICE_TYPES = EnumSet.of(PostType.POLICE_PATROL_POST, PostType.POLICE_CHECKPOINT);
    private static final Set<PostType> FIRE_TYPES   = EnumSet.of(PostType.FIRE_OPERATIONAL_POST);
    private static final Set<PostType> MEDICAL_TYPES = EnumSet.of(PostType.MEDICAL_OPERATIONAL_POST);

    public Response<OperationalPost> save(OperationalPostDto dto) {
        Role role = LoggedUser.getRole();

        // Ownership guard: STATION_ADMIN may only create posts for their own station/unit
        if (role == Role.STATION_ADMIN) {
            if (dto.getPostType() == null)
                return Response.error("Post type is required");

            if (POLICE_TYPES.contains(dto.getPostType())) {
                String myStation = LoggedUser.getPoliceStationUid();
                if (myStation == null)
                    return Response.error("You are not linked to a police station");
                if (dto.getParentStationUid() != null && !dto.getParentStationUid().equals(myStation))
                    return Response.error("You can only create posts for your own station");
                // Auto-assign to own station if omitted
                if (dto.getParentStationUid() == null) dto.setParentStationUid(myStation);

            } else if (FIRE_TYPES.contains(dto.getPostType()) || MEDICAL_TYPES.contains(dto.getPostType())) {
                String myUnit = LoggedUser.getEmergencyUnitUid();
                if (myUnit == null)
                    return Response.error("You are not linked to an emergency unit");
                if (dto.getParentUnitUid() != null && !dto.getParentUnitUid().equals(myUnit))
                    return Response.error("You can only create posts for your own unit");
                if (dto.getParentUnitUid() == null) dto.setParentUnitUid(myUnit);

            } else {
                // MULTI_AGENCY_POST requires higher privilege
                return Response.error("STATION_ADMIN cannot create MULTI_AGENCY_POST — contact Agency Admin");
            }
        }

        OperationalPost post = dto.getUid() != null
                ? repository.findByUid(dto.getUid()).orElse(new OperationalPost())
                : new OperationalPost();

        post.setName(dto.getName());
        post.setPostType(dto.getPostType());
        post.setStatus(dto.getStatus() != null ? dto.getStatus() : PostStatus.ACTIVE);
        post.setCoverageRadiusKm(dto.getCoverageRadiusKm());
        post.setNotes(dto.getNotes());

        if (dto.getAgencyUid() != null) {
            agencyRepository.findByUid(dto.getAgencyUid()).ifPresent(post::setAgency);
        }
        if (dto.getParentStationUid() != null) {
            policeStationRepository.findByUid(dto.getParentStationUid()).ifPresent(post::setParentStation);
        }
        if (dto.getParentUnitUid() != null) {
            emergencyUnitRepository.findByUid(dto.getParentUnitUid()).ifPresent(post::setParentUnit);
        }
        if (dto.getParentHospitalUid() != null) {
            hospitalRepository.findByUid(dto.getParentHospitalUid()).ifPresent(post::setParentHospital);
        }

        Location loc = new Location();
        loc.setLatitude(dto.getLatitude());
        loc.setLongitude(dto.getLongitude());
        loc.setAddress(dto.getAddress());
        post.setLocation(loc);

        return Response.success(repository.save(post));
    }

    public Response<OperationalPost> get(String uid) {
        return repository.findByUid(uid)
                .map(Response::success)
                .orElse(Response.error("Operational post not found"));
    }

    public ResponseList<OperationalPost> getAll() {
        return new ResponseList<>(repository.findByIsActiveTrue());
    }

    public ResponseList<OperationalPost> getByAgency(String agencyUid) {
        return new ResponseList<>(repository.findByAgencyUidAndIsActiveTrue(agencyUid));
    }

    public ResponseList<OperationalPost> getByParentStation(String stationUid) {
        return new ResponseList<>(repository.findByParentStationUidAndIsActiveTrue(stationUid));
    }

    public ResponseList<OperationalPost> getByParentUnit(String unitUid) {
        return new ResponseList<>(repository.findByParentUnitUidAndIsActiveTrue(unitUid));
    }

    public ResponseList<OperationalPost> getByParentHospital(String hospitalUid) {
        return new ResponseList<>(repository.findByParentHospitalUidAndIsActiveTrue(hospitalUid));
    }

    public ResponseList<OperationalPost> getNearby(double lat, double lng, double radiusKm, PostType postType) {
        List<OperationalPost> results = repository.findNearby(lat, lng, radiusKm, postType.name());
        return new ResponseList<>(results);
    }

    public Response<OperationalPost> delete(String uid) {
        Optional<OperationalPost> opt = repository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Operational post not found");
        OperationalPost post = opt.get();

        // Ownership guard for delete
        Role role = LoggedUser.getRole();
        if (role == Role.STATION_ADMIN) {
            String myStation = LoggedUser.getPoliceStationUid();
            String myUnit    = LoggedUser.getEmergencyUnitUid();
            boolean ownedByStation = post.getParentStation() != null && myStation != null
                    && post.getParentStation().getUid().equals(myStation);
            boolean ownedByUnit = post.getParentUnit() != null && myUnit != null
                    && post.getParentUnit().getUid().equals(myUnit);
            if (!ownedByStation && !ownedByUnit)
                return Response.error("You can only delete posts belonging to your station/unit");
        }

        post.setIsActive(false);
        post.setIsDeleted(true);
        return Response.success(repository.save(post));
    }
}
