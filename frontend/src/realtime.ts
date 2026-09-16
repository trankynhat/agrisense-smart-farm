import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { auth } from './api';

export interface ReadingMsg { sensorId: number; type: string; value: number; recordedAt: string; }
export interface AlertMsg { id: number; sensorId: number; type: string; value: number; threshold: number; createdAt: string; }
export interface Envelope { kind: 'reading' | 'alert'; payload: ReadingMsg | AlertMsg; }

/** Kết nối STOMP tới /topic/farm/{farmId}, gọi handler cho mỗi envelope. Trả hàm ngắt kết nối. */
export function subscribeFarm(farmId: number, onMsg: (e: Envelope) => void): () => void {
  const client = new Client({
    // SockJS dùng http(s); vite proxy /ws → backend 8080
    webSocketFactory: () => new SockJS('/api/ws') as never,
    connectHeaders: { token: auth.token ?? '' },
    reconnectDelay: 3000,
    onConnect: () => {
      client.subscribe(`/topic/farm/${farmId}`, (m: IMessage) => {
        try { onMsg(JSON.parse(m.body) as Envelope); } catch { /* bỏ qua msg lỗi */ }
      });
    },
  });
  client.activate();
  return () => { client.deactivate(); };
}
