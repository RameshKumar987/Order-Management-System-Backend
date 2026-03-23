package com.oms.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenIntrospectionResponse {

    private Boolean active;

    @JsonProperty("token_type")
    private String tokenType;

    private String scope;

    @JsonProperty("client_id")
    private String clientId;

    private String username;

    @JsonProperty("token_issue_time")
    private Long tokenIssueTime;

    @JsonProperty("token_expiration_time")
    private Long tokenExpirationTime;

    private String role;

    private List<String> scopes;
}

