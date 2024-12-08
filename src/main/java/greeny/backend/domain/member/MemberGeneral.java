package greeny.backend.domain.member;

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
public class MemberGeneral extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_general_id")
    private Long id;
    private Long memberId;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private boolean isAuto;

    public void changePassword(String password) {
        this.password = password;
    }
    public void changeIsAuto(boolean isAuto) {
        this.isAuto = isAuto;
    }
}
