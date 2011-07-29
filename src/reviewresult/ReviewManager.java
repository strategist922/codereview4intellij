package reviewresult;

import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.*;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.Tag;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.jetbrains.annotations.NotNull;
import ui.reviewpoint.ReviewPoint;

import java.util.*;

/**
 * User: Alisa.Afonina
 * Date: 7/11/11
 * Time: 6:51 PM
 */

@State(
    name = "ReviewManager",
    storages = {
       @Storage(id = "default", file = "$PROJECT_FILE$"),
       @Storage(id = "dir", file = "$PROJECT_CONFIG_DIR$/codeReview.xml", scheme = StorageScheme.DIRECTORY_BASED)
    }
)
public class ReviewManager extends AbstractProjectComponent implements PersistentStateComponent<ReviewManager.State>, VirtualFileListener {
    private final StartupManagerEx startupManager;
    private State state = new State();
    private Map<String, List<Review>> reviews = new HashMap<String, List<Review>>();
    private Map<Review, ReviewPoint> reviewPoints = new HashMap<Review, ReviewPoint>();
    private static final Logger LOG = Logger.getInstance(ReviewManager.class.getName());
    public Map<String, List<ReviewBean>> removed = new HashMap<String, List<ReviewBean>>();
    public ReviewManager(Project project, final StartupManager startupManager) {
        super(project);
        this.startupManager = (StartupManagerEx)startupManager;
        VirtualFileManager.getInstance().addVirtualFileListener(this);
    }

    public static ReviewManager getInstance(@NotNull Project project) {
        return project.getComponent(ReviewManager.class);
    }

    public void addReview(Review newReview) {
        String url = newReview.getReviewBean().getUrl();

        if(reviews.containsKey(url)) {
            List<Review> reviewList = reviews.get(url);
            reviewList.add(newReview);
            ReviewBean reviewBean = newReview.getReviewBean();
            if(!state.reviews.contains(reviewBean)) {
                state.reviews.add(reviewBean);
            }
        } else {
            ArrayList<Review> reviewsList = new ArrayList<Review>();
            reviewsList.add(newReview);
            reviews.put(url, reviewsList);
            ReviewBean reviewBean = newReview.getReviewBean();
            if(!state.reviews.contains(reviewBean)) {
                state.reviews.add(reviewBean);
            }
        }
    }

    public State getState() {
        return state;
    }

    public void loadState(State state) {
        this.state = state;
        loadReviews(state, false);

    }

    public void loadReviewsForFile(State state) {
        loadReviews(state, true);
    }

    private void loadReviews(State state, boolean part) {
        if(!part) {
            reviews = new HashMap<String, List<Review>>();
        }
        List<ReviewBean> toRemove = new ArrayList<ReviewBean>();
        for (ReviewBean reviewBean : state.reviews) {
            Review review = new Review(reviewBean, myProject);
            if(review.isValid()) {
                if(findReviewPoint(review) == null) {
                    makeReviewPoint(review);
                }
            }
            else {
                toRemove.add(review.getReviewBean());
                logInvalidReview(review);
            }
        }
        if(!toRemove.isEmpty()) {
            state.reviews.removeAll(toRemove);
        }
        updateUI();
    }


    public Map<Review, ReviewPoint> getReviewPoints() {
        return reviewPoints;
    }

    public void updateUI() {
        for (ReviewPoint point : reviewPoints.values()) {
            updateUI(point);
        }
    }
    public void updateUI(final ReviewPoint point) {
        Runnable runnable = new Runnable() {
            public void run() {
                    point.updateUI();
            }
        };
        if (startupManager.startupActivityPassed()) {
          runnable.run();
        }
        else {
          startupManager.registerPostStartupActivity(runnable);
        }
    }

    public List<Review> getReviews(VirtualFile virtualFile) {
        return reviews.get(virtualFile.getUrl());
    }

    public Set<String> getFileNames() {
        return reviews.keySet();
    }

    public ReviewPoint findReviewPoint(Review review) {
        if(reviewPoints.containsKey(review)) {
            return reviewPoints.get(review);
        }
        return null;
    }

    public void createReviewPoint(Review review) {
        if(review.isValid()) {
            ReviewPoint point = makeReviewPoint(review);
            if(point != null) {
                updateUI(point);
            }
        } else {
            logInvalidReview(review);
        }
    }

