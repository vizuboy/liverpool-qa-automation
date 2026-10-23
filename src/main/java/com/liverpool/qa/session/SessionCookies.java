package com.liverpool.qa.session;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.liverpool.qa.config.Config;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

/**
 * Inyecta en el navegador las cookies de una sesión ya iniciada.
 *
 * Formato esperado: arreglo JSON de objetos con al menos "name" y "value"; opcionales
 * "domain", "path", "secure", "httpOnly" y la expiración como "expiry" o "expirationDate"
 * (segundos epoch). Es el formato que exportan extensiones como Cookie-Editor.
 *
 * Importante: el navegador solo acepta cookies del dominio de la página actual, por eso
 * primero se abre el sitio, luego se agregan las cookies y por último se recarga.
 */
public final class SessionCookies {

    private SessionCookies() {}

    /** Devuelve cuántas cookies se pudieron agregar. */
    public static int apply(WebDriver driver) {
        driver.get(Config.BASE_URL);                       // 1. dominio correcto
        int added = 0;
        for (JsonElement element : load()) {
            try {
                driver.manage().addCookie(toCookie(element.getAsJsonObject()));
                added++;
            } catch (WebDriverException | IllegalStateException e) {
                System.err.println("Cookie omitida: " + e.getMessage());
            }
        }
        driver.navigate().refresh();                       // 3. el sitio ya ve la sesión
        return added;
    }

    private static JsonArray load() {
        try {
            String json = !Config.COOKIES_JSON.isBlank()
                    ? Config.COOKIES_JSON
                    : Files.readString(Paths.get(Config.COOKIES_FILE), StandardCharsets.UTF_8);
            return JsonParser.parseString(json).getAsJsonArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo de cookies: " + Config.COOKIES_FILE, e);
        }
    }

    private static Cookie toCookie(JsonObject o) {
        Cookie.Builder builder = new Cookie.Builder(o.get("name").getAsString(), o.get("value").getAsString())
                .path(string(o, "path", "/"))
                .isSecure(bool(o, "secure"))
                .isHttpOnly(bool(o, "httpOnly"));

        String domain = string(o, "domain", null);
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }
        JsonElement expiry = o.has("expiry") ? o.get("expiry") : o.get("expirationDate");
        if (expiry != null && !expiry.isJsonNull()) {
            builder.expiresOn(new Date((long) (expiry.getAsDouble() * 1000)));
        }
        return builder.build();
    }

    private static String string(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }

    private static boolean bool(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() && o.get(key).getAsBoolean();
    }
}
