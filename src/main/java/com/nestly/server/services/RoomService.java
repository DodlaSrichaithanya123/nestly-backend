package com.nestly.server.services;

import com.nestly.server.models.Room;
import com.nestly.server.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    // private final String uploadDir = "uploads/"; // folder inside project
    // New folder: inside resources/static/images
    private final String uploadDir = "src/main/resources/static/images/";

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getFeaturedRooms() {
        return roomRepository.findByFeaturedTrue();
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room addRoom(Room room) {
        return roomRepository.save(room);
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }

    // New method to handle file upload safely
    public Room addRoomWithFile(String name, String type, Double price, Boolean featured,
            String description, String city,
            String address, byte[] fileBytes, String originalFileName) {
        try {
            // Ensure upload directory exists
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Generate a unique filename to avoid overwriting
            String extension = "";
            int i = originalFileName.lastIndexOf('.');
            if (i > 0) {
                extension = originalFileName.substring(i);
            }
            String uniqueFileName = UUID.randomUUID() + extension;
            Path filePath = dirPath.resolve(uniqueFileName);

            // Write file to disk
            Files.write(filePath, fileBytes);

            // Save room info with file path
            // Room room = new Room(name, type, price, featured, description, uploadDir +
            // uniqueFileName);
            // Save room info with URL pointing to /images/...
            Room room = new Room(
                    name,
                    type,
                    price,
                    featured,
                    description,
                    "/images/" + uniqueFileName, // image URL
                    true, // available by default
                    city, // new city field
                    address // new address field
            );
            return roomRepository.save(room);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }

    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        roomRepository.delete(room);
    }
}
