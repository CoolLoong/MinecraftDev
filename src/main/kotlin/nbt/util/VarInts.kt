package com.demonwav.mcdev.nbt.util

import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

object VarInts {

    @Throws(IOException::class)
    fun writeInt(buffer: DataOutput, integer: Int) {
        encodeUnsigned(buffer, ((integer shl 1) xor (integer shr 31)).toLong())
    }

    @Throws(IOException::class)
    fun readInt(buffer: DataInput): Int {
        val n = decodeUnsigned(buffer).toInt()
        return (n ushr 1) xor -(n and 1)
    }

    @Throws(IOException::class)
    fun writeUnsignedInt(buffer: DataOutput, integer: Long) {
        encodeUnsigned(buffer, integer)
    }

    @Throws(IOException::class)
    fun readUnsignedInt(buffer: DataInput): Int {
        return decodeUnsigned(buffer).toInt()
    }

    @Throws(IOException::class)
    fun writeLong(buffer: DataOutput, longInteger: Long) {
        encodeUnsigned(buffer, (longInteger shl 1) xor (longInteger shr 63))
    }

    @Throws(IOException::class)
    fun readLong(buffer: DataInput): Long {
        val n = decodeUnsigned(buffer)
        return (n ushr 1) xor -(n and 1)
    }

    @Throws(IOException::class)
    fun writeUnsignedLong(buffer: DataOutput, longInteger: Long) {
        encodeUnsigned(buffer, longInteger)
    }

    @Throws(IOException::class)
    fun readUnsignedLong(buffer: DataInput): Long {
        return decodeUnsigned(buffer)
    }

    @Throws(IOException::class)
    private fun decodeUnsigned(buffer: DataInput): Long {
        var result: Long = 0
        for (shift in 0 until 64 step 7) {
            val b = buffer.readByte().toInt()
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) {
                return result
            }
        }
        throw ArithmeticException("Varint was too large")
    }

    @Throws(IOException::class)
    private fun encodeUnsigned(buffer: DataOutput, value: Long) {
        var v = value
        while (true) {
            if (v and 0x7FL.inv() == 0L) {
                buffer.writeByte(v.toInt())
                return
            } else {
                buffer.writeByte(((v.toInt() and 0x7F) or 0x80))
                v = v ushr 7
            }
        }
    }
}
