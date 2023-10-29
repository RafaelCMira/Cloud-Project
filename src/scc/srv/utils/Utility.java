package scc.srv.utils;

import jakarta.ws.rs.core.Response;

/**
 * Class with common check functions to validate requests input
 */
public class Utility {
    public static final String OK = "OK@%s";
    public static final String BAD_REQUEST = "BAD_REQUEST@Some mandatory value is empty";
    public static final String FORBIDDEN = "FORBIDDEN@%s: %s does not exist";
    public static final String NOT_FOUND = "NOT_FOUND@%s: %s does not exist";
    public static final String CONFLICT = "CONFLICT@%s with this id: %s already exists";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR@Something went wrong";

    public static Response sendResponse(String msg, Object... params) {
        var res = msg.split("@");
        Response.Status status = Response.Status.valueOf(res[0]);
        String message = String.format(res[1], params);
        return Response.status(status).entity(message).build();
    }

    public static Response processException(int statusCode, Object... params) {
        switch (statusCode) {
            case 400 -> {
                return sendResponse(BAD_REQUEST);
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
                return sendResponse(INTERNAL_SERVER_ERROR, params);
            }
        }
    }

    public static boolean badParams(String... values) {
        for (var str : values)
            if (str.isBlank())
                return true;
        return false;
    }

    // Verifies if HTTP code is OK
    public static boolean isStatusOk(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
