package by.bsuir.springbootproject.services;

import org.springframework.web.servlet.ModelAndView;

public interface AdminComplaintService {
    ModelAndView getComplaintsPage(String scope,
                                   String typeId,
                                   String sortDirection,
                                   int page);

    void updateComplaintStatus(Integer complaintId, Integer statusId);
}
