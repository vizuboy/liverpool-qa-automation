package com.liverpool.qa.tests;

import com.liverpool.qa.pages.HomePage;
import com.liverpool.qa.pages.LoginPage;
import com.liverpool.qa.pages.ProductListPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Casos NEGATIVOS que no requieren sesión (se ejecutan con un navegador limpio). */
public class GuestNegativeTests extends BaseTest {

    @Test(description = "NEG-01: login con credenciales inválidas muestra error y no inicia sesión")
    public void invalidCredentialsAreRejected() {
        LoginPage login = new LoginPage(driver);
        login.login("usuario.invalido+qa@example.com", "ContraseñaIncorrecta#123");

        Assert.assertTrue(login.hasLoginError(), "Debe mostrarse un mensaje de error de autenticación");
        Assert.assertTrue(login.isStillOnLoginPage(5), "No debe haber iniciado sesión con credenciales inválidas");
    }

    @Test(description = "NEG-02: búsqueda sin coincidencias muestra estado vacío y no productos")
    public void searchWithoutMatchesShowsEmptyState() {
        ProductListPage results = new HomePage(driver).open().search("zzqxwv9876kjh");

        Assert.assertTrue(results.showsNoResultsMessage(), "Debe mostrarse el mensaje de 'sin resultados'");
        Assert.assertFalse(results.hasResults(3), "No debería listar productos para un término sin sentido");
    }
}
