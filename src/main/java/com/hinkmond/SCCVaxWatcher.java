package com.hinkmond;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class SCCVaxWatcher {
    // Expected labels in question
    String[] answerLabels = {
            "I would like to schedule a 1st dose of the COVID-19 vaccine",
            "I would like to schedule a 2nd dose of the Moderna, Novavax or Pfizer COVID-19 vaccine",
            "I would like to schedule a COVID-19 BOOSTER shot.*",
            "I am moderately to severely immunocompromised and I would like to schedule an additional dose to complete my initial series.**"
    };

    String msmtpMailSubj = "printf 'Subject: TEST (Please delete)\\n\\n";
    String msmtpMailCmd = "' | /opt/homebrew/bin/msmtp -a default thewongs@lyriad.com 4087182577@txt.att.net";

    /////
    // Mail settings
    /////
    // Recipient's email ID needs to be mentioned.
    String to = "hinkmond@lyriad.com";
    // Sender's email ID needs to be mentioned
    String from = "sccvaxbot@lyriad.com";
    // Assuming you are sending email from localhost
    String host = "localhost";
    // Get system properties
    Properties properties = System.getProperties();

    // Get the default Session object.
    Session session = Session.getDefaultInstance(properties);

    public void watchForVaccine() {
        ChromeOptions chrome_options = new ChromeOptions();
        chrome_options.addArguments("--headless");
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
        boolean diffDetected = false;
        for (WebElement child : children) {
            String labelText = child.getText();
            System.err.println("child: " + child.getText());
            boolean checkLabel = labelText.equals(answerLabels[count++]);
            //System.err.println("... checkLabel: " + checkLabel);
            if (!checkLabel) {
                diffDetected = true;
            }
        }
        System.err.println("diff detected: " + diffDetected);

        //diffDetected = true;
        if (diffDetected) {
            sendMail("VAX choices have changed.");
        }

        // Close the browser
        driver.close();
    }

    public void sendMail(String msgParam) {
        String msg = Optional.ofNullable(msgParam).orElse("Default message");

        // Setup mail server
        properties.setProperty("mail.smtp.host", host);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            message.setSubject("This is the Subject Line!");

            // Now set the actual message
            message.setText(msg);

            // Send message
            //Transport.send(message);
            Runtime rt = Runtime.getRuntime();
            String execStr = msmtpMailSubj + msg + msmtpMailCmd;
            //String[] cmd = { "/bin/sh", "-c", "ps -ef | grep export" };
            String[] cmd = { "/bin/sh", "-c", execStr};
            try {
                Process pr = rt.exec(cmd);
                BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                String line;
                while((line=input.readLine()) != null) {
                    System.out.println(line);
                }

                int exitVal = pr.waitFor();
                System.out.println("Exited with error code "+exitVal);

            } catch (IOException | InterruptedException exception) {
                exception.printStackTrace();
            }
            System.err.println("Sent message successfully: " + execStr);
        } catch (MessagingException mEx) {
            mEx.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SCCVaxWatcher SCCVaxWatcher = new SCCVaxWatcher();
        SCCVaxWatcher.watchForVaccine();
    }
}