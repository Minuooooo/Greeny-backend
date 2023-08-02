package greeny.backend.domain.member.service;


import greeny.backend.domain.bookmark.entity.ProductBookmark;
import greeny.backend.domain.bookmark.entity.StoreBookmark;
import greeny.backend.domain.bookmark.repository.ProductBookmarkRepository;
import greeny.backend.domain.bookmark.repository.StoreBookmarkRepository;
import greeny.backend.domain.member.dto.member.CancelBookmarkRequestDto;
import greeny.backend.domain.member.dto.member.EditMemberInfoRequestDto;
import greeny.backend.domain.member.dto.member.GetMemberInfoResponseDto;
import greeny.backend.domain.member.entity.*;
import greeny.backend.domain.member.repository.*;
import greeny.backend.exception.situation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberGeneralRepository memberGeneralRepository;
    private final MemberSocialRepository memberSocialRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberAgreementRepository memberAgreementRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StoreBookmarkRepository storeBookmarkRepository;
    private final ProductBookmarkRepository productBookmarkRepository;
    private final AuthService authService;

    public Member getCurrentMember() {  // 스프링 시큐리티 컨텍스트에서 사용자 정보 가져오기
        return memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(MemberNotFoundException::new);
    }

    public GetMemberInfoResponseDto getMemberInfo() {  //회원 정보 가져오기

        Member currentMember = getCurrentMember();
        Long currentMemberId = currentMember.getId();

        if(memberGeneralRepository.existsByMemberId(currentMemberId)) {
            MemberProfile currentMemberInfo = getMemberProfile(currentMemberId);
            return GetMemberInfoResponseDto.toGeneralMemberDto(
                    currentMember.getEmail(),
                    currentMemberInfo.getName(),
                    currentMemberInfo.getPhone(),
                    currentMemberInfo.getBirth()
            );
        }

        return GetMemberInfoResponseDto.toSocialMemberDto(getMemberSocial(currentMemberId).getProvider().getName());
    }

    public void deleteMember() {

        Member currentMember = getCurrentMember();
        String key = currentMember.getEmail();

        if(refreshTokenRepository.existsById(key)) {  // 리프레쉬 토큰이 남아있는지
            refreshTokenRepository.deleteById(key);
        }

        checkAndDeleteGeneralOrSocialMember(currentMember, currentMember.getId());  // 일반, 소셜 회원인지 확인 후 삭제
    }

    @Transactional
    public void editMemberInfo(EditMemberInfoRequestDto editMemberRequestDto) {  // 비밀번호 변경

        MemberGeneral currentGeneralMember = authService.getMemberGeneral(getCurrentMember().getId());

        //현재 비밀번호를 입력받아서 회원 맞는지 체크 하기
        if(!passwordEncoder.matches(editMemberRequestDto.getPasswordToCheck(), currentGeneralMember.getPassword())) {
            throw new MemberNotEqualsException();
        }

        currentGeneralMember.changePassword(passwordEncoder.encode(editMemberRequestDto.getPasswordToChange()));  // 맞으면 변경
    }

    public void cancelBookmark(String type, CancelBookmarkRequestDto cancelBookmarkRequestDto) {  // 현재 사용자가 찜한 store or product 목록에서 삭제

        List<Long> idsToDelete = cancelBookmarkRequestDto.getIdsToDelete();
        Member currentMember = getCurrentMember();

        if(type.equals("s")) {  // 타입이 store 일 경우
            cancelStoreBookmark(
                    idsToDelete,
                    storeBookmarkRepository.findStoreBookmarksByLiker(currentMember)
            );
        } else if(type.equals("p")) {  // 타입이 product 일 경우
            cancelProductBookmark(
                    idsToDelete,
                    productBookmarkRepository.findProductBookmarksByLiker(currentMember)
            );
        } else {  // 타입이 존재하지 않을 경우
            throw new TypeDoesntExistsException();
        }
    }

    private MemberProfile getMemberProfile(Long memberId) {
        return memberProfileRepository.findByMemberId(memberId)
                .orElseThrow(MemberProfileNotFoundException::new);
    }

    private MemberAgreement getMemberAgreement(Long memberId) {
        return memberAgreementRepository.findByMemberId(memberId)
                .orElseThrow(MemberAgreementNotFoundException::new);
    }
    private MemberSocial getMemberSocial(Long memberId) {
        return memberSocialRepository.findByMemberId(memberId)
                .orElseThrow(MemberSocialNotFoundException::new);
    }
    private void checkAndDeleteGeneralOrSocialMember(Member currentMember, Long currentMemberId) {  // 일반, 소셜 회원인지 확인 후 삭제
        if(memberGeneralRepository.existsByMemberId(currentMemberId)) {  // 일반 회원일 경우
            memberGeneralRepository.delete(authService.getMemberGeneral(currentMemberId));
            memberProfileRepository.delete(getMemberProfile(currentMemberId));
        } else {  // 소셜 회원일 경우
            memberSocialRepository.delete(getMemberSocial(currentMemberId));
        }

        memberAgreementRepository.delete(getMemberAgreement(currentMemberId));
        memberRepository.delete(currentMember);
    }
    private void cancelStoreBookmark(List<Long> idsToDelete, List<StoreBookmark> foundStoreBookmarks) {  // Store 찜 목록에서 삭제
        for(Long id : idsToDelete) {
            for(StoreBookmark storeBookmark : foundStoreBookmarks) {
                if(id.equals(storeBookmark.getStore().getId())) {
                    storeBookmarkRepository.delete(storeBookmark);
                    break;
                }
            }
        }
    }
    private void cancelProductBookmark(List<Long> idsToDelete, List<ProductBookmark> foundProductBookmarks) {  // Product 찜 목록에서 삭제
        for(Long id : idsToDelete) {
            for(ProductBookmark productBookmark : foundProductBookmarks) {
                if(id.equals(productBookmark.getProduct().getId())) {
                    productBookmarkRepository.delete(productBookmark);
                    break;
                }
            }
        }
    }
}
