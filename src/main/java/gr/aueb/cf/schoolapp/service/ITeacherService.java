package gr.aueb.cf.schoolapp.service;

import gr.aueb.cf.schoolapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.schoolapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.schoolapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.schoolapp.dto.TeacherEditDTO;
import gr.aueb.cf.schoolapp.dto.TeacherInsertDTO;
import gr.aueb.cf.schoolapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.schoolapp.model.Teacher;
import org.springframework.data.domain.Page;

public interface ITeacherService {
    Teacher saveTeacher(TeacherInsertDTO teacherInsertDTO) throws EntityAlreadyExistsException, EntityInvalidArgumentException;

    //page is a generic class that contains info about the paginated page
    Page<TeacherReadOnlyDTO> getPaginatedTeachers(int page, int size);

    void updateTeacher(TeacherEditDTO teacherEditDTO) throws EntityAlreadyExistsException, EntityInvalidArgumentException, EntityNotFoundException;

    void deleteTeacherByUUID(String uuid) throws EntityNotFoundException;
}
