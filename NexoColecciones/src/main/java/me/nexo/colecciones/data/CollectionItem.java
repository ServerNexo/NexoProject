package me.nexo.colecciones.data;

import java.util.List;
import java.util.Map;

public record CollectionItem(
        String itemId,
        CollectionCategory category,
        String displayName,
        Map<Integer, List<String>> comandosRecompensa
) {}