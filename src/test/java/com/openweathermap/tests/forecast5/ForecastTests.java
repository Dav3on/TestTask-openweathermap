package com.openweathermap.tests.forecast5;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static com.openweathermap.Common.*;
import static com.openweathermap.Common.getCityIdFromMap;
import static com.openweathermap.Common.getCountryCodeFromMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.hamcrest.core.IsNot.not;

//http://openweathermap.org/forecast5
public class ForecastTests {
    public final String endpointURL = BASE_API_URL+"/forecast?appid="+API_KEY+"&";

    private ArrayList<RequestSpecification> requestParams = new ArrayList<RequestSpecification>();

    private HashMap<String, Object> city;
    private String cityName;
    private String countyCode;
    private Integer cityId;
    private Float lat;
    private Float lon;

    //You can change to @BeforeClass if needed.
    @Before
    public void setUp()
    {

        city = getRandomCity();
        cityName = getCityNameFromMap(city);
        countyCode = getCountryCodeFromMap(city);
        cityId = getCityIdFromMap(city);
        lat = getLatFromMap(city, 5);
        lon = getLonFromMap(city, 5);

        requestParams.add(new RequestSpecBuilder().addParam("id", cityId).setPort(DEFAULT_PORT).build());
        requestParams.add(new RequestSpecBuilder().addParam("q", cityName).setPort(DEFAULT_PORT).build());
        requestParams.add(new RequestSpecBuilder().addParam("lat", lat).setPort(DEFAULT_PORT).addParam("lon", lon).build());
    }

    @After
    public void tearDown() {
        cityName = null;
        countyCode = null;
        cityId = null;
        lat = null;
        lon = null;
        requestParams.clear();
    }

    /* _______________________________________________________________
                    Metadata tests
    __________________________________________________________________ */

    @Test
    public void status200WhenIdCorrectByCityId(){
        given().log().ifValidationFails().
                param("id", cityId).
        when().
                get(endpointURL).
        then().
                log().ifValidationFails().
                assertThat().statusCode(200);
    }

    @Test
    public void status200WhenNameCorrectByCityName(){
        given().log().ifValidationFails().
                param("q", cityName).
        when().
                get(endpointURL).
        then().
                log().ifValidationFails().
                assertThat().statusCode(200);
    }

    @Test
    public void status200WhenNameCorrectByLatLon(){
        given().log().ifValidationFails().
                param("lat", lat).
                param("lon", lon).
        when().
                get(endpointURL).
        then().
                log().ifValidationFails().
                assertThat().statusCode(200);
    }

    @Test
    public void status401WhenUnauthorized(){
        for (RequestSpecification paramFromList: requestParams) {
            given().log().ifValidationFails().
                    spec(paramFromList).
            when().
                    get(BASE_API_URL+"/forecast").
            then().
                    log().ifValidationFails().
                    assertThat().statusCode(401);
        }
    }

    //Test fails all the time when verifying HTML content type. Documentation sad that HTML supported.
    @Test
    public void checkResponseContentTypesByCityId(){
        //Verify all possible content types even with default (watch CONTENT_TYPES)
        for (Map.Entry<String, ContentType> entry: CONTENT_TYPES.entrySet()){
            given().log().ifValidationFails().
                    param("id", cityId).
                    param("mode", entry.getKey()).
            when().
                    get(endpointURL).
            then().
                    log().ifValidationFails().
                    assertThat().statusCode(200).and().
                    contentType(entry.getValue());
        }
    }

    @Test
    public void status404WhenIdIncorrectByCityId(){
        given().log().ifValidationFails().
                param("id", randomString()).
        when().
                get(endpointURL).
        then().
                log().ifValidationFails().
                assertThat().statusCode(404);
    }

    @Test
    public void status404WhenNameIncorrectByCityName(){
        given().log().ifValidationFails().
                param("q", randomString()).
        when().
                get(endpointURL).
        then().
                log().ifValidationFails().
                assertThat().statusCode(404);
    }

    @Test
    public void status404WhenLatIncorrectByLatLon(){
        given().log().ifValidationFails().
                param("lat", randomString()).
                param("lon", lon).
        when().
                get(endpointURL).
        then().
                log().ifValidationFails().
                assertThat().statusCode(404);
    }

    /* _______________________________________________________________
                        Error messages tests
    __________________________________________________________________ */

    //Im not sure about message. While i writing this test they switch it twice from json to html and back
    @Test
    public void checkBodyMessageWhenUnauthorized(){
        for (RequestSpecification paramFromList: requestParams) {
            given().log().ifValidationFails().
                    spec(paramFromList).
            when().
                    get(BASE_API_URL+"/forecast").
            then().
                    log().ifValidationFails().
                    assertThat().body("html.body.center.h1", equalTo("401 Authorization Required"));
        }
    }

