package com.demonwav.mcdev.nbt.util

import java.io.*
import java.nio.charset.StandardCharsets

open class LittleEndianDataInputStream(stream: InputStream) : FilterInputStream(DataInputStream(stream)), DataInput {

    protected val dataStream = DataInputStream(stream)

    @Throws(IOException::class)
    override fun readFully(bytes: ByteArray) {
        dataStream.readFully(bytes)
    }

    @Throws(IOException::class)
    override fun readFully(bytes: ByteArray, offset: Int, length: Int) {
        dataStream.readFully(bytes, offset, length)
    }

    @Throws(IOException::class)
    override fun skipBytes(amount: Int): Int {
        return dataStream.skipBytes(amount)
    }

    @Throws(IOException::class)
    override fun readBoolean(): Boolean {
        return dataStream.readBoolean()
    }

    @Throws(IOException::class)
    override fun readByte(): Byte {
        return dataStream.readByte()
    }

    @Throws(IOException::class)
    override fun readUnsignedByte(): Int {
        return dataStream.readUnsignedByte()
    }

    @Throws(IOException::class)
    override fun readShort(): Short {
        return java.lang.Short.reverseBytes(dataStream.readShort())
    }

    @Throws(IOException::class)
    override fun readUnsignedShort(): Int {
        return java.lang.Short.toUnsignedInt(java.lang.Short.reverseBytes(dataStream.readShort()))
    }

    @Throws(IOException::class)
    override fun readChar(): Char {
        return java.lang.Character.reverseBytes(dataStream.readChar())
    }

    @Throws(IOException::class)
    override fun readInt(): Int {
        return Integer.reverseBytes(dataStream.readInt())
    }

    @Throws(IOException::class)
    override fun readLong(): Long {
        return java.lang.Long.reverseBytes(dataStream.readLong())
    }

    @Throws(IOException::class)
    override fun readFloat(): Float {
        return java.lang.Float.intBitsToFloat(Integer.reverseBytes(dataStream.readInt()))
    }

    @Throws(IOException::class)
    override fun readDouble(): Double {
        return java.lang.Double.longBitsToDouble(java.lang.Long.reverseBytes(dataStream.readLong()))
    }

    @Deprecated("")
    @Throws(IOException::class)
    override fun readLine(): String {
        return dataStream.readLine()
    }

    @Throws(IOException::class)
    override fun readUTF(): String {
        val bytes = ByteArray(readUnsignedShort())
        readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
