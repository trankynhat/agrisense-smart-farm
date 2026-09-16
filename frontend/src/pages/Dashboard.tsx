import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, auth, Farm } from '../api';

export default function Dashboard() {
  const nav = useNavigate();
  const [farms, setFarms] = useState<Farm[]>([]);
  const [name, setName] = useState('');
  const [location, setLocation] = useState('');
  const [cropType, setCropType] = useState('');
  const [error, setError] = useState('');

  function logout() {
    auth.clear();
    nav('/login', { replace: true });
  }

  async function load() {
    try {
      setFarms(await api.listFarms());
    } catch (err) {
      // token hết hạn → 401 → về login
      if (err instanceof Error && err.message.includes('401')) logout();
      else setError(err instanceof Error ? err.message : 'Lỗi tải farm');
    }
  }

  useEffect(() => { load(); }, []);

  async function addFarm(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await api.createFarm(name, location, cropType);
      setName(''); setLocation(''); setCropType('');
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi tạo farm');
    }
  }

  return (
    <div className="page">
      <header>
        <h1>Nông trại của tôi</h1>
        <button className="link" onClick={logout}>Đăng xuất</button>
      </header>

      <form className="row" onSubmit={addFarm}>
        <input placeholder="Tên farm" value={name} onChange={(e) => setName(e.target.value)} required />
        <input placeholder="Vị trí" value={location} onChange={(e) => setLocation(e.target.value)} />
        <input placeholder="Loại cây" value={cropType} onChange={(e) => setCropType(e.target.value)} />
        <button type="submit">Thêm</button>
      </form>
      {error && <p className="error">{error}</p>}

      {farms.length === 0 ? (
        <p className="muted">Chưa có farm nào. Thêm farm đầu tiên ở trên.</p>
      ) : (
        <ul className="farms">
          {farms.map((f) => (
            <li key={f.id} className="clickable" onClick={() => nav(`/farms/${f.id}`)}>
              <strong>{f.name}</strong>
              <span className="muted">{f.location || '—'} · {f.cropType || '—'}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
