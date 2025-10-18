package flight;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class FlightSearchRunTests {

    private FlightSearch fs;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private String plusDays(int d) { return LocalDate.now().plusDays(d).format(FMT); }

    @BeforeEach
    void fresh() {
        fs = new FlightSearch();
    }

    //  helpers
    private void assertState(
            String depDate, String dep, boolean emer, String retDate, String dest, String clazz,
            int a, int c, int i) {
        assertEquals(depDate, fs.getDepartureDate());
        assertEquals(dep, fs.getDepartureAirportCode());
        assertEquals(emer, fs.isEmergencyRowSeating());
        assertEquals(retDate, fs.getReturnDate());
        assertEquals(dest, fs.getDestinationAirportCode());
        assertEquals(clazz, fs.getSeatingClass());
        assertEquals(a, fs.getAdultPassengerCount());
        assertEquals(c, fs.getChildPassengerCount());
        assertEquals(i, fs.getInfantPassengerCount());
    }

    // HAPPY PATHS

    @Test @DisplayName("Valid economy request (emergency row allowed) initialises state")
    void valid_all_ok() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", true,
                plusDays(3), "pvg", "economy",
                1, 0, 0
        );
        assertTrue(ok);
        assertState(plusDays(1), "mel", true, plusDays(3), "pvg", "economy", 1, 0, 0);
    }

    @Test @DisplayName("Valid same-day return is allowed")
    void valid_same_day_return() {
        boolean ok = fs.runFlightSearch(
                plusDays(2), "mel", false,
                plusDays(2), "syd", "economy",
                2, 2, 0
        );
        assertTrue(ok);
        assertState(plusDays(2), "mel", false, plusDays(2), "syd", "economy", 2, 2, 0);
    }

    // PASSENGER RULES 

    @Test @DisplayName("Total passengers 0 -> invalid (state must remain null)")
    void c1_total_zero_invalid() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", false,
                plusDays(2), "syd", "economy",
                0, 0, 0
        );
        assertFalse(ok);
        assertNull(fs.getDepartureDate());
        assertNull(fs.getDestinationAirportCode());
    }

    @Test @DisplayName("Total passengers 9 -> valid upper bound")
    void c1_total_nine_valid() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", false,
                plusDays(3), "syd", "economy",
                3, 6, 0
        );
        assertTrue(ok);
        assertState(plusDays(1), "mel", false, plusDays(3), "syd", "economy", 3, 6, 0);
    }

    @Test @DisplayName("C4: too many children for the adult count -> invalid")
    void c4_children_exceed_ratio() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", false,
                plusDays(2), "syd", "economy",
                2, 5, 0
        );
        assertFalse(ok);
        assertNull(fs.getSeatingClass());
    }

    @Test @DisplayName("C5: infants > adults -> invalid")
    void c5_infants_exceed_adults() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", false,
                plusDays(2), "syd", "economy",
                1, 0, 2
        );
        assertFalse(ok);
        assertNull(fs.getDepartureAirportCode());
    }

    @Test @DisplayName("Bonus: negative child count -> invalid")
    void bonus_negative_child_count() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", false,
                plusDays(2), "syd", "economy",
                1, -1, 0
        );
        assertFalse(ok);
        assertNull(fs.getReturnDate());
    }

    @Test @DisplayName("Bonus: negative infant count -> invalid")
    void bonus_negative_infant_count() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", false,
                plusDays(2), "syd", "economy",
                1, 0, -1
        );
        assertFalse(ok);
        assertNull(fs.getDestinationAirportCode());
    }

    // SEAT CLASS / EMERGENCY ROW 

    @Test @DisplayName("C2: children cannot be in first class")
    void c2_children_first_class_invalid() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", false,
                plusDays(2), "syd", "first",
                1, 1, 0
        );
        assertFalse(ok);
        assertNull(fs.getSeatingClass());
    }

    @Test @DisplayName("C2: children + emergency row -> invalid even in economy")
    void c2_children_emergency_row_invalid() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", true,
                plusDays(2), "syd", "economy",
                1, 1, 0
        );
        assertFalse(ok);
        assertNull(fs.getDepartureDate());
    }

    @Test @DisplayName("C3: infants cannot be in business class")
    void c3_infant_business_invalid() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", false,
                plusDays(2), "syd", "business",
                1, 0, 1
        );
        assertFalse(ok);
        assertNull(fs.getReturnDate());
    }

    @Test @DisplayName("C3: infants cannot be in emergency row")
    void c3_infant_emergency_row_invalid() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", true,
                plusDays(2), "syd", "economy",
                1, 0, 1
        );
        assertFalse(ok);
        assertNull(fs.getDestinationAirportCode());
    }

    @Test @DisplayName("C9: invalid seating class string -> invalid")
    void c9_invalid_class() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", false,
                plusDays(2), "syd", "economyy",
                1, 0, 0
        );
        assertFalse(ok);
        assertNull(fs.getSeatingClass());
    }

    @Test @DisplayName("C10: emergency row only allowed for economy")
    void c10_emergency_only_economy() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", true,
                plusDays(2), "syd", "premium economy",
                1, 0, 0
        );
        assertFalse(ok);
        assertNull(fs.getDepartureDate());
    }

    // DATES 

    @Test @DisplayName("C6: departure in the past -> invalid")
    void c6_departure_in_past() {
        String depYesterday = LocalDate.now().minusDays(1).format(FMT);
        boolean ok = fs.runFlightSearch(
                depYesterday, "mel", false,
                plusDays(2), "syd", "economy",
                1, 0, 0
        );
        assertFalse(ok);
        assertNull(fs.getDepartureDate());
    }

    @Test @DisplayName("C7: invalid calendar combo -> invalid (29/02/2026)")
    void c7_invalid_calendar_combo() {
        boolean ok = fs.runFlightSearch(
                "29/02/2026", "mel", false,
                "01/03/2026", "syd", "economy",
                1, 0, 0
        );
        assertFalse(ok);
        assertNull(fs.getReturnDate());
    }

    @Test @DisplayName("C7: wrong format -> invalid (missing leading zero)")
    void c7_wrong_format() {
        boolean ok = fs.runFlightSearch(
                "1/01/2026", "mel", false,
                "02/01/2026", "syd", "economy",
                1, 0, 0
        );
        assertFalse(ok);
        assertNull(fs.getDepartureAirportCode());
    }

    @Test @DisplayName("C8: return before departure -> invalid")
    void c8_return_before_departure() {
        String dep = plusDays(5);
        String ret = plusDays(4);
        boolean ok = fs.runFlightSearch(
                dep, "mel", false,
                ret, "syd", "economy",
                1, 0, 0
        );
        assertFalse(ok);
        assertNull(fs.getSeatingClass());
    }

    //AIRPORTS

    @Test @DisplayName("C11: unknown departure airport -> invalid")
    void c11_unknown_airport() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "xyz", false,
                plusDays(2), "syd", "economy",
                1, 0, 0
        );
        assertFalse(ok);
        assertNull(fs.getDepartureAirportCode());
    }

    @Test @DisplayName("C11: same departure and destination -> invalid")
    void c11_same_airport() {
        boolean ok = fs.runFlightSearch(
                plusDays(1), "mel", false,
                plusDays(2), "mel", "economy",
                1, 0, 0
        );
        assertFalse(ok);
        assertNull(fs.getDestinationAirportCode());
    }

    // STATE MUTATION 

    @Test @DisplayName("State remains unchanged after an invalid run (pre/post asserts)")
    void mutation_guard_explicit() {
        boolean seed = fs.runFlightSearch(
                plusDays(2), "mel", false,
                plusDays(4), "syd", "economy",
                2, 2, 0
        );
        assertTrue(seed);
        assertState(plusDays(2), "mel", false, plusDays(4), "syd", "economy", 2, 2, 0);

        boolean bad = fs.runFlightSearch(
                plusDays(10), "mel", false,
                plusDays(9), "syd", "economy",
                1, 0, 0
        );
        assertFalse(bad);

        assertState(plusDays(2), "mel", false, plusDays(4), "syd", "economy", 2, 2, 0);
    }
}
