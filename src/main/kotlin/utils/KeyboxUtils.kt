package top.qwq2333.utils

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.model.ByteString
import co.nstant.`in`.cbor.model.DataItem
import co.nstant.`in`.cbor.model.UnsignedInteger


class EekResponse {
    val challenge: ByteArray
    private val curveToGeek = HashMap<Int, ByteArray>()

    internal constructor(response: DataItem) {
        val respItems = (response as co.nstant.`in`.cbor.model.Array).dataItems
        val allEekChains = (respItems[0] as co.nstant.`in`.cbor.model.Array).dataItems
        for (entry in allEekChains) {
            val curveAndEekChain = (entry as co.nstant.`in`.cbor.model.Array).dataItems
            val curve: UnsignedInteger = curveAndEekChain[0] as UnsignedInteger
            val geek = encodeCbor(curveAndEekChain[1])
            curveToGeek.put(curve.value.toInt(), geek)
        }
        challenge = (respItems[1] as ByteString).bytes
    }

    fun getEekChain(curve: Int): ByteArray? {
        return curveToGeek.get(curve)
    }
}

val deviceInfo by lazy {
    CborBuilder()
        .addMap()

}