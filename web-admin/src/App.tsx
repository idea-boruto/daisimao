import { Routes, Route, Navigate } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import TaskManage from './pages/TaskManage';
import UserManage from './pages/UserManage';
import Layout from './components/Layout';
import Login from './pages/Login';

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="tasks" element={<TaskManage />} />
        <Route path="users" element={<UserManage />} />
      </Route>
    </Routes>
  );
}

export default App;
