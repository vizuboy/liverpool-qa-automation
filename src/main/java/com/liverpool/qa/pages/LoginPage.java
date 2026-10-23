package com.liverpool.qa.pages;

import com.liverpool.qa.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage extends BasePage {
    private static final By EMAIL = By.cssSelector(
            "#username, input[name='username'], input[type='email'], input[name='email'], #email");
    private static final By PASSWORD = By.cssSelector("#password, input[type='password']");

    private static final By SUBMIT = By.cssSelector(
            "button[data-action-button-primary='true'], button[name='action'], button[type='submit']");

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
        if (!isPresent(PASSWORD, 2)) {              
            click(clickableContaining("continuar"));
        }
        type(PASSWORD, password);
        click(SUBMIT);
    }

    public boolean hasLoginError() {
        return isPresent(ERROR_MESSAGE, 8);
    }

    public boolean isLoggedIn() {
        return waitForLoginUrl(false, Config.TIMEOUT_SECONDS);
    }

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
