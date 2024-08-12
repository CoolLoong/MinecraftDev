/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.nbt

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.nbt.editor.NbtFormat
import com.demonwav.mcdev.nbt.tags.*
import com.demonwav.mcdev.nbt.util.LittleEndianDataInputStream
import com.demonwav.mcdev.nbt.util.NetworkDataInputStream
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import java.io.*
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

object Nbt {
    private fun judgeDataInput(dataIn: DataInput): Boolean {
        try {
            val firstByte = dataIn.readUnsignedByte()//type must be compound_tag
            val fb = NbtTypeId.getById(firstByte.toByte())
            if (fb == null || fb != NbtTypeId.COMPOUND) {
                return false
            }
            dataIn.readUTF()//root compound name
            val secondByte = dataIn.readUnsignedByte()//tag type
            val sb = NbtTypeId.getById(secondByte.toByte()) ?: return false
            if (sb == NbtTypeId.END) return false
            dataIn.readUTF()//tag name
            dataIn.readTag(sb, System.currentTimeMillis(), 20)
            return true
        } catch (e: Throwable) {
            return false
        }
    }

    private fun isBedrockLevelDat(dataIn: DataInput): Boolean {
        val header = ByteArray(4)
        dataIn.readFully(header)
        dataIn.skipBytes(4)
        return header[0].toInt() >= 8 && header[1].toInt() == 0 && header[2].toInt() == 0 && header[3].toInt() == 0
    }

    private fun getActualInputStream(stream: InputStream): Pair<DataInput, NbtFormat> {
        var res: DataInput? = null
        var mode: NbtFormat = NbtFormat.BIG_ENDIAN
        stream.use {
            val byteBuf: ByteBuf = Unpooled.wrappedBuffer(stream.readAllBytes())
            byteBuf.markReaderIndex()
            val iss: InputStream = try {
                res = DataInputStream(GZIPInputStream(ByteBufInputStream(byteBuf)))
                return@use
            } catch (e: ZipException) {
                byteBuf.resetReaderIndex()
                mode = NbtFormat.BIG_ENDIAN_GZIP
                ByteBufInputStream(byteBuf)
            }

            byteBuf.markReaderIndex()
            var input: DataInput = DataInputStream(iss)
            var r = judgeDataInput(input)
            if (r) {
                res = input
                byteBuf.resetReaderIndex()
                return@use
            }
            byteBuf.resetReaderIndex()

            byteBuf.markReaderIndex()
            if (!isBedrockLevelDat(input)) {
                byteBuf.resetReaderIndex()
            }
            byteBuf.markReaderIndex()
            input = LittleEndianDataInputStream(iss)
            r = judgeDataInput(input)
            if (r) {
                res = input
                byteBuf.resetReaderIndex()
                mode = NbtFormat.LITTLE_ENDIAN
                return@use
            }
            byteBuf.resetReaderIndex()

            byteBuf.markReaderIndex()
            input = NetworkDataInputStream(iss)
            r = judgeDataInput(input)
            if (r) {
                res = input
                byteBuf.resetReaderIndex()
                mode = NbtFormat.LITTLE_ENDIAN_NETWORK
                return@use
            }
        }
        if (res == null) {
            throw MalformedNbtFileException(MCDevBundle("nbt.lang.errors.reading"))
        }
        return res!! to mode
    }

    /**
     * Parse the NBT file from the InputStream and return the root TagCompound for the NBT file. This method closes the stream when
     * it is finished with it.
     */
    @Throws(MalformedNbtFileException::class)
    fun buildTagTree(inputStream: InputStream, timeout: Long): Pair<RootCompound, NbtFormat> {
        try {
            val (stream, mode) = getActualInputStream(inputStream)

            (stream as InputStream).use {
                val tagIdByte = stream.readUnsignedByte()
                val tagId = NbtTypeId.getById(tagIdByte.toByte())
                    ?: throw MalformedNbtFileException(MCDevBundle("nbt.lang.errors.wrong_tag_id", tagIdByte))

                if (tagId != NbtTypeId.COMPOUND) {
                    throw MalformedNbtFileException(MCDevBundle("nbt.lang.errors.invalid_root"))
                }

                val start = System.currentTimeMillis()

                return RootCompound(stream.readUTF(), stream.readCompoundTag(start, timeout).tagMap) to mode
            }
        } catch (e: Throwable) {
            if (e is MalformedNbtFileException) {
                throw e
            } else {
                throw MalformedNbtFileException(MCDevBundle("nbt.lang.errors.reading"), e)
            }
        }
    }

