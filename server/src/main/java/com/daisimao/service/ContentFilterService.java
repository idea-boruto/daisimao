package com.daisimao.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class ContentFilterService {

    private final TrieNode root = new TrieNode();

    @PostConstruct
    public void init() {
        InputStream in = getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
        if (in == null) {
            log.error("sensitive-words.txt not found in classpath, filter disabled");
            return;
        }
        try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    insert(line);
                    count++;
                }
            }
            log.info("Sensitive word filter loaded {} words", count);
        } catch (IOException e) {
            log.error("Failed to read sensitive-words.txt: {}", e.getMessage());
        }
    }

    public Set<String> match(String text) {
        if (text == null || text.isEmpty()) return Collections.emptySet();
        Set<String> matched = new HashSet<>();
        int n = text.length();
        for (int i = 0; i < n; i++) {
            TrieNode node = root;
            for (int j = i; j < n; j++) {
                node = node.children.get(text.charAt(j));
                if (node == null) break;
                if (node.isEnd) {
                    matched.add(text.substring(i, j + 1));
                }
            }
        }
        return matched;
    }

    private void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isEnd = true;
    }

    private static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd;
    }
}
