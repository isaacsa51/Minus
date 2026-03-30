package com.serranoie.app.minus.sync.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class WearSyncProtocolTest {

    @Test
    fun ackPayload_serializesAndDeserializesWithEnumStatus() {
        val payload = AckPayload(
            clientGeneratedId = "abc-123",
            status = AckStatus.OK,
            reason = null
        )

        val raw = WearJson.json.encodeToString(payload)
        val decoded = WearJson.json.decodeFromString<AckPayload>(raw)

        assertEquals(payload, decoded)
    }
}
