package gdx.diablo.widget;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.utils.GdxRuntimeException;

import gdx.diablo.Diablo;
import gdx.diablo.codec.FontTBL;
import gdx.diablo.graphics.PaletteIndexedBatch;

public class Label extends com.badlogic.gdx.scenes.scene2d.ui.Label {
  public Label(int id, BitmapFont font) {
    this(id == -1 ? "" : Diablo.string.lookup(id), font);
  }

  public Label(String text, BitmapFont font) {
    super(text, new LabelStyle(font, null));
  }

  public Label(BitmapFont font) {
    this(null, font);
  }

  @Override
  public void draw(Batch batch, float a) {
    if (batch instanceof PaletteIndexedBatch) {
      draw((PaletteIndexedBatch) batch, a);
    } else {
      throw new GdxRuntimeException("Not supported");
    }
  }

  public void draw(PaletteIndexedBatch batch, float a) {
    validate();

    batch.setBlendMode(((FontTBL.BitmapFont) getStyle().font).getBlendMode());
    BitmapFontCache cache = getBitmapFontCache();
    cache.setPosition(getX(), getY());
    cache.tint(getColor());
    cache.draw(batch);
    batch.resetBlendMode();
  }

  @Override
  public boolean setText(int id) {
    setText(id == -1 ? "" : Diablo.string.lookup(id));
    return true;
  }
}