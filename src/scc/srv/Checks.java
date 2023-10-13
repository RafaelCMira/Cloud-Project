package scc.srv;

/**
 * Class with common check functions to validate requests input
 */
public class Checks {

    public static boolean badParams(String... values) {
        for (var str : values)
            if (str == null)
                return true;
        return false;
    }

    // Verifies if HTTP code is OK
    public static boolean isStatusOk(int statusCode) {
        return statusCode > 200 && statusCode < 300;
    }
}
