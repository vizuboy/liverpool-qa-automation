package com.liverpool.qa.tests;

import com.liverpool.qa.flows.ShoppingFlow;
import com.liverpool.qa.model.Category;
import com.liverpool.qa.pages.CartPage;
import com.liverpool.qa.pages.CheckoutPage;
import com.liverpool.qa.pages.HomePage;
import com.liverpool.qa.pages.ProductListPage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Casos FUNCIONALES: el flujo que pide el examen. */
public class ShoppingFlowTests extends LoggedInBaseTest {

    @DataProvider(name = "categories")
    public Object[][] categories() {
        Category[] all = Category.values();
        Object[][] data = new Object[all.length][1];
        for (int i = 0; i < all.length; i++) {
            data[i][0] = all[i];
        }
        return data;
    }

    @Test(dataProvider = "categories",
          description = "FUNC-01: cada categoría/producto del examen es navegable y lista productos")
    public void navigatesToCategoryAndListsProducts(Category category) {
        ProductListPage list = new HomePage(driver).goToCategory(category);

        Assert.assertTrue(list.hasResults(15), "No hay productos listados para: " + category);
        Assert.assertFalse(list.productUrls(1).isEmpty(), "Sin links a productos para: " + category);
    }

    @Test(description = "FUNC-02: agrega los 6 productos a la bolsa e ir a comprar ahora llega al checkout")
    public void addsAllProductsToBagAndGoesToCheckout() {
        CartPage cart = new CartPage(driver);
        cart.clearBag();                                    // precondición: bolsa limpia

        ShoppingFlow flow = new ShoppingFlow(driver);
        List<String> addedTitles = new ArrayList<>();
        for (Category category : Category.values()) {
            Optional<String> title = flow.addFirstAvailableProductOf(category);
            Assert.assertTrue(title.isPresent(), "No hay un producto disponible para agregar de: " + category);
            addedTitles.add(title.get());
        }

        Assert.assertEquals(new HashSet<>(addedTitles).size(), addedTitles.size(),
                "Dos categorías terminaron en el mismo producto (¿se leyeron resultados de la página anterior?): "
                        + addedTitles);

        cart.open();
        Assert.assertEquals(cart.itemCount(), Category.values().length,
                "La bolsa debe contener un producto por categoría. Agregados: " + addedTitles
                        + " | En la bolsa: " + cart.itemSummaries());

        CheckoutPage checkout = cart.clickBuyNow();
        Assert.assertTrue(checkout.isLoaded(),
                "'Ir a comprar ahora' no llevó al checkout. URL actual: " + driver.getCurrentUrl());
        // El flujo se detiene aquí a propósito: no se paga ni se generan pedidos reales.
    }
}
