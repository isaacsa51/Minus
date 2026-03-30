package com.serranoie.app.minus.domain.time

import java.time.Instant

interface TimeProvider {
    fun nowEpochMillis(): Long
    fun nowInstant(): Instant
}

class SystemTimeProvider : TimeProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()

    override fun nowInstant(): Instant = Instant.ofEpochMilli(nowEpochMillis())
}
