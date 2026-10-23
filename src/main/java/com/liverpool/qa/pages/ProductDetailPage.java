package com.liverpool.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class ProductDetailPage extends BasePage {
    private static final By TITLE = By.cssSelector("h1");
    private static final By ADD_TO_BAG = By.xpath("//button[contains(" + lc("normalize-space(.)")
            + ", 'agregar') and contains(" + lc("normalize-space(.)") + ", 'bolsa')]");
    private static final By ADDED_CONFIRMATION = textContainingAny(
            "agregado", "ver bolsa", "ir a la bolsa", "ir a mi bolsa");

    private static final By SIZE_PICKER = By.xpath("//button[starts-with(@data-testid, "
            + "'selection-button-simple-picker-')][contains(" + lc("normalize-space(.)") + ", 'tama') or contains("
            + lc("normalize-space(.)") + ", 'talla')]");

    private static final By SIZE_DRAWER = By.cssSelector(
            "[class*='MuiDrawer-root' i], [role='listbox'], [role='menu']");

    private static final By DIALOG_CONFIRM = By.xpath(".//button[contains(" + lc("normalize-space(.)")
            + ", 'agregar') or contains(" + lc("normalize-space(.)") + ", 'continuar') or contains("
            + lc("normalize-space(.)") + ", 'aceptar') or contains(" + lc("normalize-space(.)") + ", 'listo')]");

    private static final By DECLINE_WARRANTY = By.xpath(".//*[self::button or self::a]["
            + "starts-with(" + lc("normalize-space(.)") + ", 'no ') or starts-with(" + lc("normalize-space(.)")
            + ", 'no,') or contains(" + lc("normalize-space(.)") + ", 'sin garant') or contains("
            + lc("normalize-space(.)") + ", 'continuar sin') or contains(" + lc("normalize-space(.)")
            + ", 'omitir') or contains(" + lc("normalize-space(.)") + ", 'ahora no') or normalize-space(.)='close'"
            + " or contains(" + lc("@aria-label") + ", 'cerrar')]");

    private static final String FIND_SIZES_JS = """
            const scope = arguments[0], h1 = arguments[1], btn = arguments[2];
            const re = /^(\\d{1,2}([.,]\\d)?(\\s?(mx|mex|cm|us|eu))?|\\d{1,4}\\s?(ml|g|gb|tb)|xxs|xs|s|m|l|xl|xxl|xxxl|ch|mg|gd|[úu]nic[ao]|uni|unitalla)$/i;
            const root = scope || document.body;
            let r1 = null, rb = null;
            if (!scope) { r1 = h1.getBoundingClientRect(); rb = btn.getBoundingClientRect(); }
            const isQty = (el) => {
              let a = el.parentElement;
              for (let i = 0; a && i < 2; i++, a = a.parentElement) {
                const tx = a.textContent;
                if (/\\bremove\\b/i.test(tx) && /\\badd\\b/i.test(tx)) return true;
              }
              return false;
            };
            const bad = '[disabled],[aria-disabled="true"],[class*="disabled" i],[class*="agotad" i],[class*="unavailable" i],[class*="soldout" i]';
            const cands = [], seen = [];
            for (const el of root.querySelectorAll('*')) {
              if (el.children.length) continue;
              const t = (el.textContent || '').trim();
              if (!t || t.length > 30) continue;
              const r = el.getBoundingClientRect();
              if (r.width === 0 || r.height === 0) continue;
              if (r1 && (r.left < r1.left - 20 || r.top < r1.top || r.bottom > rb.bottom)) continue;
              if (!re.test(t)) { if (seen.length < 25 && !seen.includes(t)) seen.push(t); continue; }
              if (el.closest(bad) || isQty(el)) continue;
              cands.push(el);
            }
            return [cands, seen];
            """;

    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }

    public ProductDetailPage open(String url) {
        driver.get(url);
        dismissOverlays();
        visible(TITLE);
        return this;
    }

    public String title() {
        return visible(TITLE).getText().trim();
    }

    public boolean isAddToBagAvailable() {
        return isPresent(ADD_TO_BAG, 5) && driver.findElement(ADD_TO_BAG).isEnabled();
    }

    public boolean selectFirstAvailableOption() {
        WebElement picker = findVisibleSizePicker(2);
        if (picker != null) {
            String label = picker.getText().trim();
            if (!mentionsPlaceholder(label)) {
                System.out.println("Talla ya seleccionada: " + label);
                return true;
            }
            safeClick(picker);
            WebElement drawer = findVisibleSizeDrawer(3);
            if (drawer != null && selectSizeIn(drawer)) {
                waitForSizeDrawerToClose();
                return true;
            }
        }
        return selectSizeIn(null);
    }

    private static boolean mentionsPlaceholder(String label) {
        String text = label.toLowerCase();
        return text.contains("selecciona") || text.contains("elige");
    }

    private WebElement findVisibleSizePicker(int seconds) {
        ExpectedCondition<WebElement> picker = d -> d.findElements(SIZE_PICKER).stream()
                .filter(BasePage::safeIsDisplayed).findFirst().orElse(null);
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(picker);
        } catch (TimeoutException e) {
            return null;
        }
    }

    private WebElement findVisibleSizeDrawer(int seconds) {
        ExpectedCondition<WebElement> drawer = d -> d.findElements(SIZE_DRAWER).stream()
                .filter(BasePage::safeIsDisplayed).findFirst().orElse(null);
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(drawer);
        } catch (TimeoutException e) {
            return null;
        }
    }

    private void waitForSizeDrawerToClose() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.invisibilityOfElementLocated(SIZE_DRAWER));
        } catch (TimeoutException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private boolean selectSizeIn(WebElement scope) {
        Object raw = ((JavascriptExecutor) driver).executeScript(
                FIND_SIZES_JS, scope, driver.findElement(TITLE), driver.findElement(ADD_TO_BAG));
        List<Object> result = (List<Object>) raw;
        List<Object> candidates = (List<Object>) result.get(0);
        List<Object> seen = (List<Object>) result.get(1);

        for (Object candidate : candidates) {
            WebElement el = (WebElement) candidate;
            try {
                String text = el.getText().trim();
                scrollIntoView(el);
                safeClick(el);
                System.out.println("Talla seleccionada: " + text);
                return true;
            } catch (StaleElementReferenceException ignored) {
            }
        }
        System.out.println("Sin talla/variante que elegir (" + (scope == null ? "página" : "pop-up")
                + "). Textos en la zona: " + seen);
        return false;
    }

    private void chooseSizeInDialogIfAsked() {
        try {
            WebElement dialog = findVisibleDialog(3);
            if (dialog == null) {
                return;
            }
            if (selectSizeIn(dialog)) {
                for (WebElement confirm : dialog.findElements(DIALOG_CONFIRM)) {
                    if (safeIsDisplayed(confirm)) {
                        safeClick(confirm);
                        break;
                    }
                }
            }
        } catch (WebDriverException noDialog) {
        }
    }

    private WebElement findVisibleDialog(int seconds) {
        ExpectedCondition<WebElement> visibleDialog = d -> d.findElements(DIALOG).stream()
                .filter(BasePage::safeIsDisplayed).findFirst().orElse(null);
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(visibleDialog);
        } catch (TimeoutException e) {
            return null;
        }
    }

    private static boolean mentionsWarranty(String text) {
        String t = text.toLowerCase();
        return t.contains("garant") || t.contains("protecci") || t.contains("extendid");
    }

    private WebElement findWarrantyLayer(int seconds) {
        ExpectedCondition<WebElement> warranty = d -> {
            try {
                for (WebElement dialog : d.findElements(DIALOG)) {
                    if (safeIsDisplayed(dialog) && mentionsWarranty(dialog.getText())) {
                        return dialog;
                    }
                }
                Object layer = ((JavascriptExecutor) d).executeScript(FIND_LAYER_JS);
                if (layer instanceof WebElement && mentionsWarranty(((WebElement) layer).getText())) {
                    return (WebElement) layer;
                }
            } catch (StaleElementReferenceException ignored) {
            }
            return null;
        };
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(warranty);
        } catch (TimeoutException e) {
            return null;
        }
    }

    private void declineWarrantyIfOffered() {
        try {
            WebElement layer = findWarrantyLayer(3);
            if (layer == null) {
                return;
            }
            for (WebElement option : layer.findElements(DECLINE_WARRANTY)) {
                if (safeIsDisplayed(option)) {
                    String label = option.getText().trim();
                    safeClick(option);
                    System.out.println("Garantía rechazada con: '" + label + "'");
                    return;
                }
            }
            StringBuilder buttons = new StringBuilder();
            for (WebElement b : layer.findElements(By.cssSelector("button, a[role='button']"))) {
                if (safeIsDisplayed(b)) {
                    buttons.append('[').append(b.getText().trim().replace('\n', ' ')).append("] ");
                }
            }
            System.err.println("Apareció una oferta de garantía pero no se encontró cómo rechazarla. Botones: "
                    + buttons);
        } catch (WebDriverException ignored) {
        }
    }

    public boolean addToBag() {
        boolean selected = selectFirstAvailableOption();
        click(ADD_TO_BAG);
        if (!selected) {
            chooseSizeInDialogIfAsked();
        }
        declineWarrantyIfOffered();
        return isPresent(ADDED_CONFIRMATION, 8);
    }

    public void clickAddToBagWithoutSelectingOptions() {
        click(ADD_TO_BAG);
    }
}
