package org.sharpler.glag.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.fusesource.jansi.AnsiConsole;

/// Rendered report contents plus non-fatal messages discovered while rendering.
///
/// @param report complete report text ready to write
/// @param errors messages that should be printed for the user after rendering
public record RenderResult(
    String report,
    List<String> errors
) {
    /// Writes the rendered report to `output`.
    ///
    /// @param output destination file
    /// @throws IOException if the report cannot be written
    public void writeReport(Path output) throws IOException {
        Files.writeString(output, report);
    }

    /// Prints collected rendering errors to the terminal.
    public void printErrors() {
        for (var error : errors) {
            AnsiConsole.err().println(error);
        }
    }
}
