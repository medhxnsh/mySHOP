package com.myshop.service.ai;

/**
 * Thrown when the embedding provider is unreachable or returns garbage.
 * Unchecked on purpose: most callers degrade rather than handle it locally.
 */
public class EmbeddingUnavailableException extends RuntimeException {

    public EmbeddingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
