package com.liverpool.qa.pages;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class CheckoutPage extends BasePage {
    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        try {
            wait.until(ExpectedConditions.urlMatches("(?i).*(checkout|shipping|payment|delivery|envio|pago).*"));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
