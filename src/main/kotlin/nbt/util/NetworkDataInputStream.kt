package com.demonwav.mcdev.nbt.util

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

open class NetworkDataInputStream : LittleEndianDataInputStream {

    constructor(stream: InputStream) : super(stream)
    constructor(stream: DataInputStream) : super(stream)

    @Throws(IOException::class)
    override fun readInt(): Int {
        return VarInts.readInt(dataStream)
    }

    @Throws(IOException::class)
    override fun readLong(): Long {
        return VarInts.readLong(dataStream)
    }

    @Throws(IOException::class)
    override fun readUTF(): String {
        val length = VarInts.readUnsignedInt(dataStream)
        val bytes = ByteArray(length)
        readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
