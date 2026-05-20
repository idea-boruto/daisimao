import { useState, useEffect, useRef } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import api from '../services/api';

const tabs = [
  { to: '/', label: '任务大厅' },
  { to: '/publish', label: '发布任务', auth: true },
  { to: '/my-tasks', label: '我的任务', auth: true },
];

export default function TopNav() {
  const { user, isLoggedIn, logout } = useAuth();
  const location = useLocation();
  const [unreadCount, setUnreadCount] = useState(0);
  const pollingRef = useRef<ReturnType<typeof setInterval>>();

  useEffect(() => {
    if (!isLoggedIn) {
      setUnreadCount(0);
      return;
    }
    const fetchUnread = async () => {
      try {
        const res = (await api.get('/notifications/unread-count')) as unknown as { count: number };
        setUnreadCount(res.count ?? 0);
      } catch {
        // silent
      }
    };
    fetchUnread();
    pollingRef.current = setInterval(fetchUnread, 10_000);
    return () => {
      if (pollingRef.current) clearInterval(pollingRef.current);
    };
  }, [isLoggedIn]);

  const isActive = (to: string) =>
    location.pathname === to || (to !== '/' && location.pathname.startsWith(to));

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-4xl mx-auto px-6 h-12 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <NavLink to="/" className="text-base font-bold text-primary">
            代事猫
          </NavLink>
          <nav className="flex gap-1">
            {tabs
              .filter((t) => !t.auth || isLoggedIn)
              .map((tab) => (
                <NavLink
                  key={tab.to}
                  to={tab.to}
                  className={`px-3 py-1.5 rounded text-sm transition-colors ${
                    isActive(tab.to)
                      ? 'bg-blue-50 text-primary font-medium'
                      : 'text-gray-600 hover:text-primary'
                  }`}
                >
                  {tab.label}
                </NavLink>
              ))}
          </nav>
        </div>
        <div className="flex items-center gap-3 text-sm">
          {isLoggedIn ? (
            <>
              <NavLink
                to="/notifications"
                className={`relative px-3 py-1.5 rounded transition-colors ${
                  isActive('/notifications')
                    ? 'bg-blue-50 text-primary font-medium'
                    : 'text-gray-600 hover:text-primary'
                }`}
              >
                消息
                {unreadCount > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 bg-danger text-white text-xs rounded-full min-w-[16px] h-4 flex items-center justify-center px-0.5">
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </span>
                )}
              </NavLink>
              <NavLink
                to="/profile"
                className={`px-3 py-1.5 rounded transition-colors ${
                  isActive('/profile')
                    ? 'bg-blue-50 text-primary font-medium'
                    : 'text-gray-600 hover:text-primary'
                }`}
              >
                {user?.nickname}
              </NavLink>
              <button
                onClick={logout}
                className="text-gray-400 hover:text-danger transition-colors"
              >
                退出
              </button>
            </>
          ) : (
            <NavLink
              to="/login"
              className="px-4 py-1.5 rounded-full bg-primary text-white text-xs font-medium"
            >
              登录
            </NavLink>
          )}
        </div>
      </div>
    </header>
  );
}
