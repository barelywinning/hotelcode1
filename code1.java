import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

enum RoomType {
    SINGLE(100), DOUBLE(150), SUITE(250);

    private final int price;

    RoomType(int price) {
        this.price = price;
    }

    public int getPrice() {
        return price;
    }
}

class HotelEntity {
    private String id;

    public HotelEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}

class Room extends HotelEntity {
    private boolean isOccupied;
    private List<String> bookingHistory;
    private RoomType roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    public Room(String id, RoomType roomType) {
        super(id);
        this.isOccupied = false;
        this.bookingHistory = new ArrayList<>();
        this.roomType = roomType;
    }

    public synchronized void checkIn(String guestName, LocalDate checkInDate) throws RoomOccupiedException {
        if (isOccupied) {
            throw new RoomOccupiedException("Room is already occupied.");
        }
        isOccupied = true;
        this.checkInDate = checkInDate;
        bookingHistory.add("Checked in: " + guestName + " on " + checkInDate);
    }

    public synchronized void checkOut(LocalDate checkOutDate) throws RoomNotOccupiedException {
        if (!isOccupied) {
            throw new RoomNotOccupiedException("Room is not occupied.");
        }
        isOccupied = false;
        this.checkOutDate = checkOutDate;
        bookingHistory.add("Checked out on " + checkOutDate);
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public List<String> getBookingHistory() {
        return bookingHistory;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public int getPrice() {
        return roomType.getPrice();
    }
}

class RoomOccupiedException extends RuntimeException {
    public RoomOccupiedException(String message) {
        super(message);
    }
}

class RoomNotOccupiedException extends RuntimeException {
    public RoomNotOccupiedException(String message) {
        super(message);
    }
}

class HotelService<T extends HotelEntity> {
    private List<Room> rooms;
    private int totalRevenue = 0;

    public HotelService() {
        rooms = new ArrayList<>();
        rooms.add(new Room("101", RoomType.SINGLE));
        rooms.add(new Room("102", RoomType.DOUBLE));
        rooms.add(new Room("103", RoomType.SUITE));
    }

    public Room getRoom(String id) throws IllegalArgumentException {
        for (Room room : rooms) {
            if (room.getId().equals(id)) {
                return room;
            }
        }
        throw new IllegalArgumentException("Room not found");
    }

    public List<Room> getAvailableRooms() {
        List<Room> availableRooms = new ArrayList<>();
        for (Room room : rooms) {
            if (!room.isOccupied()) {
                availableRooms.add(room);
            }
        }
        return availableRooms;
    }

    public List<Room> getRoomsByType(RoomType type) {
        List<Room> roomsOfType = new ArrayList<>();
        for (Room room : rooms) {
            if (room.getRoomType() == type) {
                roomsOfType.add(room);
            }
        }
        return roomsOfType;
    }

    public List<Room> getAvailableRoomsOnDate(LocalDate date) {
        List<Room> availableRooms = new ArrayList<>();
        for (Room room : rooms) {
            if (!room.isOccupied()) {
                availableRooms.add(room);
            }
        }
        return availableRooms;
    }

    public void addRoom(Room room) {
        rooms.add(room);
    }

    public int getTotalRevenue() {
        return totalRevenue;
    }

    public void checkOutRoom(Room room) {
        if (room.isOccupied()) {
            totalRevenue += room.getPrice();
            room.checkOut(LocalDate.now());
        }
    }

    public List<Room> getRooms() {
        return rooms;
    }
}

public class HotelManagementApp extends Application {
    private HotelService<Room> hotelService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        hotelService = new HotelService<>();

        TextField roomIdField = new TextField();
        roomIdField.setPromptText("Enter Room ID");

        TextField guestNameField = new TextField();
        guestNameField.setPromptText("Enter Guest Name");

        DatePicker checkInDatePicker = new DatePicker();
        DatePicker checkOutDatePicker = new DatePicker();

        Button checkInButton = new Button("Check In");
        Button checkoutButton = new Button("Checkout");
        Button viewAvailableRoomsButton = new Button("View Available Rooms");
        Button viewBookingHistoryButton = new Button("View Booking History");
        Button viewRevenueButton = new Button("View Total Revenue");
        Button searchByTypeButton = new Button("Search by Room Type");
        Button addNewRoomButton = new Button("Add New Room");
        Button cancelReservationButton = new Button("Cancel Reservation");

        ChoiceBox<RoomType> roomTypeChoiceBox = new ChoiceBox<>();
        roomTypeChoiceBox.getItems().addAll(RoomType.values());

        checkInButton.setOnAction(e -> {
            String roomId = roomIdField.getText();
            String guestName = guestNameField.getText();
            LocalDate checkInDate = checkInDatePicker.getValue();
            if (roomId.isEmpty() || guestName.isEmpty() || checkInDate == null) {
                showErrorAlert("Please provide all details.");
                return;
            }

            Room room = hotelService.getRoom(roomId);

            
            Runnable checkInTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        room.checkIn(guestName, checkInDate);
                        Platform.runLater(() -> {
                            showInfoAlert("Check-in Successful", "Room " + roomId + " has been checked in for " + guestName + " on " + checkInDate);
                        });
                    } catch (RoomOccupiedException ex) {
                        Platform.runLater(() -> showErrorAlert(ex.getMessage()));
                    }
                }
            };

            
            new Thread(checkInTask).start();
        });

        checkoutButton.setOnAction(e -> {
            String roomId = roomIdField.getText();
            Room room = hotelService.getRoom(roomId);

            
            Runnable checkOutTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        hotelService.checkOutRoom(room);
                        Platform.runLater(() -> {
                            showInfoAlert("Checkout Successful", "Room " + roomId + " has been checked out.");
                        });
                    } catch (RoomNotOccupiedException ex) {
                        Platform.runLater(() -> showErrorAlert(ex.getMessage()));
                    }
                }
            };

            
            new Thread(checkOutTask).start();
        });

        viewAvailableRoomsButton.setOnAction(e -> {
            List<Room> availableRooms = hotelService.getAvailableRooms();
            StringBuilder roomsList = new StringBuilder("Available Rooms:\n");
            for (Room room : availableRooms) {
                roomsList.append(room.getId()).append(" (").append(room.getRoomType()).append(" - $").append(room.getPrice()).append(")\n");
            }
            showInfoAlert("Available Rooms", roomsList.toString());
        });

        viewBookingHistoryButton.setOnAction(e -> {
            String roomId = roomIdField.getText();
            Room room = hotelService.getRoom(roomId);
            StringBuilder history = new StringBuilder("Booking History for Room " + roomId + ":\n");
            for (String entry : room.getBookingHistory()) {
                history.append(entry).append("\n");
            }
            showInfoAlert("Booking History", history.toString());
        });

        viewRevenueButton.setOnAction(e -> {
            int revenue = hotelService.getTotalRevenue();
            showInfoAlert("Total Revenue", "Total revenue: $" + revenue);
        });

        searchByTypeButton.setOnAction(e -> {
            RoomType selectedType = roomTypeChoiceBox.getValue();
            List<Room> roomsByType = hotelService.getRoomsByType(selectedType);
            StringBuilder roomsList = new StringBuilder("Rooms by Type " + selectedType + ":\n");
            for (Room room : roomsByType) {
                roomsList.append(room.getId()).append(" - $").append(room.getPrice()).append("\n");
            }
            showInfoAlert("Rooms by Type", roomsList.toString());
        });

        addNewRoomButton.setOnAction(e -> {
            String newRoomId = roomIdField.getText();
            RoomType selectedType = roomTypeChoiceBox.getValue();
            if (newRoomId.isEmpty() || selectedType == null) {
                showErrorAlert("Please provide room ID and type.");
                return;
            }
            hotelService.addRoom(new Room(newRoomId, selectedType));
            showInfoAlert("Room Added", "Room " + newRoomId + " has been added.");
        });

        cancelReservationButton.setOnAction(e -> {
            String roomId = roomIdField.getText();
            Room room = hotelService.getRoom(roomId);
            if (room.isOccupied()) {
                Runnable cancelTask = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            room.checkOut(LocalDate.now());
                            Platform.runLater(() -> showInfoAlert("Reservation Cancelled", "Room " + roomId + " reservation has been cancelled."));
                        } catch (RoomNotOccupiedException ex) {
                            Platform.runLater(() -> showErrorAlert(ex.getMessage()));
                        }
                    }
                };
                new Thread(cancelTask).start();
            } else {
                showErrorAlert("Room is not occupied.");
            }
        });

        VBox layout = new VBox(10, roomIdField, guestNameField, checkInDatePicker, checkOutDatePicker,
                checkInButton, checkoutButton, viewAvailableRoomsButton, viewBookingHistoryButton, viewRevenueButton,
                roomTypeChoiceBox, searchByTypeButton, addNewRoomButton, cancelReservationButton);
        Scene scene = new Scene(layout, 400, 500);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Hotel Management System");
        primaryStage.show();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
