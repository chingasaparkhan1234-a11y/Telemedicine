package kz.telemedicine.billing;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BillingRepository {
    private final Path storagePath;

    public BillingRepository(Path baseDir) {
        this.storagePath = baseDir.resolve("invoices.csv");
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

    public synchronized void save(ConsultationInvoice invoice) {
        Map<String, ConsultationInvoice> all = findAll();
        all.put(invoice.getInvoiceId(), invoice);
        writeAll(all.values());
    }

    public synchronized Optional<ConsultationInvoice> findById(String id) {
        return Optional.ofNullable(findAll().get(id));
    }

    public synchronized Map<String, ConsultationInvoice> findAll() {
        Map<String, ConsultationInvoice> result = new LinkedHashMap<>();
        try {
            if (!Files.exists(storagePath)) return result;
            List<String> lines = Files.readAllLines(storagePath);
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] p = line.split(";");
                if (p.length >= 4) {
                    result.put(p[0], new ConsultationInvoice(p[0], p[1], Double.parseDouble(p[2]), ConsultationInvoice.Status.valueOf(p[3])));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private void writeAll(Collection<ConsultationInvoice> list) {
        List<String> lines = new ArrayList<>();
        for (ConsultationInvoice inv : list) {
            lines.add(String.join(";", inv.getInvoiceId(), inv.getPatientId(), String.valueOf(inv.getAmount()), inv.getStatus().name()));
        }
        try {
            Files.write(storagePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}