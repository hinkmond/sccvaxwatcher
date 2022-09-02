package com.hinkmond;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.List;

public class SCCVaxWatcher {
    // Expected labels in question
    String[] answerLabels = {
            "I would like to schedule a 1st dose of the COVID-19 vaccine",
            "I would like to schedule a 2nd dose of the Moderna, Novavax or Pfizer COVID-19 vaccine",
            "I would like to schedule a COVID-19 BOOSTER shot.*",
            "I am moderately to severely immunocompromised and I would like to schedule an additional dose to complete my initial series.**"
    };

    public void watchForVaccine() {
        ChromeOptions chrome_options = new ChromeOptions();
        chrome_options.addArguments("--window-size=860,620", "--window-position=0,0");
        WebDriver driver = new ChromeDriver(chrome_options);

        // Create ChromeDriver window
        driver.manage().window();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        //open browser with desried URL
        driver.get("https://vax.sccgov.org/");
        WebElement covidCheckBox = driver.findElement(By.id("EligibilityFormAnswer287"));
        covidCheckBox.click();

        WebElement covidAnswers = driver.findElement(By.id("EligibilityFormQuestion35"));
        List<WebElement> children = covidAnswers.findElements(By.className("scc-answer-container"));
        System.err.println("Number of children = " + children.size());

        int count = 0;
        for (WebElement child : children) {
            String labelText = child.getText();
            System.err.println("child: " + child.getText());
            boolean checkLabel = labelText.equals(answerLabels[count++]);
            System.err.println("... checkLabel: " + checkLabel);
        }

        // Close the browser
        //driver.close();
    }

    public static void main(String[] args) {
        SCCVaxWatcher SCCVaxWatcher = new SCCVaxWatcher();
        SCCVaxWatcher.watchForVaccine();
    }
}