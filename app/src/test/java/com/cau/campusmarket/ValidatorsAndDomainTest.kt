package com.cau.campusmarket

import com.cau.campusmarket.domain.model.CampusPlace
import com.cau.campusmarket.domain.model.ChatRoom
import com.cau.campusmarket.domain.model.ProductCategory
import com.cau.campusmarket.domain.model.ProductCondition
import com.cau.campusmarket.domain.model.ProductInput
import com.cau.campusmarket.domain.validation.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsAndDomainTest {

    @Test
    fun cauEmailValidation_onlyAllowsCauDomain() {
        assertNull(Validators.validateCauEmail("student@cau.ac.kr"))
        assertNotNull(Validators.validateCauEmail("student@gmail.com"))
    }

    @Test
    fun nicknameValidation_enforces2To10Chars() {
        assertNull(Validators.validateNickname("새내기22"))
        assertNotNull(Validators.validateNickname("김"))
        assertNotNull(Validators.validateNickname("일이삼사오육칠팔구십일"))
    }

    @Test
    fun productInputValidation_enforcesReadmeLimits() {
        val valid = ProductInput(
            title = "일반물리학 10판",
            description = "필기 거의 없음",
            price = 15000,
            category = ProductCategory.TEXTBOOK,
            condition = ProductCondition.GOOD,
            place = CampusPlace.LIBRARY,
            placeDetail = "정문 쪽 벤치"
        )
        assertNull(Validators.validateProductInput(valid, listOf("https://example.com/1.jpg")))
        assertNotNull(Validators.validateProductInput(valid, emptyList()))
        assertNotNull(Validators.validateProductInput(valid.copy(price = 10_000_001), listOf("a")))
    }

    @Test
    fun unreadBadge_capsAt99Plus() {
        val room = ChatRoom(unreadCount = mapOf("u1" to 0, "u2" to 3, "u3" to 125))
        assertNull(room.formattedUnreadBadge("u1"))
        assertEquals("3", room.formattedUnreadBadge("u2"))
        assertEquals("99+", room.formattedUnreadBadge("u3"))
    }
}
