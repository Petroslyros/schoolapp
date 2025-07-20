package gr.aueb.cf.schoolapp.service;

import gr.aueb.cf.schoolapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.schoolapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.schoolapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.schoolapp.dto.TeacherEditDTO;
import gr.aueb.cf.schoolapp.dto.TeacherInsertDTO;
import gr.aueb.cf.schoolapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.schoolapp.mapper.Mapper;
import gr.aueb.cf.schoolapp.model.Teacher;
import gr.aueb.cf.schoolapp.model.static_data.Region;
import gr.aueb.cf.schoolapp.repository.RegionRepository;
import gr.aueb.cf.schoolapp.repository.TeacherRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service // Marks this class as a Spring-managed service component
@RequiredArgsConstructor // Lombok generates a constructor for all final fields
@Slf4j // Lombok enables logging with 'log' variable (using SLF4J)
public class TeacherService implements ITeacherService {

    // Dependencies injected via constructor
    private final TeacherRepository teacherRepository;
    private final RegionRepository regionRepository;
    private final Mapper mapper;

    /**
     * Saves a new Teacher entity from a TeacherInsertDTO
     * Rolls back the transaction on any exception
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public Teacher saveTeacher(TeacherInsertDTO dto) throws EntityAlreadyExistsException, EntityInvalidArgumentException {

        try {
            // Check if a teacher with the given VAT already exists
            if(teacherRepository.findByVat(dto.getVat()).isPresent()) {
                throw new EntityAlreadyExistsException("Teacher" , "Teacher with vat " + dto.getVat() + " already exists");
            }

            // Fetch the Region by ID or throw if not found
            Region region = regionRepository.findById(dto.getRegionId())
                    .orElseThrow(() -> new EntityInvalidArgumentException("Region", "Invalid Region id "));

            // Map the DTO to a Teacher entity
            Teacher teacher = mapper.mapToTeacherEntity(dto);

            // Add the teacher to the region (bi-directional association)
            region.addTeacher(teacher);

            // Save the teacher to the database
            teacherRepository.save(teacher);

            // Log the successful operation
            log.info("Teacher with vat={} saved.", dto.getVat());

            // Return the saved entity
            return teacher;

        } catch (EntityAlreadyExistsException e) {
            // Log and rethrow if VAT already exists
            log.error("Save failed for teacher with vat={}. Teacher already exists", dto.getVat(), e);
            throw e;

        } catch (EntityInvalidArgumentException e){
            // Log and rethrow if region is invalid
            log.error("Save failed for teacher with vat={}. Region id={} invalid", dto.getVat(), dto.getRegionId(), e);
            throw e;
        }
    }

    /**
     * Retrieves paginated teachers from the database
     * and maps them to read-only DTOs
     */
    @Override
    @Transactional
    public Page<TeacherReadOnlyDTO> getPaginatedTeachers(int page, int size) {
        // Create a pageable object with requested page & size
        Pageable pageable = PageRequest.of(page, size);

        // Get paginated list of teachers from repository
        Page<Teacher> teacherPage = teacherRepository.findAll(pageable);

        // Map each Teacher entity to a TeacherReadOnlyDTO
        return teacherPage.map(mapper::mapToTeacherReadOnlyDTO);
    }

    /**
     * Updates an existing Teacher based on a TeacherEditDTO
     * Rolls back transaction on any exception
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public Teacher updateTeacher(TeacherEditDTO dto) throws EntityAlreadyExistsException, EntityInvalidArgumentException, EntityNotFoundException {

        try {
            // Find the existing teacher by UUID
            Teacher teacher = teacherRepository.findByUuid(dto.getUuid())
                    .orElseThrow(() -> new EntityNotFoundException("Teacher", "Teacher not found"));

            // If VAT is being changed, check if the new VAT is already taken
            if(!teacher.getVat().equals(dto.getVat())) {
                if(teacherRepository.findByVat(dto.getVat()).isEmpty()) {
                    teacher.setVat(dto.getVat());
                } else {
                    throw new EntityAlreadyExistsException("Teacher","Teacher with vat " + dto.getVat() + "already exists");
                }
            }

            // Update first and last name
            teacher.setFirstname(dto.getFirstname());
            teacher.setLastname(dto.getLastname());

            // Check if region has changed
            if(!Objects.equals(teacher.getRegion().getId(), dto.getRegionId())) {
                // Get the new region by ID
                Region region = regionRepository.findById(dto.getRegionId())
                        .orElseThrow(() -> new EntityInvalidArgumentException("Region", "Invalid region id"));

                // Remove teacher from current region (if exists)
                Region currentRegion = teacher.getRegion();
                if(currentRegion != null) {
                    currentRegion.removeTeacher(teacher); // Safe cleanup from old region
                }

                // Add teacher to new region
                region.addTeacher(teacher);
            }

            // Save the updated teacher
            teacherRepository.save(teacher);

            // Log success
            log.info("Teacher with vat={} updated.", dto.getVat());

            // Return updated teacher
            return teacher;

        } catch (EntityNotFoundException e){
            log.error("Update failed for teacher with vat={} Entity not found", dto.getVat(), e);
            throw e;

        } catch (EntityAlreadyExistsException e) {
            log.error("Update failed for teacher with vat={} Entity already exists", dto.getVat(), e);
            throw e;

        } catch (EntityInvalidArgumentException e) {
            log.error("Update failed for teacher with vat={} Region not found with id={}", dto.getVat(), dto.getRegionId(), e);
            throw e;
        }
    }
}

