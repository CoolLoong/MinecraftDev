package com.demonwav.mcdev.nbt.util

import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets

open class NetworkDataOutputStream : LittleEndianDataOutputStream {

    constructor(stream: OutputStream) : super(stream)
    constructor(stream: DataOutputStream) : super(stream)

    @Throws(IOException::class)
    override fun writeInt(value: Int) {
        VarInts.writeInt(dataStream, value)
    }

    @Throws(IOException::class)
    override fun writeLong(value: Long) {
        VarInts.writeLong(dataStream, value)
    }

    @Throws(IOException::class)
    override fun writeUTF(string: String) {
        val bytes = string.toByteArray(StandardCharsets.UTF_8)
        VarInts.writeUnsignedInt(dataStream, bytes.size.toLong())
        write(bytes)
    }
}
