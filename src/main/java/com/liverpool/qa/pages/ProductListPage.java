package com.liverpool.qa.pages;

import com.liverpool.qa.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class ProductListPage extends BasePage {
    private static final By PRODUCT_LINKS = By.cssSelector("a[href*='/tienda/pdp/']");
    private static final By NO_RESULTS = textContainingAny(
            "no encontramos", "no hay resultados", "sin resultados", "0 resultados");

    public ProductListPage(WebDriver driver) {
        super(driver);
    }

    public boolean hasResults(int seconds) {
        return isPresent(PRODUCT_LINKS, seconds);
    }

    public boolean showsNoResultsMessage() {
        return isPresent(NO_RESULTS, 8);
    }

    public List<String> productUrls(int max) {
        hasResults(Config.TIMEOUT_SECONDS);
        List<String> urls = new ArrayList<>();
        for (WebElement link : driver.findElements(PRODUCT_LINKS)) {
            String href = link.getAttribute("href");
            if (href != null && !urls.contains(href)) {
                urls.add(href);
            }
            if (urls.size() >= max) break;
        }
        return urls;
    }
}
