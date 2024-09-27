package org.example;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;


public class Main {

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        List<GroupStore> groupStores = new ArrayList<>();
        if (args.length == 0) {
            System.out.println("Введките параметр filename.txt");
        }
        if (args.length > 0 && args[0] != null) {
            String inputFile = args[0];
            ClassLoader loader = Main.class.getClassLoader();
            char[] buffer = new char[10240];
            if (inputFile.endsWith("txt")) {
                try (
                        FileReader fileReader = new FileReader(loader.getResource(inputFile).getFile());
                        BufferedReader reader = new BufferedReader(fileReader);) {
                    int rows = 0;
                    for (int ch, i = 0; (ch = reader.read()) != -1;) {
                        List<String> stringsInLine = new ArrayList<>();//Значения в строке
                        boolean skipLine = false; //лог переменная, что со строкой все плохо
                        int quoteCounter = 0; // в "String word" должно быть не больше 2 кавычек и то в начале и в конце
                        Writer writer = new StringWriter(); //нам еще нужно хранить саму строку
                        while (ch != -1 && ch != '\n' && ch != '\r') { //читка строки
                            writer.write(ch);
                            if (!skipLine) { //мы не балуемся continue и break, нам надо пробежаться по reader
                                buffer[i] = (char) ch;
                                if (ch == '"') {
                                    quoteCounter++;
                                }
                                if (ch == ';') { //отделяем ввод по ';'
                                    String word = new String(buffer, 0, i);
                                    skipLine = incorrectStringFast(word, quoteCounter); //проверка
                                    if (!skipLine) {
                                        String trimmedWord = new String(buffer, 1, i - 1);
                                        stringsInLine.add(trimmedWord);
                                    }
                                    i = 0; // обнуляем буфер, пишем новое зн-е в буфет
                                    quoteCounter = 0;
                                } else {
                                    i++;
                                }
                            }
                            ch = reader.read();
                        }
                        String line = writer.toString();
                        if (line != null && !line.isEmpty()) { // пропускаем совсем пустые строки
                            if (!skipLine) { //если строка кончилась, а все зн-я корректны, проверяем хвост
                                String word = new String(buffer, 0, i);
                                if (!incorrectStringFast(word, quoteCounter)) { //проверка
                                    String trimmedWord = new String(buffer, 1, i - 1);
                                    stringsInLine.add(trimmedWord);
                                }
                            }

                            //чекаем группы - сложная мудреная логика без комментариев
                            Integer myGroupNumber = null;
                            for (int j = 0; j < stringsInLine.size(); j++) {
                                String s = stringsInLine.get(j);
                                if (s.isEmpty()) { //пустые строки не влияют на принадлженость к группам игнор
                                    continue;
                                }
                                for (int k = 0; k < groupStores.size(); k++) {
                                    GroupStore groupStore = groupStores.get(k);
                                    if (j < groupStore.valueSetsByColumn.size()) {
                                        if (groupStore.valueSetsByColumn.get(j).contains(s)) {
                                            myGroupNumber = k;
                                            break;
                                        }
                                    }
                                }
                                if (myGroupNumber != null) {
                                    break;
                                }
                            }
                            if (myGroupNumber == null) {
                                GroupStore groupStore = new GroupStore();
                                groupStore.valueSetsByColumn = new ArrayList<>();
                                for (int j = 0; j < stringsInLine.size(); j++) {
                                    Set<String> set = new HashSet<>();
                                    set.add(stringsInLine.get(j));
                                    groupStore.valueSetsByColumn.add(set);
                                }
                                groupStore.rows = new ArrayList<>();
                                groupStore.rows.add(line);
                                groupStores.add(groupStore);
                            } else { //пополним текущее множество новой строкой  -ускор
                                GroupStore groupStore = groupStores.get(myGroupNumber);
                                for (int j = 0; j < stringsInLine.size(); j++) {
                                    String s = stringsInLine.get(j);
                                    if (s.isEmpty()) {
                                        continue;
                                    }
                                    if (j < groupStore.valueSetsByColumn.size()) {
                                        groupStore.valueSetsByColumn.get(j).add(s);
                                    } else {
                                        Set<String> set = new HashSet<>();
                                        set.add(s);
                                        groupStore.valueSetsByColumn.add(set);
                                    }
                                }
                                //проверка чтобы строки не повторялись
                                boolean existSameLine = false;
                                for (String row : groupStore.rows) {
                                    if (row.charAt(0) == line.charAt(0)) { //небольшое оптимизация
                                        if (row.equals(line)) {
                                            existSameLine = true;
                                        }
                                    }
                                }
                                if (!existSameLine) {
                                    groupStore.rows.add(line);
                                }
                            }
                            rows ++;
                            System.out.println(rows); //считал строки для себя
                        }
//                        String word;
//                        //иногда в конце строки бывает перевод каретки, а иногда нет, а нам '\n' не нужен совершенно
//                        if (ch == '\n') {
//                            word = new String(buffer, 0, i - 1);
//                        } else {
//                            word = new String(buffer, 0, i);
//                        }
                    }
                }

                for (int j = 0; j < groupStores.size(); j++) {
                    GroupStore groupStore = groupStores.get(j);
                    System.out.println("Группа " + (j + 1));
                    for (String s : groupStore.rows) {
                        System.out.println(s);
                    }
                }
            } else if (inputFile.endsWith(".gz")) {
                try (
                        FileInputStream fileIn = new FileInputStream(loader.getResource(inputFile).getFile());
                        GZIPInputStream gZIPInputStream = new GZIPInputStream(fileIn);
                        Reader reader = new InputStreamReader(gZIPInputStream, StandardCharsets.UTF_8);
                ) {
                    int rows = 0;
                    for (int ch, i = 0; (ch = reader.read()) != -1;) {
                        Writer writer = new StringWriter();
                        while (ch != -1 && ch != '\n' && ch != '\r') {
                            writer.write(ch);
                            ch = reader.read();
                        }
                        String line = writer.toString();
                        if (line != null && !line.isEmpty()) {
                            rows ++;
//                            System.out.println(rows); //считал строки для себя
                        }
                    }
//                  тут заимплементить то же самое что наверху при необходимости
                }
            }
        }
        long endTime = System.currentTimeMillis();
        long timeElapsed = endTime - startTime;
        System.out.println(timeElapsed);
    }

    static boolean incorrectStringFast(String s, int quoteCounter) {//2 варианта валидны: зн-е без кавычек внутри или пустое
        if (quoteCounter != 2) {//зн-е обяз должно быть в скобках
            return true;
        }
        if (s.length() < 2) {
            return true;
        } else return s.charAt(0) != '"' || s.charAt(s.length() - 1) != '"';
    }

//    static boolean incorrectString(String s) {//2 варианта валидны: зн-е без кавычек внутри или пустое
//        if (s.length() < 2) {//зн-е обяз должно быть в скобках
//            return true;
//        } else if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
//            String trimmedS = s.substring(1, s.length() - 1);
//            return trimmedS.contains("\"");
//        }
//        return false;
//    }

    private static class GroupStore {
        public List<Set<String>> valueSetsByColumn = new ArrayList<>(); // подмножество всех зн-ий по столбцам в i-ом стобце Set набор строк
        public List<String> rows; //строки, которые составляют текущую группу
    }
}