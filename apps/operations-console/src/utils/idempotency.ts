export interface IdempotentDraft<T> {
  readonly key: string;
  readonly payload: T;
}

export function createIdempotencyKey(prefix = "console"): string {
  return `${prefix}:${crypto.randomUUID()}`;
}

export function createIdempotentDraft<T>(payload: T, existingKey?: string): IdempotentDraft<T> {
  return {
    key: existingKey ?? createIdempotencyKey(),
    payload,
  };
}
