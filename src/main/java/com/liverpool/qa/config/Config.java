package com.liverpool.qa.config;

/**
 * Configuración central. Todo se puede sobreescribir con variables de entorno.
 * Las credenciales NUNCA van en el código: LIVERPOOL_USER / LIVERPOOL_PASSWORD.
 */
public final class Config {

    private Config() {}

    public static final String BASE_URL  = env("BASE_URL",  "https://www.liverpool.com.mx/tienda/home");
    /** Respaldo si el buscador del header no se encuentra: se le pasa el término codificado al final. */
    public static final String SEARCH_URL = env("SEARCH_URL", "https://www.liverpool.com.mx/tienda?s=");
    public static final String LOGIN_URL = env("LOGIN_URL", "https://login.liverpool.com.mx/u/login?state=hKFo2SBxV1o4VXEzUUdsYV9uaEU1Yi1NaTAzOHF3SURqbl9fZqFur3VuaXZlcnNhbC1sb2dpbqN0aWTZIEd5WXRuSXZhS1FxU3Qta1JWdG5qaTNtdGVoc3JLeXJOo2NpZNkgSjFXOVJPZ1RZaWltbkM5UGl0ZDJKd3Y3RXpqV05VQWo");
    public static final String CART_URL  = env("CART_URL",  "https://www.liverpool.com.mx/tienda/cart");

    public static final String USER     = env("LIVERPOOL_USER", "");
    public static final String PASSWORD = env("LIVERPOOL_PASSWORD", "");

    /** Ruta a un JSON con las cookies de una sesión ya iniciada (alternativa al login con usuario/contraseña). */
    public static final String COOKIES_FILE = env("COOKIES_FILE", "");
    /** Alternativa: el mismo JSON pero directamente en una variable de entorno (útil en CI con secretos). */
    public static final String COOKIES_JSON = env("COOKIES_JSON", "");

    public static final boolean HEADLESS = Boolean.parseBoolean(env("HEADLESS", "false"));
    /** Intenta navegar por el menú; si falla, cae a la búsqueda del sitio. */
    public static final boolean USE_MENU_NAVIGATION = Boolean.parseBoolean(env("USE_MENU_NAVIGATION", "true"));
    public static final int TIMEOUT_SECONDS = Integer.parseInt(env("TIMEOUT_SECONDS", "20"));

    public static boolean hasCookies() {
        return !COOKIES_FILE.isBlank() || !COOKIES_JSON.isBlank();
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
