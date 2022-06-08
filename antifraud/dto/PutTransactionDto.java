package antifraud.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
public class PutTransactionDto {
    @NotNull
    Long transactionId;
    @NotBlank
    @NotNull
    String feedback;
}
