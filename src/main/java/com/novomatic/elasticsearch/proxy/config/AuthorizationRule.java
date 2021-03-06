package com.novomatic.elasticsearch.proxy.config;

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Set;

@Data
@NoArgsConstructor
public class AuthorizationRule {
    /**
     * A rule defining a subset of users allowed to perform specified actions on specified resources.
     */
    private PrincipalConstraints principal = new PrincipalConstraints();
    /**
     * A rule defining a subset of Elasticsearch resources allowed to be accessed.
     */
    private ResourcesConstraints resources = new ResourcesConstraints();
    /**
     * A collection of actions allowed to be executed on specified resources when the principal
     * accessing them matches specified limitations.
     */
    private Set<ResourceAction> actions = Sets.newHashSet(ResourceAction.READ, ResourceAction.WRITE);

    public boolean isApplicableForIndices(Collection<String> requestIndices) {
        if (requestIndices.isEmpty()) {
            return true;
        }
        return getResources().getIndices().stream().anyMatch(indexPattern ->
                requestIndices.stream().anyMatch(requestedIndexPattern ->
                    requestedIndexPattern.matches(indexPatternToRegex(indexPattern)) ||
                    indexPattern.matches(indexPatternToRegex(requestedIndexPattern))
                ));
    }

    private static String indexPatternToRegex(String indexPattern) {
        return indexPattern
                .replace(".", "\\.")
                .replace("*", "(.*)");
    }
}
