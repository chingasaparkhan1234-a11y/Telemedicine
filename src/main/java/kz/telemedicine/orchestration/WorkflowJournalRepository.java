package kz.telemedicine.orchestration;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class WorkflowJournalRepository {
    public enum StepState {
        STARTED,
        A_COMMITTED,
        B_COMMITTED,
        COMPLETED,
        COMPENSATING_B,
        COMPENSATED
    }

    // Переименовано с Record на JournalEntry, чтобы не конфликтовать с java.lang.Record
    public static class JournalEntry {
        public final String txId;
        public final String appointmentId;
        public final String sessionId;
        public final String invoiceId;
        public StepState state;

        public JournalEntry(String txId, String appointmentId, String sessionId, String invoiceId, StepState state) {
            this.txId = txId;
            this.appointmentId = appointmentId;
            this.sessionId = sessionId;
            this.invoiceId = invoiceId;
            this.state = state;
        }
    }

    private final Path storagePath;

    public WorkflowJournalRepository(Path baseDir) {
        this.storagePath = baseDir.resolve("workflow_journal.csv");
        initFile();
    }

    private void initFile() {
        try {
            if (storagePath.getParent() != null && !Files.exists(storagePath.getParent())) {
                Files.createDirectories(storagePath.getParent());
            }
            if (!Files.exists(storagePath)) {
                Files.createFile(storagePath);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public synchronized void recordState(JournalEntry entry) {
        Map<String, JournalEntry> all = findAll();
        all.put(entry.txId, entry);
        List<String> lines = new ArrayList<>();
        for (JournalEntry r : all.values()) {
            lines.add(String.join(";", r.txId, r.appointmentId, r.sessionId, r.invoiceId, r.state.name()));
        }
        try {
            Files.write(storagePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public synchronized Map<String, JournalEntry> findAll() {
        Map<String, JournalEntry> map = new LinkedHashMap<>();
        try {
            if (!Files.exists(storagePath)) return map;
            List<String> lines = Files.readAllLines(storagePath);
            for (String l : lines) {
                if (l.isBlank()) continue;
                String[] p = l.split(";");
                if (p.length >= 5) {
                    map.put(p[0], new JournalEntry(p[0], p[1], p[2], p[3], StepState.valueOf(p[4])));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return map;
    }
}