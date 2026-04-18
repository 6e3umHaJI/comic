package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.entities.ComplaintType;

import java.util.List;

public interface ComplaintService {
    List<ComplaintType> getComplaintTypesForScope(String scope);

    void submitComplaint(Integer userId,
                         Integer targetId,
                         Integer complaintTypeId,
                         String description);
}
