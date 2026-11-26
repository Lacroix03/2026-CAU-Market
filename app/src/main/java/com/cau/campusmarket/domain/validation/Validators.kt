package com.cau.campusmarket.domain.validation

import com.cau.campusmarket.domain.model.ProductInput

object Validators {
    const val ALLOWED_EMAIL_DOMAIN = "cau.ac.kr"
    const val MIN_NICKNAME_LENGTH = 2
    const val MAX_NICKNAME_LENGTH = 10
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_TITLE_LENGTH = 50
    const val MAX_DESCRIPTION_LENGTH = 1000
    const val MAX_PLACE_DETAIL_LENGTH = 30
    const val MAX_PRICE = 10_000_000
    const val MIN_PRODUCT_PHOTOS = 1
    const val MAX_PRODUCT_PHOTOS = 15
    const val MAX_CHAT_PHOTOS = 10

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateCauEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return "학교 이메일을 입력해 주세요."
        if (!EMAIL_REGEX.matches(trimmed)) return "올바른 이메일 형식이 아닙니다."
        val domain = trimmed.substringAfter("@").lowercase()
        if (domain != ALLOWED_EMAIL_DOMAIN) {
            return "cau.ac.kr 학교 이메일만 가입할 수 있어요."
        }
        return null
    }

    fun validateLoginEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return "학교 이메일을 입력해 주세요."
        if (!EMAIL_REGEX.matches(trimmed)) return "올바른 이메일 형식이 아닙니다."
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.isBlank()) return "비밀번호를 입력해 주세요."
        if (password.length < MIN_PASSWORD_LENGTH) {
            return "비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상 입력해 주세요."
        }
        return null
    }

    fun validateNickname(nickname: String): String? {
        val trimmed = nickname.trim()
        if (trimmed.length < MIN_NICKNAME_LENGTH || trimmed.length > MAX_NICKNAME_LENGTH) {
            return "닉네임은 ${MIN_NICKNAME_LENGTH}~${MAX_NICKNAME_LENGTH}자로 입력해 주세요."
        }
        return null
    }

    fun validateProductInput(input: ProductInput, imageUris: List<String>): String? {
        if (imageUris.size < MIN_PRODUCT_PHOTOS) return "사진을 최소 1장 이상 등록해 주세요."
        if (imageUris.size > MAX_PRODUCT_PHOTOS) return "사진은 최대 15장까지 등록할 수 있어요."
        val title = input.title.trim()
        if (title.isEmpty()) return "물품명을 입력해 주세요."
        if (title.length > MAX_TITLE_LENGTH) return "물품명은 최대 50자까지 입력할 수 있어요."
        if (input.price < 0 || input.price > MAX_PRICE) {
            return "가격은 0원 ~ 10,000,000원 사이로 입력해 주세요."
        }
        if ((input.description?.length ?: 0) > MAX_DESCRIPTION_LENGTH) {
            return "상세 설명은 최대 1,000자까지 입력할 수 있어요."
        }
        if ((input.placeDetail?.length ?: 0) > MAX_PLACE_DETAIL_LENGTH) {
            return "상세 위치는 최대 30자까지 입력할 수 있어요."
        }
        return null
    }
}
