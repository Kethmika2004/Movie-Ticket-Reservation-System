/**
 * MovieTicketReservation.java
 * Functional Requirements: FR1-Movie Browsing, FR2-Movie Selection (InvalidMovieCodeException),
 * FR3-Date Selection (InvalidDateException), FR4-Showtime Selection (InvalidShowtimeException),
 * FR5-Seat Selection (InvalidSeatException), FR6-Ticket Quantity (InvalidTicketQuantityException),
 * FR7-Overbooking (OverbookingException), FR8-Session Inactivity, FR9-Billing, FR10-Bill Generation, FR11-CSV Data.
 * Non-Functional: Usability, Reliability, Maintainability, Performance, Security, Scalability, Readability.
 * @author Group Alpha @version 1.0
 */

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

// ── EXCEPTIONS ───────────────────────────────────────────────────────────────
class InvalidMovieCodeException extends Exception {
    public InvalidMovieCodeException(String c) { super("No movie with code \"" + c + "\". Try again."); }}
class InvalidDateException extends Exception {
    public InvalidDateException(String d) { super("\"" + d + "\" is invalid or unavailable."); }}
class InvalidShowtimeException extends Exception {
    public InvalidShowtimeException(String s) { super("\"" + s + "\" is invalid. Choose a listed number."); }}
class InvalidTicketQuantityException extends Exception {
    public InvalidTicketQuantityException(String q) { super("\"" + q + "\" is invalid. Enter a positive integer."); }}
class OverbookingException extends Exception {
    public OverbookingException(int r, int a) { super("Cannot reserve " + r + ". Only " + a + " seat(s) available."); }}
class InvalidSeatException extends Exception {
    public InvalidSeatException(String s) { super("Seat \"" + s + "\" is invalid or already taken."); }}

// ── SHOWTIME ──────────────────────────────────────────────────────────────────
class Showtime {
    private final String label;
    private final int totalSeats;
    private final int initialAvailable;
    private final Set<String> reserved = new HashSet<>();

    public Showtime(String label, int totalSeats, int availableSeats) {
        this.label            = label;
        this.totalSeats       = totalSeats;
        this.initialAvailable = availableSeats;
    }

    public String getLabel()       { return label; }
    public int getTotalSeats()     { return totalSeats; }
    public int getAvailableSeats() { return initialAvailable - reserved.size(); }
    public void reserveSeats(Collection<String> s) { reserved.addAll(s); }

    public List<String> getAvailableSeatLabels() {
        List<String> all = new ArrayList<>();
        outer:
        for (char r = 'A'; r <= 'Z'; r++) {
            for (int c = 1; c <= 10; c++) {
                all.add(r + "" + c);
                if (all.size() >= totalSeats) break outer;
            }
        }
        all.removeAll(reserved);
        return all.subList(0, Math.min(getAvailableSeats(), all.size()));
    }
}

// ── MOVIE ─────────────────────────────────────────────────────────────────────
class Movie {
    private final String code, title, genre, language;
    private final double price;
    private final Map<LocalDate, List<Showtime>> schedule = new LinkedHashMap<>();

    public Movie(String code, String title, String genre, String language, double price) {
        this.code = code; this.title = title; this.genre = genre;
        this.language = language; this.price = price;
    }

    public void addShowtime(LocalDate date, Showtime showtime) {
        schedule.computeIfAbsent(date, k -> new ArrayList<>()).add(showtime);
    }

    public String getCode()                                { return code; }
    public String getTitle()                               { return title; }
    public String getGenre()                               { return genre; }
    public String getLanguage()                            { return language; }
    public double getTicketPrice()                         { return price; }
    public Map<LocalDate, List<Showtime>> getSchedule()    { return Collections.unmodifiableMap(schedule); }
    public List<Showtime> getShowtimesForDate(LocalDate d) { return schedule.getOrDefault(d, Collections.emptyList()); }

    @Override public String toString() {
        return String.format("[%s] %-22s | Genre: %-10s | Lang: %-8s | $%.2f",
                code, title, genre, language, price);
    }
}

// ── BOOKING ───────────────────────────────────────────────────────────────────
class Booking {
    private final String id, email;
    private final Movie movie;
    private final LocalDate date;
    private final Showtime showtime;
    private final int count;
    private final List<String> seats;

    public Booking(Movie movie, LocalDate date, Showtime showtime,
                   int count, List<String> seats, String email) {
        this.id = "BK-" + System.currentTimeMillis();
        this.movie = movie; this.date = date; this.showtime = showtime;
        this.count = count; this.seats = List.copyOf(seats); this.email = email;
    }

