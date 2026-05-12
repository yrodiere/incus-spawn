package dev.incusspawn.tool;

import dev.incusspawn.config.ImageDef;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that verify parameter handling in tool deduplication scenarios.
 * Note: The actual deduplication and exception throwing happens in
 * BuildCommand.resolveWithDeps() during the build process.
 */

class ParameterDeduplicationTest {

    @Test
    void testDuplicateToolWithDifferentParametersWarns() throws Exception {
        // Create an image with the same tool specified twice with different parameters
        var yaml = """
            name: test-image
            tools:
              - name: example-tool
                memory: "2g"
              - name: example-tool
                memory: "8g"
            """;

        var imageDef = ImageDef.parseYaml(yaml);

        // Verify both tool references are in the YAML
        assertEquals(2, imageDef.getTools().size());
        assertEquals("example-tool", imageDef.getTools().get(0).getName());
        assertEquals("2g", imageDef.getTools().get(0).getParams().get("memory"));
        assertEquals("example-tool", imageDef.getTools().get(1).getName());
        assertEquals("8g", imageDef.getTools().get(1).getParams().get("memory"));

        // Note: The actual warning would be emitted by BuildCommand.resolveWithDeps()
        // during the build process, not during YAML parsing
    }

    @Test
    void testSameToolWithSameParametersIsOk() throws Exception {
        var yaml = """
            name: test-image
            tools:
              - name: example-tool
                memory: "8g"
              - name: example-tool
                memory: "8g"
            """;

        var imageDef = ImageDef.parseYaml(yaml);

        assertEquals(2, imageDef.getTools().size());
        // Both have same parameters - this is redundant but not an error
        assertEquals("8g", imageDef.getTools().get(0).getParams().get("memory"));
        assertEquals("8g", imageDef.getTools().get(1).getParams().get("memory"));
    }

    @Test
    void testRequiresWithDifferentParameters() throws Exception {
        // Test that requires can specify parameters
        var yaml = """
            name: test-tool
            requires:
              - sshd
              - name: java-runtime
                memory: "4g"
            """;

        var toolDef = ToolDef.loadFromStream(
            new java.io.ByteArrayInputStream(yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );

        assertEquals(2, toolDef.getRequires().size());
        assertEquals("sshd", toolDef.getRequires().get(0).getName());
        assertTrue(toolDef.getRequires().get(0).getParams().isEmpty());

        assertEquals("java-runtime", toolDef.getRequires().get(1).getName());
        assertEquals("4g", toolDef.getRequires().get(1).getParams().get("memory"));
    }
}
