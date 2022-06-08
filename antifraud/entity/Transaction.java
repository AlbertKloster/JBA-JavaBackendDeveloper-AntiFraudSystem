package antifraud.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;
    @NotNull
    @Min(1)
    private Long amount;
    @NotBlank
    @NotNull
    private String ip;
    @NotBlank
    @NotNull
    private String number;
    @NotBlank
    @NotNull
    private String region;
    @NotNull
    private LocalDateTime date;
    private String result;
    private String feedback;
}
