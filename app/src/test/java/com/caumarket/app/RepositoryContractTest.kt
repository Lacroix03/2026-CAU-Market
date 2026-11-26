package com.caumarket.app

import com.caumarket.app.core.error.*
import com.caumarket.app.data.firebase.Wire
import com.caumarket.app.data.firebase.result
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RepositoryContractTest {
    @Test fun callableTimestampPreservesNanoseconds() {
        assertEquals(Timestamp(1700000000,123456789),
            Wire.timestamp(mapOf("_seconds" to 1700000000L,"_nanoseconds" to 123456789)))
        assertEquals(Timestamp(1700000000,123456789),
            Wire.timestamp(mapOf("seconds" to 1700000000L,"nanoseconds" to 123456789)))
    }
    @Test fun failurePreservesRetryIdentifier() = runBlocking {
        val error=result<Unit>("abcdefghijklmnopqrst") {
            throw MarketException(MarketErrorCode.VALIDATION,"title")
        }.exceptionOrNull() as MarketException
        assertEquals("abcdefghijklmnopqrst",error.operationId)
        assertEquals("title",error.field)
    }
    @Test fun coroutineCancellationIsNotAnApplicationFailure() = runBlocking {
        try {
            result<Unit> {throw CancellationException("cancel")}
            fail("Cancellation was swallowed")
        } catch (_: CancellationException) { }
    }
}
