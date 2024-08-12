package com.demonwav.mcdev.nbt.util

import java.io.*
import java.nio.charset.StandardCharsets

open class LittleEndianDataOutputStream(stream: OutputStream) : FilterOutputStream(DataOutputStream(stream)),
    DataOutput {

    protected val dataStream = DataOutputStream(stream)

    @Throws(IOException::class)
    override fun write(bytes: ByteArray) {
        dataStream.write(bytes)
    }

    @Throws(IOException::class)
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        dataStream.write(bytes, offset, length)
    }

    @Throws(IOException::class)
    override fun writeBoolean(value: Boolean) {
        dataStream.writeBoolean(value)
    }

    @Throws(IOException::class)
    override fun writeByte(value: Int) {
        dataStream.writeByte(value)
    }

    @Throws(IOException::class)
    override fun writeShort(value: Int) {
        dataStream.writeShort(java.lang.Short.reverseBytes(value.toShort()).toInt())
    }

    @Throws(IOException::class)
    override fun writeChar(value: Int) {
        dataStream.writeChar(Character.reverseBytes(value.toChar()).code)
    }

    @Throws(IOException::class)
    override fun writeInt(value: Int) {
        dataStream.writeInt(Integer.reverseBytes(value))
    }

    @Throws(IOException::class)
    override fun writeLong(value: Long) {
        dataStream.writeLong(java.lang.Long.reverseBytes(value))
    }

    @Throws(IOException::class)
    override fun writeFloat(value: Float) {
        dataStream.writeInt(Integer.reverseBytes(java.lang.Float.floatToIntBits(value)))
    }

    @Throws(IOException::class)
    override fun writeDouble(value: Double) {
        dataStream.writeLong(java.lang.Long.reverseBytes(java.lang.Double.doubleToLongBits(value)))
    }

    @Throws(IOException::class)
    override fun writeBytes(string: String) {
        dataStream.writeBytes(string)
    }

    @Throws(IOException::class)
    override fun writeChars(string: String) {
        dataStream.writeChars(string)
    }

    @Throws(IOException::class)
    override fun writeUTF(string: String) {
        val bytes = string.toByteArray(StandardCharsets.UTF_8)
        writeShort(bytes.size)
        write(bytes)
    }
}
