package com.cryfio.authentication.srp;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.srp.SRP6StandardGroups;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.SRP6GroupParameters;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SrpConfiguration {
    public static final String PROTOCOL = "SRP-6a";
    public static final String GROUP_ID = "RFC5054_3072";
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final int VERIFIER_BYTES = 384;
    public static final SRP6GroupParameters GROUP = SRP6StandardGroups.rfc5054_3072;
    public static final int PROOF_BYTES = 32;
    public static Digest newDigest() {
        return new SHA256Digest();
    }
}
