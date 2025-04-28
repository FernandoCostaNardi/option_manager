import React, { useState, useEffect } from 'react';
import { BarChart2, Plus, Edit, Trash, CheckSquare, Eye, Loader2, AlertCircle } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { NovaOperacaoModal } from '../../components/NovaOperacaoModal';
import { FinalizarOperacaoModal } from '../../components/FinalizarOperacaoModal';


// Interfaces para os tipos de dados
interface OperacaoAtiva {
  id: string;
  dataEntrada: string;
  casaAnalise: string;
  corretora: string;
  opcao: string;
  logoEmpresa: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

interface OperacaoFinalizada extends OperacaoAtiva {
  dataSaida: string;
  status: 'Vencedor' | 'Perdedor';
  valorUnitarioSaida: number;
  valorTotalSaida: number;
  valorLucroPrejuizo: number;
  percentualLucroPrejuizo: number;
}

// Serviço mockado para operações (será substituído pela implementação real)
const OperacoesService = {
  obterOperacoesAtivas: async (): Promise<OperacaoAtiva[]> => {
    // Simulando uma chamada de API
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve([
          {
            id: '1',
            dataEntrada: '2023-05-15',
            casaAnalise: 'Empiricus',
            corretora: 'XP Investimentos',
            opcao: 'PETR4',
            logoEmpresa: 'https://logodownload.org/wp-content/uploads/2014/07/petrobras-logo-1-1.png',
            quantidade: 100,
            valorUnitario: 32.50,
            valorTotal: 3250.00
          },
          {
            id: '2',
            dataEntrada: '2023-06-20',
            casaAnalise: 'Suno Research',
            corretora: 'Clear',
            opcao: 'VALE3',
            logoEmpresa: 'https://logodownload.org/wp-content/uploads/2017/01/vale-logo-0.png',
            quantidade: 50,
            valorUnitario: 68.75,
            valorTotal: 3437.50
          },
          {
            id: '3',
            dataEntrada: '2023-07-10',
            casaAnalise: 'Nord Research',
            corretora: 'Rico',
            opcao: 'ITUB4',
            logoEmpresa: 'https://logodownload.org/wp-content/uploads/2016/09/itau-logo-1-1.png',
            quantidade: 200,
            valorUnitario: 28.30,
            valorTotal: 5660.00
          }
        ]);
      }, 800);
    });
  },
  
  obterOperacoesFinalizadas: async (): Promise<OperacaoFinalizada[]> => {
    // Simulando uma chamada de API
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve([
          {
            id: '4',
            dataEntrada: '2023-03-10',
            dataSaida: '2023-04-15',
            status: 'Vencedor',
            casaAnalise: 'Empiricus',
            corretora: 'XP Investimentos',
            opcao: 'BBDC4',
            logoEmpresa: 'https://logodownload.org/wp-content/uploads/2014/05/bradesco-logo-1.png',
            quantidade: 150,
            valorUnitario: 18.40,
            valorTotal: 2760.00,
            valorUnitarioSaida: 21.35,
            valorTotalSaida: 3202.50,
            valorLucroPrejuizo: 442.50,
            percentualLucroPrejuizo: 16.03
          },
          {
            id: '5',
            dataEntrada: '2023-02-05',
            dataSaida: '2023-03-20',
            status: 'Perdedor',
            casaAnalise: 'Suno Research',
            corretora: 'Clear',
            opcao: 'MGLU3',
            logoEmpresa: 'https://logodownload.org/wp-content/uploads/2017/11/magazine-luiza-logo-1.png',
            quantidade: 300,
            valorUnitario: 5.75,
            valorTotal: 1725.00,
            valorUnitarioSaida: 4.20,
            valorTotalSaida: 1260.00,
            valorLucroPrejuizo: -465.00,
            percentualLucroPrejuizo: -26.96
          }
        ]);
      }, 800);
    });
  }
};

