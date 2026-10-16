package kz.telemedicine.videocall;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class VideoSessionRepository {
    private final Path storagePath;

    public VideoSessionRepository(Path baseDir) {
        this.storagePath = baseDir.resolve("videosessions.csv");
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

    public synchronized void save(VideoSession vs) {
        Map<String, VideoSession> all = findAll();
        all.put(vs.getSessionId(), vs);
        writeAll(all.values());
    }

    public synchronized Optional<VideoSession> findById(String id) {
        return Optional.ofNullable(findAll().get(id));
    }

    public synchronized Map<String, VideoSession> findAll() {
        Map<String, VideoSession> result = new LinkedHashMap<>();
        try {
            if (!Files.exists(storagePath)) return result;
            List<String> lines = Files.readAllLines(storagePath);
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] p = line.split(";");
                if (p.length >= 4) {
                    result.put(p[0], new VideoSession(p[0], p[1], p[2], VideoSession.Status.valueOf(p[3])));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private void writeAll(Collection<VideoSession> list) {
        List<String> lines = new ArrayList<>();
        for (VideoSession vs : list) {
            lines.add(String.join(";", vs.getSessionId(), vs.getAppointmentRefId(), vs.getRoomUrl(), vs.getStatus().name()));
        }
        try {
            Files.write(storagePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}