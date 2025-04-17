import React, { useState } from 'react';
import { LogOut, User, Settings, Menu } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { Link } from 'react-router-dom';

export function Header() {
  const { logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [mobileDropdownOpen, setMobileDropdownOpen] = useState(false);

  return (
    <header className="bg-white border-b border-gray-200 fixed w-full z-20 h-16">
      <div className="flex items-center justify-between px-4 md:px-6 h-full">
        {/* Logo */}
        <div className="flex items-center">
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="mr-3 text-gray-700 md:hidden"
          >
            <Menu className="h-6 w-6" />
          </button>
          <Link to="/" className="flex items-center">
            <div className="mr-2 bg-blue-900 text-white p-2 rounded">
              <span className="text-xl font-bold">OM</span>
            </div>
            <div>
              <h2 className="text-xl font-bold leading-none">
                <span className="text-blue-600">Option</span>
                <span className="text-indigo-800">Manager</span>
              </h2>
              <p className="text-gray-500 text-xs">Gestão de Opções Financeiras</p>
            </div>
          </Link>
        </div>
        
        {/* Versão Desktop - Links diretamente visíveis */}
        <div className="hidden md:flex items-center gap-6">
          <Link to="/perfil" className="flex items-center gap-2 text-gray-600 hover:text-gray-900">
            <User className="h-5 w-5" />
            <span>Meu Perfil</span>
          </Link>
          <Link to="/configuracoes" className="flex items-center gap-2 text-gray-600 hover:text-gray-900">
            <Settings className="h-5 w-5" />
            <span>Configurações</span>
          </Link>
          <button 
            onClick={logout}
            className="flex items-center gap-2 text-red-600 hover:text-red-800"
          >
            <LogOut className="h-5 w-5" />
            <span>Sair</span>
          </button>
        </div>
        
        {/* Versão Mobile - Menu dropdown */}
        <div className="md:hidden relative">
          <button
            onClick={() => setMobileDropdownOpen(!mobileDropdownOpen)}
            className="flex items-center gap-2 text-gray-700"
          >
            <div className="bg-blue-600 rounded-full w-8 h-8 flex items-center justify-center text-white">
              <span className="text-sm font-medium">US</span>
            </div>
          </button>
          
          {mobileDropdownOpen && (
            <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg py-1 border border-gray-200 z-30">
              <Link 
                to="/perfil"
                className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                onClick={() => setMobileDropdownOpen(false)}
              >
                <User className="h-4 w-4" />
                <span>Meu Perfil</span>
              </Link>
              <Link 
                to="/configuracoes"
                className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                onClick={() => setMobileDropdownOpen(false)}
              >
                <Settings className="h-4 w-4" />
                <span>Configurações</span>
              </Link>
              <div className="border-t border-gray-100 my-1"></div>
              <button 
                onClick={() => {
                  setMobileDropdownOpen(false);
                  logout();
                }}
                className="flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-gray-100 w-full text-left"
              >
                <LogOut className="h-4 w-4" />
                <span>Sair</span>
              </button>
            </div>
          )}
        </div>
      </div>
      
      {/* Menu móvel */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-blue-900 text-white absolute top-16 left-0 w-full z-10">
          {/* Mobile menu content será gerenciado pela Sidebar */}
        </div>
      )}
    </header>
  );
}
