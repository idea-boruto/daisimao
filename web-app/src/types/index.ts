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

export interface PageData<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}
