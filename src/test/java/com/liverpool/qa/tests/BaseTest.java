package com.liverpool.qa.tests;

import com.liverpool.qa.driver.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public abstract class BaseTest {
    protected WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        DriverFactory.init();
        driver = DriverFactory.get();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        DriverFactory.quit();
    }
}
