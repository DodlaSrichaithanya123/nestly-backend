package com.nestly.server.controllers;

import com.nestly.server.models.Room;
import com.nestly.server.services.RoomService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "${frontend.url:http://localhost:5173}")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/featured")
    public List<Room> getFeaturedRooms() {
        return roomService.getFeaturedRooms();
    }

    @GetMapping
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    @GetMapping("/{id}")
    public Room getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id);
    }

    @DeleteMapping("/{id}")
    public String deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return "Room with ID " + id + " has been deleted successfully!";
    }

    @GetMapping("/test")
    public String test() {
        return "API is working!";
    }

    // Updated endpoint for file upload
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload")
    public Room addRoomWithFile(
            @RequestParam("name") String name,
            @RequestParam("type") String type,
            @RequestParam("price") Double price,
            @RequestParam("featured") Boolean featured,
            @RequestParam("description") String description,
            @RequestParam("city") String city,
            @RequestParam("address") String address,
            @RequestParam("file") MultipartFile file) {

        try {

            return roomService.addRoomWithFile(
                    name,
                    type,
                    price,
                    featured,
                    description,
                    city,
                    address,
                    file.getBytes(),
                    file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
    }
}
