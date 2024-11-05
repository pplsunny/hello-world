import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

public class DuplicateChecker {

    public static boolean hasDuplicates(Iterable<JsonNode> nodes) {
        Set<String> uniqueKeys = new HashSet<>();

        for (JsonNode node : nodes) {
            // 获取 id 和 category 字段值，并进行拼接
            String id = node.get("id").asText();
            String category = node.get("category").asText();
            String uniqueKey = id + "_" + category;

            // 如果该组合已经存在于集合中，说明有重复
            if (!uniqueKeys.add(uniqueKey)) {
                return true; // 存在重复项
            }
        }

        return false; // 没有重复项
    }
}
