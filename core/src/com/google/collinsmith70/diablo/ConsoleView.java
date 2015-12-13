package com.google.collinsmith70.diablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;
import com.google.collinsmith70.diablo.cvar.Cvar;
import com.google.collinsmith70.diablo.cvar.CvarChangeListener;
import com.google.collinsmith70.diablo.cvar.Cvars;

import java.io.PrintStream;

public class ConsoleView extends Console implements Disposable {

private final Caret CARET;
private final Texture modelBackgroundTexture;
private final Texture underlineBackgroundTexture;

private boolean isVisible;
private BitmapFont font;
private int outputOffset;

private float height;

public ConsoleView(Client client) {
    this(client, System.out);
}

public ConsoleView(Client client, PrintStream outputStream) {
    super(client, outputStream);
    this.isVisible = false;

    this.CARET = new Caret();

    Pixmap solidColorPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    solidColorPixmap.setColor(0.0f, 0.0f, 0.0f, 0.5f);
    solidColorPixmap.fill();
    modelBackgroundTexture = new Texture(solidColorPixmap);
    solidColorPixmap.dispose();
    solidColorPixmap = null;

    solidColorPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    solidColorPixmap.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    solidColorPixmap.fill();
    underlineBackgroundTexture = new Texture(solidColorPixmap);
    solidColorPixmap.dispose();
    solidColorPixmap = null;

    final CvarChangeListener<Float> consoleFontColorCvarListener = new CvarChangeListener<Float>() {
        @Override
        public void onCvarChanged(Cvar<Float> cvar, Float fromValue, Float toValue) {
            if (cvar.equals(Cvars.Client.Console.Color.a)) {
                font.getColor().a = toValue;
            } else if (cvar.equals(Cvars.Client.Console.Color.r)) {
                font.getColor().r = toValue;
            } else if (cvar.equals(Cvars.Client.Console.Color.g)) {
                font.getColor().g = toValue;
            } else if (cvar.equals(Cvars.Client.Console.Color.b)) {
                font.getColor().b = toValue;
            }
        }
    };

    Cvars.Client.Console.Font.addCvarChangeListener(new CvarChangeListener<AssetDescriptor<BitmapFont>>() {
        @Override
        public void onCvarChanged(Cvar<AssetDescriptor<BitmapFont>> cvar, AssetDescriptor<BitmapFont> fromValue, AssetDescriptor<BitmapFont> toValue) {
            AssetManager assetManager = getClient().getAssetManager();
            assetManager.load(toValue);
            assetManager.finishLoading();

            ConsoleView.this.font = assetManager.get(Cvars.Client.Console.Font.getValue());
            Cvars.Client.Console.Color.r.addCvarChangeListener(consoleFontColorCvarListener);
            Cvars.Client.Console.Color.g.addCvarChangeListener(consoleFontColorCvarListener);
            Cvars.Client.Console.Color.b.addCvarChangeListener(consoleFontColorCvarListener);
            Cvars.Client.Console.Color.a.addCvarChangeListener(consoleFontColorCvarListener);
        }
    });

    Cvars.Client.Console.Height.addCvarChangeListener(new CvarChangeListener<Float>() {
        @Override
        public void onCvarChanged(Cvar<Float> cvar, Float fromValue, Float toValue) {
            if (toValue == 0.0f) {
                ConsoleView.this.height = ConsoleView.this.getClient().getVirtualHeight() - font.getLineHeight();
            } else if (toValue == 1.0f) {
                ConsoleView.this.height = 0.0f;
            } else {
                ConsoleView.this.height = ConsoleView.this.getClient().getVirtualHeight()*(1.0f - toValue);
            }
        }
    });
}

public boolean isVisible() {
    return isVisible;
}

public void setVisible(boolean b) {
    this.isVisible = b;
    Gdx.input.setOnscreenKeyboardVisible(isVisible());
    if (isVisible()) {
        //...
    }
}

public BitmapFont getFont() {
    return font;
}

public void clear() {
    // TODO: add abstraction to clear the console (which is separate from stream)
}

@Override
public void setPosition(int position) {
    super.setPosition(position);
    if (CARET != null) {
        CARET.setVisible(true);
    }
}

public void render(Batch b) {
    if (!isVisible()) {
        return;
    }

    if (Cvars.Client.Console.Height.getValue() == 0.0f) {
        b.draw(modelBackgroundTexture, 0.0f, 0.0f, getClient().getVirtualWidth(), getClient().getVirtualHeight());
        b.draw(underlineBackgroundTexture, 0.0f, height, getClient().getVirtualWidth(), 2.0f);
    } else {
        b.draw(modelBackgroundTexture, 0.0f, height, getClient().getVirtualWidth(), getClient().getVirtualHeight() - height);
        b.draw(underlineBackgroundTexture, 0.0f, height + font.getLineHeight(), getClient().getVirtualWidth(), 2.0f);
    }

    GlyphLayout glyphs = font.draw(b, getBufferPrefix() + " " + getBuffer(), 0, height + font.getLineHeight());

    glyphs.setText(font, getBufferPrefix() + " " + getBuffer().substring(0, getPosition()));
    float x = glyphs.width;

    float width;
    if (!isBufferEmpty() && getPosition() < getBufferLength()) {
        char c = getBuffer().charAt(getPosition());
        if (Character.isSpaceChar(c)) {
            width = font.getSpaceWidth();
        } else {
            glyphs.setText(font, Character.toString(c));
            width = glyphs.width - 4;
        }
    } else {
        width = font.getSpaceWidth();
    }

    CARET.render(b, font, glyphs, x, height + 4, width, 1.0f);

    float position;
    if (Cvars.Client.Console.Height.getValue() == 0.0f) {
        position = font.getLineHeight();
    } else {
        position = height + (font.getLineHeight() * 2);
    }
    int skip = outputOffset;
    for (String line : getOutput()) {
        if (Cvars.Client.Console.Height.getValue() == 0.0f
                && position >= height + font.getLineHeight()) {
            break;
        }

        if (skip > 0) {
            skip--;
            continue;
        }

        font.draw(b, line, 0.0f, position);
        position += font.getLineHeight();
    }
}

@Override
public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    Gdx.input.setOnscreenKeyboardVisible(true);
    return super.touchDown(screenX, screenY, pointer, button);
}

