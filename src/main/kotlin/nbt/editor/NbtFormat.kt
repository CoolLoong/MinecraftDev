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

package com.demonwav.mcdev.nbt.editor

import com.demonwav.mcdev.asset.MCDevBundle

enum class NbtFormat(private val selectionNameFunc: () -> String) {
    LITTLE_ENDIAN_NETWORK({ MCDevBundle("nbt.format.little_network") }),
    BIG_ENDIAN_GZIP({ MCDevBundle("nbt.format.big_gzip") }),
    LITTLE_ENDIAN({ MCDevBundle("nbt.format.little") }),
    BIG_ENDIAN({ MCDevBundle("nbt.format.big") }), ;

    override fun toString(): String = selectionNameFunc()
}
