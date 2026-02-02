package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record TotalStatsDto( int maxCount, List<TotalCountDto> counts ) {}