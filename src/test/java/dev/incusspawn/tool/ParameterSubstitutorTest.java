package dev.incusspawn.tool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParameterSubstitutorTest {

    @Test
    void testSimpleSubstitution() {
        var substitutor = new ParameterSubstitutor(Map.of("memory", "8g"));

        assertEquals("-Xmx8g", substitutor.substitute("-Xmx${param_memory}"));
    }

    @Test
    void testMultipleSubstitutions() {
        var substitutor = new ParameterSubstitutor(
            Map.of("memory", "8g", "port", "9000")
        );

        var result = substitutor.substitute("Memory: ${param_memory}, Port: ${param_port}");
        assertEquals("Memory: 8g, Port: 9000", result);
    }

    @Test
    void testNoSubstitution() {
        var substitutor = new ParameterSubstitutor(Map.of("memory", "8g"));

        assertEquals("No params here", substitutor.substitute("No params here"));
    }

    @Test
    void testNullInput() {
        var substitutor = new ParameterSubstitutor(Map.of("memory", "8g"));

        assertNull(substitutor.substitute((String) null));
    }

    @Test
    void testEmptyParameters() {
        var substitutor = new ParameterSubstitutor(Map.of());

        assertEquals("${param_memory}", substitutor.substitute("${param_memory}"));
    }

    @Test
    void testListSubstitution() {
        var substitutor = new ParameterSubstitutor(Map.of("memory", "8g"));

        var result = substitutor.substitute(
            List.of("export MEM=${param_memory}", "echo ${param_memory}")
        );

        assertEquals(2, result.size());
        assertEquals("export MEM=8g", result.get(0));
        assertEquals("echo 8g", result.get(1));
    }

    @Test
    void testToolDefSubstitution() {
        var substitutor = new ParameterSubstitutor(
            Map.of("memory", "8g", "port", "9000")
        );

        var original = new ToolDef();
        original.setName("test-tool");
        original.setDescription("Test tool");
        original.setRun(List.of("echo Memory: ${param_memory}"));
        original.setRunAsUser(List.of("export PORT=${param_port}"));
        original.setEnv(List.of("MEMORY=${param_memory}"));

        var fileEntry = new ToolDef.FileEntry();
        fileEntry.setPath("/tmp/config");
        fileEntry.setContent("memory=${param_memory}\nport=${param_port}");
        fileEntry.setOwner("root");
        original.setFiles(List.of(fileEntry));

        var substituted = substitutor.substitute(original);

        assertEquals("test-tool", substituted.getName());
        assertEquals("echo Memory: 8g", substituted.getRun().get(0));
        assertEquals("export PORT=9000", substituted.getRunAsUser().get(0));
        assertEquals("MEMORY=8g", substituted.getEnv().get(0));
        assertEquals("memory=8g\nport=9000", substituted.getFiles().get(0).getContent());
    }

    @Test
    void testToolDefPreservesNonSubstitutedFields() {
        var substitutor = new ParameterSubstitutor(Map.of("memory", "8g"));

        var original = new ToolDef();
        original.setName("test-tool");
        original.setPackages(List.of("pkg1", "pkg2"));
        original.setRequires(List.of("dep1"));

        var substituted = substitutor.substitute(original);

        assertEquals(original.getName(), substituted.getName());
        assertEquals(original.getPackages(), substituted.getPackages());
        assertEquals(original.getRequires(), substituted.getRequires());
    }
}
