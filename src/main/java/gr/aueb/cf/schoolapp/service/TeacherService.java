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

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherService implements ITeacherService {

    private final TeacherRepository teacherRepository;
    private final RegionRepository regionRepository;
    private final Mapper mapper;

//    public TeacherService(TeacherRepository teacherRepository, RegionRepository regionRepository, Mapper mapper) {
//        this.teacherRepository = teacherRepository;
//        this.regionRepository = regionRepository;
//        this.mapper = mapper;
//    }


    @Override
    @Transactional(rollbackOn = Exception.class)
    public Teacher saveTeacher(TeacherInsertDTO dto) throws EntityAlreadyExistsException, EntityInvalidArgumentException {

        try {
            if(teacherRepository.findByVat(dto.getVat()).isPresent()) {
                throw new EntityAlreadyExistsException("Teacher" , "Teacher with vat " + dto.getVat() + " already exists");
            }

            Region region = regionRepository.findById(dto.getRegionId()).orElseThrow(() -> new EntityInvalidArgumentException("Region", "Invalid Region id "));
            Teacher teacher = mapper.mapToTeacherEntity(dto);
            region.addTeacher(teacher);
            teacherRepository.save(teacher);
            log.info("Teacher with vat={} saved.", dto.getVat());     //structured logging var = {} parametrized placeholder design pattern
            return teacher;


        } catch (EntityAlreadyExistsException e) {
            log.error("Save failed for teacher with vat={}. Teacher already exists", dto.getVat(), e);
            throw e;
        } catch (EntityInvalidArgumentException e){
            log.error("Save failed for teacher with vat={}. Region id={} invalid", dto.getVat(),dto.getRegionId(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public Page<TeacherReadOnlyDTO> getPaginatedTeachers(int page, int size) {
        Pageable pageable = PageRequest.of(page,size);
        Page<Teacher> teacherPage = teacherRepository.findAll(pageable);
        return teacherPage.map(mapper::mapToTeacherReadOnlyDTO);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Teacher updateTeacher(TeacherEditDTO dto) throws EntityAlreadyExistsException, EntityInvalidArgumentException, EntityNotFoundException {

        try {

            Teacher teacher = teacherRepository.findByUuid(dto.getUuid())
                    .orElseThrow(() -> new EntityNotFoundException("Teacher", "Teacher not found"));

            if(!teacher.getVat().equals(dto.getVat())) {
                if(teacherRepository.findByVat(dto.getVat()).isEmpty()) {
                    teacher.setVat(dto.getVat());
                } else throw new EntityAlreadyExistsException("Teacher","Teacher with vat " + dto.getVat() + "already exists");
            }

            teacher.setFirstname(dto.getFirstname());
            teacher.setLastname(dto.getLastname());

            if(!Objects.equals(teacher.getRegion().getId(), dto.getRegionId())) {
                Region region = regionRepository.findById(dto.getRegionId())
                        .orElseThrow(() -> new EntityInvalidArgumentException("Region", "Invalid region id"));
                Region currentRegion = teacher.getRegion();
                if(currentRegion != null) {
                    currentRegion.removeTeacher(teacher); // TBD
                }

                region.addTeacher(teacher);
            }

            teacherRepository.save(teacher);
            log.info("Teacher with vat={} updated.", dto.getVat());

            return teacher;


        } catch (EntityNotFoundException e){
            log.error("Update failed for teacher with vat={} Entity not found", dto.getVat(), e);
            throw e;
        } catch (EntityAlreadyExistsException e) {
            log.error("Update failed for teacher with vat={} Entity not found", dto.getVat(), e);
            throw e;
        } catch (EntityInvalidArgumentException e) {
            log.error("Update failed for teacher with vat={} Region not found with id={}", dto.getVat(), dto.getRegionId(), e);
            throw e;
        }

    }


}
