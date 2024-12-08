package greeny.backend.presentation.controller;

import greeny.backend.application.BookmarkService;
import greeny.backend.application.member.MemberService;
import greeny.backend.application.ProductService;
import greeny.backend.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import static greeny.backend.response.Response.success;
import static greeny.backend.response.SuccessMessage.*;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Product API Document")
public class ProductController {

    private final ProductService productService;
    private final BookmarkService bookmarkService;
    private final MemberService memberService;

    // 모든 사용자의 제품 목록 조회 API
    @Operation(summary = "Get simple product infos API", description = "please get product store infos.")
    @ResponseStatus(OK)
    @GetMapping("/simple")
    public Response getSimpleProductInfos(@RequestParam(required = false) String keyword, @ParameterObject Pageable pageable){
        return success(SUCCESS_TO_GET_SIMPLE_PRODUCT_INFOS, productService.getSimpleProductInfos(keyword, pageable));
    }

    // 인증된 사용자의 제품 목록 조회 API
    @Operation(summary = "Get simple product infos with auth member API", description = "please get product store infos.")
    @ResponseStatus(OK)
    @GetMapping("/auth/simple")
    public Response getSimpleProductInfosWithAuthMember(@RequestParam(required = false) String keyword, @ParameterObject Pageable pageable){
        return success(
                SUCCESS_TO_GET_SIMPLE_PRODUCT_INFOS,
                productService.getSimpleProductInfosWithAuthMember(
                        keyword,
                        bookmarkService.getProductBookmarks(memberService.getCurrentMember()),
                        pageable
                )
        );
    }

    // 제품 상세 조회 API
    @Operation(summary = "Get product info API", description = "put product id what you want to see.")
    @ResponseStatus(OK)
    @GetMapping()
    public Response getProductInfo(Long productId){
        return success(SUCCESS_TO_GET_PRODUCT_INFO, productService.getProductInfo(productId));
    }

    // 인증된 사용자에 대한 제품 상세 조회 API
    @Operation(summary = "Get product info with auth member API", description = "put product id what you want to see.")
    @ResponseStatus(OK)
    @GetMapping("/auth")
    public Response getProductInfoWithAuthMember(Long productId){
        return success(
                SUCCESS_TO_GET_PRODUCT_INFO,
                productService.getProductInfoWithAuthMember(
                        productId,
                        bookmarkService.getOptionalProductBookmark(productId, memberService.getCurrentMember().getId())
                )
        );
    }
}
