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

    /** VALIDAR: varios candidatos para el input del buscador (el header cambia entre versiones). */
    private static final By SEARCH_INPUT = By.cssSelector("#mainSearchbar, input[type='search'], input[name='s'], "
            + "input[placeholder*='uscar' i], input[aria-label*='uscar' i], input[id*='search' i]");
    /** El header usa íconos Material: el botón del buscador tiene el texto "search". */
    private static final By SEARCH_ICON = By.xpath(
            "//*[self::button or self::a or self::span or self::i][normalize-space(.)='search']");
    private static final int MENU_WAIT_SECONDS = 5;
    /** VALIDAR: enlace/botón que el sitio muestra solo cuando NO hay sesión. */
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

    /** Heurística: si el sitio sigue ofreciendo "Iniciar sesión", la sesión no es válida. */
    public boolean isLoggedOut() {
        return isPresent(LOGIN_ENTRY, 5);
    }

    /** Busca un término y devuelve la página de resultados. */
    public ProductListPage search(String term) {
        String before = driver.getCurrentUrl();
        searchRaw(term);
        // Sin esta espera se leerían los productos de la página ANTERIOR (el home ya trae productos en tendencia).
        if (!urlChangedFrom(before, 8)) {
            driver.get(Config.SEARCH_URL + URLEncoder.encode(term, StandardCharsets.UTF_8));   // respaldo
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

    /**
     * Escribe en el buscador y presiona Enter (acepta cadenas vacías, largas o con caracteres especiales).
     * Si el input no aparece (buscador colapsado o locator distinto), abre la URL de búsqueda directamente.
     */
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
            // puede estar colapsado detrás del ícono de lupa
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

    /**
     * Navega a la categoría: primero intenta por menú (como pide el examen);
     * si el menú cambió o no es accesible, usa la búsqueda del sitio como respaldo.
     */
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
                // menú distinto al esperado -> respaldo por búsqueda
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
                actions.moveToElement(element).pause(Duration.ofMillis(400)).perform();   // hover
            } else {
                click(item);
            }
        }
    }
}
