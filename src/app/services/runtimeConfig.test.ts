import { isUnsupportedRealtimeEndpoint } from './runtimeConfig';

describe('isUnsupportedRealtimeEndpoint', () => {
  it('flags API Gateway execute-api endpoints as unsupported for SockJS realtime', () => {
    expect(
      isUnsupportedRealtimeEndpoint('https://js545mgwdj.execute-api.us-east-2.amazonaws.com/prod/api/v1/ws/board')
    ).toBe(true);
  });

  it('allows localhost websocket endpoints', () => {
    expect(isUnsupportedRealtimeEndpoint('http://127.0.0.1:8080/api/v1/ws/board')).toBe(false);
  });

  it('treats malformed endpoints as supported so the caller can surface the real error', () => {
    expect(isUnsupportedRealtimeEndpoint('not-a-valid-url')).toBe(false);
  });
});