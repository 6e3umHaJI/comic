package by.bsuir.springbootproject.services;

import org.springframework.web.servlet.ModelAndView;

public interface AdminComplaintService {
    ModelAndView getComplaintsPage(String scope,
                                   String typeId,
                                   String q,
                                   String sortDirection,
                                   int page);

    String updateComplaintStatus(Integer complaintId, Integer statusId);
}
