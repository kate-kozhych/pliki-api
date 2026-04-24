package com.event.plikiapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private static final String TEXTS_DIR = "texts";
    private static final int FILE_COUNT = 50;
    private static final int WORDS_PER_FILE = 3000;

    private static final String[] VOCAB = {
        "the","be","to","of","and","a","in","that","have","it","for","not","on","with",
        "he","as","you","do","at","this","but","his","by","from","they","we","say","her",
        "she","or","an","will","my","one","all","would","there","their","what","so","up",
        "out","if","about","who","get","which","go","me","when","make","can","like","time",
        "no","just","him","know","take","people","into","year","your","good","some","could",
        "them","see","other","than","then","now","look","only","come","its","over","think",
        "also","back","after","use","two","how","our","work","first","well","way","even",
        "new","want","because","any","these","give","day","most","us","great","between",
        "need","large","often","hand","high","place","hold","free","real","life","few","north",
        "open","seem","together","next","white","children","begin","got","walk","example","ease",
        "paper","group","always","music","those","both","mark","book","letter","until","mile",
        "river","car","feet","care","second","enough","plain","girl","usual","young","ready",
        "above","ever","red","list","though","feel","talk","bird","soon","body","dog","family",
        "direct","pose","song","measure","door","product","black","short","numeral","class",
        "wind","question","happen","complete","ship","area","half","rock","order","fire",
        "south","problem","piece","told","knew","pass","since","top","whole","king","space",
        "heard","best","hour","better","true","during","hundred","five","remember","step",
        "early","hold","west","ground","interest","reach","fast","verb","sing","listen","six",
        "table","travel","less","morning","ten","simple","several","vowel","toward","war",
        "lay","against","pattern","slow","center","love","person","money","serve","appear",
        "road","map","rain","rule","govern","pull","cold","notice","voice","unit","power",
        "town","fine","drive","lead","cry","dark","machine","note","wait","plan","figure",
        "star","box","noun","field","rest","correct","lead","able","pound","done","beauty",
        "drive","stood","contain","front","teach","week","final","gave","green","oh","quick",
        "develop","ocean","warm","free","minute","strong","special","mind","behind","clear"
    };

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path dir = Paths.get(TEXTS_DIR);
        if (Files.exists(dir) && Files.list(dir).findAny().isPresent()) {
            log.info("Text files already exist in '{}', skipping generation.", TEXTS_DIR);
            return;
        }
        Files.createDirectories(dir);

        Random rng = new Random(42);
        for (int i = 1; i <= FILE_COUNT; i++) {
            int wordCount = WORDS_PER_FILE + rng.nextInt(WORDS_PER_FILE / 2);
            Path file = dir.resolve(String.format("sample_%02d.txt", i));
            Files.writeString(file, generateText(rng, wordCount));
        }
        log.info("Generated {} text files in '{}'.", FILE_COUNT, TEXTS_DIR);
    }

    private String generateText(Random rng, int wordCount) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            sb.append(VOCAB[rng.nextInt(VOCAB.length)]);
            if ((i + 1) % 12 == 0) sb.append('\n');
            else sb.append(' ');
        }
        return sb.toString();
    }
}
