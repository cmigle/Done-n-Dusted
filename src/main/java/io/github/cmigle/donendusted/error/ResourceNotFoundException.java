package io.github.cmigle.donendusted.error;

/** 404 - resursas neegzistuoja arba nepriklauso nurodytam tevui. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException guild(Long id) {
        return new ResourceNotFoundException("Guild " + id + " not found");
    }

    public static ResourceNotFoundException quest(Long guildId, Long questId) {
        return new ResourceNotFoundException("Quest " + questId + " not found in guild " + guildId);
    }

    public static ResourceNotFoundException logEntry(Long questId, Long logId) {
        return new ResourceNotFoundException("Log entry " + logId + " not found in quest " + questId);
    }
}