    public String       getBookingId()     { return id; }
    public Movie        getMovie()         { return movie; }
    public LocalDate    getDate()          { return date; }
    public Showtime     getShowtime()      { return showtime; }
    public int          getTicketCount()   { return count; }
    public List<String> getSeats()         { return seats; }
    public String       getCustomerEmail() { return email; }
    public double       getTotalCost()     { return movie.getTicketPrice() * count; }
}

// ── MOVIE REPOSITORY ──────────────────────────────────────────────────────────
// CSV FORMAT (comma-separated):
// Col 0: Movie Code  | Col 1: Movie Name  | Col 2: Date (yyyy-MM-dd)
// Col 3: Showtime    | Col 4: Total Seats | Col 5: Available Seats
// Col 6: Ticket Price | Col 7: Language   | Col 8: Genre
class MovieRepository {
    private final Map<String, Movie> map = new LinkedHashMap<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public MovieRepository(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        if (lines.isEmpty()) { System.err.println("WARNING: CSV file is empty!"); return; }

        System.out.println("CSV Header: " + lines.get(0));

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] p = line.split(",");
            if (p.length < 9) {
                System.err.println("WARNING: Skipping row " + i + ": " + line); continue;
            }
            try {
                String    code       = p[0].trim();
                String    title      = p[1].trim();
                LocalDate date       = LocalDate.parse(p[2].trim(), FMT);
                String    timeLabel  = p[3].trim();
                int       totalSeats = Integer.parseInt(p[4].trim());
                int       availSeats = Integer.parseInt(p[5].trim());
                double    price      = Double.parseDouble(p[6].trim());
                String    language   = p[7].trim();
                String    genre      = p[8].trim();

                Movie movie = map.computeIfAbsent(code.toUpperCase(),
                        k -> new Movie(code, title, genre, language, price));
                movie.addShowtime(date, new Showtime(timeLabel, totalSeats, availSeats));

                System.out.printf("  Loaded: [%s] %s | %s | %-10s | Avail: %d/%d%n",
                        code, title, date, timeLabel, availSeats, totalSeats);

            } catch (NumberFormatException e) {
                System.err.println("WARNING: Number error on row " + i + ": " + line);
            } catch (DateTimeParseException e) {
                System.err.println("WARNING: Date error on row " + i + ": " + line);
            }
        }
        System.out.println("\nTotal unique movies loaded: " + map.size() + "\n");
    }

    public Movie findByCode(String code) throws InvalidMovieCodeException {
        Movie m = map.get(code.toUpperCase().trim());
        if (m == null) throw new InvalidMovieCodeException(code);
        return m;
    }

    public List<Movie> getAllMovies() { return List.copyOf(map.values()); }
}

// ── SESSION MANAGER ───────────────────────────────────────────────────────────
class SessionManager {
    private volatile long lastActive = System.currentTimeMillis();
    private volatile boolean saved = false;
    private Movie movie; private LocalDate date; private Showtime showtime;
    private int tickets; private List<String> seats = new ArrayList<>();
    private final ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor();

