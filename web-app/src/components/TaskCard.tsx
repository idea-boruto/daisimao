import type { TaskStatus } from './StatusBadge';
import StatusBadge from './StatusBadge';
import type { Task } from '../types';
import { useNavigate } from 'react-router-dom';

const typeLabels: Record<number, string> = {
  1: '快递代取', 2: '代带饭', 3: '打印', 4: '代购', 5: '其他',
};

interface Props {
  task: Task;
}

export default function TaskCard({ task }: Props) {
  const navigate = useNavigate();

  return (
    <div
      className="bg-white rounded-lg shadow-sm p-4 mb-3 active:bg-gray-50 transition-colors"
      onClick={() => navigate(`/task/${task.id}`)}
      data-testid="task-card"
    >
      <div className="flex items-start justify-between mb-2">
        <span className="text-xs bg-blue-50 text-primary px-2 py-0.5 rounded">
          {typeLabels[task.type] || '其他'}
        </span>
        <StatusBadge status={task.status as TaskStatus} />
      </div>

      <h3 className="text-base font-medium text-gray-900 mb-1.5 line-clamp-2">
        {task.title}
      </h3>

      <div className="flex items-center justify-between text-sm">
        <span className="text-danger font-bold text-lg">¥{task.reward}</span>
        <span className="text-gray-400 text-xs">{task.deliveryLocation}</span>
      </div>

      <div className="flex items-center justify-between mt-2 text-xs text-gray-400">
        <span>
          {task.publisherNickname || '匿名用户'}
          {task.publisherCreditScore !== undefined && (
            <span className="ml-1 text-warning">{task.publisherCreditScore}分</span>
          )}
        </span>
        <span>{formatTime(task.createdAt)}</span>
      </div>
    </div>
  );
}

function formatTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60_000) return '刚刚';
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}分钟前`;
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}小时前`;
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
}
