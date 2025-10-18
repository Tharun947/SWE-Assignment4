package flight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;


public class FlightSearch {

    private String  departureDate;           
    private String  departureAirportCode;     
    private boolean emergencyRowSeating;      
    private String  returnDate;              
    private String  destinationAirportCode;   
    private String  seatingClass;            
    private int     adultPassengerCount;      
    private int     childPassengerCount;      
    private int     infantPassengerCount;     

    private static final Set<String> ALLOWED_AIRPORTS = new LinkedHashSet<>(
            Arrays.asList("syd","mel","lax","cdg","del","pvg","doh")
    );
    private static final Set<String> ALLOWED_CLASSES = new LinkedHashSet<>(
            Arrays.asList("economy","premium economy","business","first")
    );
    private static final DateTimeFormatter STRICT_DDMMYYYY =
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);

    public boolean runFlightSearch(
            String departureDate,
            String departureAirportCode,
            boolean emergencyRowSeating,
            String returnDate,
            String destinationAirportCode,
            String seatingClass,
            int adultPassengerCount,
            int childPassengerCount,
            int infantPassengerCount) {

        // cheap sanity checks
        if (anyNull(departureDate, departureAirportCode, returnDate, destinationAirportCode, seatingClass)) {
            return false;
        }

        // passenger rules 
        if (!isValidPassengers(adultPassengerCount, childPassengerCount, infantPassengerCount)) {
            return false;
        }

        //  seat class + emergency row rules 
        if (!isValidSeatingRules(seatingClass, emergencyRowSeating, childPassengerCount, infantPassengerCount)) {
            return false;
        }

        // airport codes + not equal 
        if (!isValidAirports(departureAirportCode, destinationAirportCode)) {
            return false;
        }

        //  date parsing + ordering 
        LocalDate dep = parseStrictDate(departureDate);
        LocalDate ret = parseStrictDate(returnDate);
        if (dep == null || ret == null) {
            return false; 
        }
        if (dep.isBefore(LocalDate.now())) {
            return false; 
        }
        if (ret.isBefore(dep)) {
            return false; 
        }

        commit(
                departureDate, departureAirportCode, emergencyRowSeating,
                returnDate, destinationAirportCode, seatingClass,
                adultPassengerCount, childPassengerCount, infantPassengerCount
        );
        return true;
    }

    //VALIDATION HELPERS

    private boolean isValidPassengers(int adults, int children, int infants) {
        if (adults < 0 || children < 0 || infants < 0) return false;

        int total = adults + children + infants;
        if (total < 1 || total > 9) return false;

        if (children > adults * 2) return false;

        if (infants > adults) return false;

        return true;
    }

    private boolean isValidSeatingRules(String seatClass, boolean emergencyRow, int children, int infants) {
        if (!ALLOWED_CLASSES.contains(seatClass)) return false;

        if (emergencyRow && !"economy".equals(seatClass)) return false;

        if (children > 0) {
            if (emergencyRow) return false;
            if ("first".equals(seatClass)) return false;
        }

        if (infants > 0) {
            if (emergencyRow) return false;
            if ("business".equals(seatClass)) return false;
        }

        return true;
    }

    private boolean isValidAirports(String dep, String dest) {
        if (!ALLOWED_AIRPORTS.contains(dep) || !ALLOWED_AIRPORTS.contains(dest)) return false;
        return !Objects.equals(dep, dest);
    }

    private LocalDate parseStrictDate(String ddMMyyyy) {
        try {
            return LocalDate.parse(ddMMyyyy, STRICT_DDMMYYYY);
        } catch (Exception ex) {
            return null;
        }
    }

    private void commit(
            String departureDate,
            String departureAirportCode,
            boolean emergencyRowSeating,
            String returnDate,
            String destinationAirportCode,
            String seatingClass,
            int adultPassengerCount,
            int childPassengerCount,
            int infantPassengerCount) {

        this.departureDate = departureDate;
        this.departureAirportCode = departureAirportCode;
        this.emergencyRowSeating = emergencyRowSeating;
        this.returnDate = returnDate;
        this.destinationAirportCode = destinationAirportCode;
        this.seatingClass = seatingClass;
        this.adultPassengerCount = adultPassengerCount;
        this.childPassengerCount = childPassengerCount;
        this.infantPassengerCount = infantPassengerCount;
    }


    private static boolean anyNull(Object... args) {
        for (Object o : args) if (o == null) return true;
        return false;
    }

    // GETTERS

    public String getDepartureDate() { return departureDate; }
    public String getDepartureAirportCode() { return departureAirportCode; }
    public boolean isEmergencyRowSeating() { return emergencyRowSeating; }
    public String getReturnDate() { return returnDate; }
    public String getDestinationAirportCode() { return destinationAirportCode; }
    public String getSeatingClass() { return seatingClass; }
    public int getAdultPassengerCount() { return adultPassengerCount; }
    public int getChildPassengerCount() { return childPassengerCount; }
    public int getInfantPassengerCount() { return infantPassengerCount; }
}
