import { useState, useEffect, useCallback } from 'react';
import TaskCard from '../components/TaskCard';
import api from '../services/api';
import type { Task } from '../types';

type Tab = 'published' | 'accepted';

export default function MyTasks() {
  const [tab, setTab] = useState<Tab>('published');
  const [published, setPublished] = useState<Task[]>([]);
  const [accepted, setAccepted] = useState<Task[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchTasks = useCallback(async () => {
    setLoading(true);
    try {
      if (tab === 'published') {
        const res = await api.get('/tasks/mine/published') as unknown as Task[];
        setPublished(Array.isArray(res) ? res : []);
      } else {
        const res = await api.get('/tasks/mine/accepted') as unknown as Task[];
        setAccepted(Array.isArray(res) ? res : []);
      }
    } catch {
      // silent fail
    } finally {
      setLoading(false);
    }
  }, [tab]);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  const tasks = tab === 'published' ? published : accepted;

  return (
    <div className="max-w-lg mx-auto px-4 pt-4">
      <h1 className="text-lg font-bold mb-4">我的任务</h1>

      <div className="flex rounded-lg bg-gray-100 p-1 mb-4">
        {(['published', 'accepted'] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`flex-1 py-2 rounded-md text-sm font-medium transition-colors ${
              tab === t ? 'bg-white text-primary shadow-sm' : 'text-gray-500'
            }`}
          >
            {t === 'published' ? '我发布的' : '我接的'}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="text-center py-16 text-gray-400">加载中...</div>
      ) : tasks.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          {tab === 'published' ? '还没有发布过任务' : '还没有接过任务'}
        </div>
      ) : (
        tasks.map((task) => <TaskCard key={task.id} task={task} />)
      )}
    </div>
  );
}
