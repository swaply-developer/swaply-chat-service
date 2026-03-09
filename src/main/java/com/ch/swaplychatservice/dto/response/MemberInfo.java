package com.ch.swaplychatservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * member-service 에서 가져온 회원 기본 정보 캐시용
 * Redis 에 "member:info:{memberId}" 키로 저장
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberInfo {
    private Long   memberId;
    private String nickname;
    private String profileImage;
}