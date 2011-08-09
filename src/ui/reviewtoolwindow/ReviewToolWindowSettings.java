package ui.reviewtoolwindow;

import com.intellij.ide.actions.NextOccurenceToolbarAction;
import com.intellij.ide.actions.PreviousOccurenceToolbarAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.util.xmlb.XmlSerializer;
import com.sun.imageio.plugins.common.InputStreamAdapter;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import reviewresult.ReviewManager;
import reviewresult.persistent.ReviewsState;
import utils.Util;

import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 * User: Alisa.Afonina
 * Date: 8/3/11
 * Time: 3:56 PM
 */
public class ReviewToolWindowSettings {
    private boolean groupByModule;
    private boolean groupByFile;
    private boolean searchEnabled;
    private boolean isShowPreview;
    private Project project;
    private ReviewPanel panel;

    public ReviewToolWindowSettings(Project project, ReviewPanel panel) {
        this.project = project;
        this.panel = panel;
    }

    public boolean isGroupByModule() {
        return groupByModule;
    }

    public void setGroupByModule(boolean groupByModule) {
        this.groupByModule = groupByModule;
    }

    public boolean isGroupByFile() {
        return groupByFile;
    }

    public void setGroupByFile(boolean groupByFile) {
        this.groupByFile = groupByFile;
    }

    public boolean isSearchEnabled() {
        return searchEnabled;
    }

    public void setSearchEnabled(boolean searchEnabled) {
        this.searchEnabled = searchEnabled;
    }

    public boolean isShowPreview() {
        return isShowPreview;
    }

    public void setShowPreview(boolean showPreview) {
        isShowPreview = showPreview;
    }

    private final class GroupByModuleAction extends ToggleAction implements DumbAware {

        private GroupByModuleAction() {
             super("Group reviews by module", null, IconLoader.getIcon("/actions/modul.png"));
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return groupByModule;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            groupByModule = state;
            updateUI();
        }
    }

    private final class GroupByFileAction extends ToggleAction  implements DumbAware {

        private GroupByFileAction() {
             super("Group reviews by file", null, IconLoader.getIcon("/fileTypes/unknown.png"));
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return groupByFile;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            groupByFile = state;
            updateUI();
        }
    }

    private final class SearchAction extends ToggleAction  implements DumbAware {

        private SearchAction() {
             super("Search in reviews", null, IconLoader.getIcon("/actions/find.png"));
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return searchEnabled;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            searchEnabled = state;
            if(!searchEnabled) {
                Searcher.getInstance(project).emptyFilter();
            }
            updateUI();
        }
    }

    private final class PreviewAction extends ToggleAction  implements DumbAware {

        public PreviewAction() {
            super("Preview reviews", null, IconLoader.getIcon("/actions/preview.png"));
        }



        @Override
        public boolean isSelected(AnActionEvent e) {
            return isShowPreview;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            isShowPreview = state;
            updateUI();
        }

    }

    private void updateUI() {
        panel.updateUI();
    }

    public JPanel createLeftMenu() {
        JPanel toolBar = new JPanel(new GridLayout());

        DefaultActionGroup leftGroup = new DefaultActionGroup();
        leftGroup.add(new PreviousOccurenceToolbarAction(panel));
        leftGroup.add(new NextOccurenceToolbarAction(panel));
        leftGroup.add(new PreviewAction());
        leftGroup.add(new GroupByModuleAction());
        leftGroup.add(new GroupByFileAction());
        leftGroup.add(new SearchAction());
        leftGroup.add(new ExportToFileAction());
        leftGroup.add(new ImportFromFileAction());
        toolBar.add(
            ActionManager.getInstance().createActionToolbar(ActionPlaces.TODO_VIEW_TOOLBAR, leftGroup, false).getComponent());

    return toolBar;
    }

    private final class ExportToFileAction extends AnAction  implements DumbAware {

        public ExportToFileAction() {
            super("Export to file...", null, IconLoader.getIcon("/actions/export.png"));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            FileSaverDescriptor descriptor = new FileSaverDescriptor("Save Reviews", "Export reviews to file", "xml");
            FileSaverDialog saverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);
            VirtualFileWrapper wrapper = saverDialog.save(project.getBaseDir(), null);
            if(wrapper == null) return;
            VirtualFile file = wrapper.getVirtualFile(true);

            String text = ReviewManager.getInstance(project).getExportText();

            if(file == null || !file.isWritable()) return;
            try {
                OutputStream outputStream = file.getOutputStream(null);
                outputStream.write(text.getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private final class ImportFromFileAction extends AnAction  implements DumbAware {

        public ImportFromFileAction() {
            super("Import from file...", null, IconLoader.getIcon("/actions/import.png"));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
            FileChooserDialog chooserDialog = FileChooserFactory.getInstance().createFileChooser(descriptor, project);
            VirtualFile[] files = chooserDialog.choose(null, project);
            if(files.length != 1) {return;} //may be show warning message?
            VirtualFile virtualFile = files[0];
            try {
                String contents = new String(virtualFile.contentsToByteArray());
                SAXBuilder builder = new SAXBuilder();
                Element root = builder.build(new StringReader(contents)).getRootElement();
                ReviewsState.State state = XmlSerializer.deserialize(root, ReviewsState.State.class);
                ReviewManager reviewManager = ReviewManager.getInstance(project);
                reviewManager.loadReviews(state.reviews, true);
            } catch(JDOMException e2) {
                e2.printStackTrace();
            } catch(NullPointerException e2) {
                e2.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }
}
