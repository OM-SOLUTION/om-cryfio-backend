package com.cryfio.resource;

import com.cryfio.account.domain.Account;
import com.cryfio.account.repository.AccountRepository;
import com.cryfio.common.exception.ApiException;
import com.cryfio.common.exception.ErrorCode;
import com.cryfio.resource.domain.Resource;
import com.cryfio.resource.domain.ResourceType;
import com.cryfio.resource.dto.CreateFolderRequest;
import com.cryfio.resource.dto.CreateFolderResponse;
import com.cryfio.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final AccountRepository accountRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    public CreateFolderResponse createFolder(UUID accountId,CreateFolderRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(()->new ApiException(ErrorCode.UNAUTHORIZED));
        Resource parentFolder = findParentFolder(request.parentFolderId(),accountId);
        Resource folder =Resource.createFolder(account,parentFolder,request.name());
        resourceRepository.save(folder);
        return new CreateFolderResponse(
                folder.getId(),
                parentFolder == null ? null : parentFolder.getId(),
                folder.getName(),
                folder.getCreatedAt()
        );
    }
    private Resource findParentFolder(UUID parentFolderId, UUID accountId) {
        if(parentFolderId == null) return null;
        Resource parentFolder = resourceRepository.findByIdAndOwner_Id(parentFolderId,accountId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,"Destination folder was not found"));
        if(parentFolder.getResourceType() != ResourceType.FOLDER){
            throw new ApiException(ErrorCode.INVALID_REQUEST,"Parent resource must be a folder");
        }
        return parentFolder;
    }
}
