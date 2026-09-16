import { Navigate, Route, Routes } from 'react-router-dom';
import { auth } from './api';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import FarmDetail from './pages/FarmDetail';

function RequireAuth({ children }: { children: JSX.Element }) {
  return auth.token ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<RequireAuth><Dashboard /></RequireAuth>} />
      <Route path="/farms/:farmId" element={<RequireAuth><FarmDetail /></RequireAuth>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
