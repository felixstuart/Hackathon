import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static JFrame mainFrame;
    private static int width = 1200;
    private static int height = 700;

    private static int totalStrokes = 0;
    private static int correctStrokes = 0;
    private static JLabel current;
    private static JLabel nextUp;
    private static JLabel last;

    private static JLabel accuracy;

    private static int currentIndex = 0;
    private static String nextToType;
    private static String twoAhead;
    public static void main(String[] args) throws IOException, ParseException, org.json.simple.parser.ParseException {
        String toType = getChatResponse("\"write me an essay about the importance of cheese entirely in spanish. make it at least a thousand characters\"");
        prepareGUI(toType);
    }

//    fixed by chatgpt
    public static String getChatResponse(String prompt) throws IOException, ParseException, org.json.simple.parser.ParseException {
        // Use the correct endpoint for chat completions
        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");
        httpConn.setRequestProperty("Content-Type", "applicatio/json");

        // Replace with your actual API key
        String apiKey = "";
        httpConn.setRequestProperty("Authorization", "Bearer " + apiKey);
        httpConn.setDoOutput(true);
        httpConn.setRequestProperty("Content-Type", "application/json");

        // Build the JSON payload using JSON-simple
        JSONObject jsonBody = new JSONObject();
        // Use a proper model identifier (adjust as needed)
        jsonBody.put("model", "gpt-4o");

        // Create the messages array and add our user message
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        jsonBody.put("messages", messages);

        // Write the JSON payload to the connection output stream
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
        writer.write(jsonBody.toJSONString());
        writer.flush();
        writer.close();

        // Gt the response stream
        int responseCode = httpConn.getResponseCode();
        InputStream responseStream = (responseCode / 100 == 2)
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner scanner = new Scanner(responseStream).useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        System.out.println(response);

        // Parse the response JSON
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = (JSONObject) parser.parse(response);

        // Try to extract the content from the first choice
        JSONArray choices = (JSONArray) jsonResponse.get("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject firstChoice = (JSONObject) choices.get(0);
            JSONObject messageObject = (JSONObject) firstChoice.get("message");
            if (messageObject != null) {
                String content = (String) messageObject.get("content");
                return content;
            }
        }
        return null;
    }

//    assisted by chatgpt https://chatgpt.com/c/67fd1639-fe54-8001-b5a0-140b27121ec4
    public static void prepareGUI(String typingTarget) throws IOException, ParseException, org.json.simple.parser.ParseException {
        mainFrame = new JFrame("Typing Game");
        mainFrame.setSize(width, height);
        mainFrame.setLayout(new BorderLayout());


        JPanel typingPanel = new JPanel();
        GridLayout gridLayout = new GridLayout();
        gridLayout.setColumns(2);
        typingPanel.setLayout(gridLayout);


        // Use JTextPane instead of JTextField for styled text
        JTextPane textInput = new JTextPane();
        // Optionally set an initial overall foreground color
        textInput.setForeground(Color.BLACK);

        JTextArea toType = new JTextArea(typingTarget);
        toType.setEditable(false);
        toType.setLineWrap(true);

        if (currentIndex + 1 <= typingTarget.length()) {
            nextToType = typingTarget.substring(currentIndex, currentIndex + 1);
        } else {
            nextToType = typingTarget.substring(currentIndex);
        }

        StyledDocument doc = textInput.getStyledDocument();


        Font f = new Font("sans serif", Font.PLAIN, 40);
        Map attributes = f.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        f = f.deriveFont(attributes);
         nextUp = new JLabel(twoAhead);
         current = new JLabel(nextToType);
         last = new JLabel("");


         nextUp.setForeground(Color.GRAY);
         nextUp.setFont(f);
         last.setForeground(Color.GREEN);
        last.setFont(f);
        Font font = last.getFont();
         current.setForeground(Color.BLUE);
        current.setFont(f);


//        create a set of panels to have a mini-map
        JPanel minimap = new JPanel();

        minimap.setLayout(new GridLayout(2,1));

        JLabel minimapLabel = new JLabel();

        minimapLabel.setText(typingTarget.substring(currentIndex));


        minimap.add(minimapLabel);
        minimap.add(textInput);

        JPanel charPanel = new JPanel();
        GridLayout gridLayout1 = new GridLayout();
        charPanel.setLayout(gridLayout1);
        gridLayout1.setColumns(3);
        gridLayout1.setRows(1);
        charPanel.add(last);
        charPanel.add(current);
        charPanel.add(nextUp);

        textInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        int offset = e.getOffset();
                        int length = e.getLength();  // Could be more than one character

                        // Retrieve the text that was inserted (for debugging or additional logic)
                        String typed = e.getDocument().getText(offset, length);
                        totalStrokes++;
                        if (typed.equals(nextToType)) {
                            correctStrokes++;
                            goToNextChar(typingTarget);
                            minimapLabel.setText(typingTarget.substring(currentIndex));

                            // Create a style and set its foreground color to red
                            Style style = textInput.addStyle("ColoredStyle", null);
                            StyleConstants.setForeground(style, Color.BLUE);

                            // Apply the style to the inserted text
                            doc.setCharacterAttributes(offset, length, style, false);

                            nextUp.setText(twoAhead);
                            current.setText(nextToType);
                        } else {
                            Style style = textInput.addStyle("ColoredStyle", null);
                            StyleConstants.setForeground(style, Color.RED);

                            // Apply the style to the inserted text
                            doc.setCharacterAttributes(offset, length, style, false);
                        }
                        System.out.println(accuracy());
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // Handle removal if needed
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Typically not needed for plain text components
            }
        });

        mainFrame.add(minimap, BorderLayout.SOUTH);
        mainFrame.add(charPanel, BorderLayout.CENTER);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
    }

    private static void goToNextChar(String typingTarget) {
        last.setText(nextToType);
        currentIndex++;
        nextToType = typingTarget.substring(currentIndex, currentIndex+1);
        twoAhead = typingTarget.substring(currentIndex+1, currentIndex+2);
    }

    private static float accuracy() {
        return (float) (correctStrokes / totalStrokes);
    }

}