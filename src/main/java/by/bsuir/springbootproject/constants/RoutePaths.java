package by.bsuir.springbootproject.constants;

public final class RoutePaths {

    private RoutePaths() {}

    public static final String ROOT = "/";
    public static final String HOME = "/home";
    public static final String CATALOG = "/catalog";
    public static final String COMICS = "/comics";
    public static final String PROFILE = "/profile";
    public static final String SEARCH = "/search";
    public static final String ERROR = "/error";

    public static final String AUTH = "/auth";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_LOGOUT = "/logout";
    public static final String AUTH_REGISTER_GOOGLE = "/register-google";
    public static final String AUTH_REGISTER_COMPLETE = "/register/complete";
    public static final String AUTH_LOGOUT_SUCCESS = "/logout-success";
    public static final String AUTH_ACCESS_REQUIRED = "/access-required";

    public static final String LOGIN = AUTH + AUTH_LOGIN;
    public static final String LOGOUT = AUTH + AUTH_LOGOUT;
    public static final String REGISTER_GOOGLE = AUTH + AUTH_REGISTER_GOOGLE;
    public static final String REGISTER_COMPLETE = AUTH + AUTH_REGISTER_COMPLETE;
    public static final String LOGOUT_SUCCESS = AUTH + AUTH_LOGOUT_SUCCESS;
    public static final String ACCESS_REQUIRED = AUTH + AUTH_ACCESS_REQUIRED;

    public static final String PASSWORD_RESET = "/reset";
    public static final String PASSWORD_RESET_SEND_CODE_PART = "/send-code";
    public static final String PASSWORD_RESET_CONFIRM_PART = "/confirm";

    public static final String PASSWORD_RESET_SEND_CODE = PASSWORD_RESET + PASSWORD_RESET_SEND_CODE_PART;
    public static final String PASSWORD_RESET_CONFIRM = PASSWORD_RESET + PASSWORD_RESET_CONFIRM_PART;

    public static final String OAUTH2_AUTHORIZATION_GOOGLE = "/oauth2/authorization/google";

    public static final String READ = "/read";
    public static final String ADMIN = "/admin";

    public static final String API = "/api";
    public static final String API_RATINGS = API + "/ratings";

    public static final String COLLECTIONS = "/collections";

    public static final String COLLECTIONS_SECTION = "/section";
    public static final String COLLECTIONS_CREATE = "/create";
    public static final String COLLECTIONS_RENAME = "/rename";
    public static final String COLLECTIONS_DELETE = "/delete";
    public static final String COLLECTIONS_MOVE = "/move";
    public static final String COLLECTIONS_SAVE = "/save";
    public static final String COLLECTIONS_REMOVE = "/remove";

    public static final String API_COLLECTIONS = API + COLLECTIONS;

    public static final String NOTIFICATIONS = "/notifications";
    public static final String NOTIFICATIONS_TOGGLE = "/toggle";
    public static final String NOTIFICATIONS_DELETE = "/delete";


    public static final String COLLECTIONS_COMIC_MODAL = "/comic-modal";
    public static final String COLLECTIONS_COMIC_SYNC = "/comic-sync";
}
