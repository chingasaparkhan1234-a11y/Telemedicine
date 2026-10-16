# Telemedicine Consultation Orchestrator (Facade Pattern)

Assignment 4: Facade Over a Three-Stage Chain with Persistence and Live Demo.

---

## 1. Domain Architecture & Subsystems
The system orchestrates a medical appointment workflow across three completely autonomous, decoupled subsystems ($A \to B \to C$):

- **Subsystem A: Intake & Reservation (`kz.telemedicine.appointment`)**
    - **Classes:** `Appointment`, `ScheduleValidator`, `AppointmentRepository`.
    - **State Transitions:** `NEW` $\to$ `CONFIRMED` $\to$ `CANCELLED_COMPENSATED`.
    - **Persistence:** Direct CSV gateway (`data/appointments.csv`).

- **Subsystem B: Processing & Execution (`kz.telemedicine.videocall`)**
    - **Classes:** `VideoSession`, `MediaServerAllocator`, `VideoSessionRepository`.
    - **State Transitions:** `INITIALIZING` $\to$ `ROOM_ACTIVE` $\to$ `TERMINATED_COMPENSATED`.
    - **Persistence:** Direct CSV gateway (`data/videosessions.csv`).

- **Subsystem C: Finalization & Confirmation (`kz.telemedicine.billing`)**
    - **Classes:** `ConsultationInvoice`, `PaymentRulesEngine`, `BillingRepository`.
    - **State Transitions:** `PENDING` $\to$ `PAID` $\to$ `REFUNDED_COMPENSATED`.
    - **Persistence:** Direct CSV gateway (`data/invoices.csv`).

- **Saga Orchestration Journal (`kz.telemedicine.orchestration`)**
    - **Class:** `WorkflowJournalRepository`.
    - Tracks distributed progress across restart boundaries: `STARTED` $\to$ `A_COMMITTED` $\to$ `B_COMMITTED` $\to$ `COMPLETED` (or `COMPENSATING_B` $\to$ `COMPENSATED`).

None of the subsystems import, call, or depend on classes from other subsystems.

---

## 2. Principle of Least Knowledge (Law of Demeter)

### Before (Direct Subsystem Interaction Without Facade)
Without a facade, the client is tightly coupled to the internal structures, validators, and repositories of all three subsystems:

```java
// Client must manage 6+ direct dependencies, order of operations, and rollback branches:
ScheduleValidator validator = new ScheduleValidator();
validator.validateSlot(14);

Appointment appt = new Appointment("APPT-1", "PATIENT-1", "DOC-42", 14);
appt.confirm();
apptRepo.save(appt);

try {
    MediaServerAllocator allocator = new MediaServerAllocator();
    String roomUrl = allocator.allocateRoom("APPT-1");
    VideoSession session = new VideoSession("SESS-1", "APPT-1", roomUrl);
    session.activate();
    videoRepo.save(session);

    try {
        PaymentRulesEngine paymentEngine = new PaymentRulesEngine();
        paymentEngine.processPayment(50.0, "4400123456789999");
        ConsultationInvoice invoice = new ConsultationInvoice("INV-1", "PATIENT-1", 50.0);
        invoice.markPaid();
        billingRepo.save(invoice);
    } catch (Exception ePayment) {
        // Client is forced to manually know how to undo B then A
        session.compensateTermination();
        videoRepo.save(session);
        appt.compensateCancellation();
        apptRepo.save(appt);
        throw ePayment;
    }
} catch (Exception eVideo) {
    // Client is forced to manually know how to undo A
    appt.compensateCancellation();
    apptRepo.save(appt);
    throw eVideo;
}