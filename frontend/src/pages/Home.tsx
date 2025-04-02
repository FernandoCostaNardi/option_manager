import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { LogOut, Home as HomeIcon, BarChart2, Settings, User, CreditCard, PieChart, Activity, Briefcase } from 'lucide-react';

export function Home() {
  const { logout } = useAuth();

  return (
    <div className="min-h-screen flex bg-gray-50">
      {/* Sidebar Navigation */}
      <aside className="w-64 bg-gradient-to-b from-blue-900 to-indigo-900 text-white p-4 hidden md:block">
        <div className="mb-8">
          <h2 className="text-2xl font-bold mb-1">
            <span className="text-white">Oli</span>
            <span className="text-blue-300">system</span>
          </h2>
          <p className="text-blue-200 text-xs">Gestão de Opções Financeiras</p>
        </div>
        
        <nav className="space-y-1">
          <a href="#" className="flex items-center gap-3 p-3 rounded-lg bg-white/10 text-white">
            <HomeIcon className="h-5 w-5" />
            <span>Dashboard</span>
          </a>
          <a href="#" className="flex items-center gap-3 p-3 rounded-lg hover:bg-white/10 text-blue-200 hover:text-white transition-colors">
            <BarChart2 className="h-5 w-5" />
            <span>Análises</span>
          </a>
          <a href="#" className="flex items-center gap-3 p-3 rounded-lg hover:bg-white/10 text-blue-200 hover:text-white transition-colors">
            <Briefcase className="h-5 w-5" />
            <span>Portfólio</span>
          </a>
          <a href="#" className="flex items-center gap-3 p-3 rounded-lg hover:bg-white/10 text-blue-200 hover:text-white transition-colors">
            <Activity className="h-5 w-5" />
            <span>Mercado</span>
          </a>
          <a href="#" className="flex items-center gap-3 p-3 rounded-lg hover:bg-white/10 text-blue-200 hover:text-white transition-colors">
            <PieChart className="h-5 w-5" />
            <span>Relatórios</span>
          </a>
        </nav>
        
        <div className="absolute bottom-4 left-4 right-4">
          <div className="border-t border-blue-800 pt-4 space-y-1">
            <a href="#" className="flex items-center gap-3 p-3 rounded-lg hover:bg-white/10 text-blue-200 hover:text-white transition-colors">
              <User className="h-5 w-5" />
              <span>Meu Perfil</span>
            </a>
            <a href="#" className="flex items-center gap-3 p-3 rounded-lg hover:bg-white/10 text-blue-200 hover:text-white transition-colors">
              <Settings className="h-5 w-5" />
              <span>Configurações</span>
            </a>
            <button 
              onClick={logout}
              className="w-full flex items-center gap-3 p-3 rounded-lg hover:bg-red-700/20 text-red-300 hover:text-red-200 transition-colors text-left"
            >
              <LogOut className="h-5 w-5" />
              <span>Sair</span>
            </button>
          </div>
        </div>
      </aside>
      
      {/* Mobile header */}
      <div className="md:hidden fixed top-0 inset-x-0 bg-white border-b border-gray-200 z-10">
        <div className="flex items-center justify-between p-4">
          <h2 className="text-xl font-bold">
            <span className="text-blue-600">Oli</span>
            <span className="text-indigo-800">system</span>
          </h2>
          <button
            onClick={logout}
            className="flex items-center gap-1 text-gray-500 hover:text-red-500"
          >
            <LogOut className="h-5 w-5" />
            <span>Sair</span>
          </button>
        </div>
      </div>
      
      {/* Main Content */}
      <main className="flex-1 p-4 md:p-8 md:pt-8 pt-20">
        <div className="max-w-7xl mx-auto">
          <header className="mb-8 md:flex md:items-center md:justify-between">
            <div>
              <h1 className="text-2xl md:text-3xl font-bold text-gray-800">Dashboard</h1>
              <p className="text-gray-500 mt-1">Bem-vindo ao seu painel de controle</p>
            </div>
            
            <div className="mt-4 md:mt-0 flex space-x-3">
              <button className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 flex items-center gap-2">
                <BarChart2 className="h-4 w-4" />
                <span>Relatórios</span>
              </button>
              <button className="px-4 py-2 bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-lg hover:from-blue-700 hover:to-indigo-800 shadow-sm flex items-center gap-2">
                <CreditCard className="h-4 w-4" />
                <span>Nova Operação</span>
              </button>
            </div>
          </header>
          
          {/* Dashboard Content */}
          <div className="grid md:grid-cols-3 gap-6 mb-8">
            {/* Card 1 */}
            <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
              <div className="flex justify-between items-start">
                <div>
                  <p className="text-sm text-gray-500 mb-1">Total em Opções</p>
                  <h3 className="text-2xl font-bold text-gray-800">R$ 24.875,00</h3>
                  <p className="text-sm text-green-600 mt-1 flex items-center">
                    <span>↑ 12.5%</span>
                    <span className="text-gray-500 ml-1">desde o último mês</span>
                  </p>
                </div>
                <div className="rounded-full bg-blue-50 p-3">
                  <Briefcase className="h-6 w-6 text-blue-600" />
                </div>
              </div>
            </div>
            
            {/* Card 2 */}
            <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
              <div className="flex justify-between items-start">
                <div>
                  <p className="text-sm text-gray-500 mb-1">Operações Ativas</p>
                  <h3 className="text-2xl font-bold text-gray-800">8</h3>
                  <p className="text-sm text-red-600 mt-1 flex items-center">
                    <span>↓ 2</span>
                    <span className="text-gray-500 ml-1">desde ontem</span>
                  </p>
                </div>
                <div className="rounded-full bg-indigo-50 p-3">
                  <Activity className="h-6 w-6 text-indigo-600" />
                </div>
              </div>
            </div>
            
            {/* Card 3 */}
            <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
              <div className="flex justify-between items-start">
                <div>
                  <p className="text-sm text-gray-500 mb-1">Rendimento Médio</p>
                  <h3 className="text-2xl font-bold text-gray-800">8.2%</h3>
                  <p className="text-sm text-green-600 mt-1 flex items-center">
                    <span>↑ 1.2%</span>
                    <span className="text-gray-500 ml-1">desde a semana passada</span>
                  </p>
                </div>
                <div className="rounded-full bg-green-50 p-3">
                  <PieChart className="h-6 w-6 text-green-600" />
                </div>
              </div>
            </div>
          </div>
          
          {/* Main Table */}
          <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-100">
            <div className="px-6 py-4 border-b border-gray-100">
              <h2 className="font-semibold text-gray-800">Operações Recentes</h2>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Ativo</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tipo</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Valor</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Strike</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Vencimento</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  <tr>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">PETR4</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">CALL</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">R$ 5.200,00</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">32,50</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">15/05/2025</td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">Ativa</span>
                    </td>
                  </tr>
                  <tr>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">VALE3</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">PUT</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">R$ 4.800,00</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">65,00</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">20/06/2025</td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">Ativa</span>
                    </td>
                  </tr>
                  <tr>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">ITUB4</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">CALL</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">R$ 3.500,00</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">28,75</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">10/04/2025</td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800">Pendente</span>
                    </td>
                  </tr>
                  <tr>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">BBDC4</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">PUT</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">R$ 2.900,00</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">18,00</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">05/05/2025</td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">Expirada</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div className="px-6 py-3 flex items-center justify-between border-t border-gray-200">
              <div className="text-sm text-gray-500">
                Mostrando 4 de 20 resultados
              </div>
              <div className="flex gap-2">
                <button className="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50">Anterior</button>
                <button className="px-3 py-1 border border-gray-300 rounded-md text-sm bg-blue-50 text-blue-600 border-blue-200">1</button>
                <button className="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50">2</button>
                <button className="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50">3</button>
                <button className="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50">Próximo</button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
