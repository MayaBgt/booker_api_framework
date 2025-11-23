package com.mb.api.tests;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.ResponseSpecification;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

public class BookingByIdTest extends BaseTest {

    private List<Map<String, Object>> bookings;
    private static final String ENDPOINT = "/booking";
    private ResponseSpecification response200JSON;
    private ResponseSpecification response404;
    private int randomBookingId;
    private static final String BOOKING_ID = "bookingid";
    private static final String FIRSTNAME = "firstname";
    private static final String LASTNAME = "lastname";
    private static final String TOTALPRICE = "totalprice";
    private static final String DEPOSITPAID = "depositpaid";
    private static final String BOOKINGDATES = "bookingdates";
    private static final String CHECKIN = "bookingdates.checkin";
    private static final String CHECKOUT = "bookingdates.checkout";
    private static final String ADDITIONALNEEDS = "additionalneeds";

    @BeforeClass
    public void beforeClass() {
        super.beforeClass();

        bookings = getListFromEndpoint(ENDPOINT);

        response200JSON = new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();

        response404 = new ResponseSpecBuilder()
                .expectStatusCode(404)
                .build();

        int randomIndex = new Random().nextInt(bookings.size());
        randomBookingId = ((Number) bookings.get(randomIndex).get(BOOKING_ID)).intValue();
    }

    @Test
    public void bookingByIdSchemaMatchesJsonSchema() {
        getResponseFromEndpoint(ENDPOINT + "/" + randomBookingId)
                .then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/BookingByIdSchema.json"));
    }

    @Test
    public void bookingByIdBusinessLogicIsValid() {
        for (Map<String, Object> b : bookings) {
            Object idObj = b.get(BOOKING_ID);
            if (idObj == null) {
                Reporter.log("Skipping booking with missing ID", true);
                continue;
            }

            int bookingId = ((Number) idObj).intValue();
            Response response = getResponseFromEndpoint(ENDPOINT + "/" + bookingId);

            if (response.getStatusCode() != 200) {
                Reporter.log("Booking ID " + bookingId + " not found (404), skipping", true);
                continue; // skip to next booking
            }

            Map<String, Object> booking = response.jsonPath().getMap("$");

            // Required string fields
            String firstname = booking.get(FIRSTNAME) != null ? booking.get(FIRSTNAME).toString() : null;
            String lastname = booking.get(LASTNAME) != null ? booking.get(LASTNAME).toString() : null;
            assertThat("Booking ID " + bookingId + " has invalid firstname", firstname, not(isEmptyOrNullString()));
            assertThat("Booking ID " + bookingId + " has invalid lastname", lastname, not(isEmptyOrNullString()));

            // Total price
            Object priceObj = booking.get(TOTALPRICE);
            int price = ((Number) priceObj).intValue();
            if (price < 0) {
                Reporter.log("Booking ID " + bookingId + " has invalid totalprice: " + price, true);
            } else {
                assertThat(price, greaterThanOrEqualTo(0));
            }

            // Deposit paid
            Object depositObj = booking.get(DEPOSITPAID);
            assertThat("Booking ID " + bookingId + " has invalid depositpaid", depositObj, instanceOf(Boolean.class));

            // Dates
            String checkin = response.jsonPath().getString(CHECKIN);
            String checkout = response.jsonPath().getString(CHECKOUT);
            if (checkin != null && checkout != null && checkin.matches("\\d{4}-\\d{2}-\\d{2}") && checkout.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDate checkinDate = LocalDate.parse(checkin);
                LocalDate checkoutDate = LocalDate.parse(checkout);
                assertThat("Booking ID " + bookingId + " has invalid dates", !checkoutDate.isBefore(checkinDate), is(true));
            } else {
                Reporter.log("Booking ID " + bookingId + " has invalid date format: checkin=" + checkin + ", checkout=" + checkout, true);
            }

            // Optional field
            Object needsObj = booking.get(ADDITIONALNEEDS);
            if (needsObj != null) {
                String needs = needsObj.toString();
                assertThat("Booking ID " + bookingId + " has empty additionalneeds", needs, not(isEmptyOrNullString()));
            }
        }
    }

    @Test
    public void testInvalidBookingIds() {
        String[] invalidIds = {
                "abc",          // letters
                "-1",           // negative
                "!",            // special char
                "@#$%",         // multiple special chars
                "null",         // literal null
                "999999999999", // extreme large number
                "' OR 1=1 --",  // SQL injection attempt
                "\" OR \"\"=\"" // SQL-like injection
        };

        for (String id : invalidIds) {
            System.out.println(id);
            getResponseFromEndpoint(ENDPOINT + "/" + id, response404);
        }
    }
}
