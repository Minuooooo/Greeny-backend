package greeny.backend.application;


import greeny.backend.domain.product.ProductBookmark;
import greeny.backend.presentation.dto.product.GetProductInfoResponseDto;
import greeny.backend.presentation.dto.product.GetSimpleProductInfosResponseDto;
import greeny.backend.domain.product.Product;
import greeny.backend.domain.product.ProductRepository;
import greeny.backend.exception.situation.product.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;


    // 모든 사용자에 대한 제품 목록 조회
    @Transactional
    public Page<GetSimpleProductInfosResponseDto> getSimpleProductInfos(String keyword, Pageable pageable) {

        if (StringUtils.hasText(keyword)){
            return productRepository.findProductsByNameContainingIgnoreCase(keyword,pageable)
                    .map(product -> GetSimpleProductInfosResponseDto.from(product,false));
        }

        return productRepository.findAll(pageable)
                .map(product -> GetSimpleProductInfosResponseDto.from(product,false));
    }

    // 인증된 사용자에 대한 제품 목록 조회
    @Transactional
    public Page<GetSimpleProductInfosResponseDto> getSimpleProductInfosWithAuthMember(String keyword, List<ProductBookmark> productBookmarks, Pageable pageable){

        List<GetSimpleProductInfosResponseDto> simpleProductInfos = new ArrayList<>();

        if(StringUtils.hasText(keyword)){
            return checkBookmarkedProduct(
                    productRepository.findProductsByNameContainingIgnoreCase(keyword,pageable).getContent(),
                    productBookmarks,
                    pageable
            );
        }
        return checkBookmarkedProduct(productRepository.findAll(pageable).getContent(),productBookmarks,pageable);
    }

    // 제품 상세정보 조회
    public GetProductInfoResponseDto getProductInfo(Long productId){
        Product foundProduct = getProduct(productId);
        return GetProductInfoResponseDto.from(foundProduct, false);
    }

    public GetProductInfoResponseDto getProductInfoWithAuthMember(Long productId, Optional<ProductBookmark> optionalProductBookmark) {

        if(optionalProductBookmark.isPresent())
            return GetProductInfoResponseDto.from(getProduct(productId), true);

        return GetProductInfoResponseDto.from(getProduct(productId), false);
    }

    public Product getProduct(Long productId) {
        return productRepository.findProductById(productId)
                .orElseThrow(ProductNotFoundException::new);
    }

    private Page<GetSimpleProductInfosResponseDto> checkBookmarkedProduct(List<Product> products, List<ProductBookmark> productBookmarks, Pageable pageable){
        List<GetSimpleProductInfosResponseDto> simpleProducts = new ArrayList<>();

        for (Product product: products) {
            boolean isBookmarked =false;

            for (ProductBookmark productBookmark: productBookmarks){
                if(productBookmark.getProduct().getId().equals(product.getId())){
                    isBookmarked =true;
                    simpleProducts.add(GetSimpleProductInfosResponseDto.from(product,isBookmarked));
                    productBookmarks.remove(productBookmark);
                    break;
                }
            }
            if(!isBookmarked){
                simpleProducts.add(GetSimpleProductInfosResponseDto.from(product,isBookmarked));
            }
        }
        return new PageImpl<>(
                simpleProducts,
                pageable,
                simpleProducts.size()
        );
    }
}
