package kz.telemedicine;

import kz.telemedicine.appointment.*;
import kz.telemedicine.videocall.*;
import kz.telemedicine.billing.*;
import kz.telemedicine.facade.*;
import kz.telemedicine.orchestration.*;
import kz.telemedicine.orchestration.WorkflowJournalRepository.JournalEntry;
import kz.telemedicine.orchestration.WorkflowJournalRepository.StepState;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TelemedicineFacadeTest {

    @TempDir
    Path tempDir;

    private AppointmentRepository apptRepo;
    private ScheduleValidator scheduleValidator;
    private VideoSessionRepository videoRepo;
    private MediaServerAllocator mediaAllocator;
    private BillingRepository billingRepo;
    private PaymentRulesEngine paymentEngine;
    private WorkflowJournalRepository journalRepo;
    private TelemedicineWorkflowFacade facade;

    @BeforeEach
    void setUp() {
        apptRepo = new AppointmentRepository(tempDir);
        scheduleValidator = new ScheduleValidator();
        videoRepo = new VideoSessionRepository(tempDir);
        mediaAllocator = new MediaServerAllocator();
        billingRepo = new BillingRepository(tempDir);
        paymentEngine = new PaymentRulesEngine();
        journalRepo = new WorkflowJournalRepository(tempDir);

        facade = new TelemedicineWorkflowFacade(
                apptRepo, scheduleValidator, videoRepo, mediaAllocator, billingRepo, paymentEngine, journalRepo
        );
    }

    private ConsultationAsserter givenConsultation(String txId) {
        return new ConsultationAsserter(txId, apptRepo, videoRepo, billingRepo, journalRepo);
    }

    static class ConsultationAsserter {
        private final String txId;
        private final AppointmentRepository apptRepo;
        private final VideoSessionRepository videoRepo;
        private final BillingRepository billingRepo;
        private final WorkflowJournalRepository journalRepo;

        ConsultationAsserter(String txId, AppointmentRepository a, VideoSessionRepository v, BillingRepository b, WorkflowJournalRepository j) {
            this.txId = txId;
            this.apptRepo = a;
            this.videoRepo = v;
            this.billingRepo = b;
            this.journalRepo = j;
        }

        public void hasAppointmentStatus(Appointment.Status expected) {
            assertEquals(expected, apptRepo.findById("APPT-" + txId).orElseThrow().getStatus());
        }

        public void hasVideoSessionStatus(VideoSession.Status expected) {
            assertEquals(expected, videoRepo.findById("SESS-" + txId).orElseThrow().getStatus());
        }

        public void hasNoVideoSession() {
            assertTrue(videoRepo.findById("SESS-" + txId).isEmpty());
        }

        public void hasInvoiceStatus(ConsultationInvoice.Status expected) {
            assertEquals(expected, billingRepo.findById("INV-" + txId).orElseThrow().getStatus());
        }

        public void hasJournalState(StepState expected) {
            assertEquals(expected, journalRepo.findAll().get(txId).state);
        }
    }

    @Test
    void testHappyPathExecution() {
        facade.bookCompleteConsultation("101", "P-1", "D-1", 10, 45.0, "4000123456789999");

        givenConsultation("101").hasAppointmentStatus(Appointment.Status.CONFIRMED);
        givenConsultation("101").hasVideoSessionStatus(VideoSession.Status.ROOM_ACTIVE);
        givenConsultation("101").hasInvoiceStatus(ConsultationInvoice.Status.PAID);
        givenConsultation("101").hasJournalState(StepState.COMPLETED);
    }

    @Test
    void testStepBFailureCompensatesA() {
        mediaAllocator.setSimulateOverload(true);

        assertThrows(RuntimeException.class, () ->
                facade.bookCompleteConsultation("102", "P-2", "D-1", 11, 45.0, "4000123456789999")
        );

        givenConsultation("102").hasAppointmentStatus(Appointment.Status.CANCELLED_COMPENSATED);
        givenConsultation("102").hasNoVideoSession();
        givenConsultation("102").hasJournalState(StepState.COMPENSATED);
    }

    @Test
    void testStepCFailureCompensatesBThenA() {
        paymentEngine.setSimulateDecline(true);

        assertThrows(RuntimeException.class, () ->
                facade.bookCompleteConsultation("103", "P-3", "D-1", 12, 45.0, "4000123456780000")
        );

        givenConsultation("103").hasVideoSessionStatus(VideoSession.Status.TERMINATED_COMPENSATED);
        givenConsultation("103").hasAppointmentStatus(Appointment.Status.CANCELLED_COMPENSATED);
        givenConsultation("103").hasJournalState(StepState.COMPENSATED);
    }

    @Test
    void testRestartMidChainRecovery() {
        JournalEntry crashedRecord = new JournalEntry(
                "CRASH-99", "APPT-CRASH-99", "SESS-CRASH-99", "INV-CRASH-99", StepState.B_COMMITTED
        );
        journalRepo.recordState(crashedRecord);

        Appointment a = new Appointment("APPT-CRASH-99", "P-99", "D-99", 14);
        a.confirm();
        apptRepo.save(a);

        VideoSession v = new VideoSession("SESS-CRASH-99", "APPT-CRASH-99", "https://room");
        v.activate();
        videoRepo.save(v);

        TelemedicineAdminFacade admin = new TelemedicineAdminFacade(
                new AppointmentRepository(tempDir),
                new VideoSessionRepository(tempDir),
                new BillingRepository(tempDir),
                new WorkflowJournalRepository(tempDir)
        );

        admin.resumeUnfinishedTransactions();

        givenConsultation("CRASH-99").hasAppointmentStatus(Appointment.Status.CANCELLED_COMPENSATED);
        givenConsultation("CRASH-99").hasVideoSessionStatus(VideoSession.Status.TERMINATED_COMPENSATED);
        givenConsultation("CRASH-99").hasJournalState(StepState.COMPENSATED);
    }
}