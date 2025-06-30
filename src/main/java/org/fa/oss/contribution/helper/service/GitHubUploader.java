package org.fa.oss.contribution.helper.service;

import java.nio.file.Files;
import java.nio.file.Path;
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
}