    private ReviewPoint makeReviewPoint(Review review) {
        ReviewPoint point = new ReviewPoint(review);
        reviewPoints.put(review, point);
        addReview(review);
        return point;
    }

    @Override
    public void projectOpened() {

    }

    @Override
    public void projectClosed() {}

    @Override
    public void initComponent() {}

    @Override
    public void disposeComponent() {}

    @NotNull
    @Override
    public String getComponentName() {
        return "ReviewManager";
    }

    public void clearAll() {
        state = new State();
        reviews = new HashMap<String, List<Review>>();
        reviewPoints = new HashMap<Review, ReviewPoint>();
    }

    public void removeReview(ReviewPoint pointToRemove) {
        if(pointToRemove != null) {
            pointToRemove.release();
            Review review = pointToRemove.getReview();
            reviewPoints.remove(review);
            ReviewBean reviewBean = review.getReviewBean();
            String url = reviewBean.getUrl();
            List<Review> reviewsList = reviews.get(url);
            reviewsList.remove(review);
            if(reviewsList.isEmpty()) reviews.remove(url);
            state.reviews.remove(reviewBean);
            List<ReviewBean> reviewBeans = removed.get(reviewBean.getUrl());
            if(reviewBeans == null) {
                ArrayList<ReviewBean> value = new ArrayList<ReviewBean>();
                value.add(reviewBean);
                removed.put(reviewBean.getUrl(), value);
                state.removed.add(reviewBean);
            } else {
                reviewBeans.add(reviewBean);
            }
        }
    }

    public void logInvalidReview(Review review) {
        String message = "Review with start offset " + String.valueOf(review.getStart())
                    + " and file \"" + review.getReviewBean().getUrl() + "\" became invalid";
        LOG.warn(message);
    }

    @Override
    public void propertyChanged(VirtualFilePropertyEvent event) {}

    @Override
    public void contentsChanged(VirtualFileEvent event) {}

    @Override
    public void fileCreated(VirtualFileEvent event) {}

    @Override
    public void fileDeleted(VirtualFileEvent event) {}

    @Override
    public void fileMoved(VirtualFileMoveEvent event) {}

    @Override
    public void fileCopied(VirtualFileCopyEvent event) {}

    @Override
    public void beforePropertyChange(VirtualFilePropertyEvent event) {}

    @Override
    public void beforeContentsChange(VirtualFileEvent event) {}

    @Override
    public void beforeFileDeletion(VirtualFileEvent event) {
        VirtualFile oldFile = event.getFile();
        String url = oldFile.getUrl();
        if(reviews.containsKey(url)) {
            List<Review> reviewList = reviews.get(url);
            reviews.remove(url);
            for (Review review : reviewList) {
                reviewPoints.remove(review);
                state.reviews.remove(review.getReviewBean());
            }
        }
    }

    @Override
    public void beforeFileMovement(VirtualFileMoveEvent event) {
        String  url  = event.getOldParent().getUrl() + "/" + event.getFileName();
        String  newUrl  = event.getNewParent().getUrl() + "/"  + event.getFileName();
        if(reviews.containsKey(url)) {
            List<Review> reviewList = reviews.get(url);
            reviews.remove(url);
            reviews.put(newUrl, reviewList);
            for (Review review : reviewList) {
                review.getReviewBean().setUrl(newUrl);
            }
        }
    }

    public void unloadReviewsForFile(State state) {
        for (ReviewBean reviewBean : state.reviews) {
            removeReview(findReviewPoint(new Review(reviewBean, myProject)));
        }
    }


    public List<ReviewBean> getAddedForFile(VirtualFile file) {
        List<Review> reviewsPart = reviews.get(file.getUrl());
        if(reviewsPart != null && !reviewsPart.isEmpty()) {
            List<ReviewBean> reviewBeans = new ArrayList<ReviewBean>();
            for(Review review : reviewsPart) {
                reviewBeans.add(review.getReviewBean());
            }
            return reviewBeans;
        }
        return null;
    }
    public List<ReviewBean> getRemovedForFile(VirtualFile file) {
        return removed.get(file.getUrl());
    }
    public static class State {
        @Tag("reviews")
        @AbstractCollection(surroundWithTag = false)
        public List<ReviewBean> reviews = new ArrayList<ReviewBean>();
        @Tag("removed_reviews")
        @AbstractCollection(surroundWithTag = false)
        public List<ReviewBean> removed = new ArrayList<ReviewBean>();
    }
}
