const normalizeBaseUrl = (value: string | undefined, fallback: string): string => {
  const raw = value?.trim();
  if (!raw) {
    return fallback;
  }

  return raw.replace(/\/+$/, '');
};

const env = process.env;

const parseBooleanEnv = (value: string | undefined, fallback: boolean): boolean => {
  if (value == null) {
    return fallback;
  }

  const normalized = value.trim().toLowerCase();
  if (!normalized) {
    return fallback;
  }

  return !['0', 'false', 'no', 'off'].includes(normalized);
};

/**
 * Convert HTTPS/HTTP URLs to WebSocket protocols (WSS/WS)
 * Required because API Gateway endpoints must use WebSocket protocols for WebSocket connections
 */
export const convertToWebSocketProtocol = (endpoint: string): string => {
  try {
    if (endpoint.startsWith('wss://') || endpoint.startsWith('ws://')) {
      return endpoint; // Already a WebSocket protocol
    }
    if (endpoint.startsWith('https://')) {
      return endpoint.replace('https://', 'wss://');
    }
    if (endpoint.startsWith('http://')) {
      return endpoint.replace('http://', 'ws://');
    }
    return endpoint;
  } catch {
    return endpoint;
  }
};

export const API_BASE_URL = normalizeBaseUrl(
  env.VITE_API_BASE_URL,
  'https://5vo07e6o58.execute-api.us-east-2.amazonaws.com/prod/api/v1',
);

export const WS_ENDPOINT = normalizeBaseUrl(
  env.VITE_WS_ENDPOINT,
  'https://js545mgwdj.execute-api.us-east-2.amazonaws.com/prod/api/v1/ws/board',
);

export const ENABLE_REALTIME = parseBooleanEnv(env.VITE_ENABLE_REALTIME, true);