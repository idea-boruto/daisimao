import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic } from 'antd';
import { CheckCircleOutlined, ClockCircleOutlined, TeamOutlined, WarningOutlined } from '@ant-design/icons';
import api from '../services/api';

export default function Dashboard() {
  const [stats, setStats] = useState<any>({});

  useEffect(() => {
    api.get('/admin/stats').then(setStats).catch(() => {});
  }, []);

  return (
    <div>
      <h2>数据概览</h2>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="今日订单"
              value={stats.todayOrders || 0}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="已完成"
              value={stats.completedOrders || 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="活跃用户"
              value={stats.activeUsers || 0}
              prefix={<TeamOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="纠纷中"
              value={stats.disputeOrders || 0}
              prefix={<WarningOutlined />}
              valueStyle={{ color: stats.disputeOrders > 0 ? '#ff4d4f' : undefined }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
