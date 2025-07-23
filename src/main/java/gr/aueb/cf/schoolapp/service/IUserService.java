package gr.aueb.cf.schoolapp.service;

import gr.aueb.cf.schoolapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.schoolapp.dto.UserInsertDTO;
import org.springframework.stereotype.Service;


public interface IUserService {
    void saveUser(UserInsertDTO userInsertDTO) throws EntityAlreadyExistsException;

}
