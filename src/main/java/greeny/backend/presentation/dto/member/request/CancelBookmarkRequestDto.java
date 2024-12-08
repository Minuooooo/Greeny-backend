package greeny.backend.presentation.dto.member.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelBookmarkRequestDto {
    @Schema(description = "삭제할 스토어 or 제품 북마크 id 리스트")
    private List<Long> idsToDelete;
}
