package scc.srv.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.bson.Document;
import scc.cache.Cache;
import scc.srv.authentication.Session;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Class with common check functions to validate requests input
 */
public class Utility {
    public static final String OK = "OK@%s";
    public static final String BAD_REQUEST = "BAD_REQUEST@%s";
    public static final String BAD_REQUEST_MSG = "Some mandatory value is empty";
    public static final String FORBIDDEN = "FORBIDDEN@%s: %s can't do that operation";
    public static final String NOT_FOUND = "NOT_FOUND@%s: %s does not exist";
    public static final String CONFLICT = "CONFLICT@%s with this id: %s already exists";
    public static final String UNAUTHORIZED = "UNAUTHORIZED@%s";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR@Something went wrong";

    public static final String USER_MSG = "User";
    public static final String MEDIA_MSG = "Image";
    public static final String HOUSE_MSG = "House";
    public static final String RENTAL_MSG = "Rental";


    public static final String QUESTION_MSG = "Question";

    public static final String QUESTION_ALREADY_ANSWERED = "Question %s already answered";

    public static final String INCORRECT_LOGIN = "Incorrect login.";

    public static final String INVALID_PRICE = "Error: Invalid price";

    public static final String INVALID_DISCOUNT = "Error: Invalid discount";

    public static final String RESOURCE_WAS_DELETED = "%s %s was deleted";

    private static final ObjectMapper mapper = new ObjectMapper();


    public static Response sendResponse(String msg, Object... params) {
        var res = msg.split("@");
        Response.Status status = Response.Status.valueOf(res[0]);

        if (status == Response.Status.OK)
            return Response.ok(params[0]).build();

        String message = String.format(res[1], params);
        return Response.status(status).entity(message).build();
    }

    public static Response processException(int statusCode, Object... params) {
        switch (statusCode) {
            case 400 -> {
                return sendResponse(BAD_REQUEST, params);
            }
            case 401 -> {
                return sendResponse(UNAUTHORIZED, params);
            }
            case 403 -> {
                return sendResponse(FORBIDDEN, params);
            }
            case 404 -> {
                return sendResponse(NOT_FOUND, params);
            }
            case 409 -> {
                return sendResponse(CONFLICT, params);
            }
            default -> {
                return sendResponse(String.valueOf(Response.Status.fromStatusCode(statusCode)), params);
            }
        }
    }

    /**
     * Throws exception if not appropriate user for operation on House
     */
    public static Response checkUserSession(Cookie cookie, String id) throws Exception {
        if (cookie == null)
            return sendResponse(UNAUTHORIZED, "No session initialized" + "cookie null ");

        if (cookie.getValue() == null)
            return sendResponse(UNAUTHORIZED, "No session initialized" + " " + cookie.getValue());

        Session session = null;

        String cacheRes = Cache.getCookieFromCache(Session.SESSION_PREFIX, cookie.getValue());

        if (cacheRes != null)
            session = mapper.readValue(cacheRes, Session.class);

        if (session == null)
            return sendResponse(UNAUTHORIZED, "No valid session initialized - " + "SESSION null" + "\n" +
                    Session.SESSION_PREFIX + cookie.getValue());

        if (session.getId() == null)
            return sendResponse(UNAUTHORIZED, "No valid session initialized - " + "id null");

        if (session.getId().isEmpty())
            return sendResponse(UNAUTHORIZED, "No valid session initialized - " + "id vazio");

        if (!session.getId().equals(id) && !session.getId().equals("admin"))
            return sendResponse(UNAUTHORIZED, "Invalid user : " + session.getId());

        return sendResponse(OK, "Session checked");
    }


    public static Date formatDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Instant start = LocalDate.parse(date, formatter).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return Date.from(start);
    }

    // Converts JSON to Object
    public static <T> T mapDocumentToObject(Document document, Class<T> c) {
        try {
            return mapper.readValue(document.toJson(), c);
        } catch (IOException e) {
            return null;
        }
    }

    // Convert an object to its JSON representation
    public static String itemToJsonString(Object item) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
