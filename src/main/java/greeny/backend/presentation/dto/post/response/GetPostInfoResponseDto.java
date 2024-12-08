package greeny.backend.presentation.dto.post.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import greeny.backend.domain.post.Post;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetPostInfoResponseDto {
    private Long id;
    private String writerEmail;
    private String createdAt;
    private String updatedAt;
    private String title;
    private String content;
    private List<String> fileUrls;
    private Boolean isWriter; // 화면에 수정,삭제 버튼 띄울지 판단할 때 필요
    private Integer likes; // 좋아요 수
    private Boolean isLiked; // 요청자가 좋아요를 눌렀는지?

    public static GetPostInfoResponseDto from(Post post, Boolean isWriter, Boolean liked){
        return GetPostInfoResponseDto.builder()
                .id(post.getId())
                .writerEmail(post.getWriter().getEmail())
                .title(post.getTitle())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .content(post.getContent())
                .fileUrls(post.getFileUrls())
                .isWriter(isWriter)
                .likes(post.getPostLikes().size())
                .isLiked(liked)
                .build();
    }

}
