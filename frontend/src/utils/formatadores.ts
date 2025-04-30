export const formatarMoeda = (valor: number | null | undefined): string => {
    if (valor === null || valor === undefined) {
      return 'R$ 0,00';
    }
    return valor.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    });
  };
  
  export const formatarData = (dataIso: string | null | undefined): string => {
    if (!dataIso) {
      return '-';
    }
    const data = new Date(dataIso);
    return data.toLocaleDateString('pt-BR');
  };