import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.NopProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.util.ExecutionErrorDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.tools.Tool;
import com.intellij.tools.ToolAction;
import com.intellij.tools.ToolProcessAdapter;
import com.intellij.tools.ToolsBundle;
import com.intellij.tools.ToolsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PopupAction extends AnAction {
    private final String actionId;

    public PopupAction(@NotNull Tool tool) {
        actionId = tool.getActionId();
        getTemplatePresentation().setText(tool.getName(), false);
        getTemplatePresentation().setDescription(tool.getDescription());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        final Document document = editor.getDocument();
        Tool tool = findTool(actionId);
        if (tool != null) {
            ReplaceAdapter processListener = new ReplaceAdapter(editor, project, document);
            execute(tool, ToolAction.getToolDataContext(event.getDataContext()), processListener);
        }
    }

    @Nullable
    private static Tool findTool(@NotNull String actionId) {
        for (Tool tool: ToolsProvider.getAllTools()) {
            if (actionId.equals(tool.getActionId())) {
                return tool;
            }
        }
        return null;
    }

    private void execute(Tool tool, DataContext context, @Nullable final ProcessListener processListener) {
        if (!executeIfPossible(tool, context, processListener)) {
            notifyCouldNotStart(processListener);
        }
    }

    private boolean executeIfPossible(Tool tool, DataContext context, @Nullable final ProcessListener processListener) {
        final Project project = CommonDataKeys.PROJECT.getData(context);
        if (project == null) {
            return false;
        }

        FileDocumentManager.getInstance().saveAllDocuments();
        try {
            GeneralCommandLine commandLine = tool.createCommandLine(context);
            if (commandLine == null) {
                return false;
            }
            OSProcessHandler handler = new OSProcessHandler(commandLine);
            handler.addProcessListener(new ToolProcessAdapter(project, tool.synchronizeAfterExecution(), tool.getName()));
            if (processListener != null) {
                handler.addProcessListener(processListener);
            }
            handler.startNotify();
        } catch (ExecutionException ex) {
            ExecutionErrorDialog.show(ex, ToolsBundle.message("tools.process.start.error"), project);
            notifyCouldNotStart(processListener);
            return false;
        }
        return true;
    }

    private static void notifyCouldNotStart(@Nullable ProcessListener listener) {
        if (listener != null) listener.processTerminated(new ProcessEvent(new NopProcessHandler(), -1));
    }
}
