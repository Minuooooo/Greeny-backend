package greeny.backend.application.member.auth;

import greeny.backend.config.jwt.JwtProvider;
import greeny.backend.domain.member.*;
import greeny.backend.exception.situation.member.GeneralMemberNotFoundException;
import greeny.backend.exception.situation.member.MemberNotFoundException;
import greeny.backend.exception.situation.member.auth.EmailAlreadyExistsException;
import greeny.backend.exception.situation.member.auth.LoginFailureException;
import greeny.backend.exception.situation.member.auth.RefreshTokenNotFoundException;
import greeny.backend.presentation.dto.member.auth.request.*;
import greeny.backend.presentation.dto.member.auth.response.GetIsAutoInfoResponseDto;
import greeny.backend.presentation.dto.member.auth.TokenDto;
import greeny.backend.presentation.dto.member.auth.response.GetTokenStatusInfoResponseDto;
import greeny.backend.presentation.dto.member.auth.response.TokenResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final GeneralMemberRepository generalMemberRepository;
    private final SocialMemberRepository socialMemberRepository;
    private final MemberInfoRepository memberInfoRepository;
    private final AgreementRepository agreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtProvider jwtProvider;

    public void validateSignUpInfoWithGeneral(String email) {  // 이미 가입된 이메일인지 검증
        if (memberRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    private Long validateSignUpInfoWithSocial(String email) {  // DB에 이미 존재하는 이메일일 때, 일반 로그인 이메일인지 검증
        Long foundMemberId = getMember(email).getId();

        if(generalMemberRepository.existsByMemberId(foundMemberId)) {
            throw new EmailAlreadyExistsException(email);
        }

        return foundMemberId;
    }

    public GetTokenStatusInfoResponseDto getTokenStatusInfo(String bearerToken) {  // 토큰의 유효성 검증

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            if(jwtProvider.validateToken(bearerToken.substring(7))) {
                return new GetTokenStatusInfoResponseDto(true);
            }
        }

        return new GetTokenStatusInfoResponseDto(false);
    }

    public void signUp(SignUpRequestDto signUpRequestDto) {  // 일반 회원가입
        validateSignUpInfoWithGeneral(signUpRequestDto.getEmail());  // 이미 가입된 이메일인지 검증
        saveGeneralMember(signUpRequestDto);
    }

    @Transactional
    public TokenResponseDto signInWithGeneral(LoginRequestDto loginRequestDto) {  // 일반 로그인

        String email = loginRequestDto.getEmail();
        Long foundMemberId = getMember(email).getId();

        if(!passwordEncoder.matches(loginRequestDto.getPassword(), getMemberGeneral(foundMemberId).getPassword())) {
            throw new LoginFailureException();
        }

        GeneralMember foundGeneralMember = getMemberGeneral(foundMemberId);
        boolean foundIsAuto = foundGeneralMember.isAuto();

        changeIsAutoByGeneralSignIn(loginRequestDto.getIsAuto(), foundGeneralMember, foundIsAuto);

        return authorize(email, foundMemberId.toString());
    }

    public TokenResponseDto signInWithSocial(String email, Provider provider) {  // 소셜 제공자와 사용자 프로필 정보 중 email 을 받아 소셜 로그인

        if(memberRepository.existsByEmail(email)) {  // 소셜 로그인 이용이 최초가 아닌 경우

            Long validatedMemberId = validateSignUpInfoWithSocial(email);  // DB에 이미 존재하는 이메일일 때, 일반 로그인 이메일인지 검증
            Long foundMemberId = getMember(email).getId();

            if(!agreementRepository.existsByMemberId(foundMemberId)) {

                agreementRepository.save(
                        toMemberAgreement(foundMemberId, false, false)
                );
            }

            TokenResponseDto authorizedToken = authorize(email, validatedMemberId.toString());  // 토큰 발행
            return TokenResponseDto.from(authorizedToken.getAccessToken(), authorizedToken.getRefreshToken());
        }

        saveSocialMemberExceptAgreement(email, provider);  // DB에 소셜 회원 저장
        return TokenResponseDto.excludeTokenInDto(email);
    }

    public TokenResponseDto agreementInSignUp(AgreementRequestDto agreementRequestDto) {  // 일반 회원가입 or 최초 소셜 로그인 이후 동의 항목 여부를 DB에 저장

        String email = agreementRequestDto.getEmail();
        Long foundMemberId = getMember(email).getId();

        if(!memberRepository.existsByEmail(email)) {  // DB에 이메일이 없는 경우
            throw new MemberNotFoundException();
        } else {
            if(generalMemberRepository.existsByMemberId(foundMemberId)) {  // 일반 로그인 사용자일 경우
                saveMemberAgreement(foundMemberId, agreementRequestDto);  // DB에 Member 동의 여부 저장
                return TokenResponseDto.excludeEmailInDto("nothing", "nothing");  // 토큰을 제공하지 않음
            }
        }

        saveMemberAgreement(foundMemberId, agreementRequestDto);
        return authorize(email, foundMemberId.toString());
    }

    @Transactional
    public void findPassword(FindPasswordRequestDto findPasswordRequestDto) {
        getMemberGeneral(getMember(findPasswordRequestDto.getEmail()).getId())
                .changePassword(passwordEncoder.encode(findPasswordRequestDto.getPassword()));
    }

    public GetIsAutoInfoResponseDto getIsAutoInfo(Long memberId) {
        return new GetIsAutoInfoResponseDto(getMemberGeneral(memberId).isAuto());
    }

    public TokenResponseDto reissue(TokenRequestDto tokenRequestDto) {

        String refreshToken = tokenRequestDto.getRefreshToken();
        Authentication authentication = jwtProvider.getAuthentication(tokenRequestDto.getAccessToken());
        String email = authentication.getName();

        if (!jwtProvider.validateToken(refreshToken)) {
            refreshTokenRepository.deleteById(email);
            return publishToken(authentication);
        } else {
            validateRefreshTokenOwner(email, refreshToken);
            return TokenResponseDto.excludeEmailInDto(
                    jwtProvider.generateAccessToken(authentication, (new Date()).getTime()),
                    refreshToken
            );
        }
    }

    private void saveGeneralMember(SignUpRequestDto signUpRequestDto) {  // 일반 회원 DB에 저장
        Long savedMemberId = memberRepository.save(toMember(signUpRequestDto.getEmail())).getId();
        generalMemberRepository.save(toMemberGeneral(savedMemberId, signUpRequestDto.getPassword()));
        memberInfoRepository.save(
                toMemberProfile(
                        savedMemberId,
                        signUpRequestDto.getName(),
                        signUpRequestDto.getPhone(),
                        signUpRequestDto.getBirth()
                )
        );
    }

    private void saveSocialMemberExceptAgreement(String email, Provider provider) {  // 소셜 회원 DB에 저장
        Member savedMember = memberRepository.save(toMember(email));
        socialMemberRepository.save(toMemberSocial(provider, savedMember.getId()));
    }

    private void saveMemberAgreement(Long memberId, AgreementRequestDto agreementRequestDto) {  // 일반 회원가입 or 최초 소셜 로그인 시 동의 항목 여부 DB에 저장
        agreementRepository.save(
                toMemberAgreement(
                        memberId,
                        agreementRequestDto.getPersonalInfo(),
                        agreementRequestDto.getThirdParty()
                )
        );
    }

    private Member toMember(String email) {  // Member 객체로 변환
        return Member.builder()
                .email(email)
                .role(Role.ROLE_USER)
                .build();
    }

    private GeneralMember toMemberGeneral(Long memberId, String password) {  // MemberGeneral 객체로 변환
        return GeneralMember.builder()
                .memberId(memberId)
                .password(passwordEncoder.encode(password))
                .isAuto(false)
                .build();
    }

    private SocialMember toMemberSocial(Provider provider, Long memberId) {  // MemberSocial 객체로 변환
        return SocialMember.builder()
                .memberId(memberId)
                .provider(provider)
                .build();
    }

    private MemberInfo toMemberProfile(Long memberId, String name, String phone, String birth) {  // MemberProfile 객체로 변환
        return MemberInfo.builder()
                .memberId(memberId)
                .name(name)
                .phone(phone)
                .birth(birth)
                .build();
    }

    private Agreement toMemberAgreement(Long memberId, boolean personalInfo, boolean thirdParty) {  // MemberAgreement 객체로 변환
        return Agreement.builder()
                .memberId(memberId)
                .personalInfo(personalInfo)
                .thirdParty(thirdParty)
                .build();
    }

    private Member getMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);
    }

    public GeneralMember getMemberGeneral(Long memberId) {
        return generalMemberRepository.findByMemberId(memberId)
                .orElseThrow(GeneralMemberNotFoundException::new);
    }

    private void changeIsAutoByGeneralSignIn(boolean isAutoToCheck, GeneralMember foundGeneralMember, boolean foundIsAuto) {
        if (!isAutoToCheck) {
            if (foundIsAuto) {
                foundGeneralMember.changeIsAuto(false);
            }
        } else {
            if (!foundIsAuto) {
                foundGeneralMember.changeIsAuto(true);
            }
        }
    }

    private TokenResponseDto authorize(String email, String memberId) {  // 여러 가지 경우에 대한 토큰 발행
        // CustomUserDetailsService 의 loadByUsername() 실행, 로그인을 시도하는 사용자의 email, memberId 와 DB에 있는 email, memberId 비교
        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(new UsernamePasswordAuthenticationToken(email, memberId));

        if (refreshTokenRepository.existsById(email)) {  // 해당 이메일에 대한 refresh token 이 이미 존재할 경우

            RefreshToken foundRefreshToken = getRefreshToken(email);
            String foundRefreshTokenValue = foundRefreshToken.getValue();

            if (jwtProvider.validateToken(foundRefreshTokenValue)) {  // DB 에서 가져온 refresh token 이 유효한지 검증
                return TokenResponseDto.excludeEmailInDto(
                        jwtProvider.generateAccessToken(authentication, (new Date()).getTime()),
                        foundRefreshTokenValue
                );
            } else {
                refreshTokenRepository.delete(foundRefreshToken);
            }
        }

        return publishToken(authentication);
    }

    private RefreshToken getRefreshToken(String key) {
        return refreshTokenRepository.findById(key)
                .orElseThrow(RefreshTokenNotFoundException::new);
    }

    private RefreshToken toRefreshToken(String key, String value) {
        return RefreshToken.builder()
                .key(key)
                .value(value)
                .build();
    }

    private TokenResponseDto publishToken(Authentication authentication) {  // 로그인이 처음 or refresh token 유효하지 않을 때 토큰 발행
        TokenDto generatedTokenDto = jwtProvider.generateTokenDto(authentication);
        String generatedRefreshToken = generatedTokenDto.getRefreshToken();
        refreshTokenRepository.save(toRefreshToken(authentication.getName(), generatedRefreshToken));

        return TokenResponseDto.excludeEmailInDto(generatedTokenDto.getAccessToken(), generatedRefreshToken);
    }

    private void validateRefreshTokenOwner(String email, String refreshToken) {
        if (
                !refreshTokenRepository.findById(email)
                        .orElseThrow(RefreshTokenNotFoundException::new)
                        .getValue()
                        .equals(refreshToken)
        ) {
            throw new RuntimeException("토큰의 유저 정보가 일치하지 않습니다.");
        }
    }
}