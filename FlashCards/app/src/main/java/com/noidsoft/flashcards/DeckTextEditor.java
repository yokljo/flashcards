package com.noidsoft.flashcards;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

public class DeckTextEditor extends android.support.v7.widget.AppCompatEditText {
    HashMap<Integer, String> errors = new HashMap<>();
    Paint backErrorPaint = new Paint();
    Paint frontErrorPaint = new Paint();

    final float dpUnit = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());

    public DeckTextEditor(Context context) {
        super(context, null);
    }

    public DeckTextEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    void init() {
        backErrorPaint.setColor(Color.rgb(255, 180, 180));
        frontErrorPaint.setColor(Color.rgb(100, 50, 50));

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            void removeSpans(Editable e, Class<? extends CharacterStyle> type) {
                CharacterStyle[] spans = e.getSpans(0, e.length(), type);
                for (CharacterStyle span : spans) {
                    e.removeSpan(span);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                errors.clear();

                removeSpans(editable, ForegroundColorSpan.class);

                StringReader stringReader = new StringReader(editable.toString());
                BufferedReader reader = new BufferedReader(stringReader);
                Backend.Parser parser = new Backend.Parser();
                parser.discardParsedCards = true;
                try {
                    int lineNum = 0;
                    int pos = 0;
                    String line = reader.readLine();
                    while (line != null) {
                        lineNum++;

                        Backend.Parser.LineType lineType = Backend.Parser.LineType.Error;
                        try {
                            lineType = parser.parseLine(line, lineNum);
                        } catch (Backend.LineParseException ex) {
                            errors.put(lineNum, ex.getMessage());
                        }

                        if (! line.isEmpty()) {
                            int color = Color.BLACK;
                            switch (lineType) {
                            case Note:
                                color = Color.BLACK;
                                break;
                            case Back:
                                color = Color.GREEN;
                                break;
                            case Comment:
                                color = Color.GRAY;
                                break;
                            case Error:
                                color = Color.RED;
                                break;
                            case Front:
                                color = Color.BLUE;
                                break;
                            case None:
                                color = Color.BLACK;
                                break;
                            }

                            if (color != Color.BLACK) {
                                ForegroundColorSpan span = new ForegroundColorSpan(color);
                                editable.setSpan(span, pos, pos + line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }

                        // +1 for new line.
                        pos += line.length() + 1;

                        line = reader.readLine();
                    }
                } catch (IOException e) {}
            }
        });
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);
    }

    Rect textBounds = new Rect();
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        frontErrorPaint.setTextSize(getLineHeight());
        frontErrorPaint.getTextBounds("#", 0, 1, textBounds);

        Layout layout = getLayout();
        int yOff = getScrollY();
        int topLineNum = layout.getLineForVertical(yOff - getPaddingTop());
        int bottomLineNum = layout.getLineForVertical(yOff + getHeight());

        for (int i = topLineNum; i <= bottomLineNum; i++) {
            String message = errors.get(i + 1);
            if (message != null) {
                //int pos = layout.getLineEnd(i) - 1;
                //float x = layout.getPrimaryHorizontal(pos);
                float x = layout.getLineWidth(i) + getPaddingLeft() + 10*dpUnit;
                float y = layout.getLineTop(i) + getPaddingTop();

                canvas.drawRect(x, y, getWidth(), y + getLineHeight(), backErrorPaint);

                canvas.drawText(message, x, y + getLineHeight()/2 + textBounds.height()/2, frontErrorPaint);
            }
        }
    }
}
