import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import StatusBadge from '../components/StatusBadge';
import type { TaskStatus } from '../components/StatusBadge';
import { useAuth } from '../hooks/useAuth';
import api from '../services/api';
import type { Review, Task } from '../types';

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

  // review state
  const [hasReviewed, setHasReviewed] = useState(false);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewLoading, setReviewLoading] = useState(false);

  useEffect(() => {
    api.get(`/tasks/${id}`).then((res) => {
      setTask(res as unknown as Task);
    }).catch(() => {
      setError('任务不存在或已删除');
    }).finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    if (!task || task.status !== 5 || !isLoggedIn) return;
    api.get(`/reviews/task/${task.id}/check`).then((res) => {
      setHasReviewed(res as unknown as boolean);
    }).catch(() => {});
    api.get(`/reviews/task/${task.id}`).then((res) => {
      setReviews(res as unknown as Review[]);
    }).catch(() => {});
  }, [task?.status, id, isLoggedIn]);

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

  const isParticipant = isPublisher || isAcceptor;
  const canReview = isLoggedIn && isParticipant && task.status === 5 && !hasReviewed;

  const handleSubmitReview = async () => {
    setReviewLoading(true);
    try {
      await api.post('/reviews', { taskId: task.id, rating: reviewRating, comment: reviewComment || undefined });
      setHasReviewed(true);
      setShowReviewForm(false);
      const updated = await api.get(`/reviews/task/${task.id}`) as unknown as Review[];
      setReviews(updated);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setReviewLoading(false);
    }
  };

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

      {/* Review section */}
      {task.status === 5 && isLoggedIn && (
        <div className="bg-white rounded-lg p-4 mt-4">
          <h3 className="text-sm font-bold mb-3">评价</h3>

          {reviews.map((review) => (
            <div key={review.id} className="py-2 border-b border-gray-50 last:border-0">
              <div className="flex items-center gap-1 mb-1">
                <span className="text-xs text-gray-500">{review.reviewerNickname}</span>
                <span className="text-xs text-gray-400">评价</span>
                <span className="text-xs text-gray-500">{review.targetNickname}</span>
              </div>
              <div className="flex items-center gap-1 mb-1">
                {[1, 2, 3, 4, 5].map((star) => (
                  <span key={star} className={star <= review.rating ? 'text-warning' : 'text-gray-200'}>
                    ★
                  </span>
                ))}
              </div>
              {review.comment && <p className="text-xs text-gray-500">{review.comment}</p>}
            </div>
          ))}

          {canReview && !showReviewForm && (
            <button
              onClick={() => setShowReviewForm(true)}
              className="w-full py-2 mt-2 rounded-lg bg-warning text-white text-sm font-medium"
            >
              评价{isPublisher ? '接单人' : '发单人'}
            </button>
          )}

          {showReviewForm && (
            <div className="mt-3 p-3 bg-gray-50 rounded-lg">
              <p className="text-xs text-gray-500 mb-2">选择评分</p>
              <div className="flex gap-2 mb-3">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    onClick={() => setReviewRating(star)}
                    className={`text-2xl ${star <= reviewRating ? 'text-warning' : 'text-gray-300'}`}
                  >
                    ★
                  </button>
                ))}
              </div>
              <textarea
                value={reviewComment}
                onChange={(e) => setReviewComment(e.target.value)}
                placeholder="说点什么吧（选填）"
                maxLength={200}
                className="w-full px-3 py-2 rounded-lg border border-gray-200 text-sm resize-none focus:border-primary outline-none"
                rows={2}
              />
              <div className="flex gap-2 mt-2">
                <button
                  onClick={() => setShowReviewForm(false)}
                  className="flex-1 py-2 rounded-lg bg-gray-200 text-gray-600 text-sm"
                >
                  取消
                </button>
                <button
                  onClick={handleSubmitReview}
                  disabled={reviewLoading}
                  className="flex-1 py-2 rounded-lg bg-warning text-white text-sm disabled:opacity-50"
                >
                  {reviewLoading ? '提交中...' : '提交评价'}
                </button>
              </div>
            </div>
          )}

          {hasReviewed && !showReviewForm && reviews.length > 0 && (
            <p className="text-xs text-gray-400 text-center mt-2">你已评价</p>
          )}
        </div>
      )}
    </div>
  );
}
