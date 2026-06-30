import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class ExamSystem extends JFrame {
    // CardLayout holds all screens: Login, Profile, Exam, Result
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // User Data (In-memory Mock Database)
    private String username = "student";
    private String password = "password123";
    private String displayName = "John Doe";

    // Exam States
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private int[] selectedAnswers; // Stores chosen radio button index (-1 if unanswered)
    private Timer examTimer;
    private int timeRemaining = 1800; // 30 minutes in seconds
    private int totalExamTime = 1800;
    private boolean examSubmitted = false;

    // UI Components needed globally for updates
    private JLabel timerLabel;
    private JLabel qNumberLabel;
    private JTextArea qTextArea;
    private JRadioButton[] options = new JRadioButton[4];
    private ButtonGroup optionsGroup;
    
    // Result UI Components
    private JLabel scoreLabel;
    private JLabel timeTakenLabel;
    private JTextArea breakdownArea;

    // Question Structure
    static class Question {
        String text;
        String[] options;
        int correctAnswerIndex; // 0 to 3

        public Question(String text, String[] options, int correctAnswerIndex) {
            this.text = text;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
        }
    }

    public ExamSystem() {
        setTitle("Online Examination System");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Session Management: Intercept close button
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });

        // Initialize Mock Questions
        initQuestions();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Add Screens
        mainPanel.add(createLoginScreen(), "Login");
        mainPanel.add(createProfileScreen(), "Profile");
        mainPanel.add(createExamScreen(), "Exam");
        mainPanel.add(createResultScreen(), "Result");

        add(mainPanel);
        cardLayout.show(mainPanel, "Login");
    }

    private void initQuestions() {
        questions = new ArrayList<>();
        questions.add(new Question("Which component is used to compile, debug, and execute Java programs?", 
                new String[]{"JRE", "JIT", "JDK", "JVM"}, 2));
        questions.add(new Question("Which of these keywords is used to define interfaces in Java?", 
                new String[]{"interface", "Interface", "intf", "implements"}, 0));
        questions.add(new Question("Which superclass is at the top of the Java class hierarchy?", 
                new String[]{"Class", "Object", "System", "Super"}, 1));
        questions.add(new Question("Which modifier makes a variable unchangeable after initialization?", 
                new String[]{"static", "abstract", "finalize", "final"}, 3));
        questions.add(new Question("What is the default value of a boolean variable in Java?", 
                new String[]{"true", "false", "null", "0"}, 1));
        
        selectedAnswers = new int[questions.size()];
        resetAnswers();
    }

    private void resetAnswers() {
        for (int i = 0; i < selectedAnswers.length; i++) {
            selectedAnswers[i] = -1; 
        }
    }

    // --- SCREEN 1: LOGIN ---
    private JPanel createLoginScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Student Login"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField(15);
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField(15);
        JButton loginButton = new JButton("Login");
        JLabel errorLabel = new JLabel("", SwingConstants.CENTER);
        errorLabel.setForeground(Color.RED);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(userLabel, gbc);
        gbc.gridx = 1; panel.add(userField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(passLabel, gbc);
        gbc.gridx = 1; panel.add(passField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; panel.add(loginButton, gbc);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(errorLabel, gbc);

        loginButton.addActionListener(e -> {
            String inputUser = userField.getText();
            String inputPass = new String(passField.getPassword());
            if (inputUser.equals(username) && inputPass.equals(password)) {
                errorLabel.setText("");
                userField.setText("");
                passField.setText("");
                cardLayout.show(mainPanel, "Profile");
            } else {
                errorLabel.setText("Invalid credentials! Try student / password123");
            }
        });
        return panel;
    }

    // --- SCREEN 2: PROFILE UPDATE ---
    private JPanel createProfileScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Update Profile & Launch Exam"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel nameLabel = new JLabel("Display Name:");
        JTextField nameField = new JTextField(displayName, 15);
        JLabel passLabel = new JLabel("New Password:");
        JPasswordField passField = new JPasswordField(password, 15);
        JButton startButton = new JButton("Save & Start Exam");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(nameLabel, gbc);
        gbc.gridx = 1; panel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(passLabel, gbc);
        gbc.gridx = 1; panel.add(passField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; panel.add(startButton, gbc);

        startButton.addActionListener(e -> {
            displayName = nameField.getText().trim();
            password = new String(passField.getPassword());
            if (displayName.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Fields cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            startExam();
        });
        return panel;
    }

    // --- SCREEN 3: EXAM SCREEN ---
    private JPanel createExamScreen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top Bar: Timer and welcome info
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Candidate: " + displayName);
        timerLabel = new JLabel("Time Remaining: 30:00", SwingConstants.RIGHT);
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        timerLabel.setForeground(Color.RED);
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(timerLabel, BorderLayout.EAST);
        panel.add(topPanel, BorderLayout.NORTH);

        // Center Panel: Question text and options
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        qNumberLabel = new JLabel("Question 1 of X");
        qNumberLabel.setFont(new Font("Arial", Font.BOLD, 14));
        centerPanel.add(qNumberLabel, BorderLayout.NORTH);

        qTextArea = new JTextArea(4, 50);
        qTextArea.setEditable(false);
        qTextArea.setLineWrap(true);
        qTextArea.setWrapStyleWord(true);
        qTextArea.setFont(new Font("Arial", Font.PLAIN, 14));
        centerPanel.add(new JScrollPane(qTextArea), BorderLayout.CENTER);

        // Radio Button Container
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        optionsGroup = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            options[i] = new JRadioButton();
            optionsGroup.add(options[i]);
            optionsPanel.add(options[i]);
            
            final int index = i;
            options[i].addActionListener(e -> selectedAnswers[currentQuestionIndex] = index);
        }
        centerPanel.add(optionsPanel, BorderLayout.SOUTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        // Bottom Navigation Bar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        JButton prevButton = new JButton("Previous");
        JButton nextButton = new JButton("Next");
        JButton submitButton = new JButton("Submit Exam");
        submitButton.setBackground(new Color(34, 139, 34));
        submitButton.setForeground(Color.WHITE);

        bottomPanel.add(prevButton);
        bottomPanel.add(nextButton);
        bottomPanel.add(submitButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // Event Handlers for Navigation
        prevButton.addActionListener(e -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                displayQuestion();
            }
        });

        nextButton.addActionListener(e -> {
            if (currentQuestionIndex < questions.size() - 1) {
                currentQuestionIndex++;
                displayQuestion();
            }
        });

        submitButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(panel, 
                "Are you sure you want to hand in your answers?",
                "Confirm Final Submission", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                submitExam();
            }
        });

        return panel;
    }
    private JPanel createResultScreen(){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));

