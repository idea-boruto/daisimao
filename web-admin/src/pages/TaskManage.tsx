import { useEffect, useState } from 'react';
import { Table, Tag, Select, Space, Button, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import api from '../services/api';
import type { Task } from '../types';

const typeLabels: Record<number, string> = { 1: '快递代取', 2: '食堂带饭', 3: '代打印', 4: '代购', 5: '其他' };
const statusLabels: Record<number, { text: string; color: string }> = {
  1: { text: '待接单', color: 'blue' },
  2: { text: '已接单', color: 'cyan' },
  3: { text: '进行中', color: 'processing' },
  4: { text: '待确认', color: 'orange' },
  5: { text: '已完成', color: 'success' },
  6: { text: '已取消', color: 'default' },
  7: { text: '纠纷中', color: 'error' }
};

export default function TaskManage() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<number | undefined>();

  useEffect(() => {
    loadTasks();
  }, [statusFilter]);

  const loadTasks = () => {
    setLoading(true);
    const params = statusFilter ? { status: statusFilter } : {};
    api.get('/admin/tasks', { params })
      .then((res: any) => setTasks(res.items || []))
      .catch(() => message.error('加载失败'))
      .finally(() => setLoading(false));
  };

  const handleCancel = (id: number) => {
    api.put(`/admin/tasks/${id}/force-cancel`)
      .then(() => { message.success('已强制取消'); loadTasks(); })
      .catch(() => message.error('操作失败'));
  };

  const columns: ColumnsType<Task> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '类型', dataIndex: 'type', width: 100,
      render: (t: number) => typeLabels[t] || '-' },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    { title: '赏金', dataIndex: 'reward', width: 80,
      render: (v: number) => `¥${v}` },
    { title: '状态', dataIndex: 'status', width: 100,
      render: (s: number) => {
        const st = statusLabels[s] || { text: '-', color: 'default' };
        return <Tag color={st.color}>{st.text}</Tag>;
      } },
    { title: '发布时间', dataIndex: 'createdAt', width: 170 },
    {
      title: '操作', key: 'action', width: 100,
      render: (_, record) => (
        <Space>
          {record.status === 7 && (
            <Button size="small" danger onClick={() => handleCancel(record.id)}>
              强制取消
            </Button>
          )}
        </Space>
      )
    }
  ];

  return (
    <div>
      <h2>任务管理</h2>
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder="筛选状态"
          allowClear
          style={{ width: 150 }}
          onChange={(v) => setStatusFilter(v)}
          options={Object.entries(statusLabels).map(([k, v]) => ({
            value: Number(k), label: v.text
          }))}
        />
        <Button onClick={loadTasks}>刷新</Button>
      </Space>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={tasks}
        loading={loading}
        pagination={{ pageSize: 20 }}
      />
    </div>
  );
}
