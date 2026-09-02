package com.bbpms.dispatch.dto;

/**
 * Final dispatch outcome returned by autoDispatch / manualDispatch / reassign.
 */
public record DispatchResultDTO(
        Long workOrderId,
        String workNo,
        Long installerId,
        String installerName,
        Double score,
        String candidatesJson
) {}