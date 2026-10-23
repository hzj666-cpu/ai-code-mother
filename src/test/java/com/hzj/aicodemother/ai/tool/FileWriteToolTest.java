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
}
