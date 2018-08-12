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

package com.github.ontio.core.block

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.function.Function

import com.alibaba.fastjson.JSON
import com.github.ontio.common.*
import com.github.ontio.core.Inventory
import com.github.ontio.core.InventoryType
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter

/**
 * block
 */
open class Block : Inventory() {
    var version: Int = 0
    lateinit var prevBlockHash: UInt256
    lateinit var transactionsRoot: UInt256
    lateinit var blockRoot: UInt256
    var timestamp: Int = 0
    var height: Int = 0
    var consensusData: Long = 0
    var consensusPayload: ByteArray? = null
    lateinit var nextBookkeeper: Address
    var sigData: Array<String>? = null
    lateinit var bookkeepers: Array<ByteArray>
    lateinit var transactions: Array<Transaction>
    var hash: UInt256? = null

    private lateinit var _header: Block

    val isHeader: Boolean
        get() = transactions.isEmpty()

    override val addressU160ForVerifying: Array<Address>?
        get() = null

    fun header(): Block {
        if (isHeader) {
            return this
        }
        if (!::_header.isInitialized) {
            _header = Block()
            _header.prevBlockHash = prevBlockHash
            _header.transactionsRoot = this.transactionsRoot
            _header.blockRoot = this.blockRoot
            _header.timestamp = this.timestamp
            _header.height = this.height
            _header.consensusData = this.consensusData
            _header.nextBookkeeper = this.nextBookkeeper
            _header.sigData = this.sigData
            _header.bookkeepers = this.bookkeepers
            _header.transactions = emptyArray()
        }
        return _header
    }

    override fun inventoryType(): InventoryType {
        return InventoryType.Block
    }

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        deserializeUnsigned(reader)
        var len = reader.readVarInt().toInt()
        sigData = arrayOfNulls<String>(len) as Array<String>
        for (i in 0 until len) {
            this.sigData!![i] = Helper.toHexString(reader.readVarBytes())
        }

        len = reader.readInt()
        transactions = arrayOfNulls<Transaction>(len) as Array<Transaction>
        for (i in transactions.indices) {
            transactions[i] = Transaction.deserializeFrom(reader)
        }
    }

    @Throws(IOException::class)
    override fun deserializeUnsigned(reader: BinaryReader) {
        try {
            version = reader.readInt()
            prevBlockHash = reader.readSerializable(UInt256::class.java)
            transactionsRoot = reader.readSerializable(UInt256::class.java)
            blockRoot = reader.readSerializable(UInt256::class.java)
            timestamp = reader.readInt()
            height = reader.readInt()
            consensusData = reader.readLong()
            consensusPayload = reader.readVarBytes()
            nextBookkeeper = reader.readSerializable(Address::class.java)
            val len = reader.readVarInt().toInt()
            bookkeepers = arrayOfNulls<ByteArray>(len) as Array<ByteArray>
            for (i in 0 until len) {
                this.bookkeepers[i] = reader.readVarBytes()
            }
            transactions = emptyArray()
        } catch (ex: InstantiationException) {
            throw IOException(ex)
        } catch (ex: IllegalAccessException) {
            throw IOException(ex)
        }

    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        serializeUnsigned(writer)
        writer.writeVarInt(bookkeepers.size.toLong())
        for (i in bookkeepers.indices) {
            writer.writeVarBytes(bookkeepers[i])
        }
        writer.writeVarInt(sigData!!.size.toLong())
        for (i in sigData!!.indices) {
            writer.writeVarBytes(Helper.hexToBytes(sigData!![i]))
        }
        writer.writeInt(transactions.size)
        for (i in transactions.indices) {
            writer.writeSerializable(transactions[i])
        }
    }

    @Throws(IOException::class)
    override fun serializeUnsigned(writer: BinaryWriter) {
        writer.writeInt(version)
        writer.writeSerializable(prevBlockHash)
        writer.writeSerializable(transactionsRoot)
        writer.writeSerializable(blockRoot)
        writer.writeInt(timestamp)
        writer.writeInt(height)
        writer.writeLong(consensusData)
        writer.writeVarBytes(consensusPayload!!)
        writer.writeSerializable(nextBookkeeper)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other !is Block) {
            false
        } else this.hash() == other.hash()
    }

    override fun hashCode(): Int {
        return hash().hashCode()
    }


    fun json(): Any {
        val json = mutableMapOf<String, Any>()
        val head = mutableMapOf<String, Any>()
        json["Hash"] = hash().toString()

        head["Version"] = version
        head["PrevBlockHash"] = prevBlockHash.toString()
        head["TransactionsRoot"] = transactionsRoot.toString()
        head["BlockRoot"] = blockRoot.toString()
        head["Timestamp"] = timestamp
        head["Height"] = height
        head["ConsensusData"] = consensusData and Long.MAX_VALUE
        head["NextBookkeeper"] = nextBookkeeper.toBase58()
        head["Hash"] = hash().toString()
        head["SigData"] = sigData!!
        head["Bookkeepers"] = bookkeepers.map(Helper::toHexString).toTypedArray()

        json["Header"] = head
        json["Transactions"] = transactions.map(Transaction::json).toTypedArray()
        return JSON.toJSONString(json)
    }

    fun trim(): ByteArray {
        try {
            ByteArrayOutputStream().use { ms ->
                BinaryWriter(ms).use { writer ->
                    serializeUnsigned(writer)
                    writer.writeByte(1.toByte())
                    writer.writeSerializableArray(transactions)
                    writer.flush()
                    return ms.toByteArray()
                }
            }
        } catch (ex: IOException) {
            throw UnsupportedOperationException(ex)
        }

    }

    override fun verify(): Boolean {
        return true
    }

    companion object {

        @Throws(IOException::class)
        fun fromTrimmedData(data: ByteArray, index: Int, txSelector: Function<UInt256, Transaction>?): Block {
            val block = Block()
            ByteArrayInputStream(data, index, data.size - index).use { ms ->
                try {
                    BinaryReader(ms).use { reader ->
                        block.deserializeUnsigned(reader)
                        reader.readByte()
                        if (txSelector == null) {
                            block.transactions = emptyArray()
                        } else {
                            block.transactions = arrayOfNulls<Transaction>(reader.readVarInt(0x10000000).toInt()) as Array<Transaction>
                            for (i in block.transactions.indices) {
                                block.transactions[i] = txSelector.apply(reader.readSerializable(UInt256::class.java))
                            }
                        }
                    }
                } catch (ex: InstantiationException) {
                    throw IOException(ex)
                } catch (ex: IllegalAccessException) {
                    throw IOException(ex)
                }
            }
            return block
        }
    }

}
