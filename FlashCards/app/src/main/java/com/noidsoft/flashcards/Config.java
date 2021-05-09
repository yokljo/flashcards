package com.noidsoft.flashcards;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.List;

public class Config extends Application {
    final String flashCardDirName = "FlashCardLibrary";

    String getFullFlashCardDir() {
        return getApplicationContext().getFilesDir().getAbsolutePath();
    }

    File[] getAllCardDecks() {
        File flashCardDir = new File(getFullFlashCardDir());
        File[] flashCardFiles = flashCardDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (!pathname.isDirectory()) {
                    String name = pathname.getName();
                    if (name.toLowerCase().endsWith(".txt")) {
                        return true;
                    }
                }
                return false;
            }
        });
        if (flashCardFiles != null) {
            return flashCardFiles;
        } else {
            return new File[]{};
        }
    }

    File getDeckFile(String deckName) {
        return new File(getFullFlashCardDir() + File.separator + deckName + ".txt");
    }

    String getDeckName(File file) {
        String name = file.getName();
        if (name.substring(name.length() - 4).toLowerCase().equals(".txt")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }

    File currentDeckFile = null;
}
