package com.nestly.server.services;

import com.nestly.server.models.Booking;
import com.nestly.server.models.Room;
import com.nestly.server.models.User;
import com.nestly.server.repositories.BookingRepository;
import com.nestly.server.repositories.RoomRepository;
import com.nestly.server.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PayPalService payPalService;

    public BookingService(BookingRepository bookingRepository,
            RoomRepository roomRepository,
            UserRepository userRepository,
            PayPalService payPalService) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.payPalService = payPalService;
    }

    // ✅ Create Booking
    public Booking createBooking(Long userId, Long roomId, String checkInDateStr, String checkOutDateStr,
            String paypalCaptureId, Double amount) {
        logger.info("🟢 Starting booking creation | userId={} | roomId={} | amount={}", userId, roomId, amount);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found for ID=" + userId));
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found for ID=" + roomId));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate checkInDate = LocalDate.parse(checkInDateStr, formatter);
            LocalDate checkOutDate = LocalDate.parse(checkOutDateStr, formatter);

            // Check room availability
            List<Booking> existingBookings = bookingRepository.findByRoomIdAndStatus(roomId, "CONFIRMED");
            boolean overlap = existingBookings.stream()
                    .anyMatch(
                            b -> checkInDate.isBefore(b.getCheckOutDate()) && checkOutDate.isAfter(b.getCheckInDate()));

            if (overlap)
                throw new RuntimeException("Room already booked for selected dates");

            Booking booking = Booking.builder()
                    .user(user)
                    .room(room)
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .status("CONFIRMED")
                    .paypalCaptureId(paypalCaptureId)
                    .refundStatus("PENDING")
                    .amount(amount)
                    .build();

            Booking saved = bookingRepository.save(booking);
            logger.info("✅ Booking created successfully: bookingId={} | status={}", saved.getId(), saved.getStatus());

            return saved;

        } catch (DataAccessException dae) {
            logger.error("❌ Database error while saving booking", dae);
            throw new RuntimeException("Booking failed due to database issue");
        } catch (Exception e) {
            logger.error("❌ Unexpected error during booking creation", e);
            throw new RuntimeException("Booking creation failed");
        }
    }

    // ✅ Cancel Booking and refund
    public void cancelBooking(Long bookingId) {
        logger.info("🚫 Attempting to cancel booking | bookingId={}", bookingId);

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found for ID=" + bookingId));

            // ✅ Prevent re-cancelling already cancelled bookings
            if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
                logger.info("💡 Booking {} is already cancelled. Skipping duplicate cancellation.", bookingId);
                return;
            }

            // ✅ Prevent re-refund
            if ("COMPLETED".equalsIgnoreCase(booking.getRefundStatus())) {
                logger.info("💡 Refund already completed for bookingId={}, skipping duplicate refund", bookingId);
                booking.setStatus("CANCELLED");
                bookingRepository.save(booking);
                return;
            }

            String refundStatus = "NOT_APPLICABLE";

            if (booking.getPaypalCaptureId() != null) {
                logger.info("💰 Initiating refund for captureId={}", booking.getPaypalCaptureId());
                refundStatus = payPalService.refundPayment(booking.getPaypalCaptureId(), booking.getAmount());
                logger.info("✅ Refund result for bookingId={} => {}", bookingId, refundStatus);
            }

            booking.setStatus("CANCELLED");
            booking.setRefundStatus(refundStatus != null ? refundStatus : "FAILED");

            bookingRepository.save(booking);
            logger.info("✅ Booking {} cancelled successfully. RefundStatus={}", bookingId, booking.getRefundStatus());

        } catch (DataAccessException dae) {
            logger.error("❌ Database error while cancelling booking", dae);
            throw new RuntimeException("Database issue while cancelling booking");
        } catch (Exception e) {
            logger.error("❌ Unexpected error during booking cancellation", e);
            throw new RuntimeException("Cancellation failed: " + e.getMessage());
        }
    }

    // ✅ Utility Methods
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<Booking> getBookingsByRoom(Long roomId) {
        return bookingRepository.findByRoomIdAndStatus(roomId, "CONFIRMED");
    }

    public boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        List<Booking> existingBookings = bookingRepository.findByRoomIdAndStatus(roomId, "CONFIRMED");
        return existingBookings.stream()
                .noneMatch(b -> checkIn.isBefore(b.getCheckOutDate()) && checkOut.isAfter(b.getCheckInDate()));
    }
}
