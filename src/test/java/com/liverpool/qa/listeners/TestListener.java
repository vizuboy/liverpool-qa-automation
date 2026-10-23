package com.liverpool.qa.listeners;

import com.liverpool.qa.driver.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestListener implements ITestListener {
    @Override
    public void onTestFailure(ITestResult result) {
        WebDriver driver = DriverFactory.get();
        if (!(driver instanceof TakesScreenshot)) return;
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path dir = Paths.get("target", "screenshots");
            Files.createDirectories(dir);
            String name = result.getTestClass().getRealClass().getSimpleName() + "."
                    + result.getMethod().getMethodName() + "-" + System.currentTimeMillis() + ".png";
            Files.write(dir.resolve(name), png);
        } catch (IOException | RuntimeException e) {
            System.err.println("No se pudo guardar la captura: " + e.getMessage());
        }
    }
}
