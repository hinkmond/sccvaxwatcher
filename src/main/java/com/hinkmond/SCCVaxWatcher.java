package com.hinkmond;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SCCVaxWatcher {
    final static int WORD_LENGTH = 5;
    final static boolean DEBUG = false;
    String[] correctArray = new String[WORD_LENGTH];
    HashSet<String> presentMap = new HashSet<>();
    HashSet<String> absentMap = new HashSet<>();
    HashSet<String>  guessedMap = new HashSet<>();
    ArrayList<String> wordsList = new ArrayList<>();
    final int maxScore[] = {0};
    Map<String, Integer> scoreMap = null;

    public void solvePuzzle() {
        ChromeOptions chrome_options = new ChromeOptions();
        chrome_options.addArguments("--window-size=860,620", "--window-position=0,0");
        WebDriver driver = new ChromeDriver(chrome_options);
        WebElement closeIcon;
        WebElement keyEnter;
        WebElement keyboard;

        InputStream ioStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("sgb-words.txt");

        if (ioStream == null) {
            throw new IllegalArgumentException("sgb-words.txt" + " is not found");
        }

        // Create Map of 5-letter words
        BufferedReader br = new BufferedReader(new InputStreamReader(ioStream));
        try {
            String word;
            while ((word = br.readLine()) != null) {
                wordsList.add(word);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        // Create ChromeDriver window
        driver.manage().window();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));

        //open browser with desried URL
        driver.get("https://www.nytimes.com/games/wordle/index.html");

        //document.querySelector("body > game-app").shadowRoot.querySelector("#game > game-modal")
        //   .shadowRoot.querySelector("div > div > div > game-icon").shadowRoot.querySelector("svg")
        WebElement shadowHostGameApp = driver.findElement(By.cssSelector("body > game-app"));
        SearchContext shadowRootGameApp = shadowHostGameApp.getShadowRoot();

        // Get the close icon from the nested shadow roots under the game-app shadow root
        WebElement shadowHostGameModal = shadowRootGameApp.findElement(By.cssSelector("#game > game-modal"));
        SearchContext shadowRootGameModal = shadowHostGameModal.getShadowRoot();
        WebElement shadowHostGameIcon = shadowRootGameModal.findElement(By.cssSelector("div > div > div > game-icon"));
        SearchContext shadowRootGameIcon = shadowHostGameIcon.getShadowRoot();
        closeIcon = shadowRootGameIcon.findElement(By.cssSelector("svg"));
        closeIcon.click();

        // document.querySelector("body > game-app").shadowRoot.querySelector("#game > game-keyboard")
        WebElement shadowHostGameKeyboard = shadowRootGameApp.findElement(By.cssSelector("#game > game-keyboard"));
        SearchContext shadowRootGameKeyboard = shadowHostGameKeyboard.getShadowRoot();

        // document.querySelector("body > game-app").shadowRoot.querySelector("#game > game-keyboard")
        //   .shadowRoot.querySelector("#keyboard > div:nth-child(1) > button:nth-child(1)")
        keyboard = shadowRootGameKeyboard
                .findElement(By.cssSelector("#keyboard > div:nth-child(1) > button:nth-child(1)"));
        keyEnter = shadowRootGameKeyboard
                .findElement(By.cssSelector("#keyboard > div:nth-child(3) > button:nth-child(1)"));

        String[] firstChoices =
                {"AISLE", "TEARS", "STALE", "SLIME", "STORE"};
                //{"AISLE", "TEARS", "REALS", "STALE", "SLIME", "STARE", "STORE"};
                //{"SLIME"};
        Random random = new Random();
        String nextGuess = firstChoices[random.nextInt(firstChoices.length)];
        String prevWord = null;

        for (int i=0; i<6; i++) {
            if (nextGuess != null) {
                System.out.println((i+1) + ". W.I.G. Algorithm Best Next Guess: " + nextGuess.toUpperCase() +
                        ", score: " + maxScore[0] + ", previous correctArray size: " + getNumCorrect(nextGuess, correctArray));
                keyboard.sendKeys(nextGuess);
                keyEnter.click();
                waitForTileAnimation(driver, shadowRootGameApp);
                nextGuess = getNextGuess(driver, shadowRootGameApp, i + 1);
                if (getNumCorrect(prevWord, correctArray) == 5) {
                    System.out.println("CORRECT!");
                    break;
                } else {
                    // Print top five guesses
                    System.out.print("   Top 5 guesses: ");

                    List<Map.Entry<String, Integer>> scoreList = new ArrayList<>(scoreMap.entrySet());
                    scoreList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
                    List<Map.Entry<String, Integer>> topList = scoreList.stream().limit(5)
                            .collect(Collectors.toList());

                    int count = 0;
                    for (Map.Entry<String, Integer> entry : topList) {
                        if (count > 0) {
                            System.out.print(", ");
                        }
                        System.out.print(entry.getKey() + ": " + entry.getValue());
                        count++;
                    }
                    System.out.println();
                }
                prevWord = nextGuess;
            }
        }
        // Close the browser
        //driver.close();
    }

    public String getNextGuess(WebDriver driver, SearchContext shadowRootGameApp, int currentRowNum) {
        // document.querySelector("body > game-app").shadowRoot.querySelector("#board > game-row:nth-child(1)")
        WebElement shadowHostGameRow;
        String rowCounterStr = String.valueOf(currentRowNum);
        shadowHostGameRow = shadowRootGameApp
                .findElement(By.cssSelector("#board > game-row:nth-child(" + rowCounterStr + ")"));

        String gameRowLettersStr = shadowHostGameRow.getDomProperty("_letters");
        List<Character> gameRowLettersList =
                gameRowLettersStr.chars().mapToObj(e -> (char) e).collect(Collectors.toList());

        String gameRowEvalStr = shadowHostGameRow.getDomProperty("_evaluation");
        gameRowEvalStr = gameRowEvalStr.substring(1, gameRowEvalStr.length() - 1);
        ArrayList<String> gameRowEvalList = new ArrayList<>(Arrays.asList(gameRowEvalStr.split(", ")));

        final String maxWord[] = {null};
        final int correctScore[] = {0};
        int evalPosition = 0;
        for (String evaluation: gameRowEvalList) {
            if (!gameRowLettersList.isEmpty()) {
                String letter = String.valueOf(gameRowLettersList.get(evalPosition));
                switch (evaluation) {
                    case "absent":
                        long count = gameRowLettersStr.codePoints().filter(ch -> ch == letter.charAt(0)).count();
                        if (count < 2) {
                            absentMap.add(letter);
                        }
                        break;
                    case "present":
                        presentMap.add(letter);
                        break;
                    case "correct":
                        correctArray[Integer.valueOf(evalPosition)] = letter;
                        break;
                    default:
                        break;
                }
            }
            evalPosition++;
        };

        maxScore[0] = 0;
        scoreMap = new HashMap<>();
        wordsList.forEach(word -> {
            if ((!guessedMap.contains(word)) && (getNumCorrect(word, correctArray) != 5)) {
                // Check for any absent letters
                int absentScore = 0;
                for (Character letterChar : word.toCharArray()) {
                    String letterString = String.valueOf(letterChar);
                    if (absentMap.contains(letterString)) {
                        absentScore += 9;
                    }
                }

                /////
                // Adjustments

                // Temporary word string to char array
                char[] wordCharArray = word.toCharArray();

                // Number of repeated letters in the new guess
                int repeatedLetters = 0;
                Set<String> lettersHashSet = new LinkedHashSet<>();
                for (int i = 0; i < wordCharArray.length; i++) {
                    String wordLetter = String.valueOf(wordCharArray[i]);
                    if (!lettersHashSet.contains(wordLetter)) {
                        lettersHashSet.add(wordLetter);
                    }
                }
                repeatedLetters = (5 - lettersHashSet.size()) * 2;

                // Number of same letters in previous guess as in the new guess
                int sameLetters = 0;
                for (int i=0; i<wordCharArray.length; i++) {
                    if (word.substring(i, i+1).equals(gameRowLettersStr.substring(i, i+1))) {
                        sameLetters++;
                    }
                }

                // Score for correct letters
                correctScore[0] = 0;
                int correctPosition = 0;
                for (Character wordChar : word.toCharArray()) {
                    String wordLetter = String.valueOf(wordChar);
                    if (wordLetter.equals(correctArray[correctPosition])) {
                        correctScore[0] += 33;
                    }
                    correctPosition++;
                }

                // Reduce score of new guess by number of repeated letters
                correctScore[0] -= (repeatedLetters * 8);


                // Reduce score of new guess by number of same letters as last time
                if (sameLetters > 0) {
                    correctScore[0] -= sameLetters;
                }

                if ((correctScore[0] - absentScore) > maxScore[0]  && !guessedMap.contains(maxWord)) {
                    maxScore[0] = (correctScore[0] - absentScore);
                    maxWord[0] = word;
                }

                /////
                // Calculate score for this new guess

                // Score for present letters in the new guess
                int presentScore = 1;
                for (int i=0; i<wordCharArray.length; i++) {
                    String wordLetter = String.valueOf(wordCharArray[i]);
                    if (presentMap.contains(wordLetter)) {
                        presentScore += 29;
                    }
                    // Reduce score if same letter was tried before and is not the correct letter in this position
                    if ((i == gameRowLettersStr.indexOf(wordLetter)) &&
                            (!wordLetter.equals(correctArray[i]))) {
                        presentScore--;
                    }
                }

                // Reduce score of new guess by number of repeated letters
                presentScore -= (repeatedLetters * 14);

                // Reduce score of new guess by number of same letters as last time
                if (sameLetters > 0) {
                    presentScore -= sameLetters;
                }

                // If new max score, then record this as the best guess
                if ((presentScore - absentScore) > maxScore[0] && !guessedMap.contains(maxWord)) {
                    maxScore[0] = (presentScore - absentScore);
                    maxWord[0] = word;
                }

                int score = (correctScore[0] > presentScore) ?
                        (correctScore[0] - absentScore) : (presentScore - absentScore);
                scoreMap.put(word, score);

                if (DEBUG) {
                    System.out.println("guess: " + word + ", present: " + presentScore + ", absent: " +
                            absentScore + ", score: " + (presentScore - absentScore) + ", correct: " +
                            correctScore[0] + ", repeatedLetters: " + repeatedLetters + ", sameLetter: " +
                            sameLetters);
                }

            }
        });

        guessedMap.add(maxWord[0]);
        return maxWord[0];
    }

    public void waitForTileAnimation(WebDriver driver, SearchContext shadowRootGameApp) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement shadowHostGameRow;
        SearchContext shadowRootGanmeRow;
        WebElement tile;
        String rowCounterStr;
        for (int rowCounter = 1; rowCounter < 7; rowCounter++) {
            rowCounterStr = String.valueOf(rowCounter);
            shadowHostGameRow = shadowRootGameApp
                    .findElement(By.cssSelector("#board > game-row:nth-child(" + rowCounterStr + ")"));
            shadowRootGanmeRow = shadowHostGameRow.getShadowRoot();

            String colCounterStr;
            for (int colCounter = 1; colCounter < 6; colCounter++) {
                colCounterStr = String.valueOf(colCounter);
                tile = shadowRootGanmeRow
                        .findElement(By.cssSelector("div > game-tile:nth-child(" + colCounterStr + ")"));
                wait.until(ExpectedConditions.domPropertyToBe(tile, "_animation", "idle"));
            }
        }
    }

    public int getNumCorrect(String guessWord, String[] correctArray) {
        int numCorrect = 0;
        for (int i=0; i<correctArray.length; i++) {
            if ((correctArray[i] != null) && (guessWord != null) &&
                    (guessWord.substring(i, i+1).equals(correctArray[i]))) {
                numCorrect++;
            }
        }
        return numCorrect;
    }

    public static void main(String[] args) {
        SCCVaxWatcher SCCVaxWatcher = new SCCVaxWatcher();
        SCCVaxWatcher.solvePuzzle();
    }
}