package com.dealwithpapers.dealwithpapers.controller;

import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import com.dealwithpapers.dealwithpapers.dto.PaperSearchDTO;
import com.dealwithpapers.dealwithpapers.service.ConferenceArxivService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conference-arxiv")
public class ConferenceArxivController {
    @Autowired
    private ConferenceArxivService conferenceArxivService;

    @PostMapping("/conference")
    public List<PaperDTO> searchConferencePapers(@RequestBody PaperSearchDTO searchDTO) {
        return conferenceArxivService.searchConferencePapers(searchDTO);
    }

    @PostMapping("/arxiv")
    public List<PaperDTO> searchArxivPapers(@RequestBody PaperSearchDTO searchDTO) {
        return conferenceArxivService.searchArxivPapers(searchDTO);
    }
} 