package antifraud.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PostTransactionDto {
    @NotNull
    @Min(1)
    private Long amount;
    @NotNull
    @NotBlank
    private String ip;
    @NotNull
    @NotBlank
    private String number;
}
