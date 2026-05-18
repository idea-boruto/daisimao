import { useEffect, useState } from 'react';
import { Table, Tag, Button, Input, Space, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import api from '../services/api';
import type { User } from '../types';

export default function UserManage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = () => {
    setLoading(true);
    const params = search ? { keyword: search } : {};
    api.get('/admin/users', { params })
      .then((res: any) => setUsers(res.items || []))
      .catch(() => message.error('加载失败'))
      .finally(() => setLoading(false));
  };

  const toggleFreeze = (user: User) => {
    const action = user.status === 1 ? 'freeze' : 'unfreeze';
    api.put(`/admin/users/${user.id}/${action}`)
      .then(() => { message.success('操作成功'); loadUsers(); })
      .catch(() => message.error('操作失败'));
  };

  const columns: ColumnsType<User> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '昵称', dataIndex: 'nickname', width: 120 },
    { title: '学号', dataIndex: 'studentId', width: 120 },
    { title: '校区', dataIndex: 'campus', width: 100 },
    {
      title: '信用分', dataIndex: 'creditScore', width: 100,
      render: (v: number) => {
        const color = v >= 80 ? 'green' : v >= 60 ? 'orange' : 'red';
        return <Tag color={color}>{v}</Tag>;
      }
    },
    { title: '完成单数', dataIndex: 'completedOrders', width: 100 },
    { title: '取消单数', dataIndex: 'cancelledOrders', width: 100 },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (s: number) => (
        <Tag color={s === 1 ? 'green' : 'red'}>
          {s === 1 ? '正常' : '已冻结'}
        </Tag>
      )
    },
    { title: '注册时间', dataIndex: 'createdAt', width: 170 },
    {
      title: '操作', key: 'action', width: 100,
      render: (_, record) => (
        <Button
          size="small"
          danger={record.status === 1}
          onClick={() => toggleFreeze(record)}
        >
          {record.status === 1 ? '冻结' : '解冻'}
        </Button>
      )
    }
  ];

  return (
    <div>
      <h2>用户管理</h2>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="搜索昵称/学号"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onSearch={loadUsers}
          style={{ width: 250 }}
        />
        <Button onClick={loadUsers}>刷新</Button>
      </Space>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={users}
        loading={loading}
        pagination={{ pageSize: 20 }}
      />
    </div>
  );
}
