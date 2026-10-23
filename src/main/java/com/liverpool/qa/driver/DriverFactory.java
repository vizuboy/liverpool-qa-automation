package com.liverpool.qa.driver;

import com.liverpool.qa.config.Config;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

public final class DriverFactory {
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {}

    public static void init() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080", "--disable-notifications", "--lang=es-MX");
        if (Config.HEADLESS) {
            options.addArguments("--headless=new");
        }
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        DRIVER.set(driver);
    }

    public static WebDriver get() {
        return DRIVER.get();
    }

    public static void quit() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}
