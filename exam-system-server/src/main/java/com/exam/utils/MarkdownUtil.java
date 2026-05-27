package com.exam.utils;

import com.exam.dto.QuestionImportDto;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown文档解析工具类
 * 用于从Markdown格式的文档中解析题目
 *
 * 支持格式：
 * ## 题目1
 * 类型：CHOICE
 * 难度：MEDIUM
 * 分类：1
 * 分值：5
 *
 * 下列关于Java的说法正确的是？
 * A. Java是面向对象的
 * B. Java是编译型语言
 * C. Java支持多线程
 * D. Java是弱类型语言
 *
 * 答案：A,C
 * 解析：Java是一种面向对象的、支持多线程的强类型编程语言。
 */
@Slf4j
public class MarkdownUtil {

    private static final Pattern TITLE_PATTERN = Pattern.compile("^#{1,3}\\s*(.+)$");
    private static final Pattern TYPE_PATTERN = Pattern.compile("(?:题目)?类型[：:]\s*(CHOICE|JUDGE|TEXT|选择题|判断题|简答题)");
    private static final Pattern DIFFICULTY_PATTERN = Pattern.compile("难度[：:]\s*(EASY|MEDIUM|HARD|简单|中等|困难)");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("分类[：:]\s*(\\d+)");
    private static final Pattern SCORE_PATTERN = Pattern.compile("分值[：:]\s*(\\d+)");
    private static final Pattern ANSWER_PATTERN = Pattern.compile("答案[：:]\s*(.+)");
    private static final Pattern ANALYSIS_PATTERN = Pattern.compile("解析[：:]\s*(.+)");
    private static final Pattern CHOICE_PATTERN = Pattern.compile("^([A-D])\\.\\s*(.+)$");

    public static List<QuestionImportDto> parseMarkdown(String markdown) {
        List<QuestionImportDto> questions = new ArrayList<>();
        if (markdown == null || markdown.trim().isEmpty()) {
            return questions;
        }

        String[] lines = markdown.split("\\r?\\n");
        QuestionImportDto currentQuestion = null;
        List<String> currentOptions = new ArrayList<>();
        StringBuilder currentContent = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("#")) {
                if (currentQuestion != null && currentContent.length() > 0) {
                    currentQuestion.setTitle(currentContent.toString().trim());
                    processCurrentOptions(currentQuestion, currentOptions);
                    questions.add(currentQuestion);
                }

                currentQuestion = new QuestionImportDto();
                currentOptions.clear();
                currentContent.setLength(0);

                Matcher titleMatcher = TITLE_PATTERN.matcher(line);
                if (titleMatcher.find()) {
                    String titleFromMd = titleMatcher.group(1).trim();
                    currentContent.append(titleFromMd);
                }
                continue;
            }

            if (line.contains("类型")) {
                Matcher typeMatcher = TYPE_PATTERN.matcher(line);
                if (typeMatcher.find()) {
                    String type = typeMatcher.group(1);
                    currentQuestion.setType(convertType(type));
                }
                continue;
            }

            if (line.contains("难度")) {
                Matcher diffMatcher = DIFFICULTY_PATTERN.matcher(line);
                if (diffMatcher.find()) {
                    String diff = diffMatcher.group(1);
                    currentQuestion.setDifficulty(convertDifficulty(diff));
                }
                continue;
            }

            if (line.contains("分类")) {
                Matcher catMatcher = CATEGORY_PATTERN.matcher(line);
                if (catMatcher.find()) {
                    try {
                        currentQuestion.setCategoryId(Long.parseLong(catMatcher.group(1)));
                    } catch (NumberFormatException e) {
                        currentQuestion.setCategoryId(1L);
                    }
                }
                continue;
            }

            if (line.contains("分值")) {
                Matcher scoreMatcher = SCORE_PATTERN.matcher(line);
                if (scoreMatcher.find()) {
                    try {
                        currentQuestion.setScore(Integer.parseInt(scoreMatcher.group(1)));
                    } catch (NumberFormatException e) {
                        currentQuestion.setScore(5);
                    }
                }
                continue;
            }

            if (line.startsWith("答案")) {
                Matcher ansMatcher = ANSWER_PATTERN.matcher(line);
                if (ansMatcher.find() && currentQuestion != null) {
                    String answer = ansMatcher.group(1).trim();
                    if ("CHOICE".equals(currentQuestion.getType())) {
                        currentQuestion.setAnswer(convertChoiceAnswer(answer));
                    } else {
                        currentQuestion.setAnswer(answer);
                    }
                }
                continue;
            }

            if (line.startsWith("解析")) {
                Matcher analysisMatcher = ANALYSIS_PATTERN.matcher(line);
                if (analysisMatcher.find() && currentQuestion != null) {
                    currentQuestion.setAnalysis(analysisMatcher.group(1).trim());
                }
                continue;
            }

            Matcher choiceMatcher = CHOICE_PATTERN.matcher(line);
            if (choiceMatcher.find()) {
                currentOptions.add(choiceMatcher.group(2));
                continue;
            }

