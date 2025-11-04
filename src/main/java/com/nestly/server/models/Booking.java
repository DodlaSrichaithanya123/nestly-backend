package com.nestly.server.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int guests;

    private String status; // CONFIRMED, CANCELLED

    private String paypalCaptureId; // PayPal capture ID
    private String refundStatus; // PENDING, COMPLETED, FAILED

    private Double amount; // Booking amount
}