    private fun DataInput.readCompoundTag(start: Long, timeout: Long) = checkTimeout(start, timeout) {
        val tagMap = HashMap<String, NbtTag>()

        var tagIdByte = this.readUnsignedByte().toByte()
        var tagId =
            NbtTypeId.getById(tagIdByte) ?: run {
                throw MalformedNbtFileException(MCDevBundle("nbt.lang.errors.wrong_tag_id", tagIdByte))
            }
        while (tagId != NbtTypeId.END) {
            val name = this.readUTF()

            tagMap[name] = this.readTag(tagId, start, timeout)

            tagIdByte = this.readUnsignedByte().toByte()
            tagId =
                NbtTypeId.getById(tagIdByte) ?: run {
                    throw MalformedNbtFileException(MCDevBundle("nbt.lang.errors.wrong_tag_id", tagIdByte))
                }
        }

        return@checkTimeout TagCompound(tagMap)
    }

    private fun DataInput.readByteTag(start: Long, timeout: Long) =
        checkTimeout(start, timeout) { TagByte(this.readByte()) }

    private fun DataInput.readShortTag(start: Long, timeout: Long) =
        checkTimeout(start, timeout) { TagShort(this.readShort()) }

    private fun DataInput.readIntTag(start: Long, timeout: Long) =
        checkTimeout(start, timeout) { TagInt(this.readInt()) }

    private fun DataInput.readLongTag(start: Long, timeout: Long) =
        checkTimeout(start, timeout) { TagLong(this.readLong()) }

    private fun DataInput.readFloatTag(start: Long, timeout: Long) =
        checkTimeout(start, timeout) { TagFloat(this.readFloat()) }

    private fun DataInput.readDoubleTag(start: Long, timeout: Long) =
        checkTimeout(start, timeout) { TagDouble(this.readDouble()) }

    private fun DataInput.readStringTag(start: Long, timeout: Long) =
        checkTimeout(start, timeout) { TagString(this.readUTF()) }

    private fun DataInput.readListTag(start: Long, timeout: Long) = checkTimeout(start, timeout) {
        val tagIdByte = this.readByte()
        val tagId =
            NbtTypeId.getById(tagIdByte) ?: run {
                throw MalformedNbtFileException(MCDevBundle("nbt.lang.errors.wrong_tag_id", tagIdByte))
            }

        val length = this.readInt()
        if (length <= 0) {
            return@checkTimeout TagList(tagId, emptyList())
        }

        val list = List(length) {
            this.readTag(tagId, start, timeout)
        }
        return@checkTimeout TagList(tagId, list)
    }

    private fun DataInput.readByteArrayTag(start: Long, timeout: Long) = checkTimeout(start, timeout) {
        val length = this.readInt()

        val bytes = ByteArray(length)
        this.readFully(bytes)
        return@checkTimeout TagByteArray(bytes)
    }

    private fun DataInput.readIntArrayTag(start: Long, timeout: Long) = checkTimeout(start, timeout) {
        val length = this.readInt()

        val ints = IntArray(length) {
            this.readInt()
        }

        return@checkTimeout TagIntArray(ints)
    }

    private fun DataInput.readLongArrayTag(start: Long, timeout: Long) = checkTimeout(start, timeout) {
        val length = this.readInt()

        val longs = LongArray(length) {
            this.readLong()
        }

        return@checkTimeout TagLongArray(longs)
    }

    private fun DataInput.readTag(tagId: NbtTypeId, start: Long, timeout: Long): NbtTag {
        return when (tagId) {
            NbtTypeId.END -> TagEnd
            NbtTypeId.BYTE -> this.readByteTag(start, timeout)
            NbtTypeId.SHORT -> this.readShortTag(start, timeout)
            NbtTypeId.INT -> this.readIntTag(start, timeout)
            NbtTypeId.LONG -> this.readLongTag(start, timeout)
            NbtTypeId.FLOAT -> this.readFloatTag(start, timeout)
            NbtTypeId.DOUBLE -> this.readDoubleTag(start, timeout)
            NbtTypeId.BYTE_ARRAY -> this.readByteArrayTag(start, timeout)
            NbtTypeId.STRING -> this.readStringTag(start, timeout)
            NbtTypeId.LIST -> this.readListTag(start, timeout)
            NbtTypeId.COMPOUND -> this.readCompoundTag(start, timeout)
            NbtTypeId.INT_ARRAY -> this.readIntArrayTag(start, timeout)
            NbtTypeId.LONG_ARRAY -> this.readLongArrayTag(start, timeout)
        }
    }

    private inline fun <T : Any> checkTimeout(start: Long, timeout: Long, action: () -> T): T {
        val now = System.currentTimeMillis()
        val took = now - start

        if (took > timeout) {
            throw NbtFileParseTimeoutException(MCDevBundle("nbt.lang.errors.parse_timeout", took, timeout))
        }

        return action()
    }
}
