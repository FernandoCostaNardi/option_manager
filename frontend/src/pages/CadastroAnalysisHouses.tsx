import React, { useEffect, useState } from "react";
import {
  AnalysisHouse,
  AnalysisHouseService,
  AnalysisHousePaginated,
} from "../services/analysisHouseService";
import { Pencil, Trash, Loader2, Plus } from "lucide-react";
import { ConfirmDialog } from "../components/ConfirmDialog";

interface ModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

function CadastroAnalysisHouseModal({
  open,
  onClose,
  onSuccess,
  analysisHouseId,
}: ModalProps & { analysisHouseId?: string }) {
  const [form, setForm] = useState({
    name: "",
    cnpj: "",
    website: "",
    contactEmail: "",
    contactPhone: "",
    subscriptionType: "",
    status: "Ativo",
  });
  const [loading, setLoading] = useState(() => open && !!analysisHouseId);
  const [error, setError] = useState("");

  useEffect(() => {
    if (open && analysisHouseId) {
      setLoading(true);
      AnalysisHouseService.getAnalysisHouseById(analysisHouseId)
        .then((data) => {
          setForm({
            name: data.name || "",
            cnpj: data.cnpj || "",
            website: data.website || "",
            contactEmail: data.contactEmail || "",
            contactPhone: data.contactPhone || "",
            subscriptionType: data.subscriptionType || "",
            status:
              { ACTIVE: "Ativo", INACTIVE: "Inativo" }[data.status as string] ??
              "Pendente",
          });
        })
        .catch(() => setError("Erro ao buscar casa de análise."))
        .finally(() => setLoading(false));
    } else if (open && !analysisHouseId) {
      setForm({
        name: "",
        cnpj: "",
        website: "",
        contactEmail: "",
        contactPhone: "",
        subscriptionType: "",
        status: "Ativo",
      });
      setError("");
      setLoading(false);
    }
  }, [open, analysisHouseId]);

  if (!open) return null;

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (!form.name) {
      setError("Nome é obrigatório.");
      return;
    }
    setLoading(true);
    try {
      const statusApi = form.status === "Ativo" ? "ACTIVE" : "INACTIVE";
      const payload = {
        name: form.name,
        cnpj: form.cnpj || "",
        website: form.website || "",
        contactEmail: form.contactEmail || "",
        contactPhone: form.contactPhone || "",
        subscriptionType: form.subscriptionType || "",
        status: statusApi,
      };
      if (analysisHouseId) {
        await AnalysisHouseService.updateAnalysisHouse(
          analysisHouseId,
          payload as AnalysisHouse
        );
      } else {
        await AnalysisHouseService.createAnalysisHouse(
          payload as AnalysisHouse
        );
      }
      onSuccess();
      onClose();
      setForm({
        name: "",
        cnpj: "",
        website: "",
        contactEmail: "",
        contactPhone: "",
        subscriptionType: "",
        status: "Ativo",
      });
    } catch (err) {
      setError("Erro ao salvar casa de análise.");
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
            <h2 className="text-xl font-bold mb-4">
              Cadastrar Casa de Análise
            </h2>
            {error && <div className="text-red-600 text-sm mb-2">{error}</div>}
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium">Nome *</label>
                <input
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  className="border rounded w-full px-3 py-2 mt-1"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium">CNPJ</label>
                <input
                  name="cnpj"
                  value={form.cnpj}
                  onChange={handleChange}
                  className="border rounded w-full px-3 py-2 mt-1"
                  placeholder="Opcional"
                />
              </div>
              <div>
                <label className="block text-sm font-medium">Website</label>
                <input
                  name="website"
                  value={form.website}
                  onChange={handleChange}
                  className="border rounded w-full px-3 py-2 mt-1"
                  placeholder="Opcional"
                />
              </div>
              <div>
                <label className="block text-sm font-medium">
                  Email de Contato
                </label>
                <input
                  name="contactEmail"
                  type="email"
                  value={form.contactEmail}
                  onChange={handleChange}
                  className="border rounded w-full px-3 py-2 mt-1"
                  placeholder="Opcional"
                />
              </div>
              <div>
                <label className="block text-sm font-medium">
                  Telefone de Contato
                </label>
                <input
                  name="contactPhone"
                  value={form.contactPhone}
                  onChange={handleChange}
                  className="border rounded w-full px-3 py-2 mt-1"
                  placeholder="Opcional"
                />
              </div>
              <div>
                <label className="block text-sm font-medium">
                  Tipo de Assinatura
                </label>
                <select
                  name="subscriptionType"
                  value={form.subscriptionType}
                  onChange={handleChange}
                  className="border rounded w-full px-3 py-2 mt-1"
                >
                  <option value="">Selecione...</option>
                  <option value="Basic">Básico</option>
                  <option value="Standard">Padrão</option>
                  <option value="Premium">Premium</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium">Status</label>
                <select
                  name="status"
                  value={form.status}
                  onChange={handleChange}
                  className="border rounded w-full px-3 py-2 mt-1"
                >
                  <option value="Ativo">Ativo</option>
                  <option value="Inativo">Inativo</option>
                </select>
              </div>
              <div className="flex justify-end gap-2 mt-4">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 rounded bg-gray-200"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded bg-indigo-600 text-white"
                  disabled={loading}
                >
                  {loading ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}

export function CadastroAnalysisHouses() {
  const [analysisHouses, setAnalysisHouses] = useState<AnalysisHouse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<string | undefined>(undefined);
  const [deleteLoading, setDeleteLoading] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Add these two states:
  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  // Função para buscar casas de análise
  const fetchAnalysisHouses = async (currentPage: number = page) => {
    console.log("Buscando casas de análise da página:", currentPage);
    setLoading(true);
    try {
      const data: AnalysisHousePaginated =
        await AnalysisHouseService.getAnalysisHouses(currentPage, 10);
      console.log("Dados recebidos:", data);
      setAnalysisHouses(data.content);
      // Garantir que totalPages seja pelo menos 1 para forçar exibição da paginação
      setTotalPages(Math.max(1, data.totalPages));
      setPage(currentPage);
    } catch (error) {
      console.error("Erro ao buscar casas de análise:", error);
      setAnalysisHouses([]);
      // Manter totalPages como 1 mesmo em caso de erro
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (id: string) => {
    setEditId(id);
    setModalOpen(true);
  };

  // Update the handleDelete function to open the dialog instead of using window.confirm
  const handleDelete = (id: string) => {
    setDeleteId(id);
    setConfirmDialogOpen(true);
  };

  // Add a new function to handle the confirmation
  const confirmDelete = async () => {
    if (!deleteId) return;

    setDeleteLoading(deleteId);
    try {
      await AnalysisHouseService.deleteAnalysisHouse(deleteId);
      const pageToFetch =
        analysisHouses.length === 1 && page > 0 ? page - 1 : page;
      fetchAnalysisHouses(pageToFetch);
    } catch {
      alert("Erro ao excluir casa de análise.");
    } finally {
      setDeleteLoading(null);
      setConfirmDialogOpen(false); // Close dialog after operation
      setDeleteId(null); // Clear deleteId after operation
    }
  };

  // Buscar casas de análise quando o componente monta
  useEffect(() => {
    console.log("Componente montado, buscando dados iniciais");
    fetchAnalysisHouses(0); // Buscar página inicial
  }, []); // Array de dependências vazio garante que isso roda apenas uma vez na montagem

  // Handler para mudanças de página
  const handlePageChange = (newPage: number) => {
    console.log("Mudando para página:", newPage);
    fetchAnalysisHouses(newPage);
  };

  return (
    <div className="p-4">
      <header className="mb-8 flex items-center justify-between">
        <h1 className="text-2xl md:text-3xl font-bold text-gray-800">
          Casas de Análise
        </h1>
        <button
          onClick={() => {
            setModalOpen(true);
            setEditId(undefined);
          }}
          className="px-4 py-2 bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-lg hover:from-blue-700 hover:to-indigo-800 shadow-sm flex items-center gap-2"
        >
          <Plus size={18} />
          <span>Nova Casa de Análise</span>
        </button>
      </header>

      <CadastroAnalysisHouseModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={() => fetchAnalysisHouses(page)}
        analysisHouseId={editId}
      />

      {loading ? (
        <div className="bg-white rounded-xl shadow-sm p-10 text-center border border-gray-100">
          <div className="flex flex-col items-center justify-center">
            <Loader2 className="w-10 h-10 text-purple-600 animate-spin mb-4" />
            <p className="text-gray-500">Carregando casas de análise...</p>
          </div>
        </div>
      ) : analysisHouses.length === 0 ? (
        <div className="bg-white rounded-xl shadow-sm p-10 text-center border border-gray-100">
          <p className="text-gray-500">
            Nenhuma casa de análise cadastrada ainda.
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-100">
          <div className="px-6 py-4 border-b border-gray-100">
            <h2 className="font-semibold text-gray-800">
              Casas de Análise Cadastradas
            </h2>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead>
                <tr>
                  <th className="px-6 py-3 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Nome
                  </th>
                  <th className="px-6 py-3 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    CNPJ
                  </th>
                  <th className="px-6 py-3 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Website
                  </th>
                  <th className="px-6 py-3 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 bg-gray-50"></th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {analysisHouses.map((house) => (
                  <tr key={house.id}>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {house.name}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {house.cnpj}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {house.website}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {(house.status as string) === "ACTIVE" ? (
                        <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                          Ativo
                        </span>
                      ) : (
                        <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">
                          Inativo
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button
                        onClick={() => {
                          setEditId(house.id);
                          setModalOpen(true);
                        }}
                        className="text-indigo-600 hover:text-indigo-900 mr-2"
                      >
                        <Pencil size={16} />
                      </button>
                      <button
                        onClick={() => handleDelete(house.id!)}
                        className="text-red-600 hover:text-red-900"
                      >
                        <Trash size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="px-6 py-3 flex items-center justify-between border-t border-gray-200">
            <div className="text-sm text-gray-500">
              Mostrando {analysisHouses.length} de {totalPages * 10} resultados
            </div>

            <div className="flex gap-2">
              <button
                onClick={() => page > 0 && handlePageChange(page - 1)}
                disabled={page === 0}
                className="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:hover:bg-white"
              >
                Anterior
              </button>

              {Array.from({ length: Math.min(totalPages, 3) }, (_, i) => {
                const pageNumber =
                  page <= 0
                    ? i
                    : page >= totalPages - 2
                      ? totalPages - 3 + i
                      : page - 1 + i;

                if (pageNumber < 0 || pageNumber >= totalPages) return null;

                return (
                  <button
                    key={pageNumber}
                    onClick={() => handlePageChange(pageNumber)}
                    className={`px-3 py-1 border rounded-md text-sm ${
                      page === pageNumber
                        ? "bg-blue-50 text-blue-600 border-blue-200"
                        : "border-gray-300 text-gray-600 hover:bg-gray-50"
                    }`}
                  >
                    {pageNumber + 1}
                  </button>
                );
              })}

              <button
                onClick={() =>
                  page < totalPages - 1 && handlePageChange(page + 1)
                }
                disabled={page >= totalPages - 1}
                className="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:hover:bg-white"
              >
                Próximo
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        isOpen={confirmDialogOpen}
        onClose={() => setConfirmDialogOpen(false)}
        onConfirm={confirmDelete}
        title="Excluir Casa de Análise"
        description="Tem certeza que deseja excluir esta casa de análise? Esta ação não pode ser desfeita."
        confirmText="Excluir"
        cancelText="Cancelar"
      />
    </div>
  );
}