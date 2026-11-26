package com.cryfio.resource.domain;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
@NoArgsConstructor
@AllArgsConstructor
public class FileVersionKeyEnvelopeId implements Serializable {
    private UUID fileVersion;
    private UUID recipientAccount;
    @Override
    public boolean equals(Object object) {
        if(this == object) return true;
        if(!(object  instanceof FileVersionKeyEnvelopeId that)) return false;
        return Objects.equals(fileVersion,that.fileVersion)
                && Objects.equals(recipientAccount,that.recipientAccount);
    }
    @Override
    public int hashCode() {
        return Objects.hash(fileVersion,recipientAccount);
    }
}
