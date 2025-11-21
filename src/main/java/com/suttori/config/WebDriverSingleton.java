package com.suttori.config;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class WebDriverSingleton {

    private static WebDriver webDriver;

    public static WebDriver getInstance() {
        if (webDriver == null) {
            ChromeOptions options = new ChromeOptions();
//            options.addArguments("--headless"); // Безголовый режим
//            options.addArguments("--no-sandbox"); // Отключение песочницы (часто нужно в среде с ограниченными правами)
//            options.addArguments("--disable-dev-shm-usage"); // Отключить использование shared memory (помогает в ограниченных системах)
            webDriver = new ChromeDriver(options);
            webDriver.get("https://web.usagi.one");
        }
        return webDriver;
    }

    public static void quitDriver() {
        if (webDriver != null) {
            webDriver.quit();
            webDriver = null;
        }
    }
}
