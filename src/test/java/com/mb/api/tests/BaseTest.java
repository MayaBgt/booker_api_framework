package com.mb.api.tests;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.testng.annotations.BeforeClass;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class BaseTest {
    protected RequestSpecification requestSpecification;
    protected ResponseSpecification responseSpecification;

    @BeforeClass
    public void beforeClass() {

        requestSpecification = new RequestSpecBuilder()
                .setBaseUri("https://restful-booker.herokuapp.com")
                .setContentType(ContentType.JSON)
                .log(LogDetail.URI)
                .log(LogDetail.METHOD)
                .log(LogDetail.BODY)
                .setConfig(RestAssured.config()
                        .logConfig(LogConfig.logConfig()
                                .enableLoggingOfRequestAndResponseIfValidationFails()))
                .build();

        responseSpecification = RestAssured.expect()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    protected <T> List<T> findDuplicates(List<T> list) {
        return list.stream()
                .filter(e -> Collections.frequency(list, e) > 1)
                .distinct()
                .toList();
    }
    protected List<Map<String, Object>> getListFromEndpoint(String endpoint) {
        return getListFromEndpoint(endpoint, responseSpecification);
    }

    protected List<Map<String, Object>> getListFromEndpoint(String endpoint,ResponseSpecification customSpec) {
        return given()
                .spec(requestSpecification)
        .when()
                .get(endpoint)
        .then()
                .spec(customSpec)
                .extract()
                .jsonPath()
                .getList("$");
    }

    protected Response getResponseFromEndpoint(String endpoint) {
        return getResponseFromEndpoint(endpoint, responseSpecification);
    }

    protected Response getResponseFromEndpoint(String endpoint, ResponseSpecification customSpec) {
        return given()
                .spec(requestSpecification)
        .when()
                .get(endpoint)
        .then()
                .spec(customSpec)
                .extract()
                .response();
    }
}