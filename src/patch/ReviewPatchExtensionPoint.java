package patch;

import com.intellij.openapi.diff.impl.patch.PatchEP;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import reviewresult.ReviewManager;
import reviewresult.persistent.ReviewsState;
import ui.reviewpoint.ReviewPointManager;

import java.io.IOException;
import java.io.StringReader;

/**
 * User: Alisa.Afonina
 * Date: 7/28/11
 * Time: 3:51 PM
 */
public class ReviewPatchExtensionPoint implements PatchEP{
    @NotNull
    @Override
    public String getName() {
        return "reviewresult.reviewpatchextensionpoint";
    }

    @Override
    public CharSequence provideContent(@NotNull String path) {


        String result = "";
        for(Project project : ProjectManager.getInstance().getOpenProjects()) {
            VirtualFile file = project.getBaseDir().findFileByRelativePath(path);
            result += ReviewManager.getInstance(project).getExportTextForFile(file.getUrl());
        }
        return new StringBuilder(result);
    }

    @Override
    public void consumeContent(@NotNull String path, @NotNull CharSequence content) {
        for(Project project : ProjectManager.getInstance().getOpenProjects()) {
            VirtualFile file = project.getBaseDir().findFileByRelativePath(path);
            if(file == null) return;
            try {
                SAXBuilder builder = new SAXBuilder();
                Element root = builder.build(new StringReader(content.toString())).getRootElement();
                ReviewsState.State state = XmlSerializer.deserialize(root, ReviewsState.State.class);
                ReviewManager reviewManager = ReviewManager.getInstance(project);
                reviewManager.loadReviewsForFile(state.reviews);
            } catch(JDOMException e) {
                e.printStackTrace();
            } catch(NullPointerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}