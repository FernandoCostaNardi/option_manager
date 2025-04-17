import { ApiService } from './api';

export interface Brokerage {
  id?: string;
  name: string;
  cnpj: string;
  account?: string;
  agency?: string;
}

export interface BrokeragePaginated {
  content: Brokerage[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export class BrokerageService {
  static async getBrokerages(page: number = 0, size: number = 10): Promise<BrokeragePaginated> {
    return ApiService.get(`/brokerages?page=${page}&size=${size}`);
  }

  static async getBrokerageById(id: string): Promise<Brokerage> {
    return ApiService.get(`/brokerages/${id}`);
}

  static async updateBrokerage(id: string, data: Partial<Brokerage>): Promise<Brokerage> {
    return ApiService.put(`/brokerages/${id}`, data);
  }

  static async deleteBrokerage(id: string): Promise<void> {
    return ApiService.delete(`/brokerages/${id}`);
  }

  static async createBrokerage(data: Omit<Brokerage, 'id'>): Promise<Brokerage> {
    return ApiService.post('/brokerages', data);
  }
}