package com.liverpool.qa.tests;

import com.liverpool.qa.flows.ShoppingFlow;
import com.liverpool.qa.model.Category;
import com.liverpool.qa.pages.CartPage;
import com.liverpool.qa.pages.ProductDetailPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Optional;

/** Casos NEGATIVOS sobre la bolsa (requieren sesión). */
public class CartNegativeTests extends LoggedInBaseTest {

    @Test(description = "NEG-03: no se puede agregar zapatos sin elegir talla")
    public void cannotAddShoesWithoutSelectingSize() {
        CartPage cart = new CartPage(driver);
        cart.clearBag();

        Optional<String> url = new ShoppingFlow(driver).findFirstAvailableProductUrl(Category.BOTAS);
        Assert.assertTrue(url.isPresent(), "Precondición: se necesita unas botas disponibles");

        new ProductDetailPage(driver).clickAddToBagWithoutSelectingOptions();   // sin talla

        cart.open();
        Assert.assertEquals(cart.itemCount(), 0,
                "No debe agregarse un producto con variante obligatoria sin seleccionarla");
    }

    @Test(description = "NEG-04: con la bolsa vacía se muestra el estado vacío y no se puede comprar")
    public void emptyBagCannotCheckout() {
        CartPage cart = new CartPage(driver);
        cart.clearBag();

        Assert.assertTrue(cart.isEmpty(), "Debe mostrarse el mensaje de bolsa vacía");
        Assert.assertFalse(cart.isBuyNowVisible(), "'Ir a comprar' no debe estar disponible con la bolsa vacía");
    }
}
