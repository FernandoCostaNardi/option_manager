import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';

// URL base da API
export const API_BASE_URL = 'http://localhost:8080/api';

// Criar uma instância do axios com configurações padrão
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor para adicionar o token em todas as requisições
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor para tratar erros de resposta
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Verificar se é erro de autenticação (401)
    if (error.response && error.response.status === 401) {
      console.log('Token expirado ou inválido. Redirecionando para login...');
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

// Classe de serviço API para métodos comuns
export class ApiService {
  // Método genérico para fazer requisições
  static async request(endpoint: string, method: string = 'GET', data?: any, headers: HeadersInit = {}) {
    // Constrói a URL completa
    const url = `${API_BASE_URL}${endpoint}`;
    
    // Define os headers padrão
    const defaultHeaders: HeadersInit = {
      'Content-Type': 'application/json',
      // Adicionar token de autenticação se disponível
      ...(localStorage.getItem('token') ? { 
        'Authorization': `Bearer ${localStorage.getItem('token')}` 
      } : {}),
      ...headers
    };
    
    // Constrói as opções da requisição
    const options: RequestInit = {
      method,
      headers: defaultHeaders,
    };
    
    // Adiciona body se necessário
    if (data) {
      if (data instanceof FormData) {
        // Se for FormData, remove o Content-Type para o navegador definir corretamente com boundary
        delete (options.headers as any)['Content-Type'];
        options.body = data;
      } else {
        options.body = JSON.stringify(data);
      }
    }
    
    try {
      console.log(`Fazendo requisição para: ${url}`);
      const response = await fetch(url, options);
      
      // Verifica se a resposta foi bem-sucedida
      if (!response.ok) {
        // Verifica se é erro de autenticação (401)
        if (response.status === 401) {
          console.log('Token expirado ou inválido. Redirecionando para login...');
          localStorage.removeItem('token');
          window.location.href = '/login';
          throw new Error('Sessão expirada. Por favor, faça login novamente.');
        }
        
        // Tenta extrair a mensagem de erro da resposta
        try {
          const errorData = await response.json();
          throw new Error(errorData.message || `Erro: ${response.status} ${response.statusText}`);
        } catch (e) {
          throw new Error(`Erro: ${response.status} ${response.statusText}`);
        }
      }
      
      // Se for NoContent (204), retorna true em vez de tentar parser JSON
      if (response.status === 204) {
        return true;
      }
      
      // Tenta retornar o corpo da resposta como JSON
      return await response.json();
    } catch (error) {
      console.error('API request error:', error);
      
      // Verifica se é um erro de rede (servidor offline)
      if (error instanceof TypeError && error.message.includes('Failed to fetch')) {
        throw new Error('Não foi possível conectar ao servidor. Verifique se o backend está em execução.');
      }
      
      throw error;
    }
  }
  
  // Métodos auxiliares para cada tipo de requisição
  static async get(endpoint: string, headers: HeadersInit = {}) {
    return this.request(endpoint, 'GET', undefined, headers);
  }
  
  static async post(endpoint: string, data: any, headers: HeadersInit = {}) {
    return this.request(endpoint, 'POST', data, headers);
  }
  
  static async put(endpoint: string, data: any, headers: HeadersInit = {}) {
    return this.request(endpoint, 'PUT', data, headers);
  }
  
  static async delete(endpoint: string, headers: HeadersInit = {}) {
    return this.request(endpoint, 'DELETE', undefined, headers);
  }
}
