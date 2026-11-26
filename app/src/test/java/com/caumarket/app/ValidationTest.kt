package com.caumarket.app

import com.caumarket.app.core.validation.Validation
import com.caumarket.app.core.error.MarketException
import org.junit.Assert.*
import org.junit.Test

class ValidationTest {
    @Test fun emailUsesExactSchoolDomain() {
        assertEquals("student@cau.ac.kr", Validation.email(" STUDENT@CAU.AC.KR "))
        listOf("a@sub.cau.ac.kr", "a@cau.ac.kr.evil", "a@@cau.ac.kr", "a b@cau.ac.kr").forEach {
            try { Validation.email(it); fail("Invalid email accepted") } catch (_: MarketException) { }
        }
    }
    @Test fun unicodeLengthCountsCodePoints() {
        assertEquals(2, Validation.length("😀가"))
        assertEquals("가나", Validation.nickname(" 가나 "))
    }
    @Test fun passwordIsNotTrimmedAndConfirmationMustMatch() {
        Validation.password("        ")
        assertTrue(Validation.validatePasswordConfirmation("secret12", "secret12").isSuccess)
        assertTrue(Validation.validatePasswordConfirmation("secret12", "secret12 ").isFailure)
    }
}
