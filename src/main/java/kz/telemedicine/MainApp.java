package kz.telemedicine;

import kz.telemedicine.appointment.*;
import kz.telemedicine.videocall.*;
import kz.telemedicine.billing.*;
import kz.telemedicine.facade.*;
import kz.telemedicine.orchestration.*;

import java.nio.file.*;
import java.util.Scanner;

public class MainApp {
    public static void main(String[] args) {
        Path dataDir = Paths.get("data");

        AppointmentRepository apptRepo = new AppointmentRepository(dataDir);
        ScheduleValidator scheduleValidator = new ScheduleValidator();

        VideoSessionRepository videoRepo = new VideoSessionRepository(dataDir);
        MediaServerAllocator mediaAllocator = new MediaServerAllocator();

        BillingRepository billingRepo = new BillingRepository(dataDir);
        PaymentRulesEngine paymentEngine = new PaymentRulesEngine();

        WorkflowJournalRepository journalRepo = new WorkflowJournalRepository(dataDir);

        TelemedicineWorkflowFacade clientFacade = new TelemedicineWorkflowFacade(
                apptRepo, scheduleValidator, videoRepo, mediaAllocator, billingRepo, paymentEngine, journalRepo
        );
        TelemedicineAdminFacade adminFacade = new TelemedicineAdminFacade(
                apptRepo, videoRepo, billingRepo, journalRepo
        );

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- TELEMEDICINE FACADE DEMO ---");
            System.out.println("1. Успешная цепочка (A -> B -> C: Happy Path)");
            System.out.println("2. Сбой шага B (Перегрузка сервера -> откат только A)");
            System.out.println("3. Сбой шага C (Отказ карты -> откат B, затем A)");
            System.out.println("4. Проверить состояние диска (Admin Inspection)");
            System.out.println("5. Симуляция рестарта и восстановление прерванных транзакций");
            System.out.println("6. Очистить все файлы хранилища (Clean Reset)");
            System.out.println("0. Выход");
            System.out.print("Выберите действие: ");

            String choice = scanner.nextLine().trim();
            String txId = "TX-" + System.currentTimeMillis() % 10000;

            try {
                switch (choice) {
                    case "1" -> {
                        mediaAllocator.setSimulateOverload(false);
                        paymentEngine.setSimulateDecline(false);
                        String res = clientFacade.bookCompleteConsultation(txId, "PATIENT-1", "DOC-42", 14, 50.0, "4400123456789999");
                        System.out.println(res);
                    }
                    case "2" -> {
                        mediaAllocator.setSimulateOverload(true);
                        try {
                            clientFacade.bookCompleteConsultation(txId, "PATIENT-2", "DOC-42", 15, 50.0, "4400123456789999");
                        } catch (Exception e) {
                            System.out.println("Успешно зафиксирован ожидаемый сбой: " + e.getMessage());
                        } finally {
                            mediaAllocator.setSimulateOverload(false);
                        }
                    }
                    case "3" -> {
                        mediaAllocator.setSimulateOverload(false);
                        paymentEngine.setSimulateDecline(true);
                        try {
                            clientFacade.bookCompleteConsultation(txId, "PATIENT-3", "DOC-42", 16, 75.0, "4400123456780000");
                        } catch (Exception e) {
                            System.out.println("Успешно зафиксирован ожидаемый сбой: " + e.getMessage());
                        } finally {
                            paymentEngine.setSimulateDecline(false);
                        }
                    }
                    case "4" -> adminFacade.printFullSystemAudit();
                    case "5" -> {
                        System.out.println("Перезагрузка репозиториев с диска...");
                        adminFacade.resumeUnfinishedTransactions();
                        System.out.println("Готово!");
                    }
                    case "6" -> {
                        Files.deleteIfExists(dataDir.resolve("appointments.csv"));
                        Files.deleteIfExists(dataDir.resolve("videosessions.csv"));
                        Files.deleteIfExists(dataDir.resolve("invoices.csv"));
                        Files.deleteIfExists(dataDir.resolve("workflow_journal.csv"));
                        System.out.println("Хранилище очищено!");
                    }
                    case "0" -> {
                        System.out.println("Выход.");
                        return;
                    }
                }
            } catch (Exception e) {
                System.out.println("Ошибка выполнения: " + e.getMessage());
            }
        }
    }
}