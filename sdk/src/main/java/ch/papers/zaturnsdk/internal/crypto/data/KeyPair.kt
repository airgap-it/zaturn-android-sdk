package ch.papers.zaturnsdk.internal.crypto.data

internal typealias PrivateKey = ByteArray
internal typealias PublicKey = ByteArray
internal typealias SessionKey = ByteArray

internal class KeyPair(val privateKey: PrivateKey, val publicKey: PublicKey)