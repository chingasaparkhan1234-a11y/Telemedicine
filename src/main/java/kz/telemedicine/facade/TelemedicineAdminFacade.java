package kz.telemedicine.facade;

import kz.telemedicine.appointment.AppointmentRepository;
import kz.telemedicine.videocall.VideoSessionRepository;
import kz.telemedicine.billing.BillingRepository;
import kz.telemedicine.orchestration.WorkflowJournalRepository;
import kz.telemedicine.orchestration.WorkflowJournalRepository.JournalEntry;
import kz.telemedicine.orchestration.WorkflowJournalRepository.StepState;

public class TelemedicineAdminFacade {
    private final AppointmentRepository apptRepo;
    private final VideoSessionRepository videoRepo;
    private final BillingRepository billingRepo;
    private final WorkflowJournalRepository journalRepo;

    public TelemedicineAdminFacade(
            AppointmentRepository apptRepo,
            VideoSessionRepository videoRepo,
            BillingRepository billingRepo,
            WorkflowJournalRepository journalRepo) {
        this.apptRepo = apptRepo;
        this.videoRepo = videoRepo;
        this.billingRepo = billingRepo;
        this.journalRepo = journalRepo;
    }

    public void printFullSystemAudit() {
        System.out.println("=== SYSTEM AUDIT REPORT ===");
        System.out.println("--- Subsystem A (Appointments) ---");
        apptRepo.findAll().values().forEach(a ->
                System.out.printf("  ID: %s, Patient: %s, Slot: %02d:00, Status: %s%n", a.getAppointmentId(), a.getPatientId(), a.getHourSlot(), a.getStatus()));

        System.out.println("--- Subsystem B (Video Sessions) ---");
        videoRepo.findAll().values().forEach(v ->
                System.out.printf("  ID: %s, ApptRef: %s, Status: %s%n", v.getSessionId(), v.getAppointmentRefId(), v.getStatus()));

        System.out.println("--- Subsystem C (Invoices) ---");
        billingRepo.findAll().values().forEach(i ->
                System.out.printf("  ID: %s, Patient: %s, Amount: $%.2f, Status: %s%n", i.getInvoiceId(), i.getPatientId(), i.getAmount(), i.getStatus()));

        System.out.println("--- Workflow Transactions Journal ---");
        journalRepo.findAll().values().forEach(j ->
                System.out.printf("  Tx: %s, StepState: %s%n", j.txId, j.state));
        System.out.println("===========================");
    }

    public void resumeUnfinishedTransactions() {
        for (JournalEntry r : journalRepo.findAll().values()) {
            if (r.state == StepState.A_COMMITTED) {
                System.out.println("Recovering uncompleted Tx " + r.txId + " (stuck at A_COMMITTED). Rolling back A...");
                apptRepo.findById(r.appointmentId).ifPresent(a -> {
                    a.compensateCancellation();
                    apptRepo.save(a);
                });
                r.state = StepState.COMPENSATED;
                journalRepo.recordState(r);
            } else if (r.state == StepState.B_COMMITTED || r.state == StepState.COMPENSATING_B) {
                System.out.println("Recovering uncompleted Tx " + r.txId + " (stuck at B). Rolling back B then A...");
                videoRepo.findById(r.sessionId).ifPresent(s -> {
                    s.compensateTermination();
                    videoRepo.save(s);
                });
                apptRepo.findById(r.appointmentId).ifPresent(a -> {
                    a.compensateCancellation();
                    apptRepo.save(a);
                });
                r.state = StepState.COMPENSATED;
                journalRepo.recordState(r);
            }
        }
    }
}