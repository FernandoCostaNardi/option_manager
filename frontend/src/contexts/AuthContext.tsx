import React from 'react';
import { createContext, useContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';

interface AuthContextType {
  token: string | null;
  login: (token: string) => void;
  logout: () => void;
  checkTokenExpiration: () => boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

interface TokenPayload {
  exp: number;
  [key: string]: any;
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const navigate = useNavigate();

  const login = (newToken: string) => {
    setToken(newToken);
    localStorage.setItem('token', newToken);
  };

  const logout = () => {
    setToken(null);
    localStorage.removeItem('token');
    navigate('/login');
  };

  // Verifica se o token está expirado
  const checkTokenExpiration = (): boolean => {
    if (!token) return true;
    
    try {
      const decoded = jwtDecode<TokenPayload>(token);
      const currentTime = Date.now() / 1000;
      
      if (decoded.exp < currentTime) {
        // Token expirado
        logout();
        return true;
      }
      return false;
    } catch (error) {
      console.error('Erro ao decodificar token:', error);
      logout();
      return true;
    }
  };

  // Verifica a expiração do token quando o componente é montado
  useEffect(() => {
    if (token) {
      checkTokenExpiration();
    }
  }, [token]);

  return (
    <AuthContext.Provider value={{ token, login, logout, checkTokenExpiration }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}