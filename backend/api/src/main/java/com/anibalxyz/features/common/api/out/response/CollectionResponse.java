package com.anibalxyz.features.common.api.out.response;

import java.util.List;

public record CollectionResponse<T>(List<T> data, ResponseMeta meta) implements SuccessResponse {}
