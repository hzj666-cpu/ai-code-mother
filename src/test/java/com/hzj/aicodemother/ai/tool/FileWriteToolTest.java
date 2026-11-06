package com.hzj.aicodemother.ai.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FileWriteToolTest {

    private final FileWriteTool tool = new FileWriteTool();

    @Test
    void rejectsPathsOutsideTheVueProject() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.writeFile("../../outside.txt", "content", 1L));
        assertThrows(IllegalArgumentException.class,
                () -> tool.writeFile("C:\\outside.txt", "content", 1L));
        assertThrows(IllegalArgumentException.class,
                () -> tool.writeFile("/outside.txt", "content", 1L));
    }

    @Test
    void rejectsPlatformFilesBeforeAccessingProject() {
        assertThrows(IllegalArgumentException.class,
                () -> tool.writeFile("package.json", "{}", 1L));
        assertThrows(IllegalArgumentException.class,
                () -> tool.writeFile("vite.config.js", "unsafe", 1L));
        assertThrows(IllegalArgumentException.class,
                () -> tool.writeFile("src/pages/.env", "unsafe", 1L));
    }
}
