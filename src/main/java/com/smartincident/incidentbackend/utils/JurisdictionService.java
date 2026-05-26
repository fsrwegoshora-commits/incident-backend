package com.smartincident.incidentbackend.utils;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.enums.Role;
import org.springframework.stereotype.Service;

/**
 * Centralises "who can see what" jurisdiction logic.
 *
 * Scope levels (broadest → narrowest):
 *   ALL      — ROOT: no filter, sees every record in the system.
 *   AGENCY   — AGENCY_ADMIN: scoped to own agency's records.
 *   STATION  — STATION_ADMIN / DISPATCHER / officers: scoped to own station / unit.
 *   SELF     — CITIZEN / field user: sees only their own records.
 */
@Service
public class JurisdictionService {

    public enum Scope { ALL, AGENCY, STATION, SELF }

    /** Scope level for the currently authenticated user. */
    public Scope getCurrentScope() {
        Role role = LoggedUser.getRole();
        if (role == null) return Scope.SELF;
        return switch (role) {
            case ROOT -> Scope.ALL;
            case AGENCY_ADMIN -> Scope.AGENCY;
            case STATION_ADMIN, DISPATCHER -> Scope.STATION;
            default -> Scope.SELF;
        };
    }

    /**
     * The UID to use when filtering records by jurisdiction.
     * Returns null for ALL scope (no filter needed).
     * Returns the agency UID for AGENCY scope.
     * Returns the station/unit UID for STATION scope.
     * Returns the user's own UID for SELF scope.
     */
    public String getScopeId() {
        return switch (getCurrentScope()) {
            case ALL -> null;
            case AGENCY -> LoggedUser.getAgencyUid();
            case STATION -> LoggedUser.getAnyStationUid();
            case SELF -> LoggedUser.getUid();
        };
    }

    /** True if the current user can manage (read/write) the target user. */
    public boolean canAccessUser(User target) {
        if (target == null) return false;
        return switch (getCurrentScope()) {
            case ALL -> true;
            case AGENCY -> {
                String myAgency = LoggedUser.getAgencyUid();
                String theirAgency = target.getAgency() != null ? target.getAgency().getUid() : null;
                yield myAgency != null && myAgency.equals(theirAgency);
            }
            case STATION -> {
                String myUnit = LoggedUser.getEmergencyUnitUid();
                String myPolice = LoggedUser.getPoliceStationUid();
                String theirUnit = target.getEmergencyUnit() != null ? target.getEmergencyUnit().getUid() : null;
                String theirPolice = target.getPoliceStation() != null ? target.getPoliceStation().getUid() : null;
                yield (myUnit != null && myUnit.equals(theirUnit))
                        || (myPolice != null && myPolice.equals(theirPolice));
            }
            case SELF -> {
                String myUid = LoggedUser.getUid();
                yield myUid != null && myUid.equals(target.getUid());
            }
        };
    }

    /** True if the current user has at least AGENCY scope (AGENCY_ADMIN or ROOT). */
    public boolean isAgencyOrAbove() {
        Scope s = getCurrentScope();
        return s == Scope.ALL || s == Scope.AGENCY;
    }

    /** True if the current user has at least STATION scope. */
    public boolean isStationOrAbove() {
        Scope s = getCurrentScope();
        return s == Scope.ALL || s == Scope.AGENCY || s == Scope.STATION;
    }
}
