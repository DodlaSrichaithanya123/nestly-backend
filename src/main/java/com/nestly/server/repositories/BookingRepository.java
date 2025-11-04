package com.nestly.server.repositories;

import com.nestly.server.models.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Fetch bookings by user ID
    List<Booking> findByUserId(Long userId);
    // Alternative: findByUser_Id(Long userId);

    // Fetch confirmed bookings for a specific room
    List<Booking> findByRoomIdAndStatus(Long roomId, String status);
    // Alternative: findByRoom_IdAndStatus(Long roomId, String status);
}
