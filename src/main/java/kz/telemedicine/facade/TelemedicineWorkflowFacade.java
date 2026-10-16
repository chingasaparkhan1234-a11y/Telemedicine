package kz.telemedicine.facade;

import kz.telemedicine.appointment.*;
import kz.telemedicine.videocall.*;
import kz.telemedicine.billing.*;
import kz.telemedicine.orchestration.WorkflowJournalRepository;
import kz.telemedicine.orchestration.WorkflowJournalRepository.JournalEntry;
import kz.telemedicine.orchestration.WorkflowJournalRepository.StepState;

public class TelemedicineWorkflowFacade {
    private final AppointmentRepository apptRepo;
    private final ScheduleValidator scheduleValidator;

    private final VideoSessionRepository videoRepo;
    private final MediaServerAllocator mediaAllocator;

    private final BillingRepository billingRepo;
    private final PaymentRulesEngine paymentEngine;

    private final WorkflowJournalRepository journalRepo;

    public TelemedicineWorkflowFacade(
            AppointmentRepository apptRepo,
            ScheduleValidator scheduleValidator,
            VideoSessionRepository videoRepo,
            MediaServerAllocator mediaAllocator,
            BillingRepository billingRepo,
            PaymentRulesEngine paymentEngine,
            WorkflowJournalRepository journalRepo) {
        this.apptRepo = apptRepo;
        this.scheduleValidator = scheduleValidator;
        this.videoRepo = videoRepo;
        this.mediaAllocator = mediaAllocator;
        this.billingRepo = billingRepo;
        this.paymentEngine = paymentEngine;
        this.journalRepo = journalRepo;
    }

    public String bookCompleteConsultation(String txId, String patientId, String doctorId, int hourSlot, double fee, String cardNumber) {
        scheduleValidator.validateSlot(hourSlot);

        String apptId = "APPT-" + txId;
        String sessId = "SESS-" + txId;
        String invId = "INV-" + txId;

        JournalEntry journal = new JournalEntry(txId, apptId, sessId, invId, StepState.STARTED);
        journalRepo.recordState(journal);

        // Step A: Intake & Reservation
        Appointment appt = new Appointment(apptId, patientId, doctorId, hourSlot);
        appt.confirm();
        apptRepo.save(appt);

        journal.state = StepState.A_COMMITTED;
        journalRepo.recordState(journal);

        // Step B: Video Room Allocation
        VideoSession session;
        try {
            String roomUrl = mediaAllocator.allocateRoom(apptId);
            session = new VideoSession(sessId, apptId, roomUrl);
            session.activate();
            videoRepo.save(session);

            journal.state = StepState.B_COMMITTED;
            journalRepo.recordState(journal);
        } catch (Exception eB) {
            compensateStepA(apptId);
            journal.state = StepState.COMPENSATED;
            journalRepo.recordState(journal);
            throw new RuntimeException("Consultation booking failed at Video stage. Compensated Appointment.", eB);
        }

        // Step C: Billing & Confirmation
        try {
            paymentEngine.processPayment(fee, cardNumber);
            ConsultationInvoice invoice = new ConsultationInvoice(invId, patientId, fee);
            invoice.markPaid();
            billingRepo.save(invoice);

            journal.state = StepState.COMPLETED;
            journalRepo.recordState(journal);
            return "SUCCESS: Consultation booked, video room assigned, payment settled. Tx: " + txId;
        } catch (Exception eC) {
            journal.state = StepState.COMPENSATING_B;
            journalRepo.recordState(journal);
            compensateStepB(sessId);

            compensateStepA(apptId);

            journal.state = StepState.COMPENSATED;
            journalRepo.recordState(journal);
            throw new RuntimeException("Consultation booking failed at Payment stage. Rollback completed (B then A).", eC);
        }
    }

    private void compensateStepA(String apptId) {
        apptRepo.findById(apptId).ifPresent(a -> {
            a.compensateCancellation();
            apptRepo.save(a);
        });
    }

    private void compensateStepB(String sessId) {
        videoRepo.findById(sessId).ifPresent(s -> {
            s.compensateTermination();
            videoRepo.save(s);
        });
    }
}