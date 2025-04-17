import React from 'react';
import { Outlet, Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Sidebar } from './Sidebar';
import { Header } from './Header';

export function Layout() {
  const { token } = useAuth();

  // Redireciona para o login se não estiver autenticado
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      {/* Header no topo da página */}
      <Header />
      
      {/* Conteúdo principal - sidebar e main content lado a lado */}
      <div className="flex flex-1 pt-16"> {/* pt-16 para compensar a altura do header fixo */}
        <Sidebar />
        
        <main className="flex-1 p-4 md:p-8">
          <div className="max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
