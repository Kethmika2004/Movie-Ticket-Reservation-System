# 🎬 Movie Ticket Reservation System

A Java-based console application for booking movie tickets, developed as part of the Object-Oriented Programming – Exception Handling Lab.
  
**Course:** Program Construction  
**Language:** Java 17+

---

## 📋 Features

- Browse available movies loaded from a CSV database
- Select movie, date, and showtime
- Choose preferred seats with real-time availability
- Exception handling for all invalid inputs
- Auto-saves session after 120 seconds of inactivity
- Generates a billing confirmation file on successful booking
- Email validation for billing

---

## ⚠️ Exceptions Handled

| Exception | Trigger |
|---|---|
| `InvalidMovieCodeException` | Movie code not found |
| `InvalidDateException` | Date unavailable or wrong format |
| `InvalidShowtimeException` | Showtime number out of range |
| `InvalidTicketQuantityException` | Non-positive or non-integer ticket count |
| `OverbookingException` | Requested tickets exceed available seats |
| `InvalidSeatException` | Seat invalid or already taken |

---

## 📁 Project Structure
```
📁 YourProjectName/
    ├── 📁 src/
    │     └── MovieTicketReservation.java
    └── Movie_Reservation_Dataset.csv
```

---

## 🗄️ CSV Database Format

The system reads movie data from `Movie_Reservation_Dataset.csv`.

**Format (comma-separated):**
```
Movie Code, Movie Name, Date, Showtime, Total Seats, Available Seats, Ticket Price, Language, Genre
M001, The Grand Adventure, 2025-04-01, Morning, 100, 90, 12.50, English, Adventure
```

| Column | Description |
|---|---|
| Movie Code | Unique movie identifier |
| Movie Name | Title of the movie |
| Date | Showing date (yyyy-MM-dd) |
| Showtime | Morning / Afternoon / Evening |
| Total Seats | Total capacity for that showtime |
| Available Seats | Seats still available |
| Ticket Price | Price per ticket in USD |
| Language | Movie language |
| Genre | Movie genre |

---

## 🚀 How to Run

### Option 1 — IntelliJ IDEA

1. Open the project in IntelliJ
2. Right-click `src/` folder → **Mark Directory as** → **Sources Root**
3. Place `Movie_Reservation_Dataset.csv` in the **project root folder**
4. Click the green ▶ button next to `main()` and run

### Option 2 — Terminal / Command Line
```bash
cd src
javac MovieTicketReservation.java
java MovieTicketReservation
```

> Make sure `Movie_Reservation_Dataset.csv` is inside the `src/` folder when running from terminal.

---

## 🎮 How to Use
```
1. View the list of available movies
2. Enter a movie code (e.g. M001)
3. Enter a showing date (e.g. 2025-04-01)
4. Choose a showtime (1 = Morning, 2 = Afternoon, 3 = Evening)
5. Enter number of tickets
6. Select your seats (e.g. A1, A2)
7. Enter your email address
8. Booking confirmed! Bill saved to project folder
```

---

## 📦 Classes Overview

| Class | Responsibility |
|---|---|
| `MovieTicketReservation` | Main class, orchestrates the booking flow |
| `Movie` | Stores movie details and schedule |
| `Showtime` | Manages seat availability per showtime |
| `Booking` | Holds completed booking details |
| `MovieRepository` | Reads and stores movies from CSV |
| `SessionManager` | Tracks inactivity and auto-saves session |
| `BillGenerator` | Generates booking confirmation file |

---

## 📄 Output

On successful booking, a bill file is generated in the project folder:
```
Bill_BK-<timestamp>.txt
```

A session auto-save file is created if the user is inactive for 2 minutes:
```
session_autosave.txt
```

---

## 🔧 Requirements

- Java 17 or higher
- IntelliJ IDEA (recommended) or any Java IDE
- `Movie_Reservation_Dataset.csv` in the correct directory

---

## 📝 Notes

- This is a single-file Java submission as per lab requirements
- Bill is generated as a `.txt` file simulating PDF output
- AI tools were used with human review and modification throughout development
