package rotld.apscrm.api.v1.logopedy.enums;

public enum ScreenType {
    // Existing types
    READ_TEXT,
    READ_TEXT_WITH_SUB,
    IMAGE_WORD_SYLLABLES,
    MISSING_LETTER_PAIRS,
    READ_PARAGRAPH,
    IMAGE_MISSING_LETTER,
    IMAGE_REVEAL_WORD,
    
    // New types for specialist modules
    INSTRUCTIONS,              // Display scrollable text instructions
    IMAGE_SELECTION,           // Select correct image from 3 options
    FIND_SOUND,               // Tap syllable containing specific sound
    FIND_MISSING_LETTER,      // Type missing letter in word
    FIND_NON_INTRUDER,        // Select 2 matching images from 3 options
    FORMAT_WORD               // Order scrambled letters to form word
}