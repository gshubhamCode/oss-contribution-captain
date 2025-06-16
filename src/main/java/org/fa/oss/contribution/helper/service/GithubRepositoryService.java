package org.fa.oss.contribution.helper.service;

import org.fa.oss.contribution.helper.model.GithubRepository;
import org.fa.oss.contribution.helper.utility.GithubApiHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;

@Service
public class GithubRepositoryService {

  @Autowired GithubApiHelper githubApiHelper;

  RestTemplate restTemplate = new RestTemplate();

  public GithubRepository getRepository(String repoFullName) throws URISyntaxException {
    String url = githubApiHelper.getRepoUrl(repoFullName);
    ResponseEntity<GithubRepository> response =
        restTemplate.getForEntity(url, GithubRepository.class);
    if(response.getStatusCode() == HttpStatus.OK){
      return  response.getBody();
    }

    return null;
  }
}
