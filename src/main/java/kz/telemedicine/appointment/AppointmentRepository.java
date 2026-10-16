package kz.telemedicine.appointment;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AppointmentRepository {
    private final Path storagePath;

    public AppointmentRepository(Path baseDir) {
        this.storagePath = baseDir.resolve("appointments.csv");
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

    public synchronized void save(Appointment appt) {
        Map<String, Appointment> all = findAll();
        all.put(appt.getAppointmentId(), appt);
        writeAll(all.values());
    }

    public synchronized Optional<Appointment> findById(String id) {
        return Optional.ofNullable(findAll().get(id));
    }

    public synchronized Map<String, Appointment> findAll() {
        Map<String, Appointment> result = new LinkedHashMap<>();
        try {
            if (!Files.exists(storagePath)) return result;
            List<String> lines = Files.readAllLines(storagePath);
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] p = line.split(";");
                if (p.length >= 5) {
                    result.put(p[0], new Appointment(p[0], p[1], p[2], Integer.parseInt(p[3]), Appointment.Status.valueOf(p[4])));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private void writeAll(Collection<Appointment> list) {
        List<String> lines = new ArrayList<>();
        for (Appointment a : list) {
            lines.add(String.join(";", a.getAppointmentId(), a.getPatientId(), a.getDoctorId(),
                    String.valueOf(a.getHourSlot()), a.getStatus().name()));
        }
        try {
            Files.write(storagePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}