import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SpellingPracticeApp extends JFrame {
    private static final String PROGRESS_FILE = "spelling_progress.txt";
    private static final String SESSION_HISTORY_FILE = "session_history.txt";
    private static final int WORDS_PER_SESSION = 30;
    private static final int SESSIONS_TO_AVOID = 2;
    
    private List<String> allWords = new ArrayList<>();
    private List<String> sessionWords = new ArrayList<>();
    private Map<String, Integer> mistakeCount = new HashMap<>();
    private List<List<String>> recentSessions = new ArrayList<>();
    private List<String> currentSessionMistakes = new ArrayList<>();
    private String currentWord;
    private int currentWordIndex = 0;
    private Random random = new Random();
    
    // UI Components
    private JLabel wordLabel;
    private JLabel progressLabel;
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
        
        if (!initializeData()) {
            JOptionPane.showMessageDialog(this, 
                "Error initializing application data", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        createUI();
        startNewSession();
        
        setSize(900, 650);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private boolean initializeData() {
        if (!loadWords()) {
            return false;
        }
        loadProgress();
        loadSessionHistory();
        return true;
    }
    
    private boolean loadWords() {
        // Embedded words - no external file needed
        String[] wordsArray = {
            "about", "above", "after", "again", "all", "another", "any", "anyone",
            "away", "are", "back", "baby", "ball", "be", "because", "been",
            "big", "boy", "brother", "but", "by", "called", "came", "can",
            "can't", "cat", "call", "come", "could", "dad", "day", "did",
            "dig", "do", "does", "dog", "don't", "door", "down", "fall",
            "first", "for", "from", "get", "girl", "go", "going", "good",
            "dot", "had", "he", "half", "has", "have", "help", "her",
            "here", "him", "his", "house", "how", "humans", "if", "in",
            "is", "it", "I'm", "I've", "hump", "just", "last", "laugh",
            "little", "lived", "look", "love", "made", "make", "man", "many",
            "may", "me", "more", "much", "mum", "must", "my", "name",
            "new", "next", "night", "no", "not", "now", "of", "old",
            "on", "once", "one", "or", "other", "our", "out", "over",
            "people", "play", "pull", "push", "put", "ran", "said", "school",
            "see", "seen", "she", "should", "sister", "small", "so", "some",
            "son", "tall", "than", "that", "the", "their", "them", "then",
            "there", "these", "they", "this", "three", "through", "to", "too",
            "took", "tree", "two", "up", "us", "very", "wall", "want",
            "was", "washing", "water", "watch", "watches", "way", "we", "went",
            "were", "what", "when", "where", "who", "whole", "why", "will",
            "with", "would", "yes", "you", "your", "you're", "monday", "tuesday",
            "wednesday", "thursday", "friday", "saturday", "sunday", "tick", "tock",
            "clock", "lick", "rock", "back", "snack", "neck", "stick", "duck",
            "thing", "string", "wing", "sing", "pong", "song", "think", "stink",
            "wink", "blink", "link", "pink", "yawn", "dawn", "saw", "raw",
            "law", "straw", "paw", "crawl", "jaw", "claw", "shout", "loud",
            "mouth", "round", "found", "fair", "stair", "hair", "air", "lair",
            "chair", "nice", "smile", "shine", "white", "fine", "hide", "wide",
            "like", "mine", "time", "nurse", "purse", "burn", "turn", "lurk",
            "hurl", "burp", "slurp", "hurt", "tea", "eat", "neat", "real",
            "clean", "please", "leave", "dream", "seat", "scream", "brown", "cow",
            "howl", "town", "crowd", "drown", "gown", "sort", "short", "worn",
            "horse", "sport", "snort", "fork", "goat", "boat", "toad", "oak",
            "road", "cloak", "throat", "roast", "toast", "loaf", "coat", "coal",
            "coach", "blow", "snow", "slow", "show", "know", "glow", "lay",
            "say", "tray", "stray", "green", "keep", "need", "sleep", "feel",
            "poo", "zoo", "mood", "fool", "pool", "stool", "moon", "spoon",
            "start", "car", "bar", "star", "park", "smart", "sharp", "spark",
            "toy", "enjoy", "book", "shook", "cook", "foot", "whirl", "twirl",
            "bird", "third", "dirt", "huge", "brute", "tune", "rude", "mule",
            "use", "june", "dude", "accuse", "excuse", "phone", "home", "hope",
            "rose", "spoke", "note", "broke", "stole", "rope", "those", "cake",
            "shake", "same", "game", "save", "brave", "late", "date", "spoil",
            "join", "coin", "voice", "choice", "noise", "care", "share", "dare",
            "bare", "spare", "scare", "flare", "square", "software", "chew", "stew",
            "new", "flew", "blew", "few", "crew", "newt", "screw", "drew",
            "grew", "better", "letter", "over", "never", "weather", "after", "hamster",
            "litter", "proper", "corner", "sucker", "snail", "rain", "paid", "tail",
            "drain", "paint", "sprain", "chain", "train", "stain", "fire", "hire",
            "wire", "spire", "bonfire", "inspire", "conspire", "hear", "ear", "dear",
            "fear", "gear", "near", "rear", "tear", "year", "spear", "sure",
            "pure", "cure", "picture"
        };
        
        allWords = Arrays.asList(wordsArray);
        
        if (allWords.isEmpty()) {
            return false;
        }
        
        return true;
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
    
    private void loadSessionHistory() {
        File historyFile = new File(SESSION_HISTORY_FILE);
        if (!historyFile.exists()) {
            return;
        }
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(SESSION_HISTORY_FILE));
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    List<String> sessionWords = Arrays.asList(line.split(","));
                    recentSessions.add(sessionWords);
                }
            }
            
            // Keep only the most recent sessions
            while (recentSessions.size() > SESSIONS_TO_AVOID) {
                recentSessions.remove(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveSessionHistory() {
        try {
            // Add current session to history
            recentSessions.add(new ArrayList<>(sessionWords));
            
            // Keep only the most recent sessions
            while (recentSessions.size() > SESSIONS_TO_AVOID) {
                recentSessions.remove(0);
            }
            
            // Save to file
            List<String> lines = new ArrayList<>();
            for (List<String> session : recentSessions) {
                lines.add(String.join(",", session));
            }
            
            Files.write(Paths.get(SESSION_HISTORY_FILE), lines, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING);
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
    
    private void startNewSession() {
        sessionWords.clear();
        currentSessionMistakes.clear();
        currentWordIndex = 0;
        sessionCorrect = 0;
        sessionWrong = 0;
        
        // Get words used in recent sessions
        Set<String> recentlyUsedWords = new HashSet<>();
        for (List<String> session : recentSessions) {
            recentlyUsedWords.addAll(session);
        }
        
        // Create pool of available words (avoiding recently used ones)
        List<String> availableWords = new ArrayList<>();
        for (String word : allWords) {
            if (!recentlyUsedWords.contains(word)) {
                availableWords.add(word);
            }
        }
        
        // If not enough words available, add some recently used ones back
        if (availableWords.size() < WORDS_PER_SESSION) {
            for (String word : allWords) {
                if (!availableWords.contains(word)) {
                    availableWords.add(word);
                    if (availableWords.size() >= WORDS_PER_SESSION) {
                        break;
                    }
                }
            }
        }
        
        // Select 30 words with weighted selection based on mistake count
        sessionWords = selectWeightedWords(availableWords, WORDS_PER_SESSION);
        Collections.shuffle(sessionWords);
        
        loadNextWord();
    }
    
    private List<String> selectWeightedWords(List<String> pool, int count) {
        List<String> selected = new ArrayList<>();
        List<String> weightedPool = new ArrayList<>();
        
        // Create weighted pool
        for (String word : pool) {
            int weight = mistakeCount.getOrDefault(word, 0) + 1;
            for (int i = 0; i < weight * 2; i++) { // Double weight for mistakes
                weightedPool.add(word);
            }
        }
        
        // Select words
        Set<String> usedWords = new HashSet<>();
        while (selected.size() < count && !weightedPool.isEmpty()) {
            String word = weightedPool.get(random.nextInt(weightedPool.size()));
            if (!usedWords.contains(word)) {
                selected.add(word);
                usedWords.add(word);
            }
            
            // Remove all instances of selected word from pool
            weightedPool.removeAll(Collections.singleton(word));
        }
        
        // If we need more words, add random ones from the pool
        while (selected.size() < count && selected.size() < pool.size()) {
            String word = pool.get(random.nextInt(pool.size()));
            if (!usedWords.contains(word)) {
                selected.add(word);
                usedWords.add(word);
            }
        }
        
        return selected;
    }
    
    private void createUI() {
        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Progress label
        progressLabel = new JLabel("Word 1 of " + WORDS_PER_SESSION);
        progressLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(progressLabel, gbc);
        
        // Word display
        wordLabel = new JLabel("", SwingConstants.CENTER);
        wordLabel.setFont(new Font("Arial", Font.BOLD, 72));
        wordLabel.setPreferredSize(new Dimension(700, 150));
        wordLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 4));
        gbc.gridy = 1;
        mainPanel.add(wordLabel, gbc);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        correctButton = new JButton("Correct");
        correctButton.setFont(new Font("Arial", Font.BOLD, 32));
        correctButton.setPreferredSize(new Dimension(200, 80));
        correctButton.setBackground(Color.GREEN);
        correctButton.setOpaque(true);
        correctButton.setBorderPainted(false);
        correctButton.addActionListener(this::handleCorrect);
        buttonPanel.add(correctButton);
        
        wrongButton = new JButton("Wrong");
        wrongButton.setFont(new Font("Arial", Font.BOLD, 32));
        wrongButton.setPreferredSize(new Dimension(200, 80));
        wrongButton.setBackground(Color.RED);
        wrongButton.setOpaque(true);
        wrongButton.setBorderPainted(false);
        wrongButton.addActionListener(this::handleWrong);
        buttonPanel.add(wrongButton);
        
        gbc.gridy = 2;
        mainPanel.add(buttonPanel, gbc);
        
        // Session statistics
        sessionStatsLabel = new JLabel("Session: Correct: 0 | Wrong: 0");
        sessionStatsLabel.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridy = 3;
        mainPanel.add(sessionStatsLabel, gbc);
        
        // Statistics label
        statsLabel = new JLabel("");
        statsLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 18));
        gbc.gridy = 4;
        mainPanel.add(statsLabel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Bottom panel with additional buttons
        JPanel bottomPanel = new JPanel(new FlowLayout());
        
        showStatsButton = new JButton("Show All Statistics");
        showStatsButton.setFont(new Font("Arial", Font.BOLD, 18));
        showStatsButton.setPreferredSize(new Dimension(250, 50));
        showStatsButton.addActionListener(e -> showStatistics());
        bottomPanel.add(showStatsButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void loadNextWord() {
        if (currentWordIndex >= sessionWords.size()) {
            // Session complete
            sessionComplete();
            return;
        }
        
        currentWord = sessionWords.get(currentWordIndex);
        wordLabel.setText(currentWord);
        progressLabel.setText("Word " + (currentWordIndex + 1) + " of " + WORDS_PER_SESSION);
        
        // Update stats label
        Integer mistakes = mistakeCount.get(currentWord);
        if (mistakes != null && mistakes > 0) {
            statsLabel.setText("This word was misspelled " + mistakes + " time(s) before");
        } else {
            statsLabel.setText("First time practicing this word");
        }
    }
    
    private void handleCorrect(ActionEvent e) {
        sessionCorrect++;
        currentWordIndex++;
        updateSessionStats();
        loadNextWord();
    }
    
    private void handleWrong(ActionEvent e) {
        // Simply record the mistake without asking for details
        recordMistake(currentWord);
        currentSessionMistakes.add(currentWord);
        sessionWrong++;
        currentWordIndex++;
        updateSessionStats();
        loadNextWord();
    }
    
    private void updateSessionStats() {
        sessionStatsLabel.setText(String.format("Session: Correct: %d | Wrong: %d", 
            sessionCorrect, sessionWrong));
    }
    
    private void recordMistake(String word) {
        // Update mistake count
        mistakeCount.put(word, mistakeCount.getOrDefault(word, 0) + 1);
        
        // Save progress
        saveProgress();
    }
    
    private void sessionComplete() {
        // Save session history
        saveSessionHistory();
        
        // Show summary dialog
        JDialog dialog = new JDialog(this, "Session Complete!", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Summary text
        JTextArea summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font("Arial", Font.BOLD, 16));
        
        StringBuilder summary = new StringBuilder();
        summary.append("Session Complete!\n\n");
        summary.append("Total Words: ").append(WORDS_PER_SESSION).append("\n");
        summary.append("Correct: ").append(sessionCorrect).append("\n");
        summary.append("Wrong: ").append(sessionWrong).append("\n");
        summary.append("Accuracy: ").append(String.format("%.1f%%", 
            (sessionCorrect * 100.0 / WORDS_PER_SESSION))).append("\n\n");
        
        if (!currentSessionMistakes.isEmpty()) {
            summary.append("Words to practice:\n");
            for (String word : currentSessionMistakes) {
                summary.append("  • ").append(word).append("\n");
            }
        } else {
            summary.append("Perfect score! All words spelled correctly!");
        }
        
        summaryArea.setText(summary.toString());
        JScrollPane scrollPane = new JScrollPane(summaryArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton newSessionButton = new JButton("New Session");
        newSessionButton.setFont(new Font("Arial", Font.BOLD, 16));
        newSessionButton.setPreferredSize(new Dimension(150, 40));
        newSessionButton.addActionListener(ev -> {
            dialog.dispose();
            startNewSession();
        });
        buttonPanel.add(newSessionButton);
        
        JButton exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Arial", Font.BOLD, 16));
        exitButton.setPreferredSize(new Dimension(150, 40));
        exitButton.addActionListener(ev -> System.exit(0));
        buttonPanel.add(exitButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(panel);
        
        // Disable main window buttons
        correctButton.setEnabled(false);
        wrongButton.setEnabled(false);
        
        dialog.setVisible(true);
    }
    
    private void showStatistics() {
        JDialog dialog = new JDialog(this, "Spelling Statistics", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        // Create table model
        String[] columnNames = {"Word", "Times Wrong"};
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
            model.addRow(new Object[]{word, count});
        }
        
        // Add words with no mistakes
        for (String word : allWords) {
            if (!mistakeCount.containsKey(word)) {
                model.addRow(new Object[]{word, 0});
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