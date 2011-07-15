package ui.reviewtoolwindow.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import reviewresult.ReviewItem;

import java.util.Collection;
import java.util.Collections;

/**
 * User: Alisa.Afonina
 * Date: 7/14/11
 * Time: 5:51 PM
 */
public class ReviewItemNode  extends AbstractTreeNode {
    private Project project;
    private ReviewItem reviewItem;

    public ReviewItemNode(Project project, ReviewItem reviewItem) {
        super(project, reviewItem.getText());
        this.project = project;
        this.reviewItem = reviewItem;
    }
    @NotNull
    @Override
    public Collection getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAlwaysLeaf() {
        return true;
    }

    @Override
    protected void update(PresentationData presentation) {
        presentation.setPresentableText(reviewItem.getText());
    }

    public ReviewItem getReviewItem() {
        return reviewItem;
    }

    public void setReviewItem(ReviewItem reviewItem) {
        this.reviewItem = reviewItem;
    }
}