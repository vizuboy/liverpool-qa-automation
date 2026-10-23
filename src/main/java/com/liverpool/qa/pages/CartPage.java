package com.liverpool.qa.pages;

import com.liverpool.qa.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CartPage extends BasePage {
    private static final By REMOVE_BUTTON = By.cssSelector("[data-testid^='delete-button-']");

    private static final By CONFIRM_BUTTON = By.xpath(".//button[contains(" + lc("normalize-space(.)")
            + ", 'eliminar') or contains(" + lc("normalize-space(.)") + ", 'confirmar') or "
            + lc("normalize-space(.)") + "='sí' or " + lc("normalize-space(.)") + "='si' or contains("
            + lc("normalize-space(.)") + ", 'aceptar') or contains(" + lc("normalize-space(.)") + ", 'continuar')]");

    private static final By EMPTY_MESSAGE = textContainingAny(
            "no hay artículos en la bolsa", "no hay articulos en la bolsa",
            "bolsa está vacía", "bolsa esta vacia", "no tienes productos", "bolsa vacía");

    private static final By EMPTY_BAG_TEMPLATE = By.cssSelector("[data-testid='template-empty-bag']");

    private static final String[] BUY_NOW_TEXTS = {
            "ir a comprar", "comprar ahora", "ir a pagar", "continuar compra", "continuar con la compra"};

    private static final String[] BUY_NOW_EXACT_TEXTS = {"continuar compra"};
    private static final By BUY_NOW = buyNowLocator();

    private static By buyNowLocator() {
        StringBuilder sb = new StringBuilder("//*[self::button or self::a][");
        for (int i = 0; i < BUY_NOW_TEXTS.length; i++) {
            if (i > 0) sb.append(" or ");
            String text = BUY_NOW_TEXTS[i];
            if (List.of(BUY_NOW_EXACT_TEXTS).contains(text)) {
                sb.append("normalize-space(").append(lc(".")).append(")='").append(text).append("'");
            } else {
                sb.append("contains(").append(lc("normalize-space(.)")).append(", '").append(text).append("')");
            }
        }
        return By.xpath(sb.append("]").toString());
    }

    private static final String LINE_SUMMARY_JS = """
            let a = arguments[0];
            for (let i = 0; i < 10 && a; i++, a = a.parentElement) {
              const link = a.querySelector("a[href*='/tienda/pdp/']");
              if (link) { return ((link.textContent || '').trim() || link.getAttribute('href')).slice(0, 80); }
            }
            return '(sin enlace de producto)';
            """;

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public CartPage open() {
        driver.get(Config.CART_URL);
        dismissOverlays();
        return this;
    }

    public int itemCount() {
        if (!isPresent(REMOVE_BUTTON, 8)) {
            return 0;
        }
        return displayedRemoveButtons().size();
    }

    public List<String> itemSummaries() {
        List<String> summaries = new ArrayList<>();
        for (WebElement remove : displayedRemoveButtons()) {
            try {
                Object summary = ((JavascriptExecutor) driver).executeScript(LINE_SUMMARY_JS, remove);
                summaries.add(String.valueOf(summary));
            } catch (WebDriverException e) {
                summaries.add("?");
            }
        }
        return summaries;
    }

    public boolean isEmpty() {
        return isPresent(EMPTY_MESSAGE, 8) || isPresent(EMPTY_BAG_TEMPLATE, 3);
    }

    public boolean isBuyNowVisible() {
        return isPresent(BUY_NOW, 5);
    }

    public CheckoutPage clickBuyNow() {
        try {
            click(BUY_NOW);
        } catch (TimeoutException e) {
            throw new IllegalStateException("No se encontró el botón para ir a comprar en la bolsa. "
                    + "Botones visibles: " + visibleButtonTexts(), e);
        }
        return new CheckoutPage(driver);
    }

    public void clearBag() {
        open();
        for (int i = 0; i < 40; i++) {
            if (itemCount() == 0) {
                return;
            }
            String lineId = clickFirstRemoveButton();
            if (lineId == null) {
                return;
            }
            confirmRemovalIfAsked();

            By thisLine = By.cssSelector("[data-testid='" + lineId + "']");
            ExpectedCondition<Boolean> gone = d -> d.findElements(thisLine).stream()
                    .noneMatch(BasePage::safeIsDisplayed);
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10)).until(gone);
            } catch (TimeoutException e) {
                throw new IllegalStateException("No se pudo quitar la línea '" + lineId + "' de la bolsa. "
                        + "Capas abiertas: " + describeLayers(), e);
            }
        }
        throw new IllegalStateException("No se logró vaciar la bolsa tras 40 intentos. Quedan: " + itemSummaries());
    }

    private List<WebElement> displayedRemoveButtons() {
        return driver.findElements(REMOVE_BUTTON).stream()
                .filter(BasePage::safeIsDisplayed)
                .collect(Collectors.toList());
    }

    private String clickFirstRemoveButton() {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                List<WebElement> buttons = displayedRemoveButtons();
                if (buttons.isEmpty()) {
                    return null;
                }
                WebElement target = buttons.get(0);
                String lineId = target.getAttribute("data-testid");
                scrollIntoView(target);
                safeClick(target);
                return lineId;
            } catch (StaleElementReferenceException reRendered) {
            }
        }
        throw new IllegalStateException("La bolsa se siguió re-dibujando y no se pudo hacer clic en eliminar");
    }

    private List<WebElement> currentLayers(WebDriver d) {
        List<WebElement> layers = new ArrayList<>();
        for (WebElement dialog : d.findElements(DIALOG)) {
            if (safeIsDisplayed(dialog)) {
                layers.add(dialog);
            }
        }
        Object layer = ((JavascriptExecutor) d).executeScript(FIND_LAYER_JS);
        if (layer instanceof WebElement) {
            layers.add((WebElement) layer);
        }
        return layers;
    }

    private void confirmRemovalIfAsked() {
        ExpectedCondition<Boolean> confirmed = d -> {
            for (WebElement layer : currentLayers(d)) {
                for (WebElement button : layer.findElements(CONFIRM_BUTTON)) {
                    if (safeIsDisplayed(button)) {
                        String label = button.getText().trim();
                        safeClick(button);
                        System.out.println("Eliminación confirmada con: '" + label + "'");
                        return true;
                    }
                }
            }
            return false;
        };
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .ignoring(StaleElementReferenceException.class)
                    .until(confirmed);
        } catch (WebDriverException noConfirmation) {
        }
    }

    private String describeLayers() {
        StringBuilder sb = new StringBuilder();
        try {
            for (WebElement layer : currentLayers(driver)) {
                String text = layer.getText().trim().replace('\n', ' ');
                sb.append("\n  capa: '").append(text.length() > 150 ? text.substring(0, 150) + "..." : text)
                        .append("' botones: ");
                for (WebElement b : layer.findElements(By.cssSelector("button, a[role='button']"))) {
                    if (safeIsDisplayed(b)) {
                        sb.append('[').append(b.getText().trim().replace('\n', ' ')).append("] ");
                    }
                }
            }
        } catch (WebDriverException e) {
            sb.append(" (no se pudo leer)");
        }
        return sb.length() == 0 ? "(ninguna)" : sb.toString();
    }

    private List<String> visibleButtonTexts() {
        return driver.findElements(By.cssSelector("button, a[role='button']")).stream()
                .filter(BasePage::safeIsDisplayed)
                .map(el -> el.getText().trim().replace('\n', ' '))
                .filter(t -> !t.isEmpty())
                .distinct()
                .limit(30)
                .collect(Collectors.toList());
    }
}