    @Test
    public void checkBodyMessageWhenIdIncorrectByCityId(){
        given().log().ifValidationFails().
                param("id", randomString()).
                param("mode", "json").
        when().
                get(endpointURL).
        then().
                log().ifValidationFails().
                assertThat().body("cod", equalTo("404")).and().
                body("message", equalTo("Error: Not found city"));
    }

    @Test
    public void checkBodyMessageWhenNameIncorrectByCityName(){
        given().log().ifValidationFails().
                param("q", randomString()).
                param("mode", "json").
        when().
                get(endpointURL).
        then().
                log().ifValidationFails().
                assertThat().body("cod", equalTo("404")).and().
                body("message", equalTo("Error: Not found city"));
    }

    @Test
    public void checkBodyMessageWhenLatIncorrectByLatLon(){
        given().log().ifValidationFails().
                param("lat", randomString()).
                param("lon", lon).
                param("mode", "json").
        when().
                get(endpointURL).
        then().
                log().ifValidationFails().
                assertThat().body("cod", equalTo("404")).and().
                body("message", equalTo("Error: Not found city"));
    }

    /* _______________________________________________________________
                        Payload tests
    __________________________________________________________________ */

    @Test
    public void checkCityParamsInResponseJSON(){
        //Verify that response identical by all possibles request params
        for (RequestSpecification paramFromList: requestParams) {
            given().
                    log().ifValidationFails().
                    spec(paramFromList).
                    param("mode", "json").
            when().
                    get(endpointURL).
            then().
                    log().ifValidationFails().
                    assertThat().body("city.id", equalTo(cityId)).and().
                    body("city.name", equalTo(cityName)).and().
                    body("city.country", equalTo(countyCode)).and().
                    body("city.coord.lat", equalTo(lat)).and().
                    body("city.coord.lon", equalTo(lon));
        }
    }

    @Test
    public void checkCityParamsInResponseXML(){
        Double RawLat = (Double) city.get("lat");       //I need to get raw double values for this test
        Double RawLon = (Double) city.get("lon");

        //Verify that response identical by all possibles request params
        for (RequestSpecification paramFromList: requestParams) {
            given().
                    log().ifValidationFails().
                    spec(paramFromList).
                    param("mode", "xml").
            when().
                    get(endpointURL).
            then().
                    log().ifValidationFails().
                    assertThat().body("weatherdata.location.name", equalTo(cityName)).and().
                    body("weatherdata.location.country", equalTo(countyCode)).and().
                    body("weatherdata.location.location.@latitude", equalTo(RawLat.toString())).and().
                    body("weatherdata.location.location.@longitude", equalTo(RawLon.toString()));
        }
    }

    @Test
    public void countElementsInListOfWeatherJSON(){
        for (RequestSpecification paramFromList: requestParams) {
            given().
                    log().ifValidationFails().
                    spec(paramFromList).
                    param("mode", "json").
            when().
                    get(endpointURL).
            then().
                    log().ifValidationFails().
                    assertThat().body("list.size()", is(both(greaterThan(31)).and(lessThan(41)))); /*5 days with every 3 hours range
                so it's probably "24 / 3 * 5 = 40" updates.
                But i'm not sure that my calculations are correct (sometimes its return 35) */
        }
    }

    @Test
    public void countElementsInListOfWeatherXML(){
        for (RequestSpecification paramFromList: requestParams) {
            given().
                    log().ifValidationFails().
                    spec(paramFromList).
                    param("mode", "xml").
            when().
                    get(endpointURL).
            then().
                    log().ifValidationFails().
                    assertThat().body("weatherdata.forecast.time.size()", is(both(greaterThan(31)).and(lessThan(41))));
        }
    }

    @Test
    public void checkFirstWeatherNotEmptyJSON(){
        for (RequestSpecification paramFromList: requestParams) {
            given().
                    log().ifValidationFails().
                    spec(paramFromList).
                    param("mode", "json").
            when().
                    get(endpointURL).
            then().
                    log().ifValidationFails().
                    assertThat().body("list.weather.main[0]", not(empty()));
        }
    }

    @Test
    public void checkFirstWeatherNotEmptyXML(){
        for (RequestSpecification paramFromList: requestParams) {
            given().
                    log().ifValidationFails().
                    spec(paramFromList).
                    param("mode", "xml").
            when().
                    get(endpointURL).
            then().
                    log().ifValidationFails().
                    assertThat().body("weatherdata.forecast.time.clouds[0].@value", not(empty()));
        }
    }
}
