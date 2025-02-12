package top.qwq2333.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.sec.ECPrivateKey
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.SerialName as Name

@Serializable
data class AndroidAttestation(
    @Name("NumberOfKeyboxes") @XmlElement val numberOfKeybox: Int,
    @Name("Keybox") val keyBox: Keybox
)

@Serializable
data class Keybox(
    @Name("DeviceID") val deviceId: String,
    @Name("Key") val keys: List<Key>
)

@Serializable
data class Key(
    val algorithm: String,
    @Name("PrivateKey") @XmlElement val privateKey: PrivateKey,
    @Name("CertificateChain") @XmlElement val chain: CertificateChain,
) {
    val keyPair by lazy {
        KeyStore.PrivateKeyEntry(privateKey.priv, chain.x509Chains.toTypedArray())
    }

    fun validate() {
        val errors = mutableListOf<ValidationResult.Error>()
        var parent = chain.certificates.last()

        for (i in chain.certificates.size downTo 0) {
            val cert = chain.certificates[i]
            if (parent == cert) {
                cert.checkRootCa()?.let { errors.add(it) }
                continue
            }


        }

    }
}

@Serializable
data class PrivateKey(
    val format: String,
    @XmlValue val content: String,
) {
    @OptIn(ExperimentalEncodingApi::class)
    val priv: java.security.PrivateKey by lazy {
        when (algorithm) {
            "RSA" -> {
                val keySpec = content.removePrefix("-----BEGIN RSA PRIVATE KEY-----")
                    .removeSuffix("-----END RSA PRIVATE KEY-----")
                    .run {
                        Base64.decode(this.trimIndent())
                    }.run {
                        PKCS8EncodedKeySpec(this)
                    }
                KeyFactory.getInstance(algorithm)
                    .generatePrivate(keySpec)
            }

            "EC" -> {
                val sequence = ASN1Sequence.getInstance(
                    content.removePrefix("-----BEGIN EC PRIVATE KEY-----")
                        .removeSuffix("-----END EC PRIVATE KEY-----")
                        .run {
                            Base64.decode(this.trimIndent())
                        })
                val ecKey = ECPrivateKey.getInstance(sequence)
                val id = AlgorithmIdentifier(
                    X9ObjectIdentifiers.id_ecPublicKey,
                    ecKey.parametersObject
                )
                val data = PrivateKeyInfo(id, ecKey).getEncoded()
                val keySpec = PKCS8EncodedKeySpec(data)
                KeyFactory.getInstance("EC").generatePrivate(keySpec)
            }

            else -> throw IllegalArgumentException("Unknown algorithm $algorithm")
        }
    }

    val algorithm = if (content.lowercase().contains("rsa")) {
        "RSA"
    } else {
        "EC"
    }
}

@Serializable
data class CertificateChain(
    @Name("NumberOfCertificates") @XmlElement val numberOfCertificates: Int,
    @Name("Certificate") @XmlElement val certificates: List<Certificate>,
) {
    @Transient
    val mainCertificate = certificates.first()

    val x509Chains by lazy {
        mutableListOf<X509Certificate>().apply {
            certificates.forEach { add(it.x509) }
        }.toList()
    }
}

@Serializable
data class Certificate(
    val format: String,
    @XmlValue val content: String
) {
    @OptIn(ExperimentalEncodingApi::class)
    val x509 by lazy {
        CertificateFactory.getInstance("X.509").generateCertificate(
            content.removePrefix("-----BEGIN CERTIFICATE-----")
                .removeSuffix("-----END CERTIFICATE-----")
                .run {
                    Base64.decode(this).inputStream()
                }) as X509Certificate

    }

    /**
     * @return null if is Google Root CA
     */
    fun checkRootCa(): ValidationResult.Error? = x509.publicKey.encoded.run {
        if (contentEquals(GOOGLE_ROOT_PUBLIC_KEY)) {
            null
        } else if (contentEquals(AOSP_ROOT_EC_PUBLIC_KEY) || contentEquals(AOSP_ROOT_RSA_PUBLIC_KEY)) {
            ValidationResult.Error.AOSP_ROOT_CA
        } else {
            ValidationResult.Error.UNKNOWN_ROOT_CA
        }
    }

}

data class ValidationResult(
    val valid: Boolean,
    val errors: List<Error>,
) {
    enum class Error {
        EXPIRED, KEY_INVALID, UNKNOWN_ROOT_CA, AOSP_ROOT_CA, REVOKED
    }
}