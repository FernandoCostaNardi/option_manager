/**
 * Serviço dedicado para finalizar operações
 * Implementado de forma direta para depuração
 */

export const finalizarOperacao = async (
  operationId: string,
  exitDate: string,
  exitUnitPrice: number,
  quantity?: number
): Promise<any> => {
  console.log('[API CALL] INICIANDO FINALIZAÇÃO DE OPERAÇÃO');
  console.log('[API CALL] Dados:', { operationId, exitDate, exitUnitPrice, quantity });
  
  // Verificar token
  const token = localStorage.getItem('token');
  if (!token) {
    alert('Usuário não autenticado. Faça login novamente.');
    throw new Error('Token não encontrado');
  }

  // Formato dos dados para o backend
  const payload = {
    operationId,
    exitDate,
    exitUnitPrice,
    ...(quantity && { quantity })
  };

  console.log('[API CALL] Payload:', JSON.stringify(payload));
  
  // Fazer chamada direta
  try {
    // Chamar via XMLHttpRequest síncrono para debugging
    const xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8080/api/operations/finalize', false); // síncrono para debug
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    
    console.log('[API CALL] Enviando requisição...');
    xhr.send(JSON.stringify(payload));
    console.log('[API CALL] Status da resposta:', xhr.status);
    
    if (xhr.status >= 200 && xhr.status < 300) {
      alert('Operação finalizada com sucesso!');
      return { success: true };
    } else {
      alert(`Erro ao finalizar operação: ${xhr.status} ${xhr.statusText}`);
      throw new Error(`Erro ${xhr.status}: ${xhr.responseText}`);
    }
  } catch (error) {
    console.error('[API CALL] Erro grave:', error);
    alert('Erro ao finalizar operação. Verifique o console para mais detalhes.');
    throw error;
  }
}; 