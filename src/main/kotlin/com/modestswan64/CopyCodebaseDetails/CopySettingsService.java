package com.modestswan64.CopyCodebaseDetails;

import com.intellij.openapi.components.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public final class CopySettingsService {
    private Set<String> ignoredFolders = new HashSet<>();

    public Set<String> getIgnoredFolders() {
        return Collections.unmodifiableSet(ignoredFolders);
    }

    public void setIgnoredFolders(Set<String> ignoredFolders) {
        this.ignoredFolders = new HashSet<>(ignoredFolders);
    }
}
