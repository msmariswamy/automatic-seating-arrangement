package com.seating.service;

import com.seating.dto.MasterSeatingRowDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory store for the uploaded master seating configuration.
 * Holds master rows between upload and generation requests.
 */
@Component
public class MasterSeatingStore {

    private final List<MasterSeatingRowDTO> masterRows = new ArrayList<>();

    public void setRows(List<MasterSeatingRowDTO> rows) {
        masterRows.clear();
        if (rows != null) {
            masterRows.addAll(rows);
        }
    }

    public List<MasterSeatingRowDTO> getRows() {
        return Collections.unmodifiableList(masterRows);
    }

    public boolean hasData() {
        return !masterRows.isEmpty();
    }

    public void clear() {
        masterRows.clear();
    }

    public int getRowCount() {
        return masterRows.size();
    }
}
