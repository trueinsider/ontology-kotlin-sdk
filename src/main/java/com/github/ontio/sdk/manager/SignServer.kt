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

package com.github.ontio.sdk.manager

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.network.exception.RpcException
import com.github.ontio.network.rpc.Interfaces
import com.github.ontio.network.rpc.RpcClient

import java.util.HashMap

class SignServer @Throws(Exception::class)
constructor(private val url: String) {
    private val rpcClient: Interfaces = Interfaces(url)

    @Throws(Exception::class)
    fun sendSigRawTx(rawTx: String): Any {
        val req = mutableMapOf<String, Any>()
        req["jsonrpc"] = "2.0"
        req["method"] = "sigrawtx"
        val params = mutableMapOf<String, Any>()
        params["raw_tx"] = rawTx
        req["params"] = params
        req["id"] = 1
        return send(req)
    }

    @Throws(Exception::class)
    fun sendMultiSigRawTx(rawTx: String, m: Int, pubkeys: Array<String>): Any {
        val req = mutableMapOf<String, Any>()
        req["jsonrpc"] = "2.0"
        req["method"] = "sigmutilrawtx"
        val params = mutableMapOf<String, Any>()
        params["raw_tx"] = rawTx
        params["m"] = m
        params["pub_keys"] = pubkeys
        req["params"] = params
        req["id"] = 1
        return send(req)
    }

    @Throws(Exception::class)
    fun sendSigTransferTx(asset: String, from: String, to: String, amount: Long, gasLimit: Long, gasPrice: Long): Any {
        val req = mutableMapOf<String, Any>()
        req["jsonrpc"] = "2.0"
        req["method"] = "sigtransfertx"
        val params = mutableMapOf<String, Any>()
        params["asset"] = asset
        params["from"] = from
        params["to"] = to
        params["amount"] = amount
        params["gas_limit"] = gasLimit
        params["gas_price"] = gasPrice
        req["params"] = params
        req["id"] = 1
        return send(req)
    }

    @Throws(Exception::class)
    fun sendSigNativeInvokeTx(contractAddr: String, method: String, version: Int, gasLimit: Long, gasPrice: Long, parameters: Map<*, *>): Any {
        val req = mutableMapOf<String, Any>()
        req["jsonrpc"] = "2.0"
        req["method"] = "sigtransfertx"
        val params = mutableMapOf<String, Any>()
        params["address"] = contractAddr
        params["method"] = method
        params["version"] = version
        params["gas_limit"] = gasLimit
        params["gas_price"] = gasPrice
        params["params"] = parameters
        req["params"] = params
        req["id"] = 1
        return send(req)
    }

    @Throws(Exception::class)
    fun sendSigNeoInvokeTx(contractAddr: String, version: Int, gasLimit: Long, gasPrice: Long, parameters: Map<*, *>): Any {
        val req = mutableMapOf<String, Any>()
        req["jsonrpc"] = "2.0"
        req["method"] = "sigtransfertx"
        val params = mutableMapOf<String, Any>()
        params["address"] = contractAddr
        params["version"] = version
        params["gas_limit"] = gasLimit
        params["gas_price"] = gasPrice
        params["params"] = parameters
        req["params"] = params
        req["id"] = 1
        return send(req)
    }

    @Throws(Exception::class)
    private fun send(req: Map<*, *>): Any {
        val response = rpcClient.send(req)
        println(response)
        return when {
            response == null -> throw RpcException(0, ErrorCode.ConnectUrlErr(url + "response is null. maybe is connect error"))
            response["error_code"] as Int == 0 -> response["result"]!!
            else -> throw RpcException(0, JSON.toJSONString(response))
        }
    }
}