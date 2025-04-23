import { ApiService } from './api';

export interface AnalysisHouse {
  id?: string;
  name: string;
  cnpj: string;
  website?: string;
  contactEmail?: string;
  contactPhone?: string;
  subscriptionType?: string;
}

export interface AnalysisHousePaginated {
  content: AnalysisHouse[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export class AnalysisHouseService {
  static async getAnalysisHouses(page: number = 0, size: number = 10): Promise<AnalysisHousePaginated> {
    return ApiService.get(`/analysis-houses?page=${page}&size=${size}`);
  }

  static async getAnalysisHouseById(id: string): Promise<AnalysisHouse> {
    return ApiService.get(`/analysis-houses/${id}`);
  }

  static async updateAnalysisHouse(id: string, data: Partial<AnalysisHouse>): Promise<AnalysisHouse> {
    return ApiService.put(`/analysis-houses/${id}`, data);
  }

  static async deleteAnalysisHouse(id: string): Promise<void> {
    return ApiService.delete(`/analysis-houses/${id}`);
  }

  static async createAnalysisHouse(data: Omit<AnalysisHouse, 'id'>): Promise<AnalysisHouse> {
    return ApiService.post('/analysis-houses', data);
  }
}