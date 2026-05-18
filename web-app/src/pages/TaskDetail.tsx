import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import StatusBadge from '../components/StatusBadge';
import type { TaskStatus } from '../components/StatusBadge';
import { useAuth } from '../hooks/useAuth';
import api from '../services/api';
import type { Task } from '../types';

const typeLabels: Record<number, string> = {
  1: '快递代取', 2: '代带饭', 3: '打印', 4: '代购', 5: '其他',
};

export default function TaskDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isLoggedIn } = useAuth();
  const [task, setTask] = useState<Task | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get(`/tasks/${id}`).then((res) => {
      setTask(res as unknown as Task);
    }).catch(() => {
      setError('任务不存在或已删除');
    }).finally(() => setLoading(false));
  }, [id]);

  const handleAction = async (action: string) => {
    if (!isLoggedIn) {
      navigate('/login');
      return;
    }
    setActionLoading(true);
    setError('');
    try {
      const res = await api.put(`/tasks/${id}/${action}`) as unknown as Task;
      setTask(res);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) return <div className="text-center py-16 text-gray-400">加载中...</div>;
  if (!task) return <div className="text-center py-16 text-danger">{error || '任务不存在'}</div>;

  const isPublisher = user?.userId === task.publisherId;
  const isAcceptor = user?.userId === task.acceptorId;

  const canAccept = isLoggedIn && !isPublisher && task.status === 1;
  const canStart = isAcceptor && task.status === 2 && !isPublisher;
  const canComplete = isAcceptor && task.status === 3 && !isPublisher;
  const canConfirm = isPublisher && task.status === 4;
  const canCancel = (isPublisher || isAcceptor) && [1, 2, 3].includes(task.status);

  return (
    <div className="max-w-lg mx-auto px-4 pt-4">
      <button
        onClick={() => navigate(-1)}
        className="text-gray-400 mb-4 flex items-center gap-1 text-sm"
      >
        ← 返回
      </button>

      <div className="bg-white rounded-lg p-4 mb-4">
        <div className="flex items-center justify-between mb-3">
          <span className="text-xs bg-blue-50 text-primary px-2 py-0.5 rounded">
            {typeLabels[task.type] || '其他'}
          </span>
          <StatusBadge status={task.status as TaskStatus} />
        </div>

        <h2 className="text-lg font-bold mb-3">{task.title}</h2>

        {task.description && (
          <p className="text-sm text-gray-600 mb-3">{task.description}</p>
        )}

        <div className="space-y-2 text-sm">
          <div className="flex items-center justify-between py-2 border-b border-gray-50">
            <span className="text-gray-400">跑腿费</span>
            <span className="text-danger font-bold text-lg">¥{task.reward}</span>
          </div>
          {task.pickupLocation && (
            <div className="flex items-center justify-between py-2 border-b border-gray-50">
              <span className="text-gray-400">取件地点</span>
              <span>{task.pickupLocation}</span>
            </div>
          )}
          {task.deliveryLocation && (
            <div className="flex items-center justify-between py-2 border-b border-gray-50">
              <span className="text-gray-400">送达地点</span>
              <span>{task.deliveryLocation}</span>
            </div>
          )}
          {task.deadline && (
            <div className="flex items-center justify-between py-2 border-b border-gray-50">
              <span className="text-gray-400">截止时间</span>
              <span>{new Date(task.deadline).toLocaleString('zh-CN')}</span>
            </div>
          )}
          <div className="flex items-center justify-between py-2 border-b border-gray-50">
            <span className="text-gray-400">发布人</span>
            <span>
              {task.publisherNickname || '匿名'}
              {task.publisherCreditScore !== undefined && (
                <span className="ml-1 text-warning text-xs">{task.publisherCreditScore}分</span>
              )}
            </span>
          </div>
          {task.acceptorNickname && (
            <div className="flex items-center justify-between py-2 border-b border-gray-50">
              <span className="text-gray-400">接单人</span>
              <span>{task.acceptorNickname}</span>
            </div>
          )}
          <div className="flex items-center justify-between py-2">
            <span className="text-gray-400">发布时间</span>
            <span className="text-xs">{new Date(task.createdAt).toLocaleString('zh-CN')}</span>
          </div>
        </div>
      </div>

      {error && <p className="text-danger text-sm text-center mb-3">{error}</p>}

      <div className="flex gap-3">
        {canAccept && (
          <button
            onClick={() => handleAction('accept')}
            disabled={actionLoading}
            className="flex-1 py-3 rounded-lg bg-primary text-white font-medium disabled:opacity-50"
          >
            接单
          </button>
        )}
        {canStart && (
          <button
            onClick={() => handleAction('start')}
            disabled={actionLoading}
            className="flex-1 py-3 rounded-lg bg-primary text-white font-medium disabled:opacity-50"
          >
            确认开始
          </button>
        )}
        {canComplete && (
          <button
            onClick={() => handleAction('complete')}
            disabled={actionLoading}
            className="flex-1 py-3 rounded-lg bg-success text-white font-medium disabled:opacity-50"
          >
            标记完成
          </button>
        )}
        {canConfirm && (
          <button
            onClick={() => handleAction('confirm')}
            disabled={actionLoading}
            className="flex-1 py-3 rounded-lg bg-success text-white font-medium disabled:opacity-50"
          >
            确认完成
          </button>
        )}
        {canCancel && (
          <button
            onClick={() => handleAction('cancel')}
            disabled={actionLoading}
            className="flex-1 py-3 rounded-lg bg-gray-100 text-gray-600 font-medium disabled:opacity-50"
          >
            取消
          </button>
        )}
        {!isLoggedIn && task.status === 1 && (
          <button
            onClick={() => navigate('/login')}
            className="flex-1 py-3 rounded-lg bg-primary text-white font-medium"
          >
            登录后接单
          </button>
        )}
      </div>
    </div>
  );
}
