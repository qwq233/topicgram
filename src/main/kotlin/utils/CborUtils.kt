package top.qwq2333.utils

import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.model.DataItem
import java.io.ByteArrayOutputStream

fun encodeCbor(dataItem: DataItem?): ByteArray {
    val outputStream = ByteArrayOutputStream(1024)
    CborEncoder(outputStream).encode(dataItem)
    return outputStream.toByteArray()
}

fun decodeCbor(encodedBytes: ByteArray): DataItem {
    return CborDecoder.decode(encodedBytes)[0]
}