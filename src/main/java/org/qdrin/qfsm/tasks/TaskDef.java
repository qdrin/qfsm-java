package org.qdrin.qfsm.tasks;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.*;
import lombok.Builder.Default;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDef {
    TaskType type;
    String productId;
    @Default
    OffsetDateTime wakeAt = OffsetDateTime.now();
    @Default
    Map<String, Object> variables = new HashMap<>();

    @Override
    public boolean equals(Object obj) {
        if(obj == null || getClass() != obj.getClass()) return false;
        if(this == obj) return true;
        TaskDef taskDef = (TaskDef) obj;
        boolean res = false;
        if(this.productId.equals(taskDef.productId) && this.type == taskDef.type) {
            res = true;
        }
        return res;
    }

    @Override
    public int hashCode() {
        int result = productId == null ? 0 : productId.hashCode();
        result = 31 * result + type.ordinal();
        return result;
    }
}
