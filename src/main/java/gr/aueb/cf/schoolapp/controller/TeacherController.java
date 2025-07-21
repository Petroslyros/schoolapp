package gr.aueb.cf.schoolapp.controller;

import gr.aueb.cf.schoolapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.schoolapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.schoolapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.schoolapp.dto.TeacherEditDTO;
import gr.aueb.cf.schoolapp.dto.TeacherInsertDTO;
import gr.aueb.cf.schoolapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.schoolapp.mapper.Mapper;
import gr.aueb.cf.schoolapp.model.Teacher;
import gr.aueb.cf.schoolapp.repository.RegionRepository;
import gr.aueb.cf.schoolapp.repository.TeacherRepository;
import gr.aueb.cf.schoolapp.service.ITeacherService;
import gr.aueb.cf.schoolapp.validator.TeacherInsertValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller // Declares this class as a Spring MVC controller (handles web requests)
@RequestMapping("/school/teachers") // Base URL path for all endpoints in this controller
@RequiredArgsConstructor // Lombok: generates constructor for all final fields
@Slf4j // Enables logging using SLF4J
public class TeacherController {

    // Injected dependencies (business logic, repositories, mappers, validation)
    private final ITeacherService teacherService;
    private final RegionRepository regionRepository;
    private final TeacherRepository teacherRepository;
    private final Mapper mapper;
    private final TeacherInsertValidator teacherInsertValidator;

    /**
     * GET /school/teachers/insert
     * Display the teacher creation form.
     */
    @GetMapping("/insert") // Handles HTTP GET request to "/insert"
    public String getTeacherForm(Model model) {
        model.addAttribute("teacherInsertDTO", new TeacherInsertDTO()); // Empty DTO for form binding
        model.addAttribute("regions", regionRepository.findAll(Sort.by("name"))); // Populate dropdown with sorted regions
        return "teacher-form"; // Renders the form view (Thymeleaf, JSP, etc.)
    }

    /**
     * POST /school/teachers/insert
     * Handle form submission to create a new teacher.
     */
    @PostMapping("/insert") // Handles HTTP POST request to "/insert"
    public String saveTeacher(@Valid @ModelAttribute("teacherInsertDTO") TeacherInsertDTO teacherInsertDTO,
                              BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

        Teacher savedTeacher;

        // Validate teacherInsertDTO using custom validator (checks for business rules)
        teacherInsertValidator.validate(teacherInsertDTO, bindingResult);

        // If there are validation errors, redisplay the form with error messages
        if (bindingResult.hasErrors()) {
            model.addAttribute("regions", regionRepository.findAll(Sort.by("name")));
            return "teacher-form"; // Re-render the same form
        }

        try {
            // Try to save teacher using service layer
            savedTeacher = teacherService.saveTeacher(teacherInsertDTO);

            // Convert the saved entity to a read-only DTO for display
            TeacherReadOnlyDTO readOnlyDTO = mapper.mapToTeacherReadOnlyDTO(savedTeacher);

            // Add data to redirect scope (Flash attributes persist through redirect)
            redirectAttributes.addFlashAttribute("teacher", readOnlyDTO);

            return "redirect:/school/teachers/"; // Redirect to list page to avoid resubmission
        } catch (EntityAlreadyExistsException | EntityInvalidArgumentException e) {
            // If saving fails, return form with error message
            model.addAttribute("regions", regionRepository.findAll(Sort.by("name")));
            model.addAttribute("errorMessage", e.getMessage());
            return "teacher-form";
        }
    }

    /**
     * GET /school/teachers/view?page=0&size=5
     * Displays a paginated list of teachers.
     */
    @GetMapping
    public String getPaginatedTeachers(
            @RequestParam(defaultValue = "0") int page, // URL parameter for current page (defaults to 0)
            @RequestParam(defaultValue = "5") int size, // URL parameter for page size (defaults to 5)
            Model model) {

        // Get paginated teacher DTOs from service
        Page<TeacherReadOnlyDTO> teachersPage = teacherService.getPaginatedTeachers(page, size);

        // Pass data to the view layer
        model.addAttribute("teachersPage", teachersPage); // The full Page object (with metadata)
        model.addAttribute("currentPage", page); // Needed for pagination controls
        model.addAttribute("totalPages", teachersPage.getTotalPages());

        return "teachers"; // Renders the "teachers" view (paginated list)
    }

    /**
     * GET /school/teachers/edit/{uuid}
     * Displays the form for editing a teacher.
     */
    @GetMapping("/edit/{uuid}") // {uuid} is a path variable to identify which teacher to edit
    public String showEditForm(@PathVariable String uuid, Model model) {
        try {
            // Retrieve the teacher from the repository by UUID
            Teacher teacher = teacherRepository.findByUuid(uuid)
                    .orElseThrow(() -> new EntityNotFoundException("Teacher", "Teacher not found"));

            // Add data needed for the edit form
            model.addAttribute("teacherEditDTO", mapper.mapToTeacherEditDTO(teacher));
            model.addAttribute("regions", regionRepository.findAll(Sort.by("name")));

            return "teacher-edit-form"; // Render the form
        } catch (EntityNotFoundException e) {
            log.error("Teacher with uuid={} not updated", uuid, e);
            model.addAttribute("regions", regionRepository.findAll(Sort.by("name")));
            model.addAttribute("errorMessage", e.getMessage());
            return "teacher-edit-form"; // Still show the form, but with an error message
        }
    }

    /**
     * POST /school/teachers/edit
     * Processes the form submission to update a teacher.
     */
    @PostMapping("/edit")
    public String updateTeacher(@Valid @ModelAttribute("teacherEditDTO") TeacherEditDTO teacherEditDTO,
                                BindingResult bindingResult, Model model) {

        Teacher updatedTeacher;

        // Skipped custom validator for now, but could be added if needed
        // teacherInsertValidator.validate(teacherInsertDTO, bindingResult);

        // If validation errors exist, return to the form with errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("regions", regionRepository.findAll(Sort.by("name")));
            return "teacher-edit-form";
        }

        try {
            // Try to update the teacher
            teacherService.updateTeacher(teacherEditDTO);

            // Prepare data for the result view
//            model.addAttribute("teacher", mapper.mapToTeacherReadOnlyDTO(updatedTeacher));
            return "/school/teachers/view"; // Display the updated list (or teacher info)

        } catch (EntityAlreadyExistsException | EntityInvalidArgumentException | EntityNotFoundException e) {
            // If update fails, return to the form with error
            model.addAttribute("regions", regionRepository.findAll(Sort.by("name")));
            model.addAttribute("errorMessage", e.getMessage());
            return "teacher-edit-form";
        }
    }
    @GetMapping("/school/teachers/delete/{uuid}")
    public String deleteTeacher(@PathVariable String uuid, Model model){
        try {
            teacherService.deleteTeacherByUUID(uuid);
            return "redirect:/school/teachers";
        } catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "teachers";
        }


    }
}

