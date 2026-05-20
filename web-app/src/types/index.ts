export interface User {
  userId: number;
  username: string;
  nickname: string;
  creditScore: number;
  campus?: string;
  avatarUrl?: string;
}

export interface Task {
  id: number;
  type: number;
  title: string;
  description?: string;
  reward: number;
  pickupLocation?: string;
  deliveryLocation?: string;
  status: number;
  publisherId: number;
  publisherNickname?: string;
  publisherCreditScore?: number;
  acceptorId?: number;
  acceptorNickname?: string;
  deadline?: string;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  username: string;
  nickname: string;
  isNewUser: boolean;
}

export interface Review {
  id: number;
  taskId: number;
  reviewerId: number;
  reviewerNickname: string;
  targetId: number;
  targetNickname: string;
  rating: number;
  tags?: string;
  comment?: string;
  createdAt: string;
}

export interface Notification {
  id: number;
  type: string;
  title: string;
  content: string;
  isRead: boolean;
  relatedTaskId?: number;
  createdAt: string;
}

export interface PageData<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}
