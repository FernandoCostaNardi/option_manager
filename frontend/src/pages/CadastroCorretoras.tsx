import React, { useEffect, useState } from 'react';
import { Brokerage, BrokerageService, BrokeragePaginated } from '../services/brokerageService';
import { Pencil, Trash, Loader2, Plus } from 'lucide-react';
import { Pagination } from '../components/Pagination'; // Importação do componente
import { ConfirmDialog } from '../components/ConfirmDialog';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

function CadastroCorretoraModal({ open, onClose, onSuccess, brokerageId }: ModalProps & { brokerageId?: string }) {
  const [form, setForm] = useState({ name: '', cnpj: '', account: '', agency: '' });
  const [loading, setLoading] = useState(() => open && !!brokerageId);
  const [error, setError] = useState('');
  useEffect(() => {
    if (open && brokerageId) {
      setLoading(true);
      BrokerageService.getBrokerageById(brokerageId)
        .then((data) => {
          setForm({
            name: data.name || '',
            cnpj: data.cnpj || '',
            account: data.account || '',
            agency: data.agency || ''
          });
        })
        .catch(() => setError('Erro ao buscar corretora.'))
        .finally(() => setLoading(false));
    } else if (open && !brokerageId) {
      setForm({ name: '', cnpj: '', account: '', agency: '' });
      setError('');
    }
  }, [open, brokerageId]);

  if (!open) return null;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.name || !form.cnpj) {
      setError('Nome e CNPJ são obrigatórios.');
      return;
    }
    setLoading(true);
    try {
      const payload = {
        id: brokerageId,
        name: form.name,
        cnpj: form.cnpj,
        ...(form.account ? { account: form.account } : {}),
        ...(form.agency ? { agency: form.agency } : {})
      };
      if (brokerageId) {
        await BrokerageService.updateBrokerage(brokerageId, payload);
      } else {
        await BrokerageService.createBrokerage(payload);
      }
      onSuccess();
      onClose();
      setForm({ name: '', cnpj: '', account: '', agency: '' });
    } catch (err) {
      setError('Erro ao salvar corretora.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-40">
      <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-md">
        {loading ? (
          <div className="flex justify-center items-center p-6">
            <Loader2 className="h-8 w-8 animate-spin text-indigo-600" />
          </div>
        ) : (
          <>
            <h2 className="text-xl font-bold mb-4">Cadastrar Corretora</h2>
            {error && <div className="text-red-600 text-sm mb-2">{error}</div>}
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium">Nome *</label>
                <input name="name" value={form.name} onChange={handleChange} className="border rounded w-full px-3 py-2 mt-1" required />
              </div>
              <div>
                <label className="block text-sm font-medium">CNPJ *</label>
                <input name="cnpj" value={form.cnpj} onChange={handleChange} className="border rounded w-full px-3 py-2 mt-1" required />
              </div>
              <div>
                <label className="block text-sm font-medium">Conta</label>
                <input name="account" value={form.account} onChange={handleChange} className="border rounded w-full px-3 py-2 mt-1" placeholder="Opcional" />
              </div>
              <div>
                <label className="block text-sm font-medium">Agência</label>
                <input name="agency" value={form.agency} onChange={handleChange} className="border rounded w-full px-3 py-2 mt-1" placeholder="Opcional" />
              </div>
              <div className="flex justify-end gap-2 mt-4">
                <button type="button" onClick={onClose} className="px-4 py-2 rounded bg-gray-200">Cancelar</button>
                <button type="submit" className="px-4 py-2 rounded bg-indigo-600 text-white" disabled={loading}>{loading ? 'Salvando...' : 'Salvar'}</button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}

export function CadastroCorretoras() {
  const [brokerages, setBrokerages] = useState<Brokerage[]>([]);
  const [page, setPage] = useState(0); // Estado da página (0-indexed)
  const [totalPages, setTotalPages] = useState(1); // Iniciar com 1 para forçar exibição
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<string | undefined>(undefined);
  const [deleteLoading, setDeleteLoading] = useState<string | null>(null);

  // Função para buscar corretoras
  const fetchBrokerages = async (currentPage: number = page) => {
    console.log("Buscando corretoras da página:", currentPage);
    try {
      const data: BrokeragePaginated = await BrokerageService.getBrokerages(currentPage, 10);
      console.log("Dados recebidos:", data);
      setBrokerages(data.content);
      // Garantir que totalPages seja pelo menos 1 para forçar exibição da paginação
      setTotalPages(Math.max(1, data.totalPages));
      setPage(currentPage); 
    } catch (error) {
      console.error("Erro ao buscar corretoras:", error);
      setBrokerages([]);
      // Manter totalPages como 1 mesmo em caso de erro
      setTotalPages(1);
    }
  };

  const handleEdit = (id: string) => {
    setEditId(id);
    setModalOpen(true);
  };

  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const handleDelete = async (id: string) => {
    setDeleteId(id);
    setConfirmDialogOpen(true);
  };

  const confirmDelete = async () => {
    if (!deleteId) return;
    
    setDeleteLoading(deleteId);
    try {
      await BrokerageService.deleteBrokerage(deleteId);
      // Refetch na mesma página ou ajustar se o último item for excluído
      const newTotalPages = Math.ceil((totalPages * 10 - 1) / 10); // Estimativa simples
      const pageToFetch = (brokerages.length === 1 && page > 0) ? page - 1 : page;
      fetchBrokerages(pageToFetch);
    } catch {
      alert('Erro ao excluir corretora.');
    } finally {
      setDeleteLoading(null);
    }
  };

  // Buscar corretoras quando o componente monta
  useEffect(() => {
    console.log("Componente montado, buscando dados iniciais");
    fetchBrokerages(0); // Buscar página inicial
  }, []); // Array de dependências vazio garante que isso roda apenas uma vez na montagem

  // Handler para mudanças de página do componente Pagination
  const handlePageChange = (newPage: number) => {
    console.log("Mudando para página:", newPage);
    fetchBrokerages(newPage);
  };

  return (
    <><div className="p-4">
      <header className="mb-8 flex items-center justify-between">
        <h1 className="text-2xl md:text-3xl font-bold text-gray-800">Corretoras</h1>
        <button
          onClick={() => { setModalOpen(true); setEditId(undefined); } }
          className="px-4 py-2 bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-lg hover:from-blue-700 hover:to-indigo-800 shadow-sm flex items-center gap-2"
        >
          <Plus size={18} /> {/* Add the Plus icon here */}
          <span>Nova Corretora</span>
        </button>
      </header>

      <CadastroCorretoraModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={() => fetchBrokerages(page)} // Recarregar página atual ao ter sucesso
        brokerageId={editId} />

      {brokerages.length === 0 ? (
        <div className="bg-white rounded-xl shadow-sm p-10 text-center border border-gray-100">
          <p className="text-gray-500">Nenhuma corretora cadastrada ainda.</p>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-100">
          <div className="px-6 py-4 border-b border-gray-100">
            <h2 className="font-semibold text-gray-800">Corretoras Cadastradas</h2>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Nome</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">CNPJ</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Conta</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Agência</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {brokerages.map((brokerage) => (
                  <tr key={brokerage.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{brokerage.name}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{brokerage.cnpj}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{brokerage.account || '-'}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{brokerage.agency || '-'}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button
                        onClick={() => handleEdit(brokerage.id!)}
                        className="text-indigo-600 hover:text-indigo-900 mr-4"
                        title="Editar"
                      >
                        <Pencil size={18} />
                      </button>
                      <button
                        onClick={() => handleDelete(brokerage.id!)}
                        className="text-red-600 hover:text-red-900"
                        title="Excluir"
                        disabled={deleteLoading === brokerage.id}
                      >
                        {deleteLoading === brokerage.id ?
                          <Loader2 className="animate-spin h-4 w-4" /> :
                          <Trash size={18} />}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="px-6 py-3 flex items-center justify-between border-t border-gray-200">
            <div className="text-sm text-gray-500">
              Mostrando {brokerages.length} de {totalPages * 10} resultados
            </div>

            <Pagination
              currentPage={page}
              totalPages={totalPages}
              onPageChange={handlePageChange} />
          </div>
        </div>
      )}
    </div><ConfirmDialog
        isOpen={confirmDialogOpen}
        onClose={() => setConfirmDialogOpen(false)}
        onConfirm={confirmDelete}
        title="Excluir Corretora"
        description="Tem certeza que deseja excluir esta corretora? Esta ação não pode ser desfeita."
        confirmText="Excluir"
        cancelText="Cancelar" /></>
  );
}