export interface Task {
  id: number;
  publisherId: number;
  acceptorId?: number;
  type: 1 | 2 | 3 | 4 | 5;
  title: string;
  description?: string;
  reward: number;
  pickupLocation: string;
  deliveryLocation: string;
  status: 1 | 2 | 3 | 4 | 5 | 6 | 7;
  deadline?: string;
  acceptedAt?: string;
  completedAt?: string;
  createdAt: string;
}

export interface User {
  id: number;
  username?: string;
  nickname?: string;
  avatarUrl?: string;
  realName?: string;
  studentId?: string;
  campus?: string;
  creditScore: number;
  completedOrders: number;
  cancelledOrders: number;
  status: number;
  createdAt: string;
}

export interface Review {
  id: number;
  taskId: number;
  reviewerId: number;
  targetId: number;
  rating: number;
  tags?: string;
  comment?: string;
  createdAt: string;
}
