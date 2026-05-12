package dev.incusspawn.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles parameter substitution in tool definitions.
 * Replaces ${param_name} placeholders with actual values.
 * Uses sequential String.replace() to match the existing YamlToolAction.interpolate() pattern.
 */
public class ParameterSubstitutor {

    private final Map<String, String> parameters;

    public ParameterSubstitutor(Map<String, String> parameters) {
        this.parameters = parameters != null ? parameters : Map.of();
    }

    /**
     * Substitute ${param_name} placeholders in a string.
     */
    public String substitute(String template) {
        if (template == null) return null;
        var result = template;
        for (var entry : parameters.entrySet()) {
            result = result.replace("${param_" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * Substitute in a list of strings.
     */
    public List<String> substitute(List<String> templates) {
        if (templates == null) return List.of();
        return templates.stream()
            .map(this::substitute)
            .toList();
    }

    /**
     * Substitute in ToolDef fields, returning a new ToolDef with substituted values.
     * Does NOT modify the original ToolDef.
     */
    public ToolDef substitute(ToolDef original) {
        var substituted = new ToolDef();
        substituted.setName(original.getName());
        substituted.setDescription(original.getDescription());
        substituted.setDownloads(original.getDownloads());  // No substitution needed
        substituted.setPackages(original.getPackages());    // No substitution needed
        substituted.setRequires(original.getRequires());    // No substitution needed
        substituted.setVerify(substitute(original.getVerify()));
        substituted.setParameters(original.getParameters()); // No substitution in definitions

        // Substitute in shell commands
        substituted.setRun(substitute(original.getRun()));
        substituted.setRunAsUser(substitute(original.getRunAsUser()));

        // Substitute in environment variables
        substituted.setEnv(substitute(original.getEnv()));

        // Substitute in file contents
        var substitutedFiles = new ArrayList<ToolDef.FileEntry>();
        for (var file : original.getFiles()) {
            var newFile = new ToolDef.FileEntry();
            newFile.setPath(substitute(file.getPath()));
            newFile.setContent(substitute(file.getContent()));
            newFile.setOwner(file.getOwner());
            substitutedFiles.add(newFile);
        }
        substituted.setFiles(substitutedFiles);

        // Actions are NOT substituted here - they use runtime interpolation
        substituted.setActions(original.getActions());

        return substituted;
    }
}
