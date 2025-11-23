package com.mb.api.tests;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BookingsTest extends BaseTest{

    private List<Map<String,Object>> bookings;
    private static final String ENDPOINT = "/booking";
    private static final String ID = "bookingid";
    private ResponseSpecification response200JSON;

    @BeforeClass
    public void setUp() {
        bookings = getListFromEndpoint(ENDPOINT, response200JSON);

        response200JSON = new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();
    }
    @Test
    public void bookingsAreReturned() {
        assertThat("No bookings returned", bookings.size(), greaterThan(0));
        Reporter.log("Number of bookings returned: " + bookings.size(), true);
    }
    @Test
    public void bookingsSchemaMatchesJsonSchema() {
        getResponseFromEndpoint(ENDPOINT)
                .then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/BookingsSchema.json"));
    }
    @Test
    public void requiredKeysArePresent() {
        for (int i = 0; i < bookings.size(); i++) {
            Map<String, Object> booking = bookings.get(i);
            assertThat("Booking at index " + i + " missing key: " + ID, booking, hasKey(ID));
        }
    }
    @Test
    public void idsAreUnique() {
        List<Integer> ids = bookings.stream()
                .map(b -> ((Number) b.get(ID)).intValue())
             .toList();

        List<Integer> duplicates = findDuplicates(ids);
        assertThat("Duplicate IDs found: " + duplicates, duplicates.isEmpty(), is(true));
    }
    @Test
    public void logExtraFields() {
        for (int i = 0; i < bookings.size(); i++) {
            Map<String, Object> booking = bookings.get(i);
            if (booking.size() > 1) {
                Reporter.log("Booking at index " + i + " has extra fields: " + booking.keySet(), true);
            }
        }
    }
}
