// Client API tối thiểu. Token lưu localStorage; mọi request đính Bearer nếu có.
const TOKEN_KEY = 'agrisense_token';

export const auth = {
  get token() { return localStorage.getItem(TOKEN_KEY); },
  set(token: string) { localStorage.setItem(TOKEN_KEY, token); },
  clear() { localStorage.removeItem(TOKEN_KEY); },
};

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  if (auth.token) headers.set('Authorization', `Bearer ${auth.token}`);

  const res = await fetch(`/api${path}`, { ...options, headers });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || `Lỗi ${res.status}`);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

export interface TokenResponse { token: string; email: string; }
export interface Farm { id: number; name: string; location: string | null; cropType: string | null; createdAt: string; }
export interface Sensor { id: number; farmId: number; type: string; unit: string; thresholdMin: number | null; thresholdMax: number | null; }
export interface Reading { value: number; recordedAt: string; }
export interface Alert { id: number; sensorId: number; readingValue: number; threshold: number; type: string; createdAt: string; resolved: boolean; }

export const api = {
  register: (email: string, password: string) =>
    request<TokenResponse>('/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  login: (email: string, password: string) =>
    request<TokenResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),

  listFarms: () => request<Farm[]>('/farms'),
  createFarm: (name: string, location: string, cropType: string) =>
    request<Farm>('/farms', { method: 'POST', body: JSON.stringify({ name, location, cropType }) }),

  listSensors: (farmId: number) => request<Sensor[]>(`/farms/${farmId}/sensors`),
  createSensor: (farmId: number, s: Omit<Sensor, 'id' | 'farmId'>) =>
    request<Sensor>(`/farms/${farmId}/sensors`, { method: 'POST', body: JSON.stringify(s) }),

  readings: (sensorId: number, range = '24h') =>
    request<Reading[]>(`/sensors/${sensorId}/readings?range=${range}`),

  listAlerts: (farmId: number) => request<Alert[]>(`/farms/${farmId}/alerts`),
  resolveAlert: (alertId: number) =>
    request<Alert>(`/alerts/${alertId}/resolve`, { method: 'POST' }),

  replay: (farmId: number, mode: 'historical' | 'live' = 'historical', intervalMs = 300) =>
    request<{ status: string }>(`/farms/${farmId}/replay?mode=${mode}&intervalMs=${intervalMs}`, { method: 'POST' }),

  analyze: (farmId: number) =>
    request<{ text: string }>(`/farms/${farmId}/analyze`, { method: 'POST' }),
  explainAlert: (alertId: number) =>
    request<{ text: string }>(`/alerts/${alertId}/explain`, { method: 'POST' }),
};
