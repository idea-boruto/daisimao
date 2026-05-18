export type TaskStatus = 1 | 2 | 3 | 4 | 5 | 6 | 7;

const statusConfig: Record<number, { label: string; color: string }> = {
  1: { label: '待接单', color: 'bg-blue-50 text-blue-600' },
  2: { label: '已接单', color: 'bg-purple-50 text-purple-600' },
  3: { label: '进行中', color: 'bg-orange-50 text-orange-600' },
  4: { label: '待确认', color: 'bg-yellow-50 text-yellow-600' },
  5: { label: '已完成', color: 'bg-green-50 text-green-600' },
  6: { label: '已取消', color: 'bg-gray-50 text-gray-500' },
  7: { label: '纠纷', color: 'bg-red-50 text-red-600' },
};

interface Props {
  status: TaskStatus;
}

export default function StatusBadge({ status }: Props) {
  const config = statusConfig[status] || statusConfig[1];
  return (
    <span className={`text-xs px-2 py-0.5 rounded ${config.color}`}>
      {config.label}
    </span>
  );
}
