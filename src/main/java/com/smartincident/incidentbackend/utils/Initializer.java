package com.smartincident.incidentbackend.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import com.smartincident.incidentbackend.setting.entity.AreaLevel;
import com.smartincident.incidentbackend.setting.entity.AreaType;
import com.smartincident.incidentbackend.setting.repository.AdministrativeAreaRepository;
import com.smartincident.incidentbackend.setting.repository.AreaLevelRepository;
import com.smartincident.incidentbackend.setting.repository.AreaTypeRepository;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log
@Component
@RequiredArgsConstructor
public class Initializer implements ApplicationRunner {
    private final AdministrativeAreaRepository administrativeAreaRepository;
    private final AreaLevelRepository areaLevelRepository;
    private final AreaTypeRepository areaTypeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedAreaLevel();
        seedAreaType();
        seedAdministrativeArea();
    }
}