package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.PaperSearchDTO;
import com.dealwithpapers.dealwithpapers.dto.PaperDTO;
import java.util.List;
 
public interface ConferenceArxivService {
    List<PaperDTO> searchConferencePapers(PaperSearchDTO searchDTO);
    List<PaperDTO> searchArxivPapers(PaperSearchDTO searchDTO);
} 