import React, { useState, useEffect } from 'react';
import { FileText, Plus, Eye, Trash, Download, AlertCircle, Loader2 } from 'lucide-react';
import { ImportarNotaModal } from '../../components/ImportarNotaModal';
import { NotasCorretagemService, NotaCorretagemSummary } from '../../services/notasCorretagemService';

export function NotasCorretagem() {
  // Estado para controlar as notas de corretagem
  const [notas, setNotas] = useState<NotaCorretagemSummary[]>([]);
  
  // Estado para controlar a paginação
  const [currentPage, setCurrentPage] = useState(0); // API usa paginação baseada em 0
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  
  // Estado para controlar o carregamento e erros
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Estado para controlar a visibilidade do modal
  const [modalOpen, setModalOpen] = useState(false);
  
  // Carregar as notas ao montar o componente e quando a página mudar
  useEffect(() => {
    carregarNotas();
  }, [currentPage, pageSize]);
  
  // Função para carregar as notas do backend
  const carregarNotas = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // Tentamos carregar os dados do backend
      const response = await NotasCorretagemService.obterNotas(currentPage, pageSize);
      
      setNotas(response.content || []);
      setTotalPages(response.totalPages || 1);
      setTotalItems(response.totalElements || 0);
    } catch (error) {
      console.error('Erro ao carregar notas:', error);
      
      // Mensagem de erro mais específica
      if (error instanceof Error) {
        setError(error.message);
      } else {
        setError('Não foi possível carregar as notas de corretagem. Tente novamente mais tarde.');
      }
      
      // Se não conseguimos conectar ao backend, podemos mostrar dados mockados para desenvolvimento
      if (process.env.NODE_ENV === 'development' && error instanceof Error && error.message.includes('conectar ao servidor')) {
        console.log('Usando dados mockados para desenvolvimento');
        setNotas([
          {
            id: '1',
            tradingDate: new Date().toISOString(),
            invoiceNumber: '123456',
            operationNetValue: 1500.75,
            brokerageName: 'CLEAR CORRETORA - GRUPO XP'
          },
          {
            id: '2',
            tradingDate: new Date().toISOString(),
            invoiceNumber: '654321',
            operationNetValue: 2780.50,
            brokerageName: 'RICO CORRETORA'
          }
        ]);
        setTotalPages(1);
        setTotalItems(2);
      }
    } finally {
      setLoading(false);
    }
  };
  
  // Abre o modal de importação
  const handleOpenModal = () => {
    setModalOpen(true);
  };
  
  // Fecha o modal de importação
  const handleCloseModal = () => {
    setModalOpen(false);
  };
  
  // Callback para quando a importação for bem-sucedida
  const handleImportSuccess = () => {
    // Recarregar as notas
    carregarNotas();
  };
  
  // Função para excluir uma nota
  const handleExcluirNota = async (id: string) => {
    if (!window.confirm('Tem certeza que deseja excluir esta nota de corretagem?')) {
      return;
    }
    
    setLoading(true);
    
    try {
      await NotasCorretagemService.excluirNota(id);
      
      // Recarregar as notas após excluir
      carregarNotas();
    } catch (error) {
      console.error('Erro ao excluir nota:', error);
      alert('Não foi possível excluir a nota. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };
  
  // Função para visualizar detalhes da nota
  const handleVisualizarNota = (id: string) => {
    // Aqui você pode redirecionar para uma página de detalhes da nota
    // ou abrir um modal com os detalhes
    alert(`Visualizando detalhes da nota ${id}`);
  };
  
  // Função para baixar a nota
  const handleBaixarNota = (id: string) => {
    // Aqui você pode implementar a lógica para baixar a nota
    alert(`Baixando nota ${id}`);
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
  
  // Função para mudar de página
  const paginate = (pageNumber: number) => {
    setCurrentPage(pageNumber);
  };
  
  return (
    <>
      <div className="p-6 max-w-7xl mx-auto">
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="p-6 flex justify-between items-center border-b border-gray-100">
            <h1 className="text-xl font-semibold text-gray-800 flex items-center gap-2">
              <FileText className="h-6 w-6 text-purple-500" />
              Notas de Corretagem
            </h1>
            
            {/* Botão de importar que aparece somente quando há notas ou está carregando */}
            {(notas.length > 0 || loading) && (
              <button
                onClick={handleOpenModal}
                className="px-4 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600 flex items-center gap-2"
                disabled={loading}
              >
                <Plus className="h-4 w-4" />
                Importar nova nota
              </button>
            )}
          </div>
          
          {/* Estado de carregamento */}
          {loading && !error && notas.length === 0 && (
            <div className="flex flex-col items-center justify-center py-16">
              <Loader2 className="h-12 w-12 text-purple-500 animate-spin mb-4" />
              <p className="text-gray-500">Carregando notas de corretagem...</p>
            </div>
          )}
          
          {/* Estado de erro */}
          {error && (
            <div className="flex flex-col items-center justify-center py-16">
              <div className="flex items-center gap-2 text-red-500 mb-4">
                <AlertCircle className="h-6 w-6" />
                <p>Erro ao carregar os dados</p>
              </div>
              <p className="text-gray-500 mb-6">{error}</p>
              <button
                onClick={carregarNotas}
                className="px-6 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600"
              >
                Tentar novamente
              </button>
            </div>
          )}
          
          {/* Conteúdo condicional baseado na existência de notas */}
          {!loading && !error && notas.length === 0 ? (
            // Estado vazio - nenhuma nota importada
            <div className="flex flex-col items-center justify-center py-16 px-4">
              <div className="bg-gray-50 p-6 rounded-full mb-4">
                <FileText className="h-12 w-12 text-gray-300" />
              </div>
              <p className="text-gray-500 mb-6">Nenhuma nota de corretagem importada.</p>
              <button
                onClick={handleOpenModal}
                className="px-6 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600 flex items-center gap-2"
              >
                <Plus className="h-5 w-5" />
                Importar nova nota
              </button>
            </div>
          ) : !loading && !error ? (
            // Tabela de notas importadas
            <div>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Data do Pregão
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Número da nota
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Valor Total da Nota
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Corretora
                      </th>
                      <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Ações
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200 bg-white">
                    {notas.map((nota) => (
                      <tr key={nota.id}>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          {formatarData(nota.tradingDate)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {nota.invoiceNumber}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {formatarMoeda(nota.operationNetValue)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {nota.brokerageName}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-right space-x-2 flex justify-center">
                          <button
                            onClick={() => handleVisualizarNota(nota.id)}
                            className="text-indigo-600 hover:text-indigo-900 p-1"
                            title="Visualizar"
                          >
                            <Eye className="h-5 w-5" />
                          </button>
                          <button
                            onClick={() => handleBaixarNota(nota.id)}
                            className="text-green-600 hover:text-green-900 p-1"
                            title="Baixar"
                          >
                            <Download className="h-5 w-5" />
                          </button>
                          <button
                            onClick={() => handleExcluirNota(nota.id)}
                            className="text-red-600 hover:text-red-900 p-1"
                            title="Excluir"
                          >
                            <Trash className="h-5 w-5" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              
              {/* Paginação */}
              {totalPages > 1 && (
                <div className="px-6 py-3 flex items-center justify-between border-t border-gray-200">
                  <div className="text-sm text-gray-500">
                    Mostrando {notas.length === 0 ? 0 : currentPage * pageSize + 1} a {Math.min((currentPage + 1) * pageSize, totalItems)} de {totalItems} resultados
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => paginate(currentPage - 1)}
                      disabled={currentPage === 0}
                      className={`px-3 py-1 border rounded-md text-sm ${
                        currentPage === 0
                          ? 'border-gray-200 text-gray-400 cursor-not-allowed'
                          : 'border-gray-300 text-gray-600 hover:bg-gray-50'
                      }`}
                    >
                      Anterior
                    </button>
                    
                    {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                      // Lógica para mostrar páginas ao redor da página atual
                      let pageToShow;
                      if (totalPages <= 5) {
                        pageToShow = i;
                      } else if (currentPage < 3) {
                        pageToShow = i;
                      } else if (currentPage > totalPages - 3) {
                        pageToShow = totalPages - 5 + i;
                      } else {
                        pageToShow = currentPage - 2 + i;
                      }
                      
                      return (
                        <button
                          key={pageToShow}
                          onClick={() => paginate(pageToShow)}
                          className={`px-3 py-1 border rounded-md text-sm ${
                            currentPage === pageToShow
                              ? 'bg-purple-50 text-purple-600 border-purple-200'
                              : 'border-gray-300 text-gray-600 hover:bg-gray-50'
                          }`}
                        >
                          {pageToShow + 1}
                        </button>
                      );
                    })}
                    
                    <button
                      onClick={() => paginate(currentPage + 1)}
                      disabled={currentPage === totalPages - 1}
                      className={`px-3 py-1 border rounded-md text-sm ${
                        currentPage === totalPages - 1
                          ? 'border-gray-200 text-gray-400 cursor-not-allowed'
                          : 'border-gray-300 text-gray-600 hover:bg-gray-50'
                      }`}
                    >
                      Próximo
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : null}
        </div>
      </div>
      
      {/* Modal de importação */}
      <ImportarNotaModal 
        isOpen={modalOpen}
        onClose={handleCloseModal}
        onSuccess={handleImportSuccess}
      />
    </>
  );
}
