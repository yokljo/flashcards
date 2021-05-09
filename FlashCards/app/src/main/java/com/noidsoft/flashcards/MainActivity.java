package com.noidsoft.flashcards;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.SubMenu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    Config config;

    Backend backend;

    NavigationView navigationView;

    LinearLayout contentMain;
    TextView frontLabel;
    TextView backLabel;
    Button nextButton;
    Button tryAgainLaterButton;
    SeekBar dimBackCardSlider;

    View noCardsOverlay;
    TextView noCardsDescription;
    TextView parseFailDescription;
    Button editDeckFileButton;

    Menu topMenu;

    SubMenu cardDecksList;

    boolean fileHasError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        config = (Config)getApplication();

        setContentView(R.layout.activity_main);
        contentMain = findViewById(R.id.contentMain);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        Menu menu = navigationView.getMenu();
        MenuItem cardDecksListItem = menu.findItem(R.id.cardDecksList);
        cardDecksList = cardDecksListItem.getSubMenu();
        refreshCardDecksList();

        noCardsOverlay = findViewById(R.id.noCardsOverlay);
        noCardsDescription = findViewById(R.id.noCardsDescription);
        parseFailDescription = findViewById(R.id.parseFailDescription);
        editDeckFileButton = findViewById(R.id.editDeckFileButton);

        frontLabel = findViewById(R.id.frontLabel);
        backLabel = findViewById(R.id.backLabel);
        nextButton = findViewById(R.id.nextButton);
        tryAgainLaterButton = findViewById(R.id.tryAgainLaterButton);
        dimBackCardSlider = findViewById(R.id.dimBackCardSlider);

        backend = new Backend();
        backend.setListener(new Backend.Listener() {
            @Override
            public void currentCardChanged() {
                if (backend.currentCard != null) {
                    frontLabel.setText(backend.currentCard.front);
                    backLabel.setText(backend.currentCard.back);
                } else {
                    frontLabel.setText("-");
                    backLabel.setText("-");
                }
                updateTitle();
            }

            @Override
            public void doneAllCards() {
                contentMain.setBackgroundColor(Color.rgb(255, 200, 200));
            }

            @Override
            public void cardsChanged() {
                if (fileHasError) {
                    displayError(ErrorType.FailedLoad);
                } else {
                    if (backend.cards.isEmpty()) {
                        displayError(ErrorType.NoCards);
                    } else {
                        displayError(ErrorType.None);
                    }
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contentMain.setBackgroundColor(Color.WHITE);
                backend.nextCard();
            }
        });

        tryAgainLaterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backend.tryAgainLater();
                backend.nextCard();
            }
        });

        dimBackCardSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateBackDim();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        editDeckFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editDeckFile();
            }
        });

        updateBackDim();
    }

    @Override
    protected void onResume() {
        super.onResume();
        openDeckFile(config.currentDeckFile);
        refreshCardDecksList();
    }

    void refreshCardDecksList() {
        cardDecksList.clear();
        File[] allDecks = config.getAllCardDecks();
        for (int i = 0; i < allDecks.length; i++) {
            final File file = allDecks[i];
            String name = config.getDeckName(allDecks[i]);
            MenuItem item = cardDecksList.add(0, i, 0, name);
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    openDeckFile(file);
                    return false;
                }
            });
        }
    }

    enum ErrorType {
        None,
        NoCards,
        FailedLoad,
    }

    void displayError(ErrorType errorType) {
        switch (errorType) {
        case None:
            noCardsOverlay.setVisibility(View.GONE);
            break;
        case NoCards:
            noCardsOverlay.setVisibility(View.VISIBLE);
            noCardsDescription.setVisibility(View.VISIBLE);
            parseFailDescription.setVisibility(View.GONE);
            editDeckFileButton.setVisibility(View.GONE);
            break;
        case FailedLoad:
            noCardsOverlay.setVisibility(View.VISIBLE);
            noCardsDescription.setVisibility(View.GONE);
            parseFailDescription.setVisibility(View.VISIBLE);
            editDeckFileButton.setVisibility(View.VISIBLE);
            break;
        }
    }

    void updateBackDim() {
        backLabel.setTextColor(backLabel.getTextColors().withAlpha(dimBackCardSlider.getProgress()));
    }

    void editDeckFile() {
        Intent intent = new Intent(this, EditorActivity.class);
        startActivity(intent);
    }

    void updateEditButton() {
        if (topMenu != null) {
            MenuItem item = topMenu.findItem(R.id.actionEditDeck);
            item.setVisible(config.currentDeckFile != null);
            item.setEnabled(item.isVisible());
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar, menu);
        topMenu = menu;
        updateEditButton();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.actionEditDeck) {
            editDeckFile();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.navCreateDeck) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Create new deck");

            final EditText input = new EditText(getApplicationContext());
            input.setHint("Deck name");
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    String deckName = input.getText().toString();
                    File file = config.getDeckFile(deckName);
                    if (!file.exists()) {
                        try {
                            File dataDir = new File(config.getFullFlashCardDir());
                            dataDir.mkdirs();
                            if (file.createNewFile()) {
                                openDeckFile(file);
                                //Toast.makeText(getApplicationContext(),"Created deck file", Toast.LENGTH_LONG).show();
                            } else {
                                // This shouldn't happen, because I already checked the file doesn't exist.
                                //Toast.makeText(getApplicationContext(),"Failed to create deck file: Already exists?", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(),"Failed to create deck file: " + e.toString(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(),"There's already a deck called " + deckName, Toast.LENGTH_LONG);
                    }
                }
            });
            // Create the AlertDialog object and return it
            builder.create().show();
        }
        /*if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void updateTitle() {
        if (config.currentDeckFile != null) {
            int progress = backend.cards.size() - backend.queue.size();
            setTitle(config.getDeckName(config.currentDeckFile) + " (" + progress + "/" + backend.cards.size() + ")");
        } else {
            setTitle("No deck loaded");
        }
    }

    void openDeckFile(File file) {
        config.currentDeckFile = file;

        if (file != null) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                backend.load(reader);
                reader.close();
                updateTitle();
                contentMain.setBackgroundColor(Color.WHITE);
                displayError(ErrorType.None);
                fileHasError = false;
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Deck failed to load: " + e.getMessage(), Toast.LENGTH_LONG).show();
                updateTitle();
                displayError(ErrorType.FailedLoad);
                fileHasError = true;
            }
        } else {
            displayError(ErrorType.None);
            fileHasError = false;
        }
        updateEditButton();
        refreshCardDecksList();
    }
}
