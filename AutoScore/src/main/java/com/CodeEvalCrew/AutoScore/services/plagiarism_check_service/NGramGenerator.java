package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class NGramGenerator {

    List<String> generateNGrams(String text, int n) {
        List<String> nGramsList = new ArrayList<>();

        String[] tokens = text.split(" ");

        for (int i = 0; i <= tokens.length - n; i++) {
            StringBuilder nGram = new StringBuilder();

            for (int j = 0; j < n; j++) {
                if (j > 0) {
                    nGram.append(" ");
                }
                nGram.append(tokens[i + j]);
            }

            nGramsList.add(nGram.toString());
        }

        return nGramsList;
    }
}
