import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** A registry for managing SSE emitters. */
public class EmitterRegistry {

    private final Cache<EmitterKey, SseEmitter> emitters =
            Caffeine.newBuilder()
                    .maximumSize(100)
                    .build();

    /**
     * Puts an emitter into the registry.
     */
    public boolean putEmitter(String resourceType, String resourceId, String clientKey, SseEmitter emitter) {
        EmitterKey key = new EmitterKey(resourceType, resourceId, clientKey);
        boolean replaced = emitters.getIfPresent(key) != null;
        emitters.put(key, emitter);
        return replaced;
    }

    public void forEachEmitter(BiConsumer<EmitterKey, SseEmitter> action) {
        emitters.asMap().forEach(action);
    }

    /**
     * Finds emitter by resource type and resource ID.
     */
    public Optional<Map.Entry<EmitterKey, SseEmitter>> findEntryByResourceId(String resourceType, String resourceId) {
        return emitters.asMap().entrySet().stream()
                .filter(entry -> 
                    entry.getKey().resourceType().equals(resourceType) && 
                    entry.getKey().resourceId().equals(resourceId))
                .findFirst();
    }

    /**
     * Gets an emitter from the registry.
     */
    public SseEmitter getEmitter(String resourceType, String resourceId, String clientKey) {
        return emitters.getIfPresent(new EmitterKey(resourceType, resourceId, clientKey));
    }

    /**
     * Removes an emitter from the registry.
     */
    public SseEmitter removeEmitter(String resourceType, String resourceId, String clientKey) {
        EmitterKey key = new EmitterKey(resourceType, resourceId, clientKey);
        return emitters.asMap().remove(key);
    }

    /**
     * Returns the total number of emitters in the registry.
     */
    public int size() {
        return (int) emitters.estimatedSize();
    }

    /**
     * Checks if the registry is empty.
     */
    public boolean isEmpty() {
        return emitters.asMap().isEmpty();
    }

    /** A record representing a composite key for the emitter cache. */
    public record EmitterKey(String resourceType, String resourceId, String clientKey) {}

    /** A record for client key and emitter, helpful for notification sending process */
    public record EmitterSearchResult(String clientKey, SseEmitter emitter) {}
}