            if (currentQuestion != null && !line.startsWith("答案") && !line.startsWith("解析")) {
                if (currentContent.length() > 0) {
                    currentContent.append("\n").append(line);
                } else {
                    currentContent.append(line);
                }
            }
        }

        if (currentQuestion != null && currentContent.length() > 0) {
            currentQuestion.setTitle(currentContent.toString().trim());
            processCurrentOptions(currentQuestion, currentOptions);
            questions.add(currentQuestion);
        }

        return questions;
    }

    private static void processCurrentOptions(QuestionImportDto question, List<String> options) {
        if ("CHOICE".equals(question.getType()) && !options.isEmpty()) {
            List<QuestionImportDto.ChoiceImportDto> choices = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
                QuestionImportDto.ChoiceImportDto choice = new QuestionImportDto.ChoiceImportDto();
                choice.setContent(options.get(i));
                choice.setSort(i + 1);
                choices.add(choice);
            }
            question.setChoices(choices);
        }
    }

    private static String convertType(String type) {
        return switch (type) {
            case "选择题", "CHOICE" -> "CHOICE";
            case "判断题", "JUDGE" -> "JUDGE";
            case "简答题", "TEXT" -> "TEXT";
            default -> "CHOICE";
        };
    }

    private static String convertDifficulty(String difficulty) {
        return switch (difficulty) {
            case "简单", "EASY" -> "EASY";
            case "中等", "MEDIUM" -> "MEDIUM";
            case "困难", "HARD" -> "HARD";
            default -> "MEDIUM";
        };
    }

    private static String convertChoiceAnswer(String answer) {
        return answer.replaceAll("[^A-Da-d]", "");
    }

    public static List<QuestionImportDto> parseMarkdownAdvanced(String markdown) {
        List<QuestionImportDto> questions = new ArrayList<>();
        if (markdown == null || markdown.trim().isEmpty()) {
            return questions;
        }

        String[] sections = markdown.split("(?=\\n#{1,3}\\s)");

        for (String section : sections) {
            if (section.trim().isEmpty()) {
                continue;
            }

            try {
                QuestionImportDto question = parseSection(section.trim());
                if (question != null && question.getTitle() != null && !question.getTitle().isEmpty()) {
                    questions.add(question);
                }
            } catch (Exception e) {
                log.warn("解析题目段落失败: {}", e.getMessage());
            }
        }

        return questions;
    }

    private static QuestionImportDto parseSection(String section) {
        QuestionImportDto question = new QuestionImportDto();
        List<String> options = new ArrayList<>();

        String[] lines = section.split("\\r?\\n");
        StringBuilder contentBuilder = new StringBuilder();
        boolean inOptions = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#")) {
                Matcher titleMatcher = TITLE_PATTERN.matcher(line);
                if (titleMatcher.find()) {
                    String title = titleMatcher.group(1).trim();
                    if (contentBuilder.length() > 0) {
                        contentBuilder.append("\n");
                    }
                    contentBuilder.append(title);
                }
                continue;
            }

            if (line.startsWith("答案")) {
                Matcher ansMatcher = ANSWER_PATTERN.matcher(line);
                if (ansMatcher.find()) {
                    String answer = ansMatcher.group(1).trim();
                    if (inOptions && !options.isEmpty()) {
                        processParsedOptions(question, options);
                    }
                    if ("CHOICE".equals(question.getType())) {
                        question.setAnswer(convertChoiceAnswer(answer));
                    } else {
                        question.setAnswer(answer);
                    }
                }
                inOptions = false;
                continue;
            }

            if (line.startsWith("解析")) {
                Matcher analysisMatcher = ANALYSIS_PATTERN.matcher(line);
                if (analysisMatcher.find()) {
                    question.setAnalysis(analysisMatcher.group(1).trim());
                }
                continue;
            }

            Matcher choiceMatcher = CHOICE_PATTERN.matcher(line);
            if (choiceMatcher.find()) {
                inOptions = true;
                options.add(choiceMatcher.group(2));
                continue;
            }

            Matcher typeMatcher = TYPE_PATTERN.matcher(line);
            if (typeMatcher.find()) {
                question.setType(convertType(typeMatcher.group(1)));
                if (contentBuilder.length() > 0) {
                    contentBuilder.append("\n");
                }
                contentBuilder.append(line.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "")).append("\n");
                continue;
            }

            Matcher diffMatcher = DIFFICULTY_PATTERN.matcher(line);
            if (diffMatcher.find()) {
                question.setDifficulty(convertDifficulty(diffMatcher.group(1)));
                continue;
            }

            Matcher catMatcher = CATEGORY_PATTERN.matcher(line);
            if (catMatcher.find()) {
                try {
                    question.setCategoryId(Long.parseLong(catMatcher.group(1)));
                } catch (NumberFormatException e) {
                    question.setCategoryId(1L);
                }
                continue;
            }

            Matcher scoreMatcher = SCORE_PATTERN.matcher(line);
            if (scoreMatcher.find()) {
                try {
                    question.setScore(Integer.parseInt(scoreMatcher.group(1)));
                } catch (NumberFormatException e) {
                    question.setScore(5);
                }
                continue;
            }

            if (!inOptions && !line.startsWith("答案") && !line.startsWith("解析")) {
                if (contentBuilder.length() > 0) {
                    contentBuilder.append("\n");
                }
                contentBuilder.append(line);
            }
        }

        question.setTitle(contentBuilder.toString().trim());

        if ("CHOICE".equals(question.getType()) && !options.isEmpty()) {
            processParsedOptions(question, options);
        }

        if (question.getDifficulty() == null) {
            question.setDifficulty("MEDIUM");
        }
        if (question.getScore() == null) {
            question.setScore(5);
        }
        if (question.getCategoryId() == null) {
            question.setCategoryId(1L);
        }

        return question;
    }

    private static void processParsedOptions(QuestionImportDto question, List<String> options) {
        List<QuestionImportDto.ChoiceImportDto> choices = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            QuestionImportDto.ChoiceImportDto choice = new QuestionImportDto.ChoiceImportDto();
            choice.setContent(options.get(i));
            choice.setSort(i + 1);
            choices.add(choice);
        }
        question.setChoices(choices);
    }
}