public boolean scrolled(int amount) {
    switch (amount) {
        case -1:
            outputOffset = Math.min(
                    outputOffset + 1,
                    getOutput().size());
            break;
        case 1:
            outputOffset = Math.max(
                    outputOffset - 1, 0);
            break;
    }

    return true;
}

@Override
public void dispose() {
    font.dispose();
}

private static class Caret {

    static final float DEFAULT_HOLD_DELAY = 1.0f;
    static final float DEFAULT_BLINK_DELAY = 0.5f;

    final Timer TIMER;
    final Timer.Task BLINK_TASK;

    final Texture underlineBackgroundTexture;

    float holdDelay;
    float blinkDelay;

    boolean isVisible;

    Caret() {
        this(DEFAULT_HOLD_DELAY, DEFAULT_BLINK_DELAY);
    }

    Caret(float holdDelay, float blinkDelay) {
        if (holdDelay < 0.1f) {
            throw new IllegalArgumentException("holdDelay should be >= 0.1f");
        }

        if (blinkDelay < 0.1f) {
            throw new IllegalArgumentException("blinkDelay should be >= 0.1f");
        }


        Pixmap solidColorPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        solidColorPixmap.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        solidColorPixmap.fill();
        underlineBackgroundTexture = new Texture(solidColorPixmap);
        solidColorPixmap.dispose();
        solidColorPixmap = null;

        this.holdDelay = holdDelay;
        this.blinkDelay = blinkDelay;

        this.isVisible = true;

        TIMER = new Timer();
        BLINK_TASK = new Timer.Task() {
            @Override
            public void run() {
                Caret.this.setVisible(!Caret.this.isVisible());
            }
        };

        TIMER.start();
        setVisible(true);
    }

    boolean isVisible() {
        return isVisible;
    }

    void setVisible(boolean b) {
        this.isVisible = b;
        BLINK_TASK.cancel();
        TIMER.schedule(BLINK_TASK, getHoldDelay(), getBlinkDelay());
    }

    float getHoldDelay() {
        return holdDelay;
    }

    void setHoldDelay(float holdDelay) {
        this.holdDelay = holdDelay;
    }

    float getBlinkDelay() {
        return blinkDelay;
    }

    void setBlinkDelay(float blinkDelay) {
        this.blinkDelay = blinkDelay;
    }

    void render(Batch batch, BitmapFont font, GlyphLayout glyphs, float x, float y, float width, float height) {
        if (!isVisible()) {
            return;
        }

        //font.draw(batch, "_", glyphs.width - 4, height);
        batch.draw(underlineBackgroundTexture, x, y, width, height);
    }

}

}
