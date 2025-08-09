import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SpellingPracticeApp extends JFrame {
    private static final String WORDS_FILE = "spelling_words.csv";
    private static final String MISTAKES_FILE = "spelling_mistakes.txt";
    private static final String PROGRESS_FILE = "spelling_progress.txt";
    
    private List<String> allWords = new ArrayList<>();
    private List<String> practiceWords = new ArrayList<>();
    private Map<String, Integer> mistakeCount = new HashMap<>();
    private Map<String, List<String>> mistakeDetails = new HashMap<>();
    private String currentWord;
    private Random random = new Random();
    
    // UI Components
    private JLabel wordLabel;
    private JButton correctButton;
    private JButton wrongButton;
    private JButton showStatsButton;
    private JLabel statsLabel;
    private JLabel sessionStatsLabel;
    private int sessionCorrect = 0;
    private int sessionWrong = 0;
    
    public SpellingPracticeApp() {
        setTitle("Spelling Practice");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        initializeData();
        createUI();
        loadNextWord();
        
        setSize(400, 300);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void initializeData() {
        loadWords();
        loadProgress();
        preparePracticeList();
    }
    
    private void loadWords() {
        try {
            File file = new File(WORDS_FILE);
            if (!file.exists()) {
                // Create sample CSV if it doesn't exist
                createSampleCSV();
            }
            
            List<String> lines = Files.readAllLines(Paths.get(WORDS_FILE));
            for (String line : lines) {
                String word = line.trim();
                if (!word.isEmpty()) {
                    allWords.add(word);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading words file: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void createSampleCSV() {
        String[] sampleWords = {
            "cat", "dog", "house", "tree", "book", 
            "water", "friend", "school", "happy", "color",
            "family", "beautiful", "because", "through", "enough"
        };
        
        try {
            Files.write(Paths.get(WORDS_FILE), 
                Arrays.asList(sampleWords), 
                StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadProgress() {
        File progressFile = new File(PROGRESS_FILE);
        if (!progressFile.exists()) {
            return;
        }
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(PROGRESS_FILE));
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String word = parts[0].trim();
                    int count = Integer.parseInt(parts[1].trim());
                    mistakeCount.put(word, count);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveProgress() {
        try {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : mistakeCount.entrySet()) {
                lines.add(entry.getKey() + ":" + entry.getValue());
            }
            Files.write(Paths.get(PROGRESS_FILE), lines, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void preparePracticeList() {
        // First add words that were spelled wrong before (prioritized by mistake count)
        List<String> mistakeWords = mistakeCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .filter(allWords::contains)
            .collect(Collectors.toList());
        
        practiceWords.addAll(mistakeWords);
        
        // Then add words that haven't been practiced or were correct
        for (String word : allWords) {
            if (!practiceWords.contains(word)) {
                practiceWords.add(word);
            }
        }
        
        // If no words available, use all words
        if (practiceWords.isEmpty()) {
            practiceWords.addAll(allWords);
        }
    }
    
    private void createUI() {
        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Word display
        wordLabel = new JLabel("", SwingConstants.CENTER);
        wordLabel.setFont(new Font("Arial", Font.BOLD, 36));
        wordLabel.setPreferredSize(new Dimension(300, 60));
        wordLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(wordLabel, gbc);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        correctButton = new JButton("✓ Correct");
        correctButton.setFont(new Font("Arial", Font.PLAIN, 18));
        correctButton.setBackground(new Color(144, 238, 144));
        correctButton.addActionListener(this::handleCorrect);
        buttonPanel.add(correctButton);
        
        wrongButton = new JButton("✗ Wrong");
        wrongButton.setFont(new Font("Arial", Font.PLAIN, 18));
        wrongButton.setBackground(new Color(255, 182, 193));
        wrongButton.addActionListener(this::handleWrong);
        buttonPanel.add(wrongButton);
        
        gbc.gridy = 1;
        mainPanel.add(buttonPanel, gbc);
        
        // Session statistics
        sessionStatsLabel = new JLabel("Session: Correct: 0 | Wrong: 0");
        sessionStatsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridy = 2;
        mainPanel.add(sessionStatsLabel, gbc);
        
        // Statistics label
        statsLabel = new JLabel("");
        statsLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        gbc.gridy = 3;
        mainPanel.add(statsLabel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Bottom panel with additional buttons
        JPanel bottomPanel = new JPanel(new FlowLayout());
        
        showStatsButton = new JButton("Show Statistics");
        showStatsButton.addActionListener(e -> showStatistics());
        bottomPanel.add(showStatsButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void loadNextWord() {
        if (practiceWords.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No words available to practice!", 
                "No Words", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Weighted selection - words with more mistakes are more likely to be selected
        currentWord = selectWeightedWord();
        wordLabel.setText(currentWord);
        
        // Update stats label
        Integer mistakes = mistakeCount.get(currentWord);
        if (mistakes != null && mistakes > 0) {
            statsLabel.setText("This word was misspelled " + mistakes + " time(s) before");
        } else {
            statsLabel.setText("First time practicing this word");
        }
    }
    
    private String selectWeightedWord() {
        // Create weighted list based on mistake count
        List<String> weightedList = new ArrayList<>();
        
        for (String word : practiceWords) {
            int weight = mistakeCount.getOrDefault(word, 0) + 1;
            for (int i = 0; i < weight; i++) {
                weightedList.add(word);
            }
        }
        
        return weightedList.get(random.nextInt(weightedList.size()));
    }
    
    private void handleCorrect(ActionEvent e) {
        sessionCorrect++;
        updateSessionStats();
        loadNextWord();
    }
    
    private void handleWrong(ActionEvent e) {
        // Show dialog to enter the mistake
        JDialog dialog = new JDialog(this, "Record Mistake", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel label = new JLabel("How was '" + currentWord + "' spelled?");
        panel.add(label, BorderLayout.NORTH);
        
        JTextArea textArea = new JTextArea(3, 20);
        textArea.setFont(new Font("Arial", Font.PLAIN, 16));
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton saveButton = new JButton("Save & Continue");
        saveButton.addActionListener(ev -> {
            String mistake = textArea.getText().trim();
            if (!mistake.isEmpty()) {
                recordMistake(currentWord, mistake);
            }
            sessionWrong++;
            updateSessionStats();
            dialog.dispose();
            loadNextWord();
        });
        
        panel.add(saveButton, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void updateSessionStats() {
        sessionStatsLabel.setText(String.format("Session: Correct: %d | Wrong: %d", 
            sessionCorrect, sessionWrong));
    }
    
    private void recordMistake(String word, String mistake) {
        // Update mistake count
        mistakeCount.put(word, mistakeCount.getOrDefault(word, 0) + 1);
        
        // Record mistake details
        mistakeDetails.computeIfAbsent(word, k -> new ArrayList<>()).add(mistake);
        
        // Save to file
        try {
            String entry = String.format("%s | Correct: %s | Mistake: %s | Time: %s%n", 
                new Date(), word, mistake, System.currentTimeMillis());
            Files.write(Paths.get(MISTAKES_FILE), 
                entry.getBytes(), 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        // Save progress
        saveProgress();
        
        // Re-prepare practice list to update weights
        preparePracticeList();
    }
    
    private void showStatistics() {
        JDialog dialog = new JDialog(this, "Spelling Statistics", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        // Create table model
        String[] columnNames = {"Word", "Times Wrong", "Recent Mistakes"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Populate table with data
        List<Map.Entry<String, Integer>> sortedEntries = mistakeCount.entrySet().stream()
            .sorted((e1, e2) -> {
                int cmp = e2.getValue().compareTo(e1.getValue());
                if (cmp == 0) {
                    return e1.getKey().compareTo(e2.getKey());
                }
                return cmp;
            })
            .collect(Collectors.toList());
        
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String word = entry.getKey();
            Integer count = entry.getValue();
            List<String> mistakes = mistakeDetails.get(word);
            String recentMistakes = mistakes != null && !mistakes.isEmpty() 
                ? mistakes.get(mistakes.size() - 1) 
                : "";
            model.addRow(new Object[]{word, count, recentMistakes});
        }
        
        // Add words with no mistakes
        for (String word : allWords) {
            if (!mistakeCount.containsKey(word)) {
                model.addRow(new Object[]{word, 0, ""});
            }
        }
        
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(table);
        
        dialog.add(scrollPane, BorderLayout.CENTER);
        
        // Add close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SpellingPracticeApp());
    }
}