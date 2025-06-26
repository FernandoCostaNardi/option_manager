import React, { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { OperacaoFinalizada, SortField, SortDirection } from '../../../types/operacao/operacoes.types';
import { OperacaoFinalizadaItem } from './OperacaoFinalizadaItem';
import { SortIcon } from '../../SortIcon';

interface OperacoesFinalizadasTableProps {
  operacoes: OperacaoFinalizada[];
  loading: boolean;
  onView: (id: string) => void;
  onRemove: (id: string) => void;
  onViewTargets: (id: string) => void;
  onSort: (field: SortField) => void;
  sortField: SortField;
  sortDirection: SortDirection;
}

export function OperacoesFinalizadasTable({
  operacoes,
  loading,
  onView,
  onRemove,
  onViewTargets,
  onSort,
  sortField,
  sortDirection
}: OperacoesFinalizadasTableProps) {
  const [detalhes, setDetalhes] = useState<{ [key: string]: OperacaoFinalizada[] }>({});
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [loadingDetalhes, setLoadingDetalhes] = useState<string | null>(null);

  const handleToggle = async (operacao: OperacaoFinalizada) => {
    // Se já está expandido, fechar
    if (expandedId === operacao.id) {
      setExpandedId(null);
      return;
    }
    setExpandedId(operacao.id);
    if (!detalhes[operacao.id] && operacao.groupId) {
      setLoadingDetalhes(operacao.id);
      try {
        const token = localStorage.getItem('token');
        const url = `http://localhost:8080/api/operations/group/${operacao.groupId}/exited-operations`;
        console.log('[DEBUG] Buscando detalhes da operação:', url);
        console.log('[DEBUG] Operação mãe - ID:', operacao.id, 'groupId:', operacao.groupId);
        console.log('[DEBUG] Token presente:', !!token);
        
        const resp = await fetch(url, {
          headers: token ? { 'Authorization': `Bearer ${token}` } : {},
        });
        console.log('[DEBUG] Status da resposta:', resp.status);
        console.log('[DEBUG] Response headers:', resp.headers);
        
        if (resp.ok) {
          const data = await resp.json();
          console.log('[DEBUG] Detalhes recebidos (bruto):', data);
          console.log('[DEBUG] Tipo de dados recebidos:', Array.isArray(data) ? 'Array' : typeof data);
          
          // Verificar se data é um array antes de processar
          if (!Array.isArray(data)) {
            console.error('[DEBUG] Dados recebidos não são um array:', data);
            return;
          }
          
          // Extrair operações de cada objeto que tem estrutura {operations: [...]}
          const todasOperacoes: any[] = [];
          data.forEach((item: any) => {
            if (item.operations && Array.isArray(item.operations)) {
              todasOperacoes.push(...item.operations);
            }
          });
          
          console.log('[DEBUG] Total de operações extraídas:', todasOperacoes.length);
          
          // Filtrar apenas as operações que devem aparecer nos detalhes
          const operacoesFiltradas = todasOperacoes.filter((op: any) => {
            console.log('[DEBUG] Analisando operação - ID:', op.id, 'vs operacao.id:', operacao.id);
            console.log('[DEBUG] Dados da operação:', { 
              id: op.id, 
              roleType: op.roleType, 
              quantity: op.quantity, 
              status: op.status, 
              profitLoss: op.profitLoss,
              exitDate: op.exitDate,
              sequenceNumber: op.sequenceNumber
            });
            
            // Não deve ser a mesma operação consolidada (principal critério)
            const naoEhOperacaoMae = op.id !== operacao.id;
            
            // Mostrar todas as operações exceto a operação mãe
            const passa = naoEhOperacaoMae;
            console.log('[DEBUG] Operação passa no filtro:', passa, { 
              naoEhOperacaoMae,
              operacaoMaeId: operacao.id,
              operacaoFilhaId: op.id,
              saoIguais: op.id === operacao.id
            });
            
            return passa;
          }).map(op => ({
            ...op,
            entryDate: op.entryDate,
            exitDate: op.exitDate,
            optionType: op.optionType || '',
            tradeType: op.tradeType || '',
            status: op.status || '',
            transactionType: op.transactionType || '',
            optionSeriesCode: op.optionSeriesCode || '',
            brokerageName: op.brokerageName || '',
            analysisHouseName: op.analysisHouseName || '',
            baseAssetLogoUrl: op.baseAssetLogoUrl || '',
            quantity: op.quantity || 0,
            entryUnitPrice: op.entryUnitPrice || 0,
            entryTotalValue: op.entryTotalValue || 0,
            exitUnitPrice: op.exitUnitPrice || 0,
            exitTotalValue: op.exitTotalValue || 0,
            profitLoss: op.profitLoss || 0,
            profitLossPercentage: op.profitLossPercentage || 0,
            groupId: op.groupId || null,
          }));
          
          console.log('[DEBUG] Total de operações filtradas:', operacoesFiltradas.length);
          setDetalhes(prev => ({ ...prev, [operacao.id]: operacoesFiltradas }));
        } else {
          console.error('[DEBUG] Erro ao buscar detalhes:', resp.status, resp.statusText);
          const responseText = await resp.text();
          console.error('[DEBUG] Conteúdo da resposta de erro:', responseText);
        }
      } catch (error) {
        console.error('[DEBUG] Erro na requisição:', error);
      } finally {
        setLoadingDetalhes(null);
      }
    } else if (!operacao.groupId) {
      console.warn('[DEBUG] groupId não encontrado para operação:', operacao);
    }
  };

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-100">
      <table className="min-w-full divide-y divide-gray-100">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-1 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
              
            </th>
            <th 
              className="px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('optionSerieCode')}
            >
              <span className="flex items-center justify-center">
                Opção <SortIcon currentField={sortField} field="optionSerieCode" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('optionType')}
            >
              <span className="flex items-center justify-center">
                Tipo <SortIcon currentField={sortField} field="optionType" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('entryDate')}
            >
              <span className="flex items-center justify-center">
                Entrada <SortIcon currentField={sortField} field="entryDate" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('exitDate')}
            >
              <span className="flex items-center justify-center">
                Saída <SortIcon currentField={sortField} field="exitDate" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('tradeType')}
            >
              <span className="flex items-center justify-center">
                Trade <SortIcon currentField={sortField} field="tradeType" direction={sortDirection} />
              </span>
            </th>
            <th className="px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
              Qtd
            </th>
            <th 
              className="px-1 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('entryUnitPrice')}
            >
              <span className="flex items-center justify-center">
                P. Entrada <SortIcon currentField={sortField} field="entryUnitPrice" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-1 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('exitUnitPrice')}
            >
              <span className="flex items-center justify-center">
                P. Saída <SortIcon currentField={sortField} field="exitUnitPrice" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-2 py-2 text-right text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('profitLoss')}
            >
              <span className="flex items-center justify-end">
                Resultado <SortIcon currentField={sortField} field="profitLoss" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-1 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('profitLossPercentage')}
            >
              <span className="flex items-center justify-center">
                % <SortIcon currentField={sortField} field="profitLossPercentage" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('status')}
            >
              <span className="flex items-center justify-center">
                Status <SortIcon currentField={sortField} field="status" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('analysisHouseName')}
            >
              <span className="flex items-center">
                Casa <SortIcon currentField={sortField} field="analysisHouseName" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('brokerageName')}
            >
              <span className="flex items-center">
                Corretora <SortIcon currentField={sortField} field="brokerageName" direction={sortDirection} />
              </span>
            </th>
            <th className="px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
              Ações
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-100">
          {loading ? (
            <tr>
              <td colSpan={15} className="px-4 py-8 text-center">
                <div className="flex items-center justify-center">
                  <Loader2 className="w-6 h-6 text-purple-600 animate-spin mr-2" />
                  <span className="text-gray-500">Carregando operações...</span>
                </div>
              </td>
            </tr>
          ) : operacoes.length === 0 ? (
            <tr>
              <td colSpan={15} className="px-4 py-8 text-center text-gray-500">
                Nenhuma operação finalizada encontrada.
              </td>
            </tr>
          ) : (
            operacoes.map((operacao, index) => {
              const linhas = [
                <OperacaoFinalizadaItem
                  key={operacao.id}
                  operacao={operacao}
                  onView={onView}
                  onRemove={onRemove}
                  onViewTargets={onViewTargets}
                  index={index}
                  expandedId={expandedId}
                  onToggleAccordion={handleToggle}
                />
              ];

              // Se está expandido e tem detalhes, adicionar as linhas dos detalhes
              if (expandedId === operacao.id) {
                if (loadingDetalhes === operacao.id) {
                  linhas.push(
                    <tr key={`${operacao.id}-loading`}>
                      <td colSpan={15} className="px-4 py-4 text-center bg-purple-50">
                        <div className="flex items-center justify-center">
                          <Loader2 className="w-4 h-4 text-purple-600 animate-spin mr-2" />
                          <span className="text-purple-700 text-sm">Carregando detalhes...</span>
                        </div>
                      </td>
                    </tr>
                  );
                } else if (detalhes[operacao.id]) {
                  detalhes[operacao.id].forEach((detalhe, detIndex) => {
                    linhas.push(
                      <OperacaoFinalizadaItem
                        key={`${operacao.id}-detalhe-${detalhe.id}`}
                        operacao={detalhe}
                        onView={onView}
                        onRemove={onRemove}
                        onViewTargets={onViewTargets}
                        index={detIndex}
                        isFilha={true}
                        expandedId={expandedId}
                        onToggleAccordion={handleToggle}
                      />
                    );
                  });
                  
                  // Se não há detalhes para mostrar
                  if (detalhes[operacao.id].length === 0) {
                    linhas.push(
                      <tr key={`${operacao.id}-empty`}>
                        <td colSpan={15} className="px-4 py-4 text-center bg-purple-50">
                          <span className="text-purple-700 text-sm">Nenhum detalhe adicional encontrado.</span>
                        </td>
                      </tr>
                    );
                  }
                }
              }

              return linhas;
            }).flat()
          )}
        </tbody>
      </table>
    </div>
  );
}