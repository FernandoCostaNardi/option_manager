import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiService } from '../services/api';

interface AuthGuardProps {
  children: React.ReactNode;
}

export function AuthGuard({ children }: AuthGuardProps) {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const checkAuth = async () => {
      try {
        // Verificar se há token no localStorage
        const hasToken = ApiService.isAuthenticated();
        
        if (!hasToken) {
          console.log('❌ Nenhum token encontrado. Redirecionando para login...');
          navigate('/login');
          return;
        }

        // Tentar verificar o status da autenticação com o servidor
        try {
          const isAuth = await ApiService.checkAuthStatus();
          setIsAuthenticated(isAuth);
          
          if (!isAuth) {
            console.log('❌ Token inválido ou expirado. Redirecionando para login...');
            localStorage.removeItem('token');
            navigate('/login');
          }
        } catch (error) {
          console.log('⚠️ Não foi possível verificar status com servidor, mas token existe');
          // Se não conseguir verificar com servidor mas tem token, permite acesso
          setIsAuthenticated(true);
        }
      } catch (error) {
        console.error('Erro ao verificar autenticação:', error);
        navigate('/login');
      }
    };

    checkAuth();
  }, [navigate]);

  // Mostrar loading enquanto verifica autenticação
  if (isAuthenticated === null) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="h-8 w-8 text-purple-600 mx-auto mb-4 animate-spin">⏳</div>
          <p className="text-gray-600">Verificando autenticação...</p>
        </div>
      </div>
    );
  }

  // Se não autenticado, não renderizar nada (já redirecionou)
  if (!isAuthenticated) {
    return null;
  }

  // Se autenticado, renderizar children
  return <>{children}</>;
} 