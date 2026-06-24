package com.smartincident.incidentbackend.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.permission.entity.PermissionEntity;
import com.smartincident.incidentbackend.permission.entity.RolePermission;
import com.smartincident.incidentbackend.permission.repository.PermissionRepository;
import com.smartincident.incidentbackend.permission.repository.RolePermissionRepository;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.UnitLevel;
import com.smartincident.incidentbackend.enums.UnitType;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.entity.AreaLevel;
import com.smartincident.incidentbackend.setting.entity.AreaType;
import com.smartincident.incidentbackend.setting.repository.AdministrativeAreaRepository;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.setting.repository.AreaLevelRepository;
import com.smartincident.incidentbackend.setting.repository.AreaTypeRepository;
import com.smartincident.incidentbackend.enums.AgencyType;
import com.smartincident.incidentbackend.rank.entity.AgencyRank;
import com.smartincident.incidentbackend.rank.repository.AgencyRankRepository;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Log
@Component
@RequiredArgsConstructor
public class Initializer implements ApplicationRunner {
    private final AdministrativeAreaRepository administrativeAreaRepository;
    private final AreaLevelRepository areaLevelRepository;
    private final AreaTypeRepository areaTypeRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AgencyRankRepository agencyRankRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.seed.root.phone:+255000000000}")
    private String rootPhone;

    @Value("${app.seed.root.name:System Administrator}")
    private String rootName;

    @Value("${app.seed.root.username:admin}")
    private String rootUsername;

    @Value("${app.seed.root.password:Admin@1234}")
    private String rootPassword;

    public void seedAdministrativeArea() {
        log.info("*** Seeding Administrative areas ***");

        if (administrativeAreaRepository.count() > 0) {
            log.info("Administrative areas already seeded. Skipping...");
            return;
        }

        InputStream inputStream;
        try {
            inputStream = new ClassPathResource("seed/administrative_area.csv").getInputStream();
        } catch (IOException e) {
            log.severe("Failed to load administrative_area.csv: " + e.getMessage());
            return;
        }

        List<AreaType> areaTypes = areaTypeRepository.findAll();
        Map<Long, AreaType> areaTypeMap = areaTypes.stream()
                .collect(Collectors.toMap(AreaType::getId, at -> at));

        CsvParserSettings csvParserSettings = new CsvParserSettings();
        csvParserSettings.setHeaderExtractionEnabled(true);
        CsvParser parser = new CsvParser(csvParserSettings);
        List<Record> records = parser.parseAllRecords(inputStream);

        List<AdministrativeArea> administrativeAreas = new ArrayList<>();

        for (Record rec : records) {
            Long areaTypeId = rec.getLong("area_type_id");
            AreaType areaType = areaTypeMap.get(areaTypeId);

            if (areaType != null) {
                AdministrativeArea area = new AdministrativeArea(
                        rec.getLong("id"),
                        LocalDateTime.now(),
                        rec.getString("name"),
                        rec.getString("parent_area_id") != null ? rec.getLong("parent_area_id") : null,
                        areaType,
                        rec.getString("label")
                );
                administrativeAreas.add(area);
            } else {
                log.warning("⚠️ AreaType with id " + areaTypeId + " not found for area " + rec.getString("name"));
            }
        }

        administrativeAreaRepository.saveAll(administrativeAreas);
        log.info("*** Done seeding administrative areas ***");
    }

    public void seedAreaLevel() {
        log.info("*** Seeding Area levels ***");
        InputStream inputStream;
        try {
            inputStream = new ClassPathResource("seed/area_level.csv").getInputStream();
        } catch (IOException e) {
            log.severe("Failed to load area_level.csv: " + e.getMessage());
            return;
        }

        CsvParserSettings csvParserSettings = new CsvParserSettings();
        csvParserSettings.setHeaderExtractionEnabled(true);
        CsvParser parser = new CsvParser(csvParserSettings);
        List<Record> records = parser.parseAllRecords(inputStream);

        for (Record rec : records) {
            try {
                log.info("Processing record: id=" + rec.getLong("id") + ", name=" + rec.getString("name") + ", name_sw=" + rec.getString("name_sw") + ", level=" + rec.getString("level"));
                Optional<AreaLevel> oAreaLevel = areaLevelRepository.findByName(rec.getString("name"));
                if (!oAreaLevel.isPresent()) {
                    AreaLevel areaLevel = new AreaLevel(
                            rec.getLong("id"),
                            rec.getString("name"),
                            rec.getString("name_sw"),
                            com.smartincident.incidentbackend.enums.AdministrativeAreaLevel.valueOf(rec.getString("level"))
                    );
                    log.info("Saving AreaLevel: " + areaLevel.toString());
                    areaLevelRepository.save(areaLevel);
                    log.info("Saved AreaLevel: " + areaLevel.getName());
                } else {
                    log.info("AreaLevel already exists: " + rec.getString("name"));
                }
            } catch (Exception e) {
                log.warning("Failed to process record for area level " + rec.getString("name") + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        log.info("*** Done seeding area levels ***");
    }

    public void seedAreaType() {
        log.info("*** Seeding Area types ***");
        InputStream inputStream = null;
        try {
            inputStream = new ClassPathResource("seed/area_type.csv").getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        CsvParserSettings csvParserSettings = new CsvParserSettings();
        csvParserSettings.setHeaderExtractionEnabled(true);
        CsvParser parser = new CsvParser(csvParserSettings);
        List<Record> records = parser.parseAllRecords(inputStream);
        for (Record rec : records) {
            Optional<AreaType> oAreaType = areaTypeRepository.findByName(rec.getString("name"));
            if (!oAreaType.isPresent()) {
                Optional<AreaLevel> oAreaLevel = areaLevelRepository.findById(rec.getLong("area_level_id"));
                if (oAreaLevel.isPresent()) {
                    AreaType areaType = new AreaType(
                            rec.getLong("id"),
                            rec.getString("name"),
                            rec.getString("name_sw"),
                            oAreaLevel.get(),
                            rec.getString("name_plural"),
                            rec.getString("name_plural_sw")
                    );
                    areaTypeRepository.save(areaType);
                }
            }
        }
    }

    public void dropStaleRoleConstraint() {
        // PostgreSQL CHECK constraint on users.role was generated from the old enum values.
        // Drop it so the migration and new role values are accepted. Java enum is the real guard.
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            log.info("Dropped stale users_role_check constraint (if it existed)");
        } catch (Exception e) {
            log.warning("Could not drop users_role_check constraint: " + e.getMessage());
        }
    }

    public void dropStaleUnitTypeConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE emergency_units DROP CONSTRAINT IF EXISTS emergency_units_unit_type_check");
            log.info("Dropped stale emergency_units_unit_type_check constraint (if it existed)");
        } catch (Exception e) {
            log.warning("Could not drop emergency_units_unit_type_check constraint: " + e.getMessage());
        }
    }

    public void dropStaleIncidentStatusConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE incidents DROP CONSTRAINT IF EXISTS incidents_status_check");
            log.info("Dropped stale incidents_status_check constraint (if it existed)");
        } catch (Exception e) {
            log.warning("Could not drop incidents_status_check constraint: " + e.getMessage());
        }
    }

    public void dropStalePermissionConstraint() {
        // PostgreSQL CHECK constraint on permissions.name was generated from the old enum values.
        // Drop it so new Permission enum values (VIEW_OWN_INCIDENTS, UPDATE_OWN_PROFILE, etc.) are accepted.
        try {
            jdbcTemplate.execute("ALTER TABLE permissions DROP CONSTRAINT IF EXISTS permissions_name_check");
            log.info("Dropped stale permissions_name_check constraint (if it existed)");
        } catch (Exception e) {
            log.warning("Could not drop permissions_name_check constraint: " + e.getMessage());
        }
    }

    @Transactional
    public void migrateOldRoles() {
        int agencyRep  = userRepository.migrateRole(Role.AGENCY_REP,          Role.AGENCY_ADMIN);
        int fireAdmin  = userRepository.migrateRole(Role.FIRE_STATION_ADMIN,   Role.STATION_ADMIN);
        int medAdmin   = userRepository.migrateRole(Role.MEDICAL_STATION_ADMIN, Role.STATION_ADMIN);
        if (agencyRep + fireAdmin + medAdmin > 0) {
            log.info("Role migration: " + agencyRep + " AGENCY_REP→AGENCY_ADMIN, "
                    + fireAdmin + " FIRE_STATION_ADMIN→STATION_ADMIN, "
                    + medAdmin + " MEDICAL_STATION_ADMIN→STATION_ADMIN");
        }
    }

    public void seedPermissions() {
        log.info("*** Seeding permissions ***");

        // Ensure every Permission enum value has a DB row (upsert description + category)
        for (Permission p : Permission.values()) {
            permissionRepository.findByName(p).ifPresentOrElse(
                existing -> {
                    if (existing.getCategory() == null) {
                        existing.setCategory(p.getCategory());
                        permissionRepository.save(existing);
                    }
                },
                () -> permissionRepository.save(PermissionEntity.builder()
                        .name(p)
                        .description(p.name().replace("_", " "))
                        .category(p.getCategory())
                        .build())
            );
        }

        // Default role → permission mappings
        Map<Role, Set<Permission>> defaults = new LinkedHashMap<>();
        defaults.put(Role.ROOT, EnumSet.allOf(Permission.class));
        defaults.put(Role.AGENCY_ADMIN, EnumSet.of(
                Permission.CREATE_USER, Permission.UPDATE_USER, Permission.DELETE_USER,
                Permission.ASSIGN_ROLE, Permission.MANAGE_AGENCY, Permission.MANAGE_STATIONS,
                Permission.VIEW_ANALYTICS, Permission.VIEW_AUDIT_LOGS,
                Permission.DISPATCH_INCIDENT, Permission.UPDATE_INCIDENT, Permission.CLOSE_INCIDENT,
                Permission.ESCALATE_INCIDENT, Permission.VIEW_RESPONDERS, Permission.VIEW_VEHICLES,
                Permission.VIEW_SLA_METRICS, Permission.MANAGE_AFTER_ACTION,
                Permission.VIEW_DISPATCH_CENTER, Permission.ASSIGN_DISPATCHER,
                Permission.MANAGE_SHIFTS, Permission.ASSIGN_OFFICER, Permission.VIEW_PENDING_INCIDENTS,
                Permission.MANAGE_DISPATCH_QUEUE, Permission.VIEW_SLA_BREACHES, Permission.DESIGNATE_COMMANDER,
                Permission.STATION_REVIEW_INCIDENT, Permission.SUBMIT_AAR, Permission.APPROVE_AAR,
                Permission.TRANSFER_OFFICER, Permission.MANAGE_POLICE_STATION,
                Permission.MANAGE_GEOFENCES, Permission.VIEW_LIVE_RESOURCES, Permission.VIEW_NATIONAL_ANALYTICS,
                Permission.DISPATCH_AGENCY, Permission.SEND_CHAT_MESSAGE, Permission.MANAGE_CHAT_MESSAGES,
                Permission.SUBMIT_AFTER_ACTION_REPORT, Permission.VIEW_AFTER_ACTION_REPORT,
                Permission.MANAGE_AFTER_ACTION_REPORT));
        defaults.put(Role.DISPATCH_CENTER_ADMIN, EnumSet.of(
                Permission.CREATE_USER, Permission.UPDATE_USER,
                Permission.DISPATCH_INCIDENT, Permission.UPDATE_INCIDENT, Permission.CLOSE_INCIDENT,
                Permission.ESCALATE_INCIDENT, Permission.VIEW_RESPONDERS, Permission.VIEW_VEHICLES,
                Permission.VIEW_ANALYTICS, Permission.VIEW_SLA_METRICS,
                Permission.MANAGE_AFTER_ACTION, Permission.VIEW_DISPATCH_CENTER,
                Permission.ASSIGN_DISPATCHER, Permission.MANAGE_SHIFTS,
                Permission.ASSIGN_OFFICER, Permission.MANAGE_DISPATCH_QUEUE, Permission.VIEW_SLA_BREACHES,
                Permission.DESIGNATE_COMMANDER, Permission.DISPATCHER_REVIEW_INCIDENT, Permission.SUBMIT_AAR,
                Permission.VIEW_CHECKPOINTS, Permission.MANAGE_GEOFENCES, Permission.VIEW_LIVE_RESOURCES,
                Permission.ESCALATE_RESOURCE, Permission.SEND_CHAT_MESSAGE, Permission.MANAGE_CHAT_MESSAGES,
                Permission.SUBMIT_AFTER_ACTION_REPORT, Permission.VIEW_AFTER_ACTION_REPORT,
                Permission.MANAGE_AFTER_ACTION_REPORT, Permission.DISPATCH_UNITS));
        defaults.put(Role.DISPATCHER_SUPERVISOR, EnumSet.of(
                Permission.DISPATCH_INCIDENT, Permission.UPDATE_INCIDENT, Permission.CLOSE_INCIDENT,
                Permission.ESCALATE_INCIDENT, Permission.VIEW_RESPONDERS, Permission.VIEW_VEHICLES,
                Permission.VIEW_ANALYTICS, Permission.VIEW_SLA_METRICS,
                Permission.MANAGE_AFTER_ACTION, Permission.VIEW_DISPATCH_CENTER,
                Permission.MANAGE_SHIFTS, Permission.ASSIGN_OFFICER, Permission.MANAGE_DISPATCH_QUEUE,
                Permission.VIEW_SLA_BREACHES, Permission.DESIGNATE_COMMANDER,
                Permission.DISPATCHER_REVIEW_INCIDENT, Permission.SUBMIT_AAR, Permission.VIEW_CHECKPOINTS,
                Permission.VIEW_LIVE_RESOURCES, Permission.ESCALATE_RESOURCE,
                Permission.SEND_CHAT_MESSAGE, Permission.MANAGE_CHAT_MESSAGES,
                Permission.SUBMIT_AFTER_ACTION_REPORT, Permission.VIEW_AFTER_ACTION_REPORT,
                Permission.MANAGE_AFTER_ACTION_REPORT, Permission.DISPATCH_UNITS));
        defaults.put(Role.STATION_ADMIN, EnumSet.of(
                Permission.CREATE_USER, Permission.UPDATE_USER,
                Permission.MANAGE_VEHICLES, Permission.MANAGE_STATIONS, Permission.MANAGE_CHECKPOINTS,
                Permission.VIEW_ANALYTICS, Permission.DISPATCH_INCIDENT,
                Permission.UPDATE_INCIDENT, Permission.CLOSE_INCIDENT,
                Permission.VIEW_RESPONDERS, Permission.VIEW_VEHICLES, Permission.MANAGE_SHIFTS,
                Permission.ASSIGN_OFFICER, Permission.VIEW_PENDING_INCIDENTS, Permission.VIEW_INVESTIGATION_QUEUE,
                Permission.VIEW_OFFICER_INCIDENTS, Permission.STATION_REVIEW_INCIDENT, Permission.SUBMIT_AAR,
                Permission.VIEW_AVAILABLE_OFFICERS, Permission.VIEW_STATION_APPOINTMENTS,
                Permission.VIEW_OFFICER_APPOINTMENTS, Permission.VIEW_CHECKPOINTS, Permission.VIEW_NEARBY_STATIONS,
                Permission.VIEW_LIVE_RESOURCES, Permission.ESCALATE_RESOURCE,
                Permission.DISPATCH_AGENCY, Permission.SEND_CHAT_MESSAGE, Permission.MANAGE_CHAT_MESSAGES,
                Permission.SUBMIT_AFTER_ACTION_REPORT, Permission.VIEW_AFTER_ACTION_REPORT,
                Permission.MANAGE_OFFICER_SHIFTS, Permission.VIEW_OWN_OFFICER_SHIFT,
                Permission.VIEW_OFFICER_ON_DUTY, Permission.VIEW_DEPARTMENTS));
        defaults.put(Role.DISPATCHER, EnumSet.of(
                Permission.DISPATCH_INCIDENT, Permission.UPDATE_INCIDENT,
                Permission.ESCALATE_INCIDENT, Permission.VIEW_RESPONDERS, Permission.VIEW_VEHICLES,
                Permission.VIEW_SLA_METRICS, Permission.MANAGE_AFTER_ACTION,
                Permission.ASSIGN_OFFICER, Permission.VIEW_PENDING_INCIDENTS, Permission.MANAGE_DISPATCH_QUEUE,
                Permission.VIEW_OFFICER_INCIDENTS, Permission.DISPATCHER_REVIEW_INCIDENT, Permission.VIEW_CHECKPOINTS,
                Permission.VIEW_LIVE_RESOURCES, Permission.ESCALATE_RESOURCE,
                Permission.DISPATCH_AGENCY, Permission.SEND_CHAT_MESSAGE,
                Permission.SUBMIT_AFTER_ACTION_REPORT, Permission.VIEW_AFTER_ACTION_REPORT,
                Permission.DISPATCH_UNITS));
        defaults.put(Role.POLICE_OFFICER, EnumSet.of(
                Permission.CREATE_INCIDENT, Permission.UPDATE_INCIDENT, Permission.UPLOAD_EVIDENCE,
                Permission.VIEW_VEHICLES, Permission.UPDATE_OWN_PROFILE, Permission.VIEW_OFFICER_INCIDENTS,
                Permission.VIEW_OFFICER_APPOINTMENTS, Permission.VIEW_NEARBY_STATIONS,
                Permission.VIEW_LIVE_RESOURCES, Permission.SEND_CHAT_MESSAGE,
                Permission.VIEW_OWN_OFFICER_SHIFT, Permission.VIEW_OFFICER_ON_DUTY));
        defaults.put(Role.OPERATIONAL_POST_SUPERVISOR, EnumSet.of(
                Permission.STATION_REVIEW_INCIDENT, Permission.ESCALATE_RESOURCE));
        defaults.put(Role.FIRE_OFFICER, EnumSet.of(
                Permission.UPDATE_INCIDENT, Permission.UPLOAD_EVIDENCE, Permission.UPDATE_OWN_PROFILE,
                Permission.VIEW_LIVE_RESOURCES, Permission.SEND_CHAT_MESSAGE));
        defaults.put(Role.MEDIC, EnumSet.of(
                Permission.UPDATE_INCIDENT, Permission.UPLOAD_EVIDENCE, Permission.UPDATE_OWN_PROFILE,
                Permission.VIEW_LIVE_RESOURCES, Permission.SEND_CHAT_MESSAGE));
        defaults.put(Role.CITIZEN, EnumSet.of(
                Permission.CREATE_INCIDENT, Permission.UPLOAD_EVIDENCE,
                Permission.VIEW_OWN_INCIDENTS, Permission.UPDATE_OWN_PROFILE,
                Permission.DELETE_OWN_ACCOUNT, Permission.VIEW_NEARBY_STATIONS,
                Permission.VIEW_OFFICER_ON_DUTY));

        for (Map.Entry<Role, Set<Permission>> entry : defaults.entrySet()) {
            String roleName = entry.getKey().name();
            for (Permission p : entry.getValue()) {
                if (rolePermissionRepository.findByRoleAndPermission(roleName, p).isEmpty()) {
                    PermissionEntity pe = permissionRepository.findByName(p).orElseThrow();
                    rolePermissionRepository.save(RolePermission.builder()
                            .role(roleName)
                            .permission(pe)
                            .build());
                }
            }
        }

        log.info("*** Done seeding permissions ***");
    }

    public void seedDefaultAgencies() {
        log.info("*** Seeding default agencies ***");
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("POLICE",    "Tanzania Police Force");
        defaults.put("FIRE",      "Tanzania Fire and Rescue");
        defaults.put("AMBULANCE", "Tanzania Emergency Medical Services");

        defaults.forEach((code, name) -> {
            if (!agencyRepository.existsByCode(code)) {
                Agency agency = new Agency();
                agency.setCode(code);
                agency.setName(name);
                agencyRepository.save(agency);
                log.info("Seeded agency: " + code);
            }
        });
        log.info("*** Done seeding default agencies ***");
    }

    public void seedRootUser() {
        log.info("*** Seeding ROOT user ***");
        List<User> roots = userRepository.findByRole(Role.ROOT);
        if (!roots.isEmpty()) {
            // Backfill username/password if the existing root was created before this feature
            User root = roots.get(0);
            boolean changed = false;
            if (root.getUsername() == null) {
                if (!userRepository.existsByUsername(rootUsername)) {
                    root.setUsername(rootUsername);
                    changed = true;
                }
            }
            if (root.getPasswordHash() == null) {
                root.setPasswordHash(passwordEncoder.encode(rootPassword));
                changed = true;
            }
            if (changed) {
                userRepository.save(root);
                log.info("Backfilled username/password for existing ROOT user");
            } else {
                log.info("ROOT user already exists. Skipping...");
            }
            return;
        }
        User root = new User();
        root.setName(rootName);
        root.setPhoneNumber(rootPhone);
        root.setUsername(rootUsername);
        root.setPasswordHash(passwordEncoder.encode(rootPassword));
        root.setRole(Role.ROOT);
        root.setVerified(true);
        root.setIsActive(true);
        root.setIsDeleted(false);
        userRepository.save(root);
        log.info("Seeded ROOT user: phone=" + rootPhone + ", username=" + rootUsername);
    }

    public void seedDispatchCenter() {
        log.info("*** Seeding default Dispatch Center ***");
        boolean exists = emergencyUnitRepository.findAll().stream()
                .anyMatch(u -> u.getUnitType() == UnitType.DISPATCH_CENTER);
        if (exists) { log.info("Dispatch center already exists. Skipping..."); return; }

        // Attach to first available agency (any agency is fine for a cross-agency coordination unit)
        agencyRepository.findAll().stream().findFirst().ifPresent(agency -> {
            EmergencyUnit center = EmergencyUnit.builder()
                    .name("National Emergency Coordination Center")
                    .agency(agency)
                    .unitType(UnitType.DISPATCH_CENTER)
                    .level(UnitLevel.NATIONAL)
                    .build();
            emergencyUnitRepository.save(center);
            log.info("Seeded National Emergency Coordination Center");
        });
    }

    @Transactional
    public void seedAgencyRanks() {
        if (agencyRankRepository.existsByAgencyType(AgencyType.POLICE)) {
            log.info("Agency ranks already seeded. Skipping...");
            return;
        }
        log.info("*** Seeding Agency Ranks ***");

        // ── Police Ranks (14 official Tanzania Police Force ranks) ───────────
        Object[][] policeRanks = {
            {"IGP",    "Inspector General of Police",             "Inspekta Jenerali wa Polisi",            "IGP",    1,  "Head of the Police Force appointed by the President"},
            {"CP",     "Commissioner of Police",                  "Kamishna wa Polisi",                     "CP",     2,  "Responsible for major police departments and commands"},
            {"DCP",    "Deputy Commissioner of Police",           "Naibu Kamishna wa Polisi",               "DCP",    3,  "Deputy to the Commissioner of Police"},
            {"SACP",   "Senior Assistant Commissioner of Police", "Kamishna Msaidizi Mwandamizi wa Polisi", "SACP",   4,  "Responsible for large regional commands"},
            {"ACP",    "Assistant Commissioner of Police",        "Kamishna Msaidizi wa Polisi",            "ACP",    5,  "Responsible for divisions, districts or departments"},
            {"SSP",    "Senior Superintendent of Police",         "Mrakibu Mwandamizi wa Polisi",           "SSP",    6,  "Responsible for specialized units and major operations"},
            {"SP",     "Superintendent of Police",                "Mrakibu wa Polisi",                      "SP",     7,  "Responsible for station or departmental management"},
            {"ASP",    "Assistant Superintendent of Police",      "Mrakibu Msaidizi wa Polisi",             "ASP",    8,  "Assistant to Superintendent and station-level leadership"},
            {"INSP",   "Inspector",                               "Inspekta wa Polisi",                     "INSP",   9,  "Supervises operational units and field teams"},
            {"AINSP",  "Assistant Inspector",                     "Inspekta Msaidizi wa Polisi",            "A/INSP", 10, "Assists Inspectors in operational supervision"},
            {"RSM",    "Regimental Sergeant Major",               "Meja Sajenti wa Polisi",                 "RSM",    11, "Senior non-commissioned operational leader"},
            {"SGT",    "Sergeant",                                "Sajenti wa Polisi",                      "SGT",    12, "Supervises small operational teams"},
            {"CPL",    "Corporal",                                "Koplo wa Polisi",                        "CPL",    13, "Leads small officer groups and patrol elements"},
            {"PC",     "Police Constable",                        "Konstebo wa Polisi",                     "PC",     14, "Entry-level police officer responsible for frontline duties"},
        };

        // ── Fire Ranks (10 official Tanzania Fire and Rescue Force ranks) ────
        Object[][] fireRanks = {
            {"CFO",        "Chief Fire Officer",           "Mkuu wa Zima Moto",                  "CFO",     1,  "Head of the Fire and Rescue Force"},
            {"DCFO",       "Deputy Chief Fire Officer",    "Naibu Mkuu wa Zima Moto",            "DCFO",    2,  "Deputy to the Chief Fire Officer"},
            {"SDO",        "Senior Divisional Officer",    "Afisa Mkuu wa Kitengo",              "SDO",     3,  "Responsible for major fire divisions"},
            {"DO",         "Divisional Officer",           "Afisa wa Kitengo",                   "DO",      4,  "Responsible for fire division management"},
            {"ADO",        "Assistant Divisional Officer", "Afisa Msaidizi wa Kitengo",          "ADO",     5,  "Assists Divisional Officer in operations"},
            {"SO",         "Station Officer",              "Afisa wa Kituo",                     "SO",      6,  "Responsible for fire station management"},
            {"ASO",        "Assistant Station Officer",    "Afisa Msaidizi wa Kituo",            "ASO",     7,  "Assists Station Officer in daily operations"},
            {"SUB_OFFICER","Sub-Officer",                  "Naibu Afisa",                        "SUB",     8,  "Senior non-commissioned fire officer"},
            {"LEADING_FM", "Leading Fireman",              "Kiongozi wa Zimamoto",               "L/FM",    9,  "Leads small fire response teams"},
            {"FIREMAN",    "Fireman",                      "Zimamoto",                           "FM",      10, "Entry-level fire and rescue officer"},
        };

        List<AgencyRank> ranks = new ArrayList<>();
        for (Object[] r : policeRanks) {
            ranks.add(AgencyRank.builder()
                    .code((String) r[0]).agencyType(AgencyType.POLICE)
                    .nameEnglish((String) r[1]).nameSwahili((String) r[2])
                    .abbreviation((String) r[3]).rankOrder((Integer) r[4])
                    .description((String) r[5]).isSystemDefined(true)
                    .build());
        }
        for (Object[] r : fireRanks) {
            ranks.add(AgencyRank.builder()
                    .code((String) r[0]).agencyType(AgencyType.FIRE)
                    .nameEnglish((String) r[1]).nameSwahili((String) r[2])
                    .abbreviation((String) r[3]).rankOrder((Integer) r[4])
                    .description((String) r[5]).isSystemDefined(true)
                    .build());
        }
        agencyRankRepository.saveAll(ranks);
        log.info("Seeded " + ranks.size() + " agency ranks (14 police + 10 fire)");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        dropStaleRoleConstraint();
        dropStalePermissionConstraint();
        dropStaleIncidentStatusConstraint();
        dropStaleUnitTypeConstraint();
        migrateOldRoles();
        seedAreaLevel();
        seedAreaType();
        seedAdministrativeArea();
        seedDefaultAgencies();
        seedDispatchCenter();
        seedPermissions();
        seedRootUser();
        seedAgencyRanks();
    }
}