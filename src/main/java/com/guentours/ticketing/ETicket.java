package com.guentours.ticketing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "e_tickets")
public class ETicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "booking_id", nullable = false)
    private String bookingId;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @Column(name = "provider_confirmation_number")
    private String providerConfirmationNumber;

    @Lob
    @Column(name = "document")
    private String document;

    /** Public MinIO URL of the branded PDF (company logo, plus the reseller's when the booking
     *  went through one) - null when PDF generation failed, in which case {@link #document} (the
     *  plain-text fallback) is still available. */
    @Column(name = "pdf_url", length = 512)
    private String pdfUrl;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    protected ETicket() {
        // JPA
    }

    public ETicket(String bookingId, String ticketNumber, String providerConfirmationNumber, String document) {
        this.bookingId = bookingId;
        this.ticketNumber = ticketNumber;
        this.providerConfirmationNumber = providerConfirmationNumber;
        this.document = document;
    }

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getProviderConfirmationNumber() {
        return providerConfirmationNumber;
    }

    public String getDocument() {
        return document;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
