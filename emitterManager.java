import EmitterRegistry.EmitterKey;
import EmitterRegistry.EmitterSearchResult;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages Server-Sent Events (SSE) emitters for different resource types.
 */
public class EmitterManager {

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    public final EmitterRegistry emitterRegistry = new EmitterRegistry();

    /**
     * Looks up an emitter for a specific resource type and resource ID.
     */
    public SseEmitter lookupEmitter(String resourceType, String resourceId, String clientKey) {
        if (resourceId.isEmpty() || clientKey.isEmpty()) {
            return null;
        }
        return emitterRegistry.getEmitter(resourceType, resourceId, clientKey);
    }

    /**
     * Pings all emitters and removes inactive ones.
     */
    public void pingAllEmitters() {
        emitterRegistry.forEachEmitter((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("ping").data("checking-if-alive"));
            } catch (Exception e) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {}
                emitterRegistry.removeEmitter(key.resourceType(), key.resourceId(), key.clientKey());
            }
        });
    }

    /**
     * Registers a new SSE emitter.
     */
    public void registerEmitter(SseEmitter emitter, String resourceType, String resourceId, String clientKey) {
        if (resourceId.isEmpty() || clientKey.isEmpty()) {
            return;
        }

        emitter.onCompletion(() -> removeEmitter(resourceType, resourceId, clientKey));
        emitter.onTimeout(() -> {
            removeEmitter(resourceType, resourceId, clientKey);
            emitter.complete();
        });
        emitter.onError(e -> removeEmitter(resourceType, resourceId, clientKey));

        emitterRegistry.putEmitter(resourceType, resourceId, clientKey, emitter);
    }

    /**
     * Searches for emitter given resourceType and resourceId.
     */
    public Optional<EmitterSearchResult> searchEmitter(String resourceType, String resourceId) {
        Optional<Map.Entry<EmitterKey, SseEmitter>> entryOpt = 
            emitterRegistry.findEntryByResourceId(resourceType, resourceId);
        
        if (entryOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Map.Entry<EmitterKey, SseEmitter> entry = entryOpt.get();
        EmitterKey key = entry.getKey();
        return Optional.of(new EmitterSearchResult(key.clientKey(), entry.getValue()));
    }

    /**
     * Removes an SSE emitter.
     */
    public void removeEmitter(String resourceType, String resourceId, String clientKey) {
        if (resourceId.isEmpty() || clientKey.isEmpty()) {
            return;
        }
        emitterRegistry.removeEmitter(resourceType, resourceId, clientKey);
    }
}