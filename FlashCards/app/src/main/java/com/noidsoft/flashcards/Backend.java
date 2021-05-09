package com.noidsoft.flashcards;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Backend {
    interface Listener {
        void cardsChanged();
        void currentCardChanged();
        void doneAllCards();
    }

    static class Card {
        int lineNum = -1;
        String front = "";
        String back = "";
        List<String> notes = new ArrayList<>();
    }

    static class AllParseException extends Exception {
        AllParseException(String message) {
            super(message);
        }
    }

    static class LineParseException extends Exception {
        LineParseException(String message) {
            super(message);
        }
    }

    static class Parser {
        enum LineType {
            None,
            Front,
            Back,
            Note,
            Comment,
            Error,
        }

        boolean discardParsedCards = false;
        Card card = null;
        int cardStartLine = -1;
        String front = null;
        String back = null;
        List<Card> cards = new ArrayList<>();

        void reset() {
            card = null;
            cardStartLine = -1;
            front = null;
            back = null;
            cards = new ArrayList<>();
        }

        LineType parseLine(String line, int lineNum) throws LineParseException {
            String sline = line.trim();
            if (sline.startsWith("#")) {
                return LineType.Comment;
            } else if (sline.isEmpty()) {
                if (front != null && back == null) {
                    throw new LineParseException("No back side");
                }
                card = null;
                cardStartLine = -1;
                front = null;
                back = null;
                return LineType.None;
            } else if (sline.startsWith("-")) {
                if (card == null) {
                    throw new LineParseException("Bad note");
                }
                card.notes.add(sline.substring(1).trim());
                return LineType.Note;
            } else {
                if (front == null) {
                    cardStartLine = lineNum;
                    front = sline;
                    return LineType.Front;
                } else if (back == null) {
                    back = sline;
                    card = new Card();
                    card.lineNum = cardStartLine;
                    card.front = front;
                    card.back = back;

                    if (!discardParsedCards) {
                        cards.add(card);
                    }
                    return LineType.Back;
                } else {
                    throw new LineParseException("Too many card lines");
                }
            }
        }

        void parseAllFromReader(BufferedReader reader) throws AllParseException, IOException {
            int lineNum = 0;
            String line = reader.readLine();
            while (line != null) {
                lineNum++;
                try {
                    parseLine(line, lineNum);
                } catch (LineParseException lineEx) {
                    throw new AllParseException(lineEx.getMessage() + " on line " + lineNum);
                }
                line = reader.readLine();
            }
        }
    }

    Listener listener;

    List<Card> cards = new ArrayList<>();
    List<Card> queue = new ArrayList<>();
    Card currentCard = null;
    List<Card> failedCards = new ArrayList<>();
    Random random = new Random();

    void setListener(Listener listener) {
        this.listener = listener;
        listener.cardsChanged();
        listener.currentCardChanged();
        listener.doneAllCards();
    }

    void load(BufferedReader reader) throws AllParseException, IOException {
        Parser parser = new Parser();
        parser.parseAllFromReader(reader);
        cards = parser.cards;
        queue.clear();
        nextCard();
        listener.cardsChanged();
    }

    void nextCard() {
        if (queue.isEmpty()) {
            List<Card> cardsClone = new ArrayList<>(cards);
            while (cardsClone.size() > 0) {
                int index = random.nextInt(cardsClone.size());
                Card randomCard = cardsClone.remove(index);
                queue.add(randomCard);
            }
            listener.doneAllCards();
        }

        if (! queue.isEmpty()) {
            currentCard = queue.remove(queue.size() - 1);
            listener.currentCardChanged();
        } else if (currentCard != null) {
            currentCard = null;
            listener.currentCardChanged();
        }
    }

    void tryAgainLater() {
        if (! failedCards.contains(currentCard)) {
            failedCards.add(currentCard);
        }

        if (queue.size() > 10) {
            queue.add(queue.size() - 10, currentCard);
        } else {
            queue.add(0, currentCard);
        }
    }
}
