package com.nestly.server.controllers;

import com.nestly.server.models.Booking;
import com.nestly.server.services.BookingService;
import com.nestly.server.services.PayPalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "${frontend.url:http://localhost:5173}")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;
    private final PayPalService payPalService;

    public BookingController(BookingService bookingService, PayPalService payPalService) {
        this.bookingService = bookingService;
        this.payPalService = payPalService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> bookingData) {
        try {
            Long userId = ((Number) bookingData.get("userId")).longValue();
            Long roomId = ((Number) bookingData.get("roomId")).longValue();
            String checkInDate = (String) bookingData.get("checkInDate");
            String checkOutDate = (String) bookingData.get("checkOutDate");
            String paypalCaptureId = (String) bookingData.get("paypalCaptureId");
            Double amount = ((Number) bookingData.get("amount")).doubleValue();

            boolean available = bookingService.isRoomAvailable(roomId,
                    LocalDate.parse(checkInDate), LocalDate.parse(checkOutDate));
            if (!available)
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Room not available for the selected dates");

            Booking booking = bookingService.createBooking(userId, roomId, checkInDate, checkOutDate, paypalCaptureId,
                    amount);
            return ResponseEntity.ok(booking);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating booking: " + e.getMessage());
        }
    }

    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/room/{roomId}/booked-dates")
    public List<Map<String, LocalDate>> getBookedDates(@PathVariable Long roomId) {
        List<Booking> bookings = bookingService.getBookingsByRoom(roomId);
        return bookings.stream()
                .map(b -> Map.of(
                        "checkInDate", b.getCheckInDate(),
                        "checkOutDate", b.getCheckOutDate()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public Optional<Booking> getBookingById(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @PutMapping("/cancel/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId) {
        try {
            // Fetch the booking first
            Booking booking = bookingService.getBookingById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // If there's a PayPal payment, attempt a refund
            if (booking.getPaypalCaptureId() != null && !booking.getPaypalCaptureId().isEmpty()) {
                String refundStatus = payPalService.refundPayment(booking.getPaypalCaptureId(), booking.getAmount());
                booking.setRefundStatus(refundStatus); // optional: store refund status in booking
            }

            // Cancel booking in DB
            bookingService.cancelBooking(bookingId);

            Booking updatedBooking = bookingService.getBookingById(bookingId).get();
            return ResponseEntity.ok(updatedBooking);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error cancelling booking: " + e.getMessage());
        }
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<?> getMyBookings(@RequestParam Long userId) {
        try {
            List<Booking> bookings = bookingService.getBookingsByUser(userId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching bookings: " + e.getMessage());
        }
    }
}