scoreLabel = new JLabel("Score: ", SwingConstants.CENTER);
scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));

timeTakenLabel = new JLabel("Time Taken: ", SwingConstants.CENTER);

topPanel.add(scoreLabel);
topPanel.add(timeTakenLabel);

panel.add(topPanel, BorderLayout.NORTH);

breakdownArea = new JTextArea();
breakdownArea.setEditable(false);
breakdownArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

panel.add(new JScrollPane(breakdownArea), BorderLayout.CENTER);

JButton logoutButton = new JButton("Logout");
panel.add(logoutButton, BorderLayout.SOUTH);

logoutButton.addActionListener(e -> {
    examSubmitted = false;
    resetAnswers();
    cardLayout.show(mainPanel, "Login");
});

return panel;
}

// ==========================
// EXAM CONTROL ENGINE
// ==========================

private void startExam() {
    currentQuestionIndex = 0;
    timeRemaining = totalExamTime;
    examSubmitted = false;

    resetAnswers();
    displayQuestion();

    cardLayout.show(mainPanel, "Exam");

    if (examTimer != null && examTimer.isRunning()) {
        examTimer.stop();
    }

    examTimer = new Timer(1000, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {

            timeRemaining--;

            int minutes = timeRemaining / 60;
            int seconds = timeRemaining % 60;

            timerLabel.setText(
                    String.format("Time Remaining: %02d:%02d", minutes, seconds));

            if (timeRemaining <= 0) {
                examTimer.stop();

                JOptionPane.showMessageDialog(
                        mainPanel,
                        "Time expired! Your exam will be submitted automatically.",
                        "Timeout",
                        JOptionPane.WARNING_MESSAGE
                );

                submitExam();
            }
        }
    });

    examTimer.start();
}

private void displayQuestion() {

    Question q = questions.get(currentQuestionIndex);

    qNumberLabel.setText(
            String.format("Question %d of %d",
                    currentQuestionIndex + 1,
                    questions.size())
    );

    qTextArea.setText(q.text);

    optionsGroup.clearSelection();

    for (int i = 0; i < 4; i++) {

        options[i].setText(q.options[i]);

        if (selectedAnswers[currentQuestionIndex] == i) {
            options[i].setSelected(true);
        }
    }
}

private void submitExam() {

    if (examTimer != null) {
        examTimer.stop();
    }

    examSubmitted = true;

    int score = 0;
    int timeTaken = totalExamTime - timeRemaining;

    StringBuilder breakdown = new StringBuilder();

    breakdown.append(
            String.format("Performance Summary for %s:\n", displayName)
    );

    breakdown.append(
            "==================================================\n\n"
    );

    for (int i = 0; i < questions.size(); i++) {

        Question q = questions.get(i);

        int chosen = selectedAnswers[i];

        boolean correct = (chosen == q.correctAnswerIndex);

        if (correct) {
            score++;
        }

        breakdown.append(
                String.format("Q%d: %s\n", i + 1, q.text)
        );

        breakdown.append(
                String.format(
                        "   Your Answer: %s\n",
                        (chosen == -1)
                                ? "[No Selection]"
                                : q.options[chosen]
                )
        );

        breakdown.append(
                String.format(
                        "   Correct Answer: %s\n",
                        q.options[q.correctAnswerIndex]
                )
        );

        breakdown.append(
                String.format(
                        "   Status: %s\n\n",
                        correct
                                ? "✔ CORRECT"
                                : "❌ INCORRECT"
                )
        );
    }

    int minTaken = timeTaken / 60;
    int secTaken = timeTaken % 60;

    scoreLabel.setText(
            String.format(
                    "Final Score: %d out of %d",
                    score,
                    questions.size()
            )
    );

    timeTakenLabel.setText(
            String.format(
                    "Time Elapsed: %02d minutes %02d seconds",
                    minTaken,
                    secTaken
            )
    );

    breakdownArea.setText(breakdown.toString());
    breakdownArea.setCaretPosition(0);

    cardLayout.show(mainPanel, "Result");
}

private void handleWindowClosing() {

    if (mainPanel.getComponent(2).isVisible() && !examSubmitted) {

        int option = JOptionPane.showConfirmDialog(
                this,
                "Closing the application will forfeit your exam. Progress will not be saved. Quit anyway?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {

            if (examTimer != null) {
                examTimer.stop();
            }

            System.exit(0);
        }

    } else {
        System.exit(0);
    }
}

public static void main(String[] args) {

    SwingUtilities.invokeLater(() -> {
        new ExamSystem().setVisible(true);
    });
}
}

    
