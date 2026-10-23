package com.liverpool.qa.tests;

import com.liverpool.qa.config.Config;
import com.liverpool.qa.pages.HomePage;
import com.liverpool.qa.pages.LoginPage;
import com.liverpool.qa.session.SessionCookies;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

public abstract class LoggedInBaseTest extends BaseTest {
    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUp")
    public void startSession() {
        if (Config.hasCookies()) {
            int added = SessionCookies.apply(driver);
            Assert.assertTrue(added > 0, "No se pudo agregar ninguna cookie; revisa el formato del JSON");
            Assert.assertFalse(new HomePage(driver).isLoggedOut(),
                    "Las cookies se agregaron (" + added + ") pero el sitio sigue sin sesión: "
                            + "probablemente expiraron; exporta cookies nuevas");
            return;
        }
        if (Config.USER.isBlank() || Config.PASSWORD.isBlank()) {
            throw new SkipException("Define COOKIES_FILE/COOKIES_JSON o LIVERPOOL_USER y LIVERPOOL_PASSWORD");
        }
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(Config.USER, Config.PASSWORD);
        Assert.assertTrue(loginPage.isLoggedIn(), "No se pudo iniciar sesión con las credenciales dadas");
    }
}
