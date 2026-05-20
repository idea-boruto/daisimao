import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import type { Notification } from '../types';

export default function Notifications() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = (await api.get('/notifications')) as unknown as Notification[];
      setNotifications(Array.isArray(res) ? res : []);
    } catch {
      setError('加载失败，请检查网络后重试');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  const handleMarkRead = async (id: number) => {
    try {
      await api.put(`/notifications/${id}/read`);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
      );
    } catch {
      // silent
    }
  };

  const handleClick = (n: Notification) => {
    if (!n.isRead) handleMarkRead(n.id);
    if (n.relatedTaskId) navigate(`/task/${n.relatedTaskId}`);
  };

  if (loading) {
    return <div className="text-center py-16 text-gray-400">加载中...</div>;
  }

  if (error) {
    return (
      <div className="text-center py-16">
        <p className="text-gray-500 mb-4">{error}</p>
        <button
          onClick={fetchNotifications}
          className="px-4 py-2 rounded bg-primary text-white text-sm hover:opacity-90 transition-opacity"
        >
          重试
        </button>
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-lg font-bold mb-4">消息通知</h1>
      {notifications.length === 0 ? (
        <div className="text-center py-16 text-gray-400">暂无通知</div>
      ) : (
        <div className="space-y-1">
          {notifications.map((n) => (
            <div
              key={n.id}
              onClick={() => handleClick(n)}
              role="button"
              data-testid="notification-item"
              className={`p-4 rounded-lg cursor-pointer transition-colors ${
                n.isRead
                  ? 'bg-white text-gray-500'
                  : 'bg-blue-50 text-gray-800 font-medium'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-sm">{n.content}</span>
                {!n.isRead && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleMarkRead(n.id);
                    }}
                    className="ml-2 text-xs text-primary hover:underline flex-shrink-0"
                  >
                    标为已读
                  </button>
                )}
              </div>
              <p className="text-xs text-gray-400 mt-1">
                {new Date(n.createdAt).toLocaleString('zh-CN')}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
