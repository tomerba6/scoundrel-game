package com.tomer.scoundrel.tutorial;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * A one-bit local marker: has the player seen the tutorial? A sibling of
 * {@link com.tomer.scoundrel.runs.RunLog} — the path is injected, a missing file
 * simply means "not yet," and {@link #clear()} is a recoverable soft-delete
 * (moves the marker aside to a {@code .bak} sibling) so progress reset can make
 * the game "new" again without destroying anything. Pure Java.
 */
public final class TutorialFlag {

    private final Path file;

    public TutorialFlag(Path file) {
        this.file = file;
    }

    /** True once the tutorial has been seen (or skipped) — i.e. the marker exists. */
    public boolean isSeen() {
        return Files.exists(file);
    }

    /** Record that the player has seen the tutorial; idempotent. */
    public void markSeen() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, "v=1" + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("could not mark the tutorial seen at " + file, e);
        }
    }

    /**
     * Forgets that the tutorial was seen by moving the marker aside to a {@code
     * .bak} sibling (overwriting any earlier backup), so the next launch offers
     * it again while the old marker stays recoverable from disk. A no-op when it
     * was never seen.
     */
    public void clear() {
        try {
            if (Files.exists(file)) {
                Path backup = file.resolveSibling(file.getFileName().toString() + ".bak");
                Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("could not clear " + file, e);
        }
    }
}
