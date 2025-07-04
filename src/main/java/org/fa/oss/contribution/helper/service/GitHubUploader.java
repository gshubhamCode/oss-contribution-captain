package org.fa.oss.contribution.helper.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.kohsuke.github.*;

public class GitHubUploader {

  private final GitHub github;
  private final GHRepository repo;


  public GitHubUploader(String token, String repoName) throws Exception {
    this.github = new GitHubBuilder().withOAuthToken(token).build();
    this.repo = github.getRepository(repoName);
  }

  public void uploadJsonFile(String localFilePath, String targetPath, String commitMessage)
      throws Exception {
    byte[] contentBytes = Files.readAllBytes(Path.of(localFilePath));
    try {
      GHContent file = repo.getFileContent(targetPath);
      file.update(new String(contentBytes), commitMessage); // updates existing file
    } catch (GHFileNotFoundException e) {
      repo.createContent()
          .path(targetPath)
          .content(new String(contentBytes)) // creates new file if it doesn't exist
          .message(commitMessage)
          .commit();
    }
  }
  public void uploadFolderInZip(String folderPath, String targetPath, String commitMessage) throws Exception {
    // Create temp zip file path
    Path tempZip = Files.createTempFile("logs-", ".zip");

    try {
      // Zip the folder into tempZip
      zipFolder(folderPath, tempZip.toString());

      // Upload the zip file to GitHub
      uploadFile(tempZip.toString(), targetPath, commitMessage);
    } finally {
      // Clean up temp zip file
      Files.deleteIfExists(tempZip);
    }
  }

  // Helper method to zip folder
  private void zipFolder(String sourceDirPath, String zipFilePath) throws IOException {
    try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(Paths.get(zipFilePath)))) {
      Path pp = Paths.get(sourceDirPath);
      Files.walk(pp)
          .filter(path -> !Files.isDirectory(path))
          .forEach(
              path -> {
                ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString().replace("\\", "/"));
                try {
                  zs.putNextEntry(zipEntry);
                  Files.copy(path, zs);
                  zs.closeEntry();
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    }
  }

  public void uploadFile(String localFilePath, String targetPath, String commitMessage) throws Exception {
    byte[] contentBytes = Files.readAllBytes(Path.of(localFilePath));
    try {
      GHContent file = repo.getFileContent(targetPath);
      file.update(contentBytes, commitMessage); // pass byte[] for update
    } catch (GHFileNotFoundException e) {
      repo.createContent()
              .path(targetPath)
              .content(contentBytes) // pass byte[] for create
              .message(commitMessage)
              .commit();
    }
  }


}
