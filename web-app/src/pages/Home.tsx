import { useState, useEffect, useCallback } from 'react';
import TaskCard from '../components/TaskCard';
import api from '../services/api';
import type { Task } from '../types';

const typeOptions = [
  { value: '', label: '全部' },
  { value: '1', label: '快递代取' },
  { value: '2', label: '代带饭' },
  { value: '3', label: '打印' },
  { value: '4', label: '代购' },
  { value: '5', label: '其他' },
];

export default function Home() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [type, setType] = useState('');
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);

  const fetchTasks = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = { page, size: 20 };
      if (type) params.type = type;
      const res = await api.get('/tasks', { params }) as unknown as {
        items: Task[];
        total: number;
      };
      if (page === 1) {
        setTasks(res.items);
      } else {
        setTasks((prev) => [...prev, ...res.items]);
      }
      setTotal(res.total);
    } catch {
      // silent fail, empty list
    } finally {
      setLoading(false);
    }
  }, [type, page]);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  const handleTypeChange = (t: string) => {
    setType(t);
    setPage(1);
  };

  const hasMore = tasks.length < total;

  return (
    <div className="max-w-lg mx-auto px-4 pt-4">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-bold">任务大厅</h1>
      </div>

      <div className="flex gap-2 mb-4 overflow-x-auto pb-1 scrollbar-hide">
        {typeOptions.map((opt) => (
          <button
            key={opt.value}
            onClick={() => handleTypeChange(opt.value)}
            className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-colors ${
              (opt.value === '' && type === '') || opt.value === type
                ? 'bg-primary text-white'
                : 'bg-white text-gray-600 border border-gray-200'
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {loading && tasks.length === 0 ? (
        <div className="text-center py-16 text-gray-400">加载中...</div>
      ) : tasks.length === 0 ? (
        <div className="text-center py-16">
          <p className="text-gray-400 mb-2">还没有任务</p>
          <p className="text-gray-300 text-sm">快来发布第一个任务吧</p>
        </div>
      ) : (
        <>
          {tasks.map((task) => (
            <TaskCard key={task.id} task={task} />
          ))}
          {hasMore && (
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={loading}
              className="w-full py-3 text-primary text-sm font-medium disabled:opacity-50"
            >
              {loading ? '加载中...' : '加载更多'}
            </button>
          )}
        </>
      )}
    </div>
  );
}