export function Operacoes() {
  // Estados para as operações
  const [operacoesAtivas, setOperacoesAtivas] = useState<OperacaoAtiva[]>([]);
  const [operacoesFinalizadas, setOperacoesFinalizadas] = useState<OperacaoFinalizada[]>([]);
  
  // Estados para controle de carregamento e erros
  const [loadingAtivas, setLoadingAtivas] = useState(false);
  const [loadingFinalizadas, setLoadingFinalizadas] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Estado para controlar a aba ativa
  const [activeTab, setActiveTab] = useState("ativas");
  
  // Estado para controlar o modal de nova operação
  const [modalNovaOperacaoAberto, setModalNovaOperacaoAberto] = useState(false);

  // Estado para controlar o modal de finalizar operação
  const [modalFinalizarOperacaoAberto, setModalFinalizarOperacaoAberto] = useState(false);
  const [operacaoParaFinalizar, setOperacaoParaFinalizar] = useState<string | null>(null);

  // Carregar as operações ao montar o componente
  useEffect(() => {
    carregarOperacoesAtivas();
    carregarOperacoesFinalizadas();
  }, []);
  
  // Função para carregar operações ativas
  const carregarOperacoesAtivas = async () => {
    setLoadingAtivas(true);
    setError(null);
    
    try {
      const response = await OperacoesService.obterOperacoesAtivas();
      setOperacoesAtivas(response);
    } catch (error) {
      console.error('Erro ao carregar operações ativas:', error);
      setError('Não foi possível carregar as operações ativas. Tente novamente mais tarde.');
    } finally {
      setLoadingAtivas(false);
    }
  };
  
  // Função para carregar operações finalizadas
  const carregarOperacoesFinalizadas = async () => {
    setLoadingFinalizadas(true);
    setError(null);
    
    try {
      const response = await OperacoesService.obterOperacoesFinalizadas();
      setOperacoesFinalizadas(response);
    } catch (error) {
      console.error('Erro ao carregar operações finalizadas:', error);
      setError('Não foi possível carregar as operações finalizadas. Tente novamente mais tarde.');
    } finally {
      setLoadingFinalizadas(false);
    }
  };

  // Função para formatar valores em reais
  const formatarMoeda = (valor: number) => {
    return valor.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    });
  };
  
  // Função para formatar a data
  const formatarData = (dataIso: string) => {
    const data = new Date(dataIso);
    return data.toLocaleDateString('pt-BR');
  };

  // Funções para ações nas operações ativas
  const handleEditar = (id: string) => {
    alert(`Editando operação ${id}`);
  };

  const handleRemover = (id: string) => {
    if (window.confirm('Tem certeza que deseja remover esta operação?')) {
      alert(`Removendo operação ${id}`);
    }
  };

  const handleFinalizar = (id: string) => {
    setOperacaoParaFinalizar(id);
    setModalFinalizarOperacaoAberto(true);
  };

  // Função para visualizar operação finalizada
  const handleVisualizar = (id: string) => {
    alert(`Visualizando detalhes da operação ${id}`);
  };
  
  // Handlers para o modal de nova operação
  const abrirModalNovaOperacao = () => {
    setModalNovaOperacaoAberto(true);
  };

  const fecharModalNovaOperacao = () => {
    setModalNovaOperacaoAberto(false);
  };
  
  const handleNovaOperacaoSucesso = () => {
    // Recarregar as operações ativas após adicionar uma nova
    carregarOperacoesAtivas();
    // Mudar para a aba de operações ativas
    setActiveTab("ativas");
    // Fechar o modal
    setModalNovaOperacaoAberto(false);
  };
  
  // Funções para o modal de finalizar operação
  const fecharModalFinalizarOperacao = () => {
    setModalFinalizarOperacaoAberto(false);
    setOperacaoParaFinalizar(null);
  };
  
  const handleFinalizarOperacaoSucesso = () => {
    // Recarregar as operações após finalizar
    carregarOperacoesAtivas();
    carregarOperacoesFinalizadas();
    // Fechar o modal
    fecharModalFinalizarOperacao();
  };

  return (
    <div className="container mx-auto py-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <BarChart2 className="w-7 h-7 text-purple-600" /> Operações
        </h1>
        <button
          className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
          onClick={abrirModalNovaOperacao}
        >
          <Plus className="w-5 h-5" /> Nova Operação
        </button>
      </div>
      
      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <TabsList className="mb-4">
          <TabsTrigger value="ativas" className="px-4 py-2">Operações Ativas</TabsTrigger>
          <TabsTrigger value="finalizadas" className="px-4 py-2">Operações Finalizadas</TabsTrigger>
        </TabsList>
        
        <TabsContent value="ativas">
          {loadingAtivas ? (
            <div className="flex justify-center items-center py-10">
              <Loader2 className="animate-spin w-8 h-8 text-purple-600" />
              <span className="ml-3 text-gray-600">Carregando operações ativas...</span>
            </div>
          ) : error ? (
            <div className="flex items-center gap-2 p-4 bg-red-100 text-red-700 rounded-lg">
              <AlertCircle className="w-5 h-5" /> {error}
            </div>
          ) : (
            <div className="overflow-x-auto mt-4">
              <table className="min-w-full bg-white rounded-lg shadow">
                <thead>
                  <tr className="bg-gray-100 text-gray-700">
                    <th className="px-4 py-2">Opção</th>
                    <th className="px-4 py-2">Data Entrada</th>
                    <th className="px-4 py-2">Casa de Análise</th>
                    <th className="px-4 py-2">Corretora</th>
                    <th className="px-4 py-2">Quantidade</th>
                    <th className="px-4 py-2">Valor Unitário</th>
                    <th className="px-4 py-2">Valor Total</th>
                    <th className="px-4 py-2">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {operacoesAtivas.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="text-center py-6 text-gray-500">Nenhuma operação ativa encontrada.</td>
                    </tr>
                  ) : (
                    operacoesAtivas.map((op) => (
                      <tr key={op.id} className="border-b hover:bg-gray-50">
                        <td className="px-4 py-2 flex items-center gap-2">
                          <img src={op.logoEmpresa} alt={op.opcao} className="w-7 h-7 rounded-full object-contain" />
                          <span>{op.opcao}</span>
                        </td>
                        <td className="px-4 py-2 text-center">{formatarData(op.dataEntrada)}</td>
                        <td className="px-4 py-2 text-center">{op.casaAnalise}</td>
                        <td className="px-4 py-2 text-center">{op.corretora}</td>
                        <td className="px-4 py-2 text-center">{op.quantidade}</td>
                        <td className="px-4 py-2 text-right">{formatarMoeda(op.valorUnitario)}</td>
                        <td className="px-4 py-2 text-right">{formatarMoeda(op.valorTotal)}</td>
                        <td className="px-4 py-2 flex gap-2 justify-center">
                          <button 
                            title="Editar" 
                            className="p-2 rounded hover:bg-gray-100" 
                            onClick={() => handleEditar(op.id)}
                          >
                            <Edit className="w-4 h-4 text-blue-600" />
                          </button>
                          <button 
                            title="Finalizar" 
                            className="p-2 rounded hover:bg-gray-100" 
                            onClick={() => handleFinalizar(op.id)}
                          >
                            <CheckSquare className="w-4 h-4 text-green-600" />
                          </button>
                          <button 
                            title="Remover" 
                            className="p-2 rounded hover:bg-gray-100" 
                            onClick={() => handleRemover(op.id)}
                          >
                            <Trash className="w-4 h-4 text-red-600" />
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </TabsContent>
        
        <TabsContent value="finalizadas">
          {loadingFinalizadas ? (
            <div className="flex justify-center items-center py-10">
              <Loader2 className="animate-spin w-8 h-8 text-purple-600" />
              <span className="ml-3 text-gray-600">Carregando operações finalizadas...</span>
            </div>
          ) : error ? (
            <div className="flex items-center gap-2 p-4 bg-red-100 text-red-700 rounded-lg">
              <AlertCircle className="w-5 h-5" /> {error}
            </div>
          ) : (
            <div className="overflow-x-auto mt-4">
              <table className="min-w-full bg-white rounded-lg shadow">
                <thead>
                  <tr className="bg-gray-100 text-gray-700">
                    <th className="px-4 py-2">Opção</th>
                    <th className="px-4 py-2">Data Entrada</th>
                    <th className="px-4 py-2">Data Saída</th>
                    <th className="px-4 py-2">Casa de Análise</th>
                    <th className="px-4 py-2">Corretora</th>
                    <th className="px-4 py-2">Quantidade</th>
                    <th className="px-4 py-2">Valor Entrada</th>
                    <th className="px-4 py-2">Valor Saída</th>
                    <th className="px-4 py-2">Lucro / Prejuízo</th>
                    <th className="px-4 py-2">% Lucro / Prejuízo</th>
                    <th className="px-4 py-2">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {operacoesFinalizadas.length === 0 ? (
                    <tr>
                      <td colSpan={11} className="text-center py-6 text-gray-500">Nenhuma operação finalizada encontrada.</td>
                    </tr>
                  ) : (
                    operacoesFinalizadas.map((op) => (
                      <tr key={op.id} className="border-b hover:bg-gray-50">
                        <td className="px-4 py-2 flex items-center gap-2">
                          <img src={op.logoEmpresa} alt={op.opcao} className="w-7 h-7 rounded-full object-contain" />
                          <span>{op.opcao}</span>
                        </td>
                        <td className="px-4 py-2 text-center">{formatarData(op.dataEntrada)}</td>
                        <td className="px-4 py-2 text-center">{formatarData(op.dataSaida)}</td>
                        <td className="px-4 py-2 text-center">{op.casaAnalise}</td>
                        <td className="px-4 py-2 text-center">{op.corretora}</td>
                        <td className="px-4 py-2 text-center">{op.quantidade}</td>
                        <td className="px-4 py-2 text-right">{formatarMoeda(op.valorUnitario)}</td>
                        <td className="px-4 py-2 text-right">{formatarMoeda(op.valorUnitarioSaida)}</td>
                        <td className={`px-4 py-2 text-right ${op.valorLucroPrejuizo >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                          {formatarMoeda(op.valorLucroPrejuizo)}
                        </td>
                        <td className={`px-4 py-2 text-center ${op.percentualLucroPrejuizo >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                          {op.percentualLucroPrejuizo.toFixed(2)}%
                        </td>
                        <td className="px-4 py-2">
                          <span className={`px-2 py-1 text-xs font-semibold rounded-full ${op.status === 'Vencedor' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                            {op.status}
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </TabsContent>
      </Tabs>
      
      {/* Modal de Nova Operação */}
      <NovaOperacaoModal 
        isOpen={modalNovaOperacaoAberto} 
        onClose={fecharModalNovaOperacao} 
        onSuccess={handleNovaOperacaoSucesso} 
      />
      
      {/* Modal de Finalizar Operação */}
      {modalFinalizarOperacaoAberto && (
        <FinalizarOperacaoModal 
          isOpen={modalFinalizarOperacaoAberto} 
          operacaoId={operacaoParaFinalizar} 
          onClose={fecharModalFinalizarOperacao}
          onSuccess={handleFinalizarOperacaoSucesso} 
        />
      )}
    </div>
  );
}