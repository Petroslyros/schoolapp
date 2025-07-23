package gr.aueb.cf.schoolapp.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserInsertDTO {

    @NotNull(message = "The username cannot be null")
    @Size(min = 2, max = 20, message = "THe username must be within 2 and 20 characters")
    private String username;

    @Pattern(regexp = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*+=])^.{8,}$",
            message = "The pass needs to contain at least 1 special character, 1 small digit, 1 digit without white space")
    private String password;

    @NotNull(message = "The role cannot be empty")
    private String role;
}
