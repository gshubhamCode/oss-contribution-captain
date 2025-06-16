package org.fa.oss.contribution.helper.utility;

import org.fa.oss.contribution.helper.constants.Github;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class GithubApiHelper {

  public String getIssuesUrl(int pageNumber) throws URISyntaxException {
    return getPaginatedUrl(Github.SEARCH_URL, pageNumber);
  }

  public String getPaginatedUrl(String endpointUrl, int pageNumber) throws URISyntaxException {
    String url = Github.GIT_API_BASE_URL + endpointUrl;
    return UriComponentsBuilder.fromUri(new URI(url))
        .queryParam("q", "label:\"good first issue\"+state:open+stars:>50")
        .queryParam("sort", "updated")
        .queryParam("order", "desc")
        .queryParam("page", pageNumber)
        .queryParam("per_page", Github.PAGE_SIZE)
        .build()
        .toUriString();
  }

  public String getRepoUrl(String repoFullName) throws URISyntaxException {
    return UriComponentsBuilder.fromUri(new URI(Github.GIT_API_BASE_URL))
        .path(Github.REPO_URL)
        .path(repoFullName)
        .build()
        .toUriString();
  }
}
