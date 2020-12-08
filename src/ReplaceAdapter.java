import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

public class ReplaceAdapter extends ProcessAdapter {
    private final ProcessOutput output;
    private final Editor editor;
    private final Project project;
    private final Document document;

    public ReplaceAdapter(Editor editor, Project project, Document document) {
        this.output = new ProcessOutput();
        this.editor = editor;
        this.project = project;
        this.document = document;
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        addToOutput(event.getText(), outputType);
    }

    protected void addToOutput(String text, Key outputType) {
        if (outputType == ProcessOutputTypes.STDOUT) {
            output.appendStdout(text);
        }
        else if (outputType == ProcessOutputTypes.STDERR) {
            output.appendStderr(text);
        }
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        output.setExitCode(event.getExitCode());
        String text = output.getStdout();
        if (!text.isEmpty()) {
            VirtualFileManager.getInstance().asyncRefresh(() -> {
                Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
                int start = primaryCaret.getSelectionStart();
                int end = primaryCaret.getSelectionEnd();
                WriteCommandAction.runWriteCommandAction(project, null, null, () -> {
                    document.replaceString(start, end, text);
                });
            });
        }
    }
}
