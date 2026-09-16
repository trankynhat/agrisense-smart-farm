import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { api, Alert, Sensor } from '../api';
import { AlertMsg, Envelope, ReadingMsg, subscribeFarm } from '../realtime';

interface Point { t: string; value: number; }

export default function FarmDetail() {
  const farmId = Number(useParams().farmId);
  const [sensors, setSensors] = useState<Sensor[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [series, setSeries] = useState<Point[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [analysis, setAnalysis] = useState('');
  const [busy, setBusy] = useState(false);
  const [live, setLive] = useState(false);
  const selectedRef = useRef<number | null>(null);
  const sensorsRef = useRef<Sensor[]>([]);

  selectedRef.current = selected;
  sensorsRef.current = sensors;

  // load sensors + alerts, subscribe realtime
  useEffect(() => {
    (async () => {
      const s = await api.listSensors(farmId);
      setSensors(s);
      if (s.length) setSelected(s[0].id);
      setAlerts(await api.listAlerts(farmId));
    })();

    const unsub = subscribeFarm(farmId, (e: Envelope) => {
      if (e.kind === 'reading') {
        const r = e.payload as ReadingMsg;
        // chỉ vẽ nếu reading thuộc sensor đang chọn
        const sel = selectedRef.current;
        const selSensor = sensorsRef.current.find((x) => x.id === sel);
        if (selSensor && r.type === selSensor.type) {
          setSeries((prev) => [...prev.slice(-199), { t: r.recordedAt, value: r.value }]);
        }
      } else {
        const a = e.payload as AlertMsg;
        setAlerts((prev) => [{
          id: a.id, sensorId: a.sensorId, readingValue: a.value, threshold: a.threshold,
          type: a.type, createdAt: a.createdAt, resolved: false,
        }, ...prev]);
      }
    });
    return unsub;
  }, [farmId]);

  // load lịch sử khi đổi sensor
  useEffect(() => {
    if (selected == null) return;
    api.readings(selected, '24h').then((rs) =>
      setSeries(rs.map((r) => ({ t: r.recordedAt, value: r.value }))));
  }, [selected]);

  async function startReplay(mode: 'historical' | 'live') {
    setLive(mode === 'live');
    await api.replay(farmId, mode, mode === 'live' ? 500 : 100);
  }

  async function runAnalyze() {
    setBusy(true);
    setAnalysis('');
    try {
      const res = await api.analyze(farmId);
      setAnalysis(res.text);
    } finally {
      setBusy(false);
    }
  }

  async function explain(id: number) {
    const res = await api.explainAlert(id);
    alert(res.text);
  }

  const selSensor = sensors.find((s) => s.id === selected);

  return (
    <div className="page">
      <header>
        <h1><Link to="/" className="link">←</Link> Nông trại #{farmId}</h1>
        <div className="row">
          <button onClick={() => startReplay('historical')}>Replay lịch sử</button>
          <button onClick={() => startReplay('live')}>Live {live ? '●' : ''}</button>
        </div>
      </header>

      <div className="tabs">
        {sensors.map((s) => (
          <button key={s.id} className={s.id === selected ? 'tab active' : 'tab'}
                  onClick={() => setSelected(s.id)}>
            {s.type}
          </button>
        ))}
      </div>

      <div className="chart">
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={series} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="t" tickFormatter={(t) => String(t).slice(11, 16)} minTickGap={40} />
            <YAxis unit={selSensor ? ` ${selSensor.unit}` : ''} width={70} />
            <Tooltip labelFormatter={(t) => String(t).replace('T', ' ').slice(0, 16)} />
            <Line type="monotone" dataKey="value" stroke="#2d6a3e" dot={false} isAnimationActive={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <div className="ai">
        <button onClick={runAnalyze} disabled={busy}>
          {busy ? 'Đang phân tích...' : '🤖 Phân tích sức khỏe (AI)'}
        </button>
        {analysis && <pre className="ai-text">{analysis}</pre>}
      </div>

      <h2>Cảnh báo</h2>
      {alerts.length === 0 ? (
        <p className="muted">Chưa có cảnh báo. Chạy replay để sinh dữ liệu.</p>
      ) : (
        <ul className="farms">
          {alerts.map((a) => (
            <li key={a.id} className={a.resolved ? 'resolved' : ''}>
              <span>
                <strong>{a.type === 'min' ? '▼' : '▲'} {a.readingValue.toFixed(1)}</strong>
                <span className="muted"> (ngưỡng {a.threshold}) · {a.createdAt.replace('T', ' ').slice(0, 16)}</span>
              </span>
              <span className="row">
                <button className="link" onClick={() => explain(a.id)}>Giải thích</button>
                {!a.resolved && (
                  <button className="link" onClick={() =>
                    api.resolveAlert(a.id).then((u) =>
                      setAlerts((prev) => prev.map((x) => x.id === u.id ? u : x)))}>
                    Đã xử lý
                  </button>
                )}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
