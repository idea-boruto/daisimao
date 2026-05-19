import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import api from '../services/api';

export default function Profile() {
  const { user, logout, updateUser } = useAuth();
  const navigate = useNavigate();
  const [editing, setEditing] = useState(false);
  const [nickname, setNickname] = useState(user?.nickname || '');
  const [campus, setCampus] = useState(user?.campus || '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const handleSave = async () => {
    setSaving(true);
    setError('');
    try {
      await api.put('/user/profile', { nickname, campus });
      updateUser({ nickname, campus });
      setEditing(false);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-lg mx-auto px-4 pt-4">
      <h1 className="text-lg font-bold mb-4">我的</h1>

      <div className="bg-white rounded-lg p-6 mb-4 text-center">
        <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3">
          <span className="text-2xl">👤</span>
        </div>

        {editing ? (
          <div className="space-y-3">
            {error && <p className="text-danger text-xs">{error}</p>}
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="昵称"
              maxLength={32}
              className="w-full px-4 py-2 rounded-lg border border-gray-200 text-center focus:border-primary outline-none"
            />
            <input
              type="text"
              value={campus}
              onChange={(e) => setCampus(e.target.value)}
              placeholder="校区（如：北区）"
              maxLength={32}
              className="w-full px-4 py-2 rounded-lg border border-gray-200 text-center focus:border-primary outline-none"
            />
            <div className="flex gap-2">
              <button
                onClick={() => setEditing(false)}
                className="flex-1 py-2 rounded-lg bg-gray-100 text-gray-600 text-sm"
              >
                取消
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="flex-1 py-2 rounded-lg bg-primary text-white text-sm disabled:opacity-50"
              >
                {saving ? '保存中...' : '保存'}
              </button>
            </div>
          </div>
        ) : (
          <>
            <h2 className="text-lg font-bold">{user?.nickname || '未设置昵称'}</h2>
            <p className="text-sm text-gray-400 mt-1">@{user?.username}</p>
            <button
              onClick={() => setEditing(true)}
              className="mt-3 text-sm text-primary"
            >
              编辑资料
            </button>
          </>
        )}
      </div>

      <div className="bg-white rounded-lg divide-y divide-gray-50">
        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-sm text-gray-600">信用分</span>
          <span className="text-warning font-bold">{user?.creditScore ?? 100}</span>
        </div>
        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-sm text-gray-600">用户名</span>
          <span className="text-sm text-gray-400">@{user?.username}</span>
        </div>
        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-sm text-gray-600">校区</span>
          <span className="text-sm text-gray-400">{user?.campus || '未设置'}</span>
        </div>
      </div>

      <button
        onClick={handleLogout}
        className="w-full mt-6 py-3 rounded-lg bg-white text-danger text-sm font-medium border border-gray-100"
      >
        退出登录
      </button>
    </div>
  );
}
