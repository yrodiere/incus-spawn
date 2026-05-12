package dev.incusspawn.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * A tool installation defined in YAML. Each definition declares
 * packages to install, shell commands to run, files to write, and
 * environment variables to export.
 */
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolDef {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private String name;
    private String description = "";
    private List<DownloadEntry> downloads = List.of();
    private List<String> packages = List.of();
    private List<String> run = List.of();
    @JsonProperty("run_as_user")
    private List<String> runAsUser = List.of();
    private List<FileEntry> files = List.of();
    private List<String> env = List.of();
    @JsonDeserialize(using = ToolRef.Deserializer.class)
    private List<ToolRef> requires = List.of();
    private String verify;
    private List<ActionEntry> actions = List.of();
    private Map<String, ParameterDef> parameters = Map.of();

    private transient volatile String cachedFingerprint;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<DownloadEntry> getDownloads() { return downloads; }
    public void setDownloads(List<DownloadEntry> downloads) { this.downloads = downloads; }
    public List<String> getPackages() { return packages; }
    public void setPackages(List<String> packages) { this.packages = packages; }
    public List<String> getRun() { return run; }
    public void setRun(List<String> run) { this.run = run; }
    public List<String> getRunAsUser() { return runAsUser; }
    public void setRunAsUser(List<String> runAsUser) { this.runAsUser = runAsUser; }
    public List<FileEntry> getFiles() { return files; }
    public void setFiles(List<FileEntry> files) { this.files = files; }
    public List<String> getEnv() { return env; }
    public void setEnv(List<String> env) { this.env = env; }
    public List<ToolRef> getRequires() { return requires; }
    public void setRequires(List<ToolRef> requires) { this.requires = requires; }
    public String getVerify() { return verify; }
    public void setVerify(String verify) { this.verify = verify; }
    public List<ActionEntry> getActions() { return actions; }
    public void setActions(List<ActionEntry> actions) { this.actions = actions; }
    public Map<String, ParameterDef> getParameters() { return parameters; }
    public void setParameters(Map<String, ParameterDef> parameters) {
        this.parameters = parameters != null ? parameters : Map.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DownloadEntry {
        private String url;
        private String sha256;
        private String extract;
        private Map<String, String> links = Map.of();

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getSha256() { return sha256; }
        public void setSha256(String sha256) { this.sha256 = sha256; }
        public String getExtract() { return extract; }
        public void setExtract(String extract) { this.extract = extract; }
        public Map<String, String> getLinks() { return links; }
        public void setLinks(Map<String, String> links) { this.links = links; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileEntry {
        private String path;
        private String content;
        private String owner;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionEntry {
        private String label;
        private String type;
        @JsonProperty("requires_running")
        private boolean requiresRunning = true;
        private String expand;
        private String url;
        private String command;
        private String text;
        @JsonProperty("auto_return")
        private boolean autoReturn = false;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public boolean isRequiresRunning() { return requiresRunning; }
        public void setRequiresRunning(boolean requiresRunning) { this.requiresRunning = requiresRunning; }
        public String getExpand() { return expand; }
        public void setExpand(String expand) { this.expand = expand; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public boolean isAutoReturn() { return autoReturn; }
        public void setAutoReturn(boolean autoReturn) { this.autoReturn = autoReturn; }
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolRef {
        private String name;
        @JsonProperty("params")
        private Map<String, String> params = Map.of();

        public ToolRef() {}

        public ToolRef(String name) {
            this.name = name;
            this.params = Map.of();
        }

        public ToolRef(String name, Map<String, String> params) {
            this.name = name;
            this.params = params != null ? params : Map.of();
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Map<String, String> getParams() { return params; }
        public void setParams(Map<String, String> params) {
            this.params = params != null ? params : Map.of();
        }

        public static class Deserializer extends StdDeserializer<List<ToolRef>> {
            public Deserializer() { super(List.class); }

            @Override
            public List<ToolRef> deserialize(JsonParser p, DeserializationContext ctxt)
                    throws IOException {
                var result = new ArrayList<ToolRef>();

                if (p.currentToken() != JsonToken.START_ARRAY) {
                    throw new IOException("Expected array for requires field");
                }

                while (p.nextToken() != JsonToken.END_ARRAY) {
                    if (p.currentToken() == JsonToken.VALUE_STRING) {
                        // Simple string form: "maven-3"
                        result.add(new ToolRef(p.getText()));
                    } else if (p.currentToken() == JsonToken.START_OBJECT) {
                        // Object form: { name: "tool", param1: "value1", ... }
                        String name = null;
                        Map<String, String> params = new LinkedHashMap<>();

                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            var field = p.currentName();
                            p.nextToken();
                            if ("name".equals(field)) {
                                name = p.getText();
                            } else if ("params".equals(field)) {
                                // Handle nested params object (from JSON serialization)
                                if (p.currentToken() == JsonToken.START_OBJECT) {
                                    while (p.nextToken() != JsonToken.END_OBJECT) {
                                        var paramName = p.currentName();
                                        p.nextToken();
                                        params.put(paramName, p.getValueAsString());
                                    }
                                }
                            } else {
                                // All other fields are parameters (flat form)
                                params.put(field, p.getValueAsString());
                            }
                        }

                        if (name != null) {
                            result.add(new ToolRef(name, params));
                        } else {
                            throw new IOException("Tool object in requires must have a 'name' field");
                        }
                    }
                }

                return result;
            }
        }
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParameterDef {
        private String type;  // "string", "integer", "boolean", "enum"
        @JsonProperty("default")
        private String defaultValue;
        private String description;
        private String pattern;  // regex for string validation
        private Integer min;     // for integer type
        private Integer max;     // for integer type
        private List<String> options = List.of();  // for enum type

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDefault() { return defaultValue; }
        public void setDefault(String defaultValue) { this.defaultValue = defaultValue; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        public Integer getMin() { return min; }
        public void setMin(Integer min) { this.min = min; }
        public Integer getMax() { return max; }
        public void setMax(Integer max) { this.max = max; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
    }

    public String contentFingerprint() {
        var result = cachedFingerprint;
        if (result != null) return result;
        var sb = new StringBuilder();
        for (var d : downloads) {
            sb.append("dl=").append(d.url).append(',').append(d.sha256)
                    .append(',').append(d.extract);
            new TreeMap<>(d.links).forEach((k, v) -> sb.append(',').append(k).append('=').append(v));
            sb.append('\n');
        }
        packages.stream().sorted().forEach(p -> sb.append("pkg=").append(p).append('\n'));
        run.forEach(r -> sb.append("run=").append(r).append('\n'));
        runAsUser.forEach(r -> sb.append("run_as_user=").append(r).append('\n'));
        for (var f : files) {
            sb.append("file=").append(f.path).append(',').append(f.content)
                    .append(',').append(f.owner).append('\n');
        }
        env.stream().sorted().forEach(e -> sb.append("env=").append(e).append('\n'));
        for (var r : requires.stream().sorted(Comparator.comparing(ToolRef::getName)).toList()) {
            sb.append("requires=").append(r.getName());
            if (!r.getParams().isEmpty()) {
                r.getParams().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sb.append(',').append(e.getKey()).append('=').append(e.getValue()));
            }
            sb.append('\n');
        }
        if (verify != null) sb.append("verify=").append(verify).append('\n');
        parameters.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> {
                    var p = e.getValue();
                    sb.append("param=").append(e.getKey()).append(',')
                      .append(p.getType()).append(',')
                      .append(p.getDefault()).append(',')
                      .append(p.getPattern()).append(',')
                      .append(p.getMin()).append(',')
                      .append(p.getMax()).append(',');
                    if (p.getOptions() != null && !p.getOptions().isEmpty()) {
                        p.getOptions().stream().sorted().forEach(opt -> sb.append(opt).append(';'));
                    }
                    sb.append('\n');
                });
        result = sha256hex(sb.toString());
        cachedFingerprint = result;
        return result;
    }

    /**
     * Compute composite fingerprints that incorporate transitive deps.
     * Each tool's entry in the result includes the fingerprints of all its
     * transitive dependencies, so a change in any dep propagates upward.
     *
     * @param rawFingerprints  tool name → own contentFingerprint()
     * @param depMap           tool name → list of required tool names
     * @return tool name → composite fingerprint (includes dep tree)
     */
    public static Map<String, String> compositeFingerprints(
            Map<String, String> rawFingerprints, Map<String, java.util.List<String>> depMap) {
        var cache = new TreeMap<String, String>();
        for (var name : rawFingerprints.keySet()) {
            resolveComposite(name, rawFingerprints, depMap, cache, new java.util.LinkedHashSet<>());
        }
        return cache;
    }

    private static String resolveComposite(String name, Map<String, String> rawFps,
                                            Map<String, java.util.List<String>> depMap,
                                            Map<String, String> cache,
                                            java.util.Set<String> resolving) {
        if (cache.containsKey(name)) return cache.get(name);
        if (!resolving.add(name)) return rawFps.getOrDefault(name, "");
        var sb = new StringBuilder(rawFps.getOrDefault(name, ""));
        var deps = depMap.getOrDefault(name, java.util.List.of());
        for (var dep : deps.stream().sorted().toList()) {
            sb.append("+dep=").append(dep).append(':')
                    .append(resolveComposite(dep, rawFps, depMap, cache, resolving));
        }
        var result = deps.isEmpty() ? sb.toString() : sha256hex(sb.toString());
        cache.put(name, result);
        resolving.remove(name);
        return result;
    }

    public static String sha256hex(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static ToolDef loadFromStream(InputStream is) throws IOException {
        return YAML.readValue(is, ToolDef.class);
    }
}
