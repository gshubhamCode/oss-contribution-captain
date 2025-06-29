package org.fa.oss.contribution.helper.controller;

import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.IssueSummaryResultListDTO;
import org.fa.oss.contribution.helper.service.OllamaSummaryService;
import org.fa.oss.contribution.helper.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/summaries")
public class SummaryController {

    @Autowired
    private  SummaryService summaryService;


    @GetMapping
    public IssueSummaryResultListDTO getCachedSummaries(@RequestParam(defaultValue = "0") int limit) {
        return summaryService.getSummaries(limit);
    }

    @GetMapping("/refresh")
    public IssueSummaryResultListDTO refreshSummaries(@RequestParam(defaultValue = "0") int limit) {
        return summaryService.generateSummaries(limit);
    }

    @PostMapping
    public IssueSummaryResultListDTO getSummary( @RequestBody List<IssueDTO> issueDTO) {
        return summaryService.generateSummaries(issueDTO);
    }

}

