/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 *  The ontology is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The ontology is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.ontio.core.transaction

import java.io.*
import java.util.*

import com.github.ontio.common.*
import com.github.ontio.core.Inventory
import com.github.ontio.core.InventoryType
import com.github.ontio.core.asset.Sig
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter

/**
 *
 */
abstract class Transaction protected constructor(var txType: TransactionType) : Inventory() {
    var version: Byte = 0
    var nonce = Random().nextInt()
    var gasPrice: Long = 0
    var gasLimit: Long = 0
    var payer = Address()
    var attributes: Array<Attribute>? = null
    var sigs = emptyArray<Sig>()

    override val addressU160ForVerifying: Array<Address>?
        get() = null

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        deserializeUnsigned(reader)
        try {
            sigs = reader.readSerializableArray(Sig::class.java)
        } catch (ex: InstantiationException) {
            throw RuntimeException(ex)
        } catch (ex: IllegalAccessException) {
            throw RuntimeException(ex)
        }
    }

    @Throws(IOException::class)
    override fun deserializeUnsigned(reader: BinaryReader) {
        txType = TransactionType.valueOf(reader.readByte())
        nonce = reader.readInt()
        version = reader.readByte()
        gasPrice = reader.readLong()
        gasLimit = reader.readLong()
        payer = reader.readSerializable(Address::class.java)

        deserializeUnsignedWithoutType(reader)
    }

    @Throws(IOException::class)
    private fun deserializeUnsignedWithoutType(reader: BinaryReader) {
        try {
            deserializeExclusiveData(reader)
            attributes = reader.readSerializableArray(Attribute::class.java)
        } catch (ex: InstantiationException) {
            throw IOException(ex)
        } catch (ex: IllegalAccessException) {
            throw IOException(ex)
        }
    }

    @Throws(IOException::class)
    protected open fun deserializeExclusiveData(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        serializeUnsigned(writer)
        writer.writeSerializableArray(sigs)
    }

    @Throws(IOException::class)
    override fun serializeUnsigned(writer: BinaryWriter) {
        writer.writeByte(version)
        writer.writeByte(txType.value())
        writer.writeInt(nonce)
        writer.writeLong(gasPrice)
        writer.writeLong(gasLimit)
        writer.writeSerializable(payer)
        serializeExclusiveData(writer)
        writer.writeSerializableArray(attributes!!)
    }

    @Throws(IOException::class)
    protected open fun serializeExclusiveData(writer: BinaryWriter) {
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other == null) {
            return false
        }
        if (other !is Transaction) {
            return false
        }
        return hash() == other.hash()
    }

    override fun hashCode(): Int {
        return hash().hashCode()
    }

    override fun inventoryType(): InventoryType {
        return InventoryType.TX
    }

    open fun json(): MutableMap<String, Any> {
        val json = mutableMapOf<String, Any>()
        json["Hash"] = hash().toString()
        json["Version"] = version.toInt()
        json["Nonce"] = nonce and -0x1
        json["TxType"] = txType.value().toInt() and 0xFF
        json["GasPrice"] = gasPrice
        json["GasLimit"] = gasLimit
        json["Payer"] = payer.toBase58()
        json["Attributes"] = attributes!!.map(Attribute::json).toTypedArray()
        json["Sigs"] = sigs.map(Sig::json).toTypedArray()
        return json
    }

    override fun verify(): Boolean {
        return true
    }

    companion object {
        @Throws(IOException::class)
        @JvmOverloads
        fun deserializeFrom(value: ByteArray, offset: Int = 0): Transaction {
            ByteArrayInputStream(value, offset, value.size - offset).use { ms -> BinaryReader(ms).use { reader -> return deserializeFrom(reader) } }
        }

        @Throws(IOException::class)
        fun deserializeFrom(reader: BinaryReader): Transaction {
            try {
                val ver = reader.readByte()
                val type = TransactionType.valueOf(reader.readByte())
                val typeName = "com.github.ontio.core.payload." + type.toString()
                val transaction = Class.forName(typeName).newInstance() as Transaction
                transaction.nonce = reader.readInt()
                transaction.version = ver
                transaction.gasPrice = reader.readLong()
                transaction.gasLimit = reader.readLong()
                transaction.payer = reader.readSerializable(Address::class.java)
                transaction.deserializeUnsignedWithoutType(reader)
                transaction.sigs = Array(reader.readVarInt().toInt()) { reader.readSerializable(Sig::class.java) }
                return transaction
            } catch (ex: ClassNotFoundException) {
                throw IOException(ex)
            } catch (ex: InstantiationException) {
                throw IOException(ex)
            } catch (ex: IllegalAccessException) {
                throw IOException(ex)
            }
        }
    }

}