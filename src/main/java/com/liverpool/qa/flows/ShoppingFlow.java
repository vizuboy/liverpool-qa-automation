package com.liverpool.qa.flows;

import com.liverpool.qa.model.Category;
import com.liverpool.qa.pages.HomePage;
import com.liverpool.qa.pages.ProductDetailPage;
import com.liverpool.qa.pages.ProductListPage;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

public class ShoppingFlow {
    private static final int MAX_CANDIDATES = 5;

    private final WebDriver driver;

    public ShoppingFlow(WebDriver driver) {
        this.driver = driver;
    }

    public Optional<String> findFirstAvailableProductUrl(Category category) {
        ProductListPage list = new HomePage(driver).goToCategory(category);
        for (String url : list.productUrls(MAX_CANDIDATES)) {
            ProductDetailPage pdp = new ProductDetailPage(driver).open(url);
            if (pdp.isAddToBagAvailable()) {
                return Optional.of(url);
            }
        }
        return Optional.empty();
    }

    public Optional<String> addFirstAvailableProductOf(Category category) {
        if (findFirstAvailableProductUrl(category).isEmpty()) {
            return Optional.empty();
        }
        ProductDetailPage pdp = new ProductDetailPage(driver);
        String title = pdp.title();
        boolean confirmed = pdp.addToBag();
        System.out.println("Agregado a la bolsa: " + title + " (confirmación visible: " + confirmed + ")");
        return Optional.of(title);
    }
}
