package com.ch.swaplychatservice.service;

import com.ch.swaplychatservice.dto.response.MemberInfo;
import com.ch.swaplychatservice.dto.response.ProductInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 같은 swaply DB 안의 member / product 테이블을 직접 조회
 *   - 마이크로서비스 원칙상 Feign Client 가 맞지만,
 *     현재 멤버/상품 서비스가 같은 DB를 공유하므로 JDBC로 직접 조회
 *   - 추후 Feign 으로 교체 용이하게 인터페이스 형태 유지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberQueryService {

    private final JdbcTemplate jdbc;

    public MemberInfo getMember(Long memberId) {
        try {
            return jdbc.queryForObject(
                    "SELECT member_id, nickname, profile_image FROM member WHERE member_id = ?",
                    (rs, row) -> MemberInfo.builder()
                            .memberId(rs.getLong("member_id"))
                            .nickname(rs.getString("nickname"))
                            .profileImage(rs.getString("profile_image"))
                            .build(),
                    memberId
            );
        } catch (Exception e) {
            log.warn("회원 조회 실패: memberId={}", memberId);
            return MemberInfo.builder().memberId(memberId).nickname("알 수 없음").build();
        }
    }

    public ProductInfo getProduct(Long productId) {
        try {
            return jdbc.queryForObject(
                    """
                    SELECT p.product_id, p.title, p.price, p.seller_id,
                           pi.image_url AS thumbnail_url
                    FROM product p
                    LEFT JOIN product_image pi
                           ON pi.product_id = p.product_id AND pi.is_thumbnail = 1
                    WHERE p.product_id = ?
                    LIMIT 1
                    """,
                    (rs, row) -> ProductInfo.builder()
                            .productId(rs.getLong("product_id"))
                            .title(rs.getString("title"))
                            .price(rs.getLong("price"))
                            .sellerId(rs.getLong("seller_id"))
                            .thumbnailUrl(rs.getString("thumbnail_url"))
                            .build(),
                    productId
            );
        } catch (Exception e) {
            log.warn("상품 조회 실패: productId={}", productId);
            return ProductInfo.builder().productId(productId).title("알 수 없는 상품").build();
        }
    }
}
