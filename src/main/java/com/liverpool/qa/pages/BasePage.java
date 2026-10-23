package com.liverpool.qa.pages;

import com.liverpool.qa.config.Config;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {
    protected static final By DIALOG = By.cssSelector("[role='dialog'], [aria-modal='true'], [class*='modal' i]");

    protected static final String FIND_LAYER_JS = """
            const vw = window.innerWidth, vh = window.innerHeight;
            let found = null;
            for (const el of document.querySelectorAll('body *')) {
              const cs = getComputedStyle(el);
              if (cs.position !== 'fixed' || cs.visibility === 'hidden' || cs.display === 'none') continue;
              const r = el.getBoundingClientRect();
              if (r.width * r.height < 0.15 * vw * vh) continue;
              if (!el.querySelector('button, a[role="button"]')) continue;
              if (el.querySelector('h1')) continue;
              found = el;
            }
            return found;
            """;

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.TIMEOUT_SECONDS));
    }

    protected static String lc(String expr) {
        return "translate(" + expr + ", 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚ', 'abcdefghijklmnopqrstuvwxyzáéíóú')";
    }

    protected static By clickableContaining(String text) {
        return By.xpath("//*[self::button or self::a][contains(" + lc("normalize-space(.)") + ", '"
                + text.toLowerCase() + "')]");
    }

    protected static By textContainingAny(String... phrases) {
        StringBuilder sb = new StringBuilder("//*[text()[");
        for (int i = 0; i < phrases.length; i++) {
            if (i > 0) sb.append(" or ");
            sb.append("contains(").append(lc(".")).append(", '").append(phrases[i].toLowerCase()).append("')");
        }
        return By.xpath(sb.append("]]").toString());
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement visible(By locator, int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected boolean isPresent(By locator, int seconds) {
        try {
            visible(locator, seconds);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected void click(By locator) {
        ExpectedCondition<Boolean> clicked = d -> {
            WebElement el = ExpectedConditions.elementToBeClickable(locator).apply(d);
            if (el == null) return false;
            try {
                el.click();
                return true;
            } catch (ElementClickInterceptedException first) {
                dismissOverlays();
                scrollIntoView(el);
                try {
                    el.click();
                    return true;
                } catch (ElementClickInterceptedException second) {
                    String detail = String.valueOf(second.getMessage()).split("\\R")[0];
                    System.err.println("Click interceptado en " + locator + " -> " + detail);
                    jsClick(el);
                    return true;
                }
            }
        };
        new WebDriverWait(driver, Duration.ofSeconds(Config.TIMEOUT_SECONDS))
                .ignoring(StaleElementReferenceException.class)
                .until(clicked);
    }

    protected void type(By locator, String text) {
        WebElement el = visible(locator);
        el.clear();
        el.sendKeys(text);
    }

    protected void safeClick(WebElement el) {
        try {
            el.click();
        } catch (WebDriverException e) {
            jsClick(el);
        }
    }

    protected void jsClick(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    protected static boolean safeIsDisplayed(WebElement el) {
        try {
            return el.isDisplayed();
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    protected void scrollIntoView(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    public void dismissOverlays() {
        By[] candidates = {
                By.id("onetrust-accept-btn-handler"),
                clickableContaining("aceptar todas"),
                clickableContaining("entendido"),
                By.cssSelector("button[aria-label='Cerrar'], button[aria-label='Close']")
        };
        for (By candidate : candidates) {
            try {
                for (WebElement el : driver.findElements(candidate)) {
                    if (el.isDisplayed()) {
                        el.click();
                        break;
                    }
                }
            } catch (WebDriverException ignored) {
            }
        }
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public boolean looksLikeErrorPage() {
        String title = driver.getTitle() == null ? "" : driver.getTitle().toLowerCase();
        return title.matches(".*(error|404|500|no encontrad|not found).*");
    }
}
