package com.liverpool.qa.pages;

import com.liverpool.qa.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Login de Liverpool (Salesforce Experience Cloud): locators ya validados contra el sitio real.
 * El formulario es de UN paso (correo + contraseña visibles a la vez) y muestra el error
 * "Correo electrónico o contraseña incorrecta" en #error-element-password.
 */
public class LoginPage extends BasePage {

    /**
     * El login de Liverpool (Salesforce Experience Cloud) NO usa type="email":
     * el campo de correo es <input id="username" name="username" type="text" inputmode="email">.
     * Por eso el locator real es #username y no #email.
     */
    private static final By EMAIL = By.cssSelector(
            "#username, input[name='username'], input[type='email'], input[name='email'], #email");
    private static final By PASSWORD = By.cssSelector("#password, input[type='password']");
    /** Hay dos <button type="submit"> (el principal y "Continuar con Apple"): se elige el principal. */
    private static final By SUBMIT = By.cssSelector(
            "button[data-action-button-primary='true'], button[name='action'], button[type='submit']");
    /** Texto real del sitio: "Correo electrónico o contraseña incorrecta" (#error-element-password). */
    private static final By ERROR_MESSAGE = textContainingAny(
            "incorrect", "inválid", "invalid", "no es correcto", "no coincide", "no pudimos");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        driver.get(Config.LOGIN_URL);
        dismissOverlays();
        return this;
    }

    public void login(String user, String password) {
        open();
        type(EMAIL, user);
        if (!isPresent(PASSWORD, 2)) {              // flujo de 2 pasos
            click(clickableContaining("continuar"));
        }
        type(PASSWORD, password);
        click(SUBMIT);
    }

    public boolean hasLoginError() {
        return isPresent(ERROR_MESSAGE, 8);
    }

    /** Se considera logueado cuando ya no estamos en la URL de login. */
    public boolean isLoggedIn() {
        return waitForLoginUrl(false, Config.TIMEOUT_SECONDS);
    }

    /** Tras un login fallido la URL sigue en /u/login: no hay sesión iniciada. */
    public boolean isStillOnLoginPage(int seconds) {
        return waitForLoginUrl(true, seconds);
    }

    private boolean waitForLoginUrl(boolean expectLogin, int seconds) {
        ExpectedCondition<Boolean> urlContainsLogin = ExpectedConditions.urlContains("login");
        ExpectedCondition<Boolean> condition = expectLogin
                ? urlContainsLogin
                : ExpectedConditions.not(urlContainsLogin);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(condition);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