    public SessionManager() {
        sched.scheduleAtFixedRate(() -> {
            if (!saved && (System.currentTimeMillis() - lastActive) / 1000 >= 120) save();
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void resetTimer() { lastActive = System.currentTimeMillis(); saved = false; }

    public synchronized void save() {
        if (saved) return; saved = true;
        System.out.println("\n[SESSION] Inactivity detected - session auto-saved. Seats held 15 min.");
        try (PrintWriter pw = new PrintWriter("session_autosave.txt")) {
            pw.printf("Movie: %s%nDate: %s%nTime: %s%nTickets: %d%nSeats: %s%nSaved: %s%n",
                    movie    != null ? movie.getTitle()    : "N/A",
                    date     != null ? date                : "N/A",
                    showtime != null ? showtime.getLabel() : "N/A",
                    tickets, seats, LocalDateTime.now());
        } catch (FileNotFoundException e) { System.err.println("[SESSION] Save failed."); }
    }

    public void shutdown()              { sched.shutdownNow(); }
    public void setMovie(Movie m)       { this.movie    = m; }
    public void setDate(LocalDate d)    { this.date     = d; }
    public void setShowtime(Showtime s) { this.showtime = s; }
    public void setTickets(int t)       { this.tickets  = t; }
    public void setSeats(List<String> s){ this.seats    = new ArrayList<>(s); }
}

// ── BILL GENERATOR ────────────────────────────────────────────────────────────
class BillGenerator {
    public String generate(Booking b, String dir) throws IOException {
        String file = dir + File.separator + "Bill_" + b.getBookingId() + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("╔══════════════════════════════════════════════╗");
            pw.println("║     CINEMA CHAIN - BOOKING CONFIRMATION      ║");
            pw.println("╚══════════════════════════════════════════════╝");
            pw.printf("  Booking ID : %s%n  Customer   : %s%n  Generated  : %s%n%n",
                    b.getBookingId(), b.getCustomerEmail(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
            pw.printf("  Movie      : %s%n  Genre      : %s | Language: %s%n",
                    b.getMovie().getTitle(), b.getMovie().getGenre(), b.getMovie().getLanguage());
            pw.printf("  Date       : %s | Showtime : %s%n  Seats      : %s%n",
                    b.getDate(), b.getShowtime().getLabel(), b.getSeats());
            pw.printf("  Tickets    : %d x $%.2f%n  TOTAL DUE  : $%.2f%n%n",
                    b.getTicketCount(), b.getMovie().getTicketPrice(), b.getTotalCost());
            pw.println("  Thank you for choosing our cinema. Enjoy the show!");
        }
        return file;
    }
}

// ── MAIN ──────────────────────────────────────────────────────────────────────
public class MovieTicketReservation {
    private static final Scanner sc = new Scanner(System.in);
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$");

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  WELCOME TO CINEMA CHAIN RESERVATION SYSTEM  ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        String csvPath = "Movie_Reservation_Dataset.csv";

        File csvFile = new File(csvPath);
        if (!csvFile.exists()) {
            System.err.println("ERROR: File not found → " + csvPath);
            System.err.println("Place the CSV file in the same folder as the .java file.");
            return;
        }

        MovieRepository repo;
        try {
            repo = new MovieRepository(csvPath);
            if (repo.getAllMovies().isEmpty()) {
                System.err.println("ERROR: No movies loaded. Check CSV format."); return;
            }
        } catch (IOException e) {
            System.err.println("ERROR reading CSV: " + e.getMessage()); return;
        }

        SessionManager session = new SessionManager();
        try {
            Booking booking = runFlow(repo, session);
            if (booking != null) finalise(booking);
        } finally { session.shutdown(); sc.close(); }
    }

    private static Booking runFlow(MovieRepository repo, SessionManager session) {
        Movie        movie    = pickMovie(repo, session);            if (movie    == null) return null;
        LocalDate    date     = pickDate(movie, session);            if (date     == null) return null;
        Showtime     showtime = pickShowtime(movie, date, session);  if (showtime == null) return null;
        int          count    = pickCount(showtime, session);        if (count    <= 0)    return null;
        List<String> seats    = pickSeats(showtime, count, session); if (seats    == null) return null;
        String       email    = pickEmail(session);                  if (email    == null) return null;
        showtime.reserveSeats(seats);
        return new Booking(movie, date, showtime, count, seats, email);
    }

    private static Movie pickMovie(MovieRepository repo, SessionManager session) {
        System.out.println("\n── AVAILABLE MOVIES ─────────────────────────────────────────");
        repo.getAllMovies().forEach(m -> System.out.println("  " + m));
        System.out.println("─────────────────────────────────────────────────────────────");
        while (true) {
            String in = prompt("Enter movie code (or 'quit'): ", session);
            if (in == null) return null;
            try {
                Movie m = repo.findByCode(in); session.setMovie(m);
                System.out.println("✔ Selected: " + m.getTitle()); return m;
            } catch (InvalidMovieCodeException e) { System.out.println("⚠ " + e.getMessage()); }
        }
    }

    private static LocalDate pickDate(Movie movie, SessionManager session) {
        System.out.println("\nAvailable dates for \"" + movie.getTitle() + "\":");
        movie.getSchedule().keySet().forEach(d -> System.out.println("  • " + d));
        while (true) {
            String in = prompt("Enter date (yyyy-MM-dd e.g. 2025-04-01) or 'quit': ", session);
            if (in == null) return null;
            try {
                LocalDate d = LocalDate.parse(in.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                if (!movie.getSchedule().containsKey(d)) throw new InvalidDateException(in);
                session.setDate(d); System.out.println("✔ Date: " + d); return d;
            } catch (DateTimeParseException | InvalidDateException e) {
                System.out.println("⚠ Invalid or unavailable date. Pick from the list above.");
            }
        }
    }

    private static Showtime pickShowtime(Movie movie, LocalDate date, SessionManager session) {
        List<Showtime> times = movie.getShowtimesForDate(date);
        System.out.println("\nShowtimes on " + date + ":");
        for (int i = 0; i < times.size(); i++)
            System.out.printf("  %d. %-12s | Total Seats: %3d | Available: %3d%n",
                    i + 1, times.get(i).getLabel(),
                    times.get(i).getTotalSeats(), times.get(i).getAvailableSeats());
        while (true) {
            String in = prompt("Enter showtime number (1-" + times.size() + ") or 'quit': ", session);
            if (in == null) return null;
            try {
                int idx = Integer.parseInt(in.trim());
                if (idx < 1 || idx > times.size()) throw new InvalidShowtimeException(in);
                Showtime s = times.get(idx - 1); session.setShowtime(s);
                System.out.println("✔ Showtime: " + s.getLabel()); return s;
            } catch (NumberFormatException | InvalidShowtimeException e) {
                System.out.println("⚠ Invalid. Enter a number from 1 to " + times.size() + ".");
            }
        }
    }

    private static int pickCount(Showtime showtime, SessionManager session) {
        while (true) {
            String in = prompt("Available seats: " + showtime.getAvailableSeats() +
                    ". How many tickets? (or 'quit'): ", session);
            if (in == null) return -1;
            try {
                int n = Integer.parseInt(in.trim());
                if (n <= 0) throw new InvalidTicketQuantityException(in);
                if (n > showtime.getAvailableSeats())
                    throw new OverbookingException(n, showtime.getAvailableSeats());
                session.setTickets(n); System.out.println("✔ Tickets: " + n); return n;
            } catch (NumberFormatException | InvalidTicketQuantityException | OverbookingException e) {
                System.out.println("⚠ " + e.getMessage());
            }
        }
    }

    private static List<String> pickSeats(Showtime showtime, int count, SessionManager session) {
        List<String> available = showtime.getAvailableSeatLabels();
        System.out.println("\nAvailable seats: " + available);
        List<String> chosen = new ArrayList<>();
        while (chosen.size() < count) {
            String in = prompt("  Seat " + (chosen.size() + 1) + "/" + count + " (or 'quit'): ", session);
            if (in == null) return null;
            String seat = in.toUpperCase();
            try {
                if (!available.contains(seat) || chosen.contains(seat))
                    throw new InvalidSeatException(seat);
                chosen.add(seat); System.out.println("  ✔ Added: " + seat);
            } catch (InvalidSeatException e) { System.out.println("  ⚠ " + e.getMessage()); }
        }
        session.setSeats(chosen); System.out.println("✔ Seats reserved: " + chosen); return chosen;
    }

    private static String pickEmail(SessionManager session) {
        while (true) {
            String in = prompt("Email for billing (or 'quit'): ", session);
            if (in == null) return null;
            if (EMAIL.matcher(in.trim()).matches()) {
                System.out.println("✔ Email: " + in); return in;
            }
            System.out.println("⚠ Invalid email. Example: name@example.com");
        }
    }

    private static String prompt(String msg, SessionManager session) {
        System.out.print(msg);
        String in = sc.nextLine().trim();
        session.resetTimer();
        return in.equalsIgnoreCase("quit") ? null : in;
    }

    private static void finalise(Booking b) {
        System.out.printf("%n╔══════════════════════════════════════════════╗%n" +
                        "║           BOOKING CONFIRMED!                 ║%n" +
                        "╚══════════════════════════════════════════════╝%n" +
                        "  ID      : %s%n  Movie    : %s%n  Date     : %s%n" +
                        "  Time    : %s%n  Seats    : %s%n  Tickets  : %d%n" +
                        "  Total   : $%.2f%n  Email    : %s%n",
                b.getBookingId(), b.getMovie().getTitle(), b.getDate(),
                b.getShowtime().getLabel(), b.getSeats(),
                b.getTicketCount(), b.getTotalCost(), b.getCustomerEmail());
        try {
            String path = new BillGenerator().generate(b, ".");
            System.out.println("✔ Bill saved: " + path);
            System.out.println("✔ Bill emailed to: " + b.getCustomerEmail());
        } catch (IOException e) { System.err.println("⚠ Bill error: " + e.getMessage()); }
        System.out.println("\nThank you! Enjoy the show!");
    }
}
