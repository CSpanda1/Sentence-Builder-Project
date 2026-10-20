package importer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class BookImporter
{
    public static void main(String[] args)
    {
        BookImporter importer = new BookImporter();

        //1. Read book
        File bookFile = new File("pg11.txt");
        StringBuilder bookText = new StringBuilder();

        try(Scanner reader = new Scanner(bookFile))
        {
            // tracking for reading lines
            int lineRead = 0;

            boolean skipChapterTitle = false;

            while(reader.hasNextLine())
            {
                String line = reader.nextLine();

                // Skip line if it contains * and whitespace
                if (line.matches("[\\s*]+") && line.contains("*"))
                {
                    continue;
                }

                // Skip the chapter headings such as CHAPTER I
                if (line.trim().matches("(?i)CHAPTER\\s+[IVXLCDM]+\\."))
                {
                    skipChapterTitle = true;
                    continue;
                }

                // Skip chapter names in the book
                if (line.trim().equalsIgnoreCase("Down the Rabbit-Hole") || line.trim().equalsIgnoreCase("The Pool of Tears") || line.trim().equalsIgnoreCase("A Caucus-Race and a Long Tale") ||
                        line.trim().equalsIgnoreCase("The Rabbit Sends in a Little Bill") || line.trim().equalsIgnoreCase("Advice from a Caterpillar") || line.trim().equalsIgnoreCase("Pig and Pepper") || line.trim().equalsIgnoreCase("A Mad Tea-Party") || line.trim().equalsIgnoreCase("The Queen’s Croquet-Ground") ||
                        line.trim().equalsIgnoreCase("The Mock Turtle’s Story") || line.trim().equalsIgnoreCase("The Lobster Quadrille") || line.trim().equalsIgnoreCase("Who Stole the Tarts?") || line.trim().equalsIgnoreCase("Alice’s Evidence"))
                {
                    continue;
                }

                // Store the line in the StringBuilder
                bookText.append(line).append("\n");

                // Increment the line read counter
                lineRead++;

                // Progress message for every 500 lines read
                if (lineRead % 500 == 0)
                {
                    System.out.println("Read " + lineRead + " lines...");
                }
            }

            System.out.println("Finished reading the book");

        }
        catch (FileNotFoundException e)
        {
            System.err.println("Book file not found: " + e.getMessage());
            return;
        }

        // 2. Extract sentences and words
        String text = bookText.toString();

        // Read the first sentence from the book
        String firstSentence ="Alice was beginning to get very tired of sitting";
        // Read end of story
        String endOfStory = "THE END";

        // Find the index of the first sentence and the end of the story
        int startIndex = text.indexOf(firstSentence);
        int endIndex = text.indexOf(endOfStory);

        // Check if the start and end indices are valid
        if(startIndex == -1 || endIndex == -1 || startIndex >= endIndex)
        {
            System.out.println("Could not find the specified sentences in the text.");
            return;
        }


        // Extract sentences
        String[] sentences = importer.extractSentences(text, startIndex, endIndex);

        // Extract words
        String[] words = importer.extractWords(text, startIndex, endIndex);

        for(int i = 0; i < Math.min(80, sentences.length); i++)
        {
            System.out.println("Sentence " + (i + 1) + ": " + sentences[i].replaceAll("\\s+", " "));
        }

        // Display the first 20 words
        for(int i = 0; i < Math.min(80, words.length); i++) {
            System.out.println("Word " + (i + 1) + ": " + words[i]);
        }

        // Display results
        System.out.println("Total sentence segments: " + sentences.length);
        System.out.println("Total words: " + words.length);



        // 3. Calculate statistics

        System.out.println("Connecting to MySQL...");

        // 4. Open connection
        // 5. Insert data using JDBC
        // 6. Commit transaction and close connection

        System.out.println("Book import completed!");


    }

    /**
     * Extracts sentences from the given text between the specified start and end indices.
     * @param text The text to extract sentences from.
     * @param startIndex The start index of the text to extract.
     * @param endIndex The end index of the text to extract.
     * @return An array of sentences.
     */
    public String[] extractSentences(String text, int startIndex, int endIndex)
    {
        // Extract the relevant portion of the text
        String relevantText = text.substring(startIndex, endIndex).trim();


        // Remove underscores in the sentence
        relevantText = relevantText.replaceAll("(?<!\\w)_(?=\\w)", "").replaceAll("(?<=\\w)_(?!\\w)", "");

        // Split the story into sentences using regex to handle punctuation and quotes
        String[] sentences = relevantText.split("(?<=[.!?][”\"'])\\s+|(?<=[.!?])\\s+");

        return sentences;


    }

    /**
     * Extracts words from the given text between the specified start and end indices.
     * @param text The text to extract words from.
     * @param startIndex The start index of the text to extract.
     * @param endIndex The end index of the text to extract.
     * @return An array of words.
     */
    public String[] extractWords(String text, int startIndex, int endIndex)
    {

        // Extract the relevant portion of the text
        String relevantText = text.substring(startIndex, endIndex).trim();

        // Remove underscores in the sentence
        relevantText = relevantText.replaceAll("(?<!\\w)_(?=\\w)", "").replaceAll("(?<=\\w)_(?!\\w)", "");

        // Normalize curly apostrophes
        relevantText = relevantText.replaceAll("[‘’]", "'");

        // Convert to lowercase
        relevantText = relevantText.toLowerCase();

        // Extract words
        String[] words = relevantText.split("[^\\p{L}\\p{N}']+");

        return words;

    }


}


