import React, { useState } from 'react';
import { 
  Home as HomeIcon, 
  FileText, 
  FolderOpen, 
  BarChart2, 
  Upload,
  ChevronLeft, 
  ChevronRight,
  ChevronDown,
  Building2,
  Presentation,
  CreditCard,
  BarChart,
  Calendar,
  FileSpreadsheet,
  Activity
} from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';

export function Sidebar() {
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [openSubmenu, setOpenSubmenu] = useState<string | null>(null);
  
  // Função para verificar se o link está ativo
  const isActive = (path: string) => location.pathname === path;

  // Função para alternar o estado de colapso do menu
  const toggleSidebar = () => {
    setCollapsed(!collapsed);
  };

  // Função para lidar com o clique nos itens do menu principal
  const handleMenuItemClick = (menuId: string) => {
    if (collapsed) {
      // Se o menu estiver colapsado, expande ele primeiro
      setCollapsed(false);
      setOpenSubmenu(menuId);
    } else {
      // Se já estiver expandido, alterna o submenu
      setOpenSubmenu(openSubmenu === menuId ? null : menuId);
    }
  };

  // Função para verificar se um submenu deve estar ativo
  const isSubmenuActive = (paths: string[]) => {
    return paths.some(path => location.pathname.startsWith(path));
  };

  return (
    <>
      {/* Desktop Sidebar */}
      <aside 
        className={`${collapsed ? 'w-20' : 'w-64'} bg-gradient-to-b from-purple-800 to-indigo-900 text-white p-4 hidden md:block h-[calc(100vh-4rem)] transition-all duration-300 ease-in-out relative`}
      >
        {/* Botão para expandir/colapsar */}
        <button 
          onClick={toggleSidebar}
          className="absolute -right-3 top-5 bg-purple-600 rounded-full p-1 text-white shadow-md hover:bg-purple-700 transition-colors"
        >
          {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
        </button>
        
        <nav className="space-y-1 mt-8">
          {/* Dashboard - Sem submenu */}
          <Link to="/" className={`flex items-center ${collapsed ? 'justify-center' : 'gap-3'} p-3 rounded-lg ${isActive('/') ? 'bg-white/10 text-white' : 'hover:bg-white/10 text-purple-200 hover:text-white transition-colors'}`}>
            <HomeIcon className="h-6 w-6 min-w-6" />
            {!collapsed && <span>Dashboard</span>}
          </Link>
          
          {/* Cadastro - Com submenu */}
          <div className="space-y-1">
            <button 
              onClick={() => handleMenuItemClick('cadastro')}
              className={`w-full flex items-center ${collapsed ? 'justify-center' : 'justify-between'} p-3 rounded-lg ${isSubmenuActive(['/cadastro']) ? 'bg-white/10 text-white' : 'hover:bg-white/10 text-purple-200 hover:text-white transition-colors'}`}
            >
              <div className={`flex items-center ${collapsed ? 'justify-center' : 'gap-3'}`}>
                <FolderOpen className="h-6 w-6 min-w-6" />
                {!collapsed && <span>Cadastro</span>}
              </div>
              {!collapsed && <ChevronDown className={`h-4 w-4 transition-transform ${openSubmenu === 'cadastro' ? 'rotate-180' : ''}`} />}
            </button>
            
            {!collapsed && openSubmenu === 'cadastro' && (
              <div className="ml-12 space-y-1 mt-1">
                <Link to="/cadastro/corretoras" className={`block p-2 rounded-md ${isActive('/cadastro/corretoras') ? 'bg-white/10 text-white' : 'text-purple-200 hover:text-white hover:bg-white/5'}`}>
                  <div className="flex items-center gap-2">
                    <Building2 className="h-4 w-4" />
                    <span>Corretoras</span>
                  </div>
                </Link>
                <Link to="/cadastro/casas-analise" className={`block p-2 rounded-md ${isActive('/cadastro/casas-analise') ? 'bg-white/10 text-white' : 'text-purple-200 hover:text-white hover:bg-white/5'}`}>
                  <div className="flex items-center gap-2">
                    <Presentation className="h-4 w-4" />
                    <span>Casas de Análise</span>
                  </div>
                </Link>
              </div>
            )}
          </div>
          
          {/* Transações - Com submenu */}
          <div className="space-y-1">
            <button 
              onClick={() => handleMenuItemClick('transacoes')}
              className={`w-full flex items-center ${collapsed ? 'justify-center' : 'justify-between'} p-3 rounded-lg ${isSubmenuActive(['/transacoes']) ? 'bg-white/10 text-white' : 'hover:bg-white/10 text-purple-200 hover:text-white transition-colors'}`}
            >
              <div className={`flex items-center ${collapsed ? 'justify-center' : 'gap-3'}`}>
                <CreditCard className="h-6 w-6 min-w-6" />
                {!collapsed && <span>Transações</span>}
              </div>
              {!collapsed && <ChevronDown className={`h-4 w-4 transition-transform ${openSubmenu === 'transacoes' ? 'rotate-180' : ''}`} />}
            </button>
            
            {!collapsed && openSubmenu === 'transacoes' && (
              <div className="ml-12 space-y-1 mt-1">
                <Link to="/transacoes/movimentacoes" className={`block p-2 rounded-md ${isActive('/transacoes/movimentacoes') ? 'bg-white/10 text-white' : 'text-purple-200 hover:text-white hover:bg-white/5'}`}>
                  <div className="flex items-center gap-2">
                    <FileText className="h-4 w-4" />
                    <span>Movimentações</span>
                  </div>
                </Link>
              </div>
            )}
          </div>
          
          {/* Operações - Com submenu */}
          <div className="space-y-1">
            <button 
              onClick={() => handleMenuItemClick('operacoes')}
              className={`w-full flex items-center ${collapsed ? 'justify-center' : 'justify-between'} p-3 rounded-lg ${isSubmenuActive(['/operacoes']) ? 'bg-white/10 text-white' : 'hover:bg-white/10 text-purple-200 hover:text-white transition-colors'}`}
            >
              <div className={`flex items-center ${collapsed ? 'justify-center' : 'gap-3'}`}>
                <BarChart className="h-6 w-6 min-w-6" />
                {!collapsed && <span>Operações</span>}
              </div>
              {!collapsed && <ChevronDown className={`h-4 w-4 transition-transform ${openSubmenu === 'operacoes' ? 'rotate-180' : ''}`} />}
            </button>
            
            {!collapsed && openSubmenu === 'operacoes' && (
              <div className="ml-12 space-y-1 mt-1">
                <Link to="/operacoes/operacoes" className={`block p-2 rounded-md ${isActive('/operacoes/operacoes') ? 'bg-white/10 text-white' : 'text-purple-200 hover:text-white hover:bg-white/5'}`}>
                  <div className="flex items-center gap-2">
                    <BarChart2 className="h-4 w-4" />
                    <span>Operações</span>
                  </div>
                </Link>
                <Link to="/operacoes/dados-diarios" className={`block p-2 rounded-md ${isActive('/operacoes/dados-diarios') ? 'bg-white/10 text-white' : 'text-purple-200 hover:text-white hover:bg-white/5'}`}>
                  <div className="flex items-center gap-2">
                    <Calendar className="h-4 w-4" />
                    <span>Dados Diários</span>
                  </div>
                </Link>
              </div>
            )}
          </div>
          
          {/* Importações - Com submenu */}
          <div className="space-y-1">
            <button 
              onClick={() => handleMenuItemClick('importacoes')}
              className={`w-full flex items-center ${collapsed ? 'justify-center' : 'justify-between'} p-3 rounded-lg ${isSubmenuActive(['/importacoes']) ? 'bg-white/10 text-white' : 'hover:bg-white/10 text-purple-200 hover:text-white transition-colors'}`}
            >
              <div className={`flex items-center ${collapsed ? 'justify-center' : 'gap-3'}`}>
                <Upload className="h-6 w-6 min-w-6" />
                {!collapsed && <span>Importações</span>}
              </div>
              {!collapsed && <ChevronDown className={`h-4 w-4 transition-transform ${openSubmenu === 'importacoes' ? 'rotate-180' : ''}`} />}
            </button>
            
            {!collapsed && openSubmenu === 'importacoes' && (
              <div className="ml-12 space-y-1 mt-1">
                <Link to="/importacoes/notas-corretagem" className={`block p-2 rounded-md ${isActive('/importacoes/notas-corretagem') ? 'bg-white/10 text-white' : 'text-purple-200 hover:text-white hover:bg-white/5'}`}>
                  <div className="flex items-center gap-2">
                    <FileSpreadsheet className="h-4 w-4" />
                    <span>Notas de Corretagem</span>
                  </div>
                </Link>
                <Link to="/importacoes/dashboard-processamento" className={`block p-2 rounded-md ${isActive('/importacoes/dashboard-processamento') ? 'bg-white/10 text-white' : 'text-purple-200 hover:text-white hover:bg-white/5'}`}>
                  <div className="flex items-center gap-2">
                    <Activity className="h-4 w-4" />
                    <span>Dashboard Processamento</span>
                  </div>
                </Link>
              </div>
            )}
          </div>
        </nav>
      </aside>
      
      {/* Mobile sidebar será controlado pelo Header */}
    </>
  );
}