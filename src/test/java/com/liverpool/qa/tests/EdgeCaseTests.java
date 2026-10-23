package com.liverpool.qa.tests;

import com.liverpool.qa.flows.ShoppingFlow;
import com.liverpool.qa.model.Category;
import com.liverpool.qa.pages.CartPage;
import com.liverpool.qa.pages.HomePage;
import com.liverpool.qa.pages.ProductDetailPage;
import org.openqa.selenium.NoAlertPresentException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Optional;

public class EdgeCaseTests extends LoggedInBaseTest {
    @Test(description = "EDGE-01: búsqueda vacía no rompe el sitio")
    public void emptySearchDoesNotBreakSite() {
        HomePage home = new HomePage(driver).open();
        home.searchRaw("");

        Assert.assertTrue(driver.getCurrentUrl().contains("liverpool.com.mx"), "Se salió del dominio");
        Assert.assertFalse(home.looksLikeErrorPage(), "La búsqueda vacía produjo una página de error");
    }

    @Test(description = "EDGE-02: caracteres especiales / intento de script no se ejecutan ni rompen la página")
    public void specialCharactersAreHandledSafely() {
        HomePage home = new HomePage(driver).open();
        home.searchRaw("<script>alert('xss')</script> & \"'%00");

        try {
            driver.switchTo().alert();
            Assert.fail("Se ejecutó un alert: posible XSS reflejado en la búsqueda");
        } catch (NoAlertPresentException expected) {
        }
        Assert.assertFalse(home.looksLikeErrorPage(), "Los caracteres especiales produjeron una página de error");
    }

    @Test(description = "EDGE-03: término de búsqueda muy largo (500 caracteres) se maneja sin error")
    public void veryLongSearchTermIsHandled() {
        HomePage home = new HomePage(driver).open();
        home.searchRaw("a".repeat(500));

        Assert.assertFalse(home.looksLikeErrorPage(), "Un término muy largo produjo una página de error");
    }

    @Test(description = "EDGE-04: agregar dos veces el mismo producto deja la bolsa consistente y comprable")
    public void addingSameProductTwiceKeepsBagConsistent() {
        CartPage cart = new CartPage(driver);
        cart.clearBag();

        Optional<String> url = new ShoppingFlow(driver).findFirstAvailableProductUrl(Category.PERFUMES);
        Assert.assertTrue(url.isPresent(), "Precondición: se necesita un perfume disponible");

        ProductDetailPage pdp = new ProductDetailPage(driver);
        pdp.addToBag();
        pdp.open(url.get());
        pdp.addToBag();

        cart.open();
        int lines = cart.itemCount();
        Assert.assertTrue(lines >= 1 && lines <= 2,
                "El mismo producto debe sumar cantidad o a lo más duplicar la línea. Líneas: " + lines);
        Assert.assertTrue(cart.isBuyNowVisible(), "La bolsa con productos debe permitir 'Ir a comprar'");
    }

    @Test(description = "EDGE-05: la bolsa persiste tras recargar y tras navegar a otra página")
    public void bagPersistsAfterRefreshAndNavigation() {
        CartPage cart = new CartPage(driver);
        cart.clearBag();

        Optional<String> title = new ShoppingFlow(driver).addFirstAvailableProductOf(Category.PERFUMES);
        Assert.assertTrue(title.isPresent(), "Precondición: se necesita un perfume disponible");

        cart.open();
        int before = cart.itemCount();
        Assert.assertTrue(before > 0, "El producto debería estar en la bolsa");

        driver.navigate().refresh();
        Assert.assertEquals(cart.itemCount(), before, "La bolsa cambió tras recargar la página");

        new HomePage(driver).open();
        cart.open();
        Assert.assertEquals(cart.itemCount(), before, "La bolsa cambió tras navegar a otra página y volver");
    }
}
