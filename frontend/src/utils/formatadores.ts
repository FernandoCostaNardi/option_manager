export const formatarMoeda = (valor: number | null | undefined): string => {
    if (valor === null || valor === undefined) {
      return 'R$ 0,00';
    }
    return valor.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    });
  };
  
  export const formatarData = (data: string | number[] | null | undefined): string => {
    if (!data) {
      return '-';
    }
    
    // Se for um array [ano, mês, dia]
    if (Array.isArray(data) && data.length === 3) {
      const [ano, mes, dia] = data;
      // Formatação direta sem criar objeto Date para evitar problemas de timezone
      return `${String(dia).padStart(2, '0')}/${String(mes).padStart(2, '0')}/${ano}`;
    }
    
    // Se for uma string no formato YYYY-MM-DD
    if (typeof data === 'string' && data.match(/^\d{4}-\d{2}-\d{2}$/)) {
      const [ano, mes, dia] = data.split('-');
      // Formatação direta sem criar objeto Date para evitar problemas de timezone
      return `${dia}/${mes}/${ano}`;
    }
    
    // Se for uma string ISO ou outro formato
    if (typeof data === 'string') {
      try {
        const dateObj = new Date(data);
        return dateObj.toLocaleDateString('pt-BR');
      } catch (error) {
        return '-';
      }
    }
    
    return '-';
  };