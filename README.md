# Hollow Auction Application

A JavaFX-based desktop application for managing and participating in online auctions.

## Features

*   **User Authentication**: Register and log in securely.
*   **Browse Auctions**: View currently active auctions and their details.
*   **Bidding System**: Place bids on items in real-time.
*   **Seller Dashboard**: Create new auctions and manage your listed items.
*   **Real-time Updates**: Track current prices and highest bidders.

## Project Structure

The application follows an MVC-like architecture:

*   **`model`**: Contains data models (Request/Response objects for the API).
*   **`network`**: Handles API communication using Retrofit.
*   **`service`**: Business logic layer (e.g., AuthService).
*   **`controller`**: JavaFX UI controllers, divided by feature:
    *   `auth`: Login and Registration views.
    *   `auction`: Bidding, browsing, and creating auction views.
    *   `layout`: Main application framework.
    *   `navigation`: Scene management.
*   **`AuctionLauncher`**: Main entry point of the JavaFX application.

## Technologies Used

*   **JavaFX**: UI Framework.
*   **Retrofit & Gson**: REST API client and JSON parsing.

## Getting Started

1.  Clone the repository.
2.  Ensure you have Java and Maven installed.
3.  Build the project.
4.  Run `AuctionLauncher.java` to start the application.

## UML Diagram

A full class diagram is available in `project_diagram.puml`. You can render this file using [PlantUML](https://plantuml.com/).
