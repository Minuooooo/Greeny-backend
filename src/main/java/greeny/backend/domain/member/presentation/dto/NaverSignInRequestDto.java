package greeny.backend.domain.member.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class NaverSignInRequestDto {

    @NotBlank(message = "인가 코드를 입력해주세요.")
    @Schema(description = "인가 코드")
    private String authorizationCode;

    @NotBlank(message = "상태 코드를 입력해주세요.")
    @Schema(description = "상태 코드")
    private String state;
}