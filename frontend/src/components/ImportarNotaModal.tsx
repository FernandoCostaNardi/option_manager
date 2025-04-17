import React, { useState, useRef } from 'react';
import { Upload, X, FileText, Loader2 } from 'lucide-react';
import { NotasCorretagemService } from '../services/notasCorretagemService';

interface ImportarNotaModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function ImportarNotaModal({ isOpen, onClose, onSuccess }: ImportarNotaModalProps) {
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const filesArray = Array.from(e.target.files).filter(file => file.type === 'application/pdf');
      setSelectedFiles(prevFiles => [...prevFiles, ...filesArray]);
      setUploadError(null);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (e.dataTransfer.files) {
      const filesArray = Array.from(e.dataTransfer.files).filter(file => file.type === 'application/pdf');
      setSelectedFiles(prevFiles => [...prevFiles, ...filesArray]);
      setUploadError(null);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
  };

  const handleRemoveFile = (index: number) => {
    setSelectedFiles(prevFiles => prevFiles.filter((_, i) => i !== index));
  };

  const handleImportar = async () => {
    if (selectedFiles.length === 0) return;
    
    setIsUploading(true);
    setUploadError(null);
    
    try {
      // Chamar o serviço de importação de notas
      await NotasCorretagemService.importarNotas(selectedFiles);
      
      // Limpar os arquivos selecionados
      setSelectedFiles([]);
      
      // Notificar o componente pai sobre o sucesso
      onSuccess();
      
      // Fechar o modal
      onClose();
    } catch (error) {
      console.error('Erro ao importar notas:', error);
      setUploadError(error instanceof Error ? error.message : 'Erro ao importar as notas. Tente novamente.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleClickUploadArea = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        <div className="flex justify-between items-center p-4 border-b">
          <h2 className="text-lg font-medium flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Importar Nota de Corretagem
          </h2>
          <button 
            onClick={onClose}
            disabled={isUploading}
            className="text-gray-500 hover:text-gray-700 disabled:text-gray-400"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="p-6">
          {/* Área de upload */}
          <div
            onClick={handleClickUploadArea}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
              isUploading
                ? 'border-gray-300 bg-gray-50'
                : 'border-gray-300 hover:border-purple-500'
            }`}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileChange}
              className="hidden"
              accept=".pdf"
              multiple
              disabled={isUploading}
            />
            
            {isUploading ? (
              <div className="flex flex-col items-center">
                <Loader2 className="h-12 w-12 animate-spin text-purple-500 mb-3" />
                <p className="text-gray-700">Processando arquivos...</p>
              </div>
            ) : selectedFiles.length === 0 ? (
              <>
                <Upload className="h-12 w-12 mx-auto text-gray-400 mb-3" />
                <p className="text-gray-700 mb-1">Clique para selecionar ou arraste o arquivo PDF aqui</p>
                <p className="text-sm text-gray-500">Apenas arquivos PDF são suportados</p>
              </>
            ) : (
              <div className="space-y-2">
                <p className="text-sm text-gray-700 mb-2 font-medium">Arquivos selecionados:</p>
                {selectedFiles.map((file, index) => (
                  <div key={index} className="flex items-center justify-between bg-purple-50 p-2 rounded">
                    <div className="flex items-center gap-2">
                      <FileText className="h-4 w-4 text-purple-600" />
                      <span className="text-sm text-gray-700 truncate max-w-[250px]">{file.name}</span>
                    </div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleRemoveFile(index);
                      }}
                      className="text-gray-500 hover:text-red-500"
                      disabled={isUploading}
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                ))}
                <p className="text-xs text-purple-600 mt-2">
                  Clique ou arraste mais arquivos para adicionar
                </p>
              </div>
            )}
          </div>
          
          {/* Mensagem de erro */}
          {uploadError && (
            <div className="mt-4 text-red-500 text-sm bg-red-50 p-2 rounded border border-red-200">
              <p>{uploadError}</p>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 p-4 border-t bg-gray-50 rounded-b-lg">
          <button
            onClick={onClose}
            disabled={isUploading}
            className="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:bg-gray-100 disabled:text-gray-500"
          >
            Cancelar
          </button>
          <button
            onClick={handleImportar}
            disabled={selectedFiles.length === 0 || isUploading}
            className={`px-4 py-2 text-white rounded-lg flex items-center gap-2 ${
              selectedFiles.length === 0 || isUploading
                ? 'bg-purple-300 cursor-not-allowed'
                : 'bg-purple-500 hover:bg-purple-600'
            }`}
          >
            {isUploading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <FileText className="h-4 w-4" />
            )}
            Importar
          </button>
        </div>
      </div>
    </div>
  );
}
