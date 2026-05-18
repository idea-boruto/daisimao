import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';
import BottomNav from './components/BottomNav';
import Login from './pages/Login';
import Home from './pages/Home';
import Publish from './pages/Publish';
import TaskDetail from './pages/TaskDetail';
import MyTasks from './pages/MyTasks';
import Profile from './pages/Profile';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isLoggedIn } = useAuth();
  if (!isLoggedIn) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  const { isLoggedIn } = useAuth();

  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50">
        <div className="pb-16">
          <Routes>
            <Route path="/login" element={
              isLoggedIn ? <Navigate to="/" replace /> : <Login />
            } />
            <Route path="/" element={<Home />} />
            <Route path="/publish" element={
              <ProtectedRoute><Publish /></ProtectedRoute>
            } />
            <Route path="/task/:id" element={<TaskDetail />} />
            <Route path="/my-tasks" element={
              <ProtectedRoute><MyTasks /></ProtectedRoute>
            } />
            <Route path="/profile" element={
              <ProtectedRoute><Profile /></ProtectedRoute>
            } />
          </Routes>
        </div>
        {isLoggedIn && <BottomNav />}
      </div>
    </BrowserRouter>
  );
}
