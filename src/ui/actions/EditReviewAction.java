package ui.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import ui.forms.EditReviewForm;
import ui.reviewpoint.ReviewPoint;

import javax.swing.*;
import java.awt.*;

/**
 * User: Alisa.Afonina
 * Date: 7/20/11
 * Time: 5:03 PM
 */
public class EditReviewAction extends AnAction {
    private ReviewPoint reviewPoint;
    public EditReviewAction(String title, ReviewPoint reviewPoint) {
        super(title);
        this.reviewPoint = reviewPoint;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = PlatformDataKeys.PROJECT.getData(dataContext);
        if (project == null) return;
        Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
        if (editor != null) {
            final EditorGutterComponentEx gutterComponent = ((EditorEx)editor).getGutterComponentEx();
            Point point = gutterComponent.getPoint(reviewPoint.getGutterIconRenderer());
            if (point != null) {
                final Icon icon = reviewPoint.getGutterIconRenderer().getIcon();
                EditReviewForm editReviewForm = new EditReviewForm(reviewPoint.getReview());
                BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().createDialogBalloonBuilder(editReviewForm.getContent(), "Add Comment");
                Balloon balloon = balloonBuilder.createBalloon();
                editReviewForm.setBalloon(balloon);
                Point centerIconPoint = new Point(point.x + icon.getIconWidth() / 2 + gutterComponent.getIconsAreaWidth(), point.y + icon.getIconHeight() / 2);
                balloon.show(new RelativePoint(gutterComponent, centerIconPoint), Balloon.Position.atRight);
            }
        }
    }
}
