import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    const name = username.trim();
    if (!name) {
      setError('请输入用户名');
      return;
    }

    setLoading(true);
    setError('');
    try {
      console.log('[Login] calling login...');
      await login(name);
      console.log('[Login] login done, navigating to /');
      navigate('/', { replace: true });
    } catch (e: any) {
      console.error('[Login] error:', e);
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-center px-6 bg-white">
      <div className="max-w-sm mx-auto w-full">
        <div className="text-center mb-10">
          <h1 className="text-3xl font-bold text-primary mb-2">代事猫</h1>
          <p className="text-gray-400 text-sm">校园微任务撮合平台</p>
        </div>

        <form onSubmit={handleLogin}>
          <div className="mb-6">
            <label className="block text-sm text-gray-600 mb-1.5">用户名</label>
            <input
              type="text"
              maxLength={32}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="输入用户名即可登录"
              className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-colors"
              data-testid="username-input"
              autoFocus
            />
          </div>

          {error && (
            <p className="text-danger text-sm mb-4 text-center">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading || !username.trim()}
            className="w-full py-3 rounded-lg bg-primary text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
          >
            {loading ? '登录中...' : '登录 / 注册'}
          </button>
        </form>

        <p className="text-center text-xs text-gray-400 mt-6">
          未注册用户名将自动创建账号
        </p>
      </div>
    </div>
  );
}
