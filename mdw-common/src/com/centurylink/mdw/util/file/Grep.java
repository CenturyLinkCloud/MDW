package com.centurylink.mdw.util.file;

import com.centurylink.mdw.model.Jsonable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Recursively searches linewise through all files matching a glob pattern.
 */
public class Grep {

    private Path root;
    private List<PathMatcher> matchers = new ArrayList<>();
    private int count;

    /**
     * Line limit.
     */
    private int limit = Integer.MAX_VALUE;
    public int getLimit() {
        return limit;
    }
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @param root root path
     * @param globs files pattern(s)
     */
    public Grep(Path root, String... globs) {
        this.root = root;
        if (globs != null) {
            for (String glob : globs) {
                this.matchers.add(FileSystems.getDefault().getPathMatcher(
                        "glob:" + root.toString().replace('\\', '/') + "/" + glob.replace('\\', '/')));
            }
        }
    }

    /**
     * Find all line matches (mapped per file path).
     */
    public Map<Path,List<LineMatches>> find(String search) throws IOException {
        count = 0;
        Pattern pattern = Pattern.compile(search);
        Map<Path,List<LineMatches>> fileMatches = new LinkedHashMap<>();
        Files.walkFileTree(root,
            EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    for (PathMatcher matcher : matchers) {
                        if (matcher.matches(path)) {
                            List<LineMatches> lineMatches = search(path, pattern);
                            if (lineMatches != null)
                                fileMatches.put(root.relativize(path), lineMatches);
                        }
                    }
                    return count == 0 || count < limit ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
                }
            }
        );

        return fileMatches;
    }

    private int lineIndex;
    private Matcher lineMatcher;
    private Map<Integer,LineMatches> matchMap;
    protected List<LineMatches> search(Path path, Pattern pattern) throws IOException {
        lineIndex = 0;
        lineMatcher = null;
        matchMap = null;
        try (Stream<String> stream = Files.lines(path)) {
            stream.forEachOrdered(line -> {
                if (count < limit) {
                    if (lineMatcher == null)
                        lineMatcher = pattern.matcher(line);
                    else
                        lineMatcher.reset(line);

                    while (lineMatcher.find()) {
                        if (matchMap == null)
                            matchMap = new LinkedHashMap<>();

                        LineMatches matches = matchMap.get(lineIndex);
                        if (matches == null) {
                            matches = new LineMatches(lineIndex, line);
                            matchMap.put(lineIndex, matches);
                            count++;
                        }
                        matches.getMatches().add(new LineMatch(lineMatcher.start(), lineMatcher.end()));
                    }
                }
                lineIndex++;
            });
            List<LineMatches> lineMatches = null;
            if (matchMap != null) { // NOSONAR
                lineMatches = new ArrayList<>();
                for (int index : matchMap.keySet()) {
                    lineMatches.add(matchMap.get(index));
                }
            }
            return lineMatches;
        }
        catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    public class LineMatches implements Jsonable {

        LineMatches(int index, String line) {
            this.index = index;
            this.line = line;
            this.matches = new ArrayList<>();
        }

        private int index;
        public int getIndex() { return index; }

        private String line;
        public String getLine() { return line; }

        private List<LineMatch> matches;
        public List<LineMatch> getMatches() { return matches; }
    }

    public class LineMatch implements Jsonable {

        LineMatch(int start, int end) {
            this.start = start;
            this.end = end;
        }

        private int start;
        public int getStart() { return start; }

        private int end;
        public int getEnd() { return end; }
    }

    /**
     * TODO: color
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Syntax: grep <pattern> <glob>");
            System.exit(-1);
        }
        Grep grep = new Grep(Paths.get("."), args[1].split("\\s+"));
        Map<Path,List<LineMatches>> pathMatches = grep.find(args[0]);

        for (Path path : pathMatches.keySet()) {
            System.out.println(path + ":");
            Optional<LineMatches> lineMatch = pathMatches.get(path).stream().reduce((max, lineMatches) -> {
                return lineMatches.index > max.index ? lineMatches : max;
            });
            int maxIndex = 0;
            if (lineMatch.isPresent()) {
                maxIndex = lineMatch.get().index;
            }
            int padLen = 2 + String.valueOf(maxIndex).length();
            for (LineMatches lineMatches : pathMatches.get(path)) {
                String idx = String.valueOf(lineMatches.index);
                while (idx.length() < padLen) {
                    idx = " " + idx;
                }
                System.out.println(idx + ": " + lineMatches.line);
            }

        }
        if (grep.count >= grep.limit)
            System.err.println("(Showing first " + grep.count + " results)");
        System.exit(grep.count);
    }
}