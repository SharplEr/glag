package io.github.sharpler.glag.output;

import java.util.List;

/// Rendered report contents plus non-fatal messages discovered while rendering.
///
/// @param report complete report text ready to write
/// @param errors messages that should be printed for the user after rendering
public record RenderResult(
    String report,
    List<String> errors
) {}
