package greeny.backend.domain.review;

import greeny.backend.domain.AuditEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoreReviewImage extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_review_image_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_review_id")
    private StoreReview storeReview;

    private String imageUrl;

    public StoreReviewImage getEntity(StoreReview storeReview, String url) {
        return StoreReviewImage.builder()
                .storeReview(storeReview)
                .imageUrl(url)
                .build();
    }
}
