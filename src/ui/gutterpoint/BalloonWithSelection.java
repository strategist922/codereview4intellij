package ui.gutterpoint;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.ui.popup.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.PositionTracker;
import reviewresult.Review;

import javax.swing.*;
import java.awt.*;

/**
 * User: Alisa.Afonina
 * Date: 8/26/11
 * Time: 3:55 PM
 */
public class BalloonWithSelection{
    private Review review = null;
    private Balloon balloon = null;
    private Editor editor;
    private Point target;
    final private RangeHighlighter highlighter;
    private static final TextAttributes REVIEW_ATTRIBUTES = new TextAttributes(null, new Color(224,255,204), null, null, Font.PLAIN);

    public BalloonWithSelection() {
        highlighter = null;
    }

    public BalloonWithSelection(final Review review, Editor editor, Point target, JComponent balloonContent, String title) {
        this.review = review;
        this.editor = editor;
        this.target = target;

        this.highlighter = editor.getMarkupModel().addRangeHighlighter(review.getStart(),
                                                                       review.getEnd(),
                                                                       HighlighterLayer.SELECTION - 12,
                                                                       REVIEW_ATTRIBUTES,
                                                                       HighlighterTargetArea.EXACT_RANGE);
        BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().createDialogBalloonBuilder(balloonContent, title);
        balloonBuilder.setHideOnClickOutside(true);
        balloonBuilder.setHideOnKeyOutside(true);
        balloon = balloonBuilder.createBalloon();
        this.balloon.addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                    BalloonWithSelection.this.review.setActivated(false);
                    if(highlighter.isValid()) {
                        highlighter.dispose();
                    }
            }
        });
    }


    public void dispose() {
        if(balloon != null && !balloon.isDisposed())
            balloon.dispose();
        if(review != null)
            review.setActivated(false);
        if(highlighter != null) {
                    if(highlighter.isValid()) {
                        highlighter.dispose();
            }
        }
    }

    public boolean isValid() {
        return balloon != null && !balloon.wasFadedOut() && !balloon.isDisposed();
    }

    public void showBalloon( final JComponent contentComponent) {
        if(!review.isValid()) return;
        if(balloon == null) return;
        this.review.setActivated(true);
        balloon.show(new ReviewPositionTracker(editor, contentComponent, target), Balloon.Position.below);
    }

    public Review getReview() {
        return review;
    }

    public boolean compare(Review review) {
        if(this.review != null) {
            return review.equals(this.review);
        }
        return false;
    }


    private class ReviewPositionTracker extends PositionTracker<Balloon> {

        private final Editor editor;
        private final JComponent component;
        private final Point point;

        public ReviewPositionTracker(Editor editor, JComponent component, Point point) {
            super(component);
            this.editor = editor;
            this.component = component;
            this.point = point;
        }

        @Override
        public RelativePoint recalculateLocation(final Balloon object) {
            if (!editor.getScrollingModel().getVisibleArea().contains(point)) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        object.hide();
                        //disposeActiveBalloon();
                    }
                });
                 if(!object.isDisposed()) {
                    final PositionTracker<Balloon> tracker = this;
                    final VisibleAreaListener listener = new VisibleAreaListener() {
                       @Override
                       public void visibleAreaChanged(VisibleAreaEvent e) {
                            if(e.getNewRectangle().contains(point) && object.isDisposed() && !balloon.isDisposed()) {
                                balloon.show(tracker, Balloon.Position.above);
                                editor.getScrollingModel().removeVisibleAreaListener(this);
                            }
                       }
                    };
                    editor.getScrollingModel().addVisibleAreaListener(listener);
                 }
            }
            return new RelativePoint(component, point);
        }

    }
}