package com.liverpool.qa.pages;

import com.liverpool.qa.config.Config;
import com.liverpool.qa.model.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class HomePage extends BasePage {
    private static final By SEARCH_INPUT = By.cssSelector("#mainSearchbar, input[type='search'], input[name='s'], "
            + "input[placeholder*='uscar' i], input[aria-label*='uscar' i], input[id*='search' i]");

    private static final By SEARCH_ICON = By.xpath(
            "//*[self::button or self::a or self::span or self::i][normalize-space(.)='search']");
    private static final int MENU_WAIT_SECONDS = 5;

    private static final By LOGIN_ENTRY = By.xpath("//*[self::a or self::button][contains("
            + lc("normalize-space(.)") + ", 'inicia sesión') or contains("
            + lc("normalize-space(.)") + ", 'iniciar sesión')]");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public HomePage open() {
        driver.get(Config.BASE_URL);
        dismissOverlays();
        return this;
    }

    public boolean isLoggedOut() {
        return isPresent(LOGIN_ENTRY, 5);
    }

    public ProductListPage search(String term) {
        String before = driver.getCurrentUrl();
        searchRaw(term);

        if (!urlChangedFrom(before, 8)) {
            driver.get(Config.SEARCH_URL + URLEncoder.encode(term, StandardCharsets.UTF_8));   
        }
        return new ProductListPage(driver);
    }

    private boolean urlChangedFrom(String before, int seconds) {
        ExpectedCondition<Boolean> changed = d -> !d.getCurrentUrl().equals(before);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(changed);
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    public void searchRaw(String term) {
        WebElement input = findSearchInput();
        if (input != null) {
            input.clear();
            input.sendKeys(term, Keys.ENTER);
            return;
        }
        driver.get(Config.SEARCH_URL + URLEncoder.encode(term, StandardCharsets.UTF_8));
        dismissOverlays();
    }

    private WebElement findSearchInput() {
        try {
            return visible(SEARCH_INPUT, 5);
        } catch (WebDriverException notVisibleYet) {
        }
        try {
            for (WebElement icon : driver.findElements(SEARCH_ICON)) {
                if (icon.isDisplayed()) {
                    icon.click();
                    break;
                }
            }
            return visible(SEARCH_INPUT, 5);
        } catch (WebDriverException e) {
            return null;
        }
    }

    public ProductListPage goToCategory(Category category) {
        open();
        if (Config.USE_MENU_NAVIGATION && category.menuPath().length > 0) {
            try {
                String before = driver.getCurrentUrl();
                navigateViaMenu(category.menuPath());
                if (!urlChangedFrom(before, 8)) {
                    throw new IllegalStateException("El menú no navegó");
                }
                ProductListPage list = new ProductListPage(driver);
                if (list.hasResults(10)) {
                    return list;
                }
            } catch (RuntimeException ignored) {
            }
            open();
        }
        return search(category.searchTerm());
    }

    private void navigateViaMenu(String[] path) {
        Actions actions = new Actions(driver);
        for (int i = 0; i < path.length; i++) {
            By item = By.xpath("//*[self::a or self::button or self::span]["
                    + lc("normalize-space(.)") + "='" + path[i].toLowerCase() + "']");
            WebElement element = visible(item, MENU_WAIT_SECONDS);
            if (i < path.length - 1) {
                actions.moveToElement(element).pause(Duration.ofMillis(400)).perform();   
            } else {
                click(item);
            }
        }
    }
}
