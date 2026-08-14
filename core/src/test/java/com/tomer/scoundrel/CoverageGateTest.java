package com.tomer.scoundrel;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The coverage gate names the packages it protects one by one
 * ({@code includes = [...]} in {@code core/build.gradle}), which is deliberate —
 * an allow-list cannot silently stop matching the way a filter can.
 *
 * <p>It has one hazard, and this is it: a <b>new</b> pure package is not gated
 * until somebody remembers to add it to that list, and nothing about forgetting
 * looks like a failure. The build stays green, the badge stays high, and a whole
 * package quietly sits outside the thing that is supposed to be protecting it.
 *
 * <p>So the rule is checked rather than remembered: <b>every package under
 * {@code com.tomer.scoundrel} that imports no LibGDX must appear in the
 * gate.</b> Purity is the test for whether a package <em>can</em> be gated — a
 * package with no {@code com.badlogic.gdx} import is headlessly testable by
 * construction, so there is never a reason for it not to be held to the
 * threshold.
 *
 * <p>The root package is not a candidate: {@code ScoundrelGame} lives there and
 * is GL-bound, so the package as a whole is not pure even though {@code CrashLog}
 * and {@code Progress} are. Gating is per package, and that is the granularity
 * Gradle offers.
 */
class CoverageGateTest {

    private static final Path BUILD_FILE = Path.of("build.gradle");
    private static final Path SOURCES =
            Path.of("src", "main", "java", "com", "tomer", "scoundrel");

    /** The gate lists them fully qualified; only the last segment varies. */
    private static final Pattern GATED =
            Pattern.compile("'com\\.tomer\\.scoundrel\\.([a-z][a-z0-9]*)'");

    @Test
    void everyPurePackageIsGated() throws IOException {
        Set<String> gated = gatedPackages();
        List<String> ungated = new ArrayList<>();
        for (String pkg : purePackages()) {
            if (!gated.contains(pkg)) {
                ungated.add(pkg);
            }
        }
        assertTrue(ungated.isEmpty(),
                "these packages import no LibGDX, so they are headlessly testable and belong "
                        + "in the coverage gate's includes list in core/build.gradle: " + ungated
                        + "\n  currently gated: " + gated);
    }

    @Test
    void everyGatedPackageStillExists() throws IOException {
        Set<String> present = new TreeSet<>();
        try (Stream<Path> dirs = Files.list(SOURCES)) {
            dirs.filter(Files::isDirectory)
                    .forEach(d -> present.add(d.getFileName().toString()));
        }
        List<String> missing = new ArrayList<>();
        for (String pkg : gatedPackages()) {
            if (!present.contains(pkg)) {
                missing.add(pkg);
            }
        }
        assertTrue(missing.isEmpty(),
                "the gate names packages that no longer exist — a rename would otherwise leave "
                        + "the rule quietly matching nothing: " + missing);
    }

    /** The `includes` list as the build file actually declares it. */
    private Set<String> gatedPackages() throws IOException {
        String build = Files.readString(BUILD_FILE);
        Set<String> found = new TreeSet<>();
        Matcher m = GATED.matcher(build);
        while (m.find()) {
            found.add(m.group(1));
        }
        assertTrue(found.size() >= 2,
                "could not parse the gate's includes list out of core/build.gradle — the format "
                        + "changed and this test is no longer reading it");
        return found;
    }

    /** A package is a candidate for gating when nothing in it imports LibGDX. */
    private Set<String> purePackages() throws IOException {
        Set<String> pure = new TreeSet<>();
        try (Stream<Path> dirs = Files.list(SOURCES)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                if (!importsLibGdx(dir)) {
                    pure.add(dir.getFileName().toString());
                }
            }
        }
        return pure;
    }

    private boolean importsLibGdx(Path dir) throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (Files.readString(file).contains("com.badlogic.gdx")) {
                    return true;
                }
            }
        }
        return false;
    }
}
