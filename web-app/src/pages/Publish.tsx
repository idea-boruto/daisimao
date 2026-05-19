import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const taskTypes = [
  { value: 1, label: '快递代取' },
  { value: 2, label: '代带饭' },
  { value: 3, label: '打印' },
  { value: 4, label: '代购' },
  { value: 5, label: '其他' },
];

export default function Publish() {
  const navigate = useNavigate();
  const [type, setType] = useState(1);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [reward, setReward] = useState('');
  const [pickupLocation, setPickupLocation] = useState('');
  const [deliveryLocation, setDeliveryLocation] = useState('');
  const [deadline, setDeadline] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      setError('请输入任务标题');
      return;
    }
    if (!reward || Number(reward) < 1 || Number(reward) > 20) {
      setError('跑腿费需在 1-20 元之间');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const res = await api.post('/tasks', {
        type,
        title: title.trim(),
        description,
        reward: Number(reward),
        pickupLocation,
        deliveryLocation,
        deadline: deadline || undefined,
      }) as unknown as { id: number };
      navigate(`/task/${res.id}`, { replace: true });
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1 className="text-lg font-bold mb-4">发布任务</h1>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm text-gray-600 mb-1.5">任务类型</label>
          <div className="flex gap-2">
            {taskTypes.map((t) => (
              <button
                key={t.value}
                type="button"
                onClick={() => setType(t.value)}
                className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                  type === t.value
                    ? 'bg-primary text-white'
                    : 'bg-white text-gray-600 border border-gray-200'
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="block text-sm text-gray-600 mb-1.5">任务标题 *</label>
          <input
            type="text"
            maxLength={128}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="例如：帮拿中通快递"
            className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          />
        </div>

        <div>
          <label className="block text-sm text-gray-600 mb-1.5">任务描述</label>
          <textarea
            maxLength={512}
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="详细描述任务要求..."
            className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-primary focus:ring-1 focus:ring-primary outline-none resize-none"
          />
        </div>

        <div>
          <label className="block text-sm text-gray-600 mb-1.5">跑腿费 (¥) *</label>
          <input
            type="number"
            min={1}
            max={20}
            step={0.5}
            value={reward}
            onChange={(e) => setReward(e.target.value)}
            placeholder="1-20 元"
            className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm text-gray-600 mb-1.5">取件地点</label>
            <input
              type="text"
              value={pickupLocation}
              onChange={(e) => setPickupLocation(e.target.value)}
              placeholder="如：菜鸟驿站"
              className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-primary focus:ring-1 focus:ring-primary outline-none"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1.5">送达地点</label>
            <input
              type="text"
              value={deliveryLocation}
              onChange={(e) => setDeliveryLocation(e.target.value)}
              placeholder="如：北区12号楼"
              className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-primary focus:ring-1 focus:ring-primary outline-none"
            />
          </div>
        </div>

        <div>
          <label className="block text-sm text-gray-600 mb-1.5">截止时间（可选）</label>
          <input
            type="datetime-local"
            value={deadline}
            onChange={(e) => setDeadline(e.target.value)}
            className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-primary focus:ring-1 focus:ring-primary outline-none"
          />
        </div>

        {error && (
          <p className="text-danger text-sm text-center">{error}</p>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full py-3 rounded-lg bg-primary text-white font-medium disabled:opacity-50 transition-opacity"
        >
          {loading ? '发布中...' : '立即发布'}
        </button>
      </form>
    </div>
  );
}
