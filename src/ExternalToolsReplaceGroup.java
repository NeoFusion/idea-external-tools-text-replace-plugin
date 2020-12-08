import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.tools.Tool;
import com.intellij.tools.ToolManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExternalToolsReplaceGroup extends ActionGroup {
    @Override
    public void update(@NotNull AnActionEvent event) {
        final Project project = event.getProject();
        final Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabledAndVisible(project != null && editor != null && editor.getSelectionModel().hasSelection());
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent event) {
        List<Tool> list = ToolManager.getInstance().getTools();
        AnAction[] actions = new AnAction[list.size()];
        int n = 0;
        for (Tool tool: list) {
            actions[n] = new PopupAction(tool);
            n++;
        }
        return actions;
    }
}
