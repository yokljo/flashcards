package com.noidsoft.flashcards;

import android.content.Intent;
import android.graphics.Color;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class EditorActivity extends AppCompatActivity {
    Config config;

    DeckTextEditor textEditor;

    boolean allowFailedSave = false;

    Menu topMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        config = (Config)getApplication();

        setContentView(R.layout.activity_editor);

        Toolbar toolbar = findViewById(R.id.toolbar);

        textEditor = findViewById(R.id.editText);
        textEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                allowFailedSave = false;

                updateErrorButton();
            }
        });

        try {
            load();
        } catch (Exception e) {
            onBackPressed();
        }
    }

    void updateErrorButton() {
        if (topMenu != null) {
            MenuItem item = topMenu.findItem(R.id.actionNextError);
            item.setVisible(! textEditor.errors.isEmpty());
            item.setEnabled(item.isVisible());
        }
    }

    @Override
    protected void onPause() {
        save();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        boolean saved = save();
        if (saved || allowFailedSave) {
            super.onBackPressed();
        } else {
            allowFailedSave = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.editor_toolbar, menu);
        topMenu = menu;
        updateErrorButton();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.actionSaveDeck) {
            save();
            return true;
        } else if (id == R.id.actionNextError) {
            List<Integer> errorLines = new ArrayList<>();
            for (int line : textEditor.errors.keySet()) {
                errorLines.add(line);
            }

            if (errorLines.size() > 0) {
                Collections.sort(errorLines);
                int line = errorLines.get(0);

                int currentLine = textEditor.getLayout().getLineForOffset(textEditor.getSelectionStart()) + 1;
                if (errorLines.contains(currentLine)) {
                    int atIndex = errorLines.indexOf(currentLine) + 1;
                    if (atIndex < errorLines.size()) {
                        line = errorLines.get(atIndex);
                    }
                }

                int lineStart = textEditor.getLayout().getLineStart(line - 1);
                int lineEnd = textEditor.getLayout().getLineEnd(line - 1);
                textEditor.setSelection(lineStart, lineEnd);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    void load() throws IOException {
        if (config.currentDeckFile != null) {
            //byte[] data = Files.readAllBytes(config.currentDeckFile.getAbsolutePath());
            //String contents = new String(data, "UTF-8");
            //editText.setText(contents);
            FileReader fileReader = new FileReader(config.currentDeckFile);
            BufferedReader reader = new BufferedReader(fileReader);
            try {
                //Backend.Parser parser = new Backend.Parser();
                //parser.parseAllFromReader(reader);
                StringBuilder allLines = new StringBuilder();
                String line = reader.readLine();
                while (line != null) {
                    if (allLines.length() > 0) {
                        allLines.append("\n");
                    }
                    allLines.append(line);
                    line = reader.readLine();
                }
                textEditor.setText(allLines);
            } finally {
                reader.close();
            }
        }
    }

    boolean save() {
        boolean success = false;
        Toast toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);
        try {
            FileWriter writer = new FileWriter(config.currentDeckFile);
            writer.write(textEditor.getText().toString());
            writer.close();

            toast.setText("Saved");
            toast.setDuration(Toast.LENGTH_SHORT);
            success = true;
        } catch (Exception e) {
            toast.setText("Save failed: " + e.getMessage());
            toast.setDuration(Toast.LENGTH_LONG);
        }

        toast.show();
        return success;
    }
}
