package ui.gutterpoint;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import reviewresult.Review;
import reviewresult.ReviewManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Alisa.Afonina
 * Date: 8/2/11
 * Time: 11:30 AM
 */
public class ReviewPointManager extends AbstractProjectComponent implements DumbAware{
    private final Map<Review, ReviewPoint> reviewPoints = new HashMap<Review, ReviewPoint>();

    public ReviewPointManager(Project project) {
        super(project);
    }

    public static ReviewPointManager getInstance(Project project) {
        return project.getComponent(ReviewPointManager.class);
    }

    public Map<Review, ReviewPoint> getReviewPoints() {
        return Collections.unmodifiableMap(reviewPoints);
    }

    public void updateUI() {
        for (ReviewPoint point : reviewPoints.values()) {
            point.updateUI();
        }
    }

    public ReviewPoint findReviewPoint(Review review) {
            return reviewPoints.get(review);
    }

    private ReviewPoint createReviewPoint(Review review) {
        ReviewPoint point = new ReviewPoint(review);
        reviewPoints.put(review, point);
        return point;
    }

    public void reloadReviewPoint(Review review) {
        ReviewPoint reviewPoint = findReviewPoint(review);
        if(reviewPoint == null) {
            if(review.isValid())
                reviewPoint = createReviewPoint(review);
        }
        if(review.getReviewBean().isDeleted())
            reviewPoints.remove(review);

        if(reviewPoint != null)
            reviewPoint.updateUI();
    }
}