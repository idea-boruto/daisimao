import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';
import type { LoginResponse } from '../types';

export function useAuth() {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('user');
    return raw ? JSON.parse(raw) : null;
  });

  const isLoggedIn = !!token;

  useEffect(() => {
    if (token) {
      localStorage.setItem('token', token);
    } else {
      localStorage.removeItem('token');
    }
  }, [token]);

  useEffect(() => {
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      localStorage.removeItem('user');
    }
  }, [user]);

  const login = useCallback(async (username: string) => {
    const res = await api.post('/auth/login', { username }) as unknown as LoginResponse;
    const userData = {
      userId: res.userId,
      username: res.username,
      nickname: res.nickname,
    };
    localStorage.setItem('token', res.token);
    localStorage.setItem('user', JSON.stringify(userData));
    setToken(res.token);
    setUser(userData);
    return res;
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  const updateUser = useCallback((updates: Record<string, unknown>) => {
    setUser((prev: Record<string, unknown> | null) => prev ? { ...prev, ...updates } : null);
  }, []);

  return { token, user, isLoggedIn, login, logout, updateUser };
}
