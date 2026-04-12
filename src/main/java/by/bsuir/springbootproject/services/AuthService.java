package by.bsuir.springbootproject.services;

import by.bsuir.springbootproject.dto.GoogleRegistrationForm;
import by.bsuir.springbootproject.dto.PendingGoogleRegistration;
import by.bsuir.springbootproject.entities.User;

public interface AuthService {
    User completeGoogleRegistration(PendingGoogleRegistration pending, GoogleRegistrationForm form);
}