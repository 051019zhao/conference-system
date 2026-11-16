package com.scut.conference.service;

import com.scut.conference.common.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {
    @TempDir Path uploadDir;

    @Test
    void acceptsWordProofAndKeepsItUnderUploadDirectory() {
        FileStorageService storage = storage();
        MockMultipartFile proof = new MockMultipartFile("file", "proof.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{'P', 'K', 3, 4, 1, 2});
        String stored = storage.storeSupplement(proof);
        assertTrue(Path.of(stored).startsWith(uploadDir));
        assertTrue(Path.of(stored).toFile().isFile());
    }

    @Test
    void rejectsOversizedProofAndWrongWordHeader() {
        FileStorageService storage = storage();
        MockMultipartFile tooLarge = new MockMultipartFile("file", "proof.zip", "application/zip",
                new byte[10 * 1024 * 1024 + 1]);
        assertThrows(BizException.class, () -> storage.storeSupplement(tooLarge));
        MockMultipartFile fakeWord = new MockMultipartFile("file", "proof.docx", "application/zip",
                new byte[]{'N', 'O', 'T', 'Z', 'I', 'P'});
        assertThrows(BizException.class, () -> storage.storeSupplement(fakeWord));
    }

    private FileStorageService storage() {
        FileStorageService storage = new FileStorageService();
        ReflectionTestUtils.setField(storage, "baseDir", uploadDir.toString());
        return storage;
    }
}
