package antifraud.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter @Setter
@NoArgsConstructor
public class PutRoleDto {
    @NotBlank
    @NotNull
    String username;
    @NotBlank
    @NotNull
    String role;
}
