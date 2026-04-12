package com.oms.auth_service.dto;

public record UpdateAddressRequest(
    String addressLine,
    String city,
    String state,
    String pincode,
    Double latitude,
    Double longitude
) {}
