import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Layout } from './components/Layout';
import { NotasCorretagem } from './pages/importacoes/NotasCorretagem';
import { DashboardInvoiceProcessing } from './pages/importacoes/DashboardInvoiceProcessing';
import { CadastroCorretoras } from './pages/CadastroCorretoras';
import { CadastroAnalysisHouses } from './pages/CadastroAnalysisHouses';
import { Operacoes } from './pages/operacoes/Operacoes';
import { Toaster } from 'react-hot-toast';

function App() {
  return (
    <Router>
      <AuthProvider>
        <div className="min-h-screen bg-gray-50">
          {/* Provedor de toast para notificações */}
          <Toaster 
            position="top-right"
            toastOptions={{
              duration: 4000,
              style: {
                background: '#fff',
                color: '#333',
              },
              success: {
                style: {
                  border: '1px solid #10b981',
                },
              },
              error: {
                style: {
                  border: '1px solid #ef4444',
                },
              },
            }}
          />
          
          <Routes>
            {/* Rotas de autenticação */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            
            {/* Rotas protegidas com Layout compartilhado */}
            <Route path="/" element={<Layout />}>
              {/* Dashboard */}
              <Route index element={<Home />} />
              
              {/* Cadastros */}
              <Route path="cadastro">
                <Route path="corretoras" element={<CadastroCorretoras />} />
                <Route path="casas-analise" element={<CadastroAnalysisHouses />} />
              </Route>
              
              {/* Transações */}
              <Route path="transacoes">
                <Route path="movimentacoes" element={<div className="p-4">Movimentações</div>} />
              </Route>
              
              {/* Operações */}
              <Route path="operacoes">
                <Route path="operacoes" element={<Operacoes />} />
                <Route path="dados-diarios" element={<div className="p-4">Dados Diários</div>} />
              </Route>
              
              {/* Importações */}
              <Route path="importacoes">
                <Route path="notas-corretagem" element={<NotasCorretagem />} />
                <Route path="dashboard-processamento" element={<DashboardInvoiceProcessing />} />
              </Route>
              
              {/* Rotas de perfil */}
              <Route path="perfil" element={<div className="p-4">Meu Perfil</div>} />
              <Route path="configuracoes" element={<div className="p-4">Configurações</div>} />
            </Route>
            
            {/* Rota de fallback */}
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </div>
      </AuthProvider>
    </Router>
  );
}

export default App;