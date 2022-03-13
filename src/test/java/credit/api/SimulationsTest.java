package credit.api;
import static io.restassured.RestAssured.given;
import static  org.hamcrest.CoreMatchers.*;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static io.restassured.RestAssured.when;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)



class BookingsTests {
    final String bookingDate = "2022-03-11T17:23:14";
    final String email = "kunio@gmail.com";
    final String name = "Paulina";
    String userId;
    Response responseCreateNewUser;
    @BeforeAll
    public void postCreateNewUser(){
         responseCreateNewUser =
        given()
                .body("{\n" +
                        "  \"email\": \""+email+"\",\n" +
                        "  \"name\": \""+name+"\"\n" +
                        "}")
                .contentType(ContentType.JSON)
                .when()
                .post("http://127.0.0.1:8900/user");
        JsonPath json = responseCreateNewUser.jsonPath();
        userId = json.get("id");
    }

    @Test
    //Test checking if email address and name are valid
    public void TestPostCreateNewUser() {
        responseCreateNewUser.then()
                .statusCode(201)
                .body("email", Matchers.equalTo(email))
                .body("name", Matchers.equalTo(name))
                .body("id", Matchers.matchesPattern("kunio@gmail\\.com-\\d\\.\\d+"))
                .body("bookings", Matchers.empty());
    }

    @Test
    //Test checking if wrong ID always returns status 500
    public void createNewBookingForWrongId() {
        given()
                .body("{\n" +
                        "  \"date\": \""+bookingDate+"\",\n" +
                        "  \"destination\": \"CHI\",\n" +
                        "  \"id\": \"123\",\n" +
                        "  \"origin\": \"PAR\"\n" +
                        "}")
                .contentType(ContentType.JSON)
                .when()
                .post("http://127.0.0.1:8900/booking")
                .then()
                .statusCode(500)
                .body("status", Matchers.equalTo(500))
                .body("message", Matchers.equalTo("could not execute statement; SQL [n/a]; constraint [\"FKTDE8EDPC976R2GJVN4RY5V9M7: PUBLIC.BOOKING FOREIGN KEY(ID_USER) REFERENCES PUBLIC.USER(ID) ('123')\"; SQL statement:\ninsert into booking (date, destination, id_user, origin, id_booking) values (?, ?, ?, ?, ?) [23506-196]]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement"))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/booking"))
                .body("timestamp", Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(\\+\\d{4})?"));


    }

    @Test
    //post checking if IATA code with not only Upercase letters will work
    public void createNewBookingForWrongIataFormat() {
        given()
                .body("{\n" +
                        "  \"date\": \""+bookingDate+"\",\n" +
                        "  \"destination\": \"CHI\",\n" +
                        "  \"id\": \""+userId+"\",\n" +
                        "  \"origin\": \"Par\"\n" +
                        "}")
                .contentType(ContentType.JSON)
                .when()
                .post("http://127.0.0.1:8900/booking")
                .then()
                .statusCode(409)
                .body(Matchers.equalTo("Origin or Destination is not a IATA code (Three Uppercase Letters)"));
    }
    @Test
    //post checking if IATA code with not only Upercase letters will work
    public void createNewBookingForUser() {
        given()
                .body("{\n" +
                        "  \"date\": \""+bookingDate+"\",\n" +
                        "  \"destination\": \"CHI\",\n" +
                        "  \"id\": \""+userId+"\",\n" +
                        "  \"origin\": \"PAR\"\n" +
                        "}")
                .contentType(ContentType.JSON)
                .when()
                .post("http://127.0.0.1:8900/booking")
                .then()
                .statusCode(201);
    }
    @Test
    //get test na status 200, zwykly smoke test (postawiony serwer zwraca pusta liste)
    public void getBookingNotEmptyUserList() {
        createNewBookingForUser();
        when()
                .get("http://127.0.0.1:8900/booking?id="+userId+"&date="+bookingDate)
                .then()
                .statusCode(200)
                .body("isEmpty()", Matchers.is(false));
    }

    @Test
        // get test opisujacy format daty + format timestampa za pomoca regexa (chcialam pokazac porownywanie formatu daty)
    public void getBookingWrongDate() {
        when()
                .get("http://127.0.0.1:8900/booking?id="+userId+"&date=1.02.22")
                .then()
                .statusCode(500)
                .body("status", Matchers.equalTo(500))
                .body("message", Matchers.equalTo("Format date not valid"))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/booking"))
                .body("timestamp", Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(\\+\\d{4})?"));

    }

    @Test
        //Test checking if wrong user ID always returns empty booking list
    public void getBookingWrongIndex() {
        when()
                .get("http://127.0.0.1:8900/booking?id=123&date="+bookingDate)
                .then()
                .statusCode(200)
                .body("isEmpty()", Matchers.is(true));
    }



    @Test
        //Test checking wrong ID format
    public void getWrongUserId() {
        when()
                .get("http://127.0.0.1:8900/user?id=123")
                .then()
                .statusCode(404)
                .body(Matchers.equalTo("User not found"));
    }
    @Test
        //Test checking correct user ID format
    public void getCorrectUserId() {
        when()
                .get("http://127.0.0.1:8900/user?id="+userId)
                .then()
                .statusCode(200)
                .body("email", Matchers.equalTo(email))
                .body("name", Matchers.equalTo(name))
                .body("id", Matchers.equalTo(userId));
    }


    @Test
    // Test checking if server has any users
    public void getAllTheUsers() {
        when()
                .get("http://127.0.0.1:8900/user/all")
                .then()
                .statusCode(200)
                .body("isEmpty()", Matchers.is(false));

    }
}
