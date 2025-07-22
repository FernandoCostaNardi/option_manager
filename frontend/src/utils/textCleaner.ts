/**
 * Função para limpar caracteres especiais que podem vir codificados do backend
 */
export function cleanSpecialCharacters(text: string): string {
  if (!text) return text;
  
  console.log('🧹 Limpando texto:', text);
  
  // Primeiro, tentar decodificar caracteres Unicode escapados
  let cleaned = text;
  
  // Decodificar caracteres Unicode escapados (formato \u00e3)
  cleaned = cleaned.replace(/\\u([0-9a-fA-F]{4})/g, (match, hex) => {
    const char = String.fromCharCode(parseInt(hex, 16));
    console.log(`🔄 Decodificando ${match} -> ${char}`);
    return char;
  });
  
  // Se o texto ainda contém caracteres Unicode não escapados, tentar decodificar
  try {
    // Tentar decodificar como JSON string
    if (cleaned.includes('\\u')) {
      cleaned = JSON.parse('"' + cleaned + '"');
      console.log('🔄 Decodificado via JSON.parse:', cleaned);
    }
  } catch (e) {
    console.log('⚠️ Não foi possível decodificar via JSON.parse');
  }
  
  // Substituições específicas para caracteres problemáticos
  cleaned = cleaned
    .replace(/\\u00ed/g, 'í') // í
    .replace(/\\u00e7/g, 'ç') // ç
    .replace(/\\u00f5/g, 'õ') // õ
    .replace(/\\u00e3/g, 'ã') // ã
    .replace(/\\u00e1/g, 'á') // á
    .replace(/\\u00e9/g, 'é') // é
    .replace(/\\u00f3/g, 'ó') // ó
    .replace(/\\u00fa/g, 'ú') // ú
    .replace(/\\u00e0/g, 'à') // à
    .replace(/\\u00e8/g, 'è') // è
    .replace(/\\u00ec/g, 'ì') // ì
    .replace(/\\u00f2/g, 'ò') // ò
    .replace(/\\u00f9/g, 'ù') // ù
    .replace(/\\u00c7/g, 'Ç') // Ç
    .replace(/\\u00d5/g, 'Õ') // Õ
    .replace(/\\u00c3/g, 'Ã') // Ã
    .replace(/\\u00c1/g, 'Á') // Á
    .replace(/\\u00c9/g, 'É') // É
    .replace(/\\u00d3/g, 'Ó') // Ó
    .replace(/\\u00da/g, 'Ú') // Ú
    .replace(/\\u00c0/g, 'À') // À
    .replace(/\\u00c8/g, 'È') // È
    .replace(/\\u00cc/g, 'Ì') // Ì
    .replace(/\\u00d2/g, 'Ò') // Ò
    .replace(/\\u00d9/g, 'Ù') // Ù
    .replace(/\\u00e2/g, 'â') // â
    .replace(/\\u00ea/g, 'ê') // ê
    .replace(/\\u00ee/g, 'î') // î
    .replace(/\\u00f4/g, 'ô') // ô
    .replace(/\\u00fb/g, 'û') // û
    .replace(/\\u00c2/g, 'Â') // Â
    .replace(/\\u00ca/g, 'Ê') // Ê
    .replace(/\\u00ce/g, 'Î') // Î
    .replace(/\\u00d4/g, 'Ô') // Ô
    .replace(/\\u00db/g, 'Û'); // Û
  
  // Remover o símbolo ? no início das mensagens (se vier do backend)
  cleaned = cleaned.replace(/^\?+\s*/, '');
  
  // Substituições específicas para problemas conhecidos
  cleaned = cleaned
    .replace(/conclu do/g, 'concluído')
    .replace(/opera es/g, 'operações')
    .replace(/processamento/g, 'processamento')
    .replace(/criadas/g, 'criadas');
  
  console.log('🧹 Texto limpo:', cleaned);
  
  return cleaned;
}

/**
 * Função específica para decodificar UTF-8 corretamente
 */
export function decodeUTF8(text: string): string {
  if (!text) return text;
  
  console.log('🔤 Decodificando UTF-8:', text);
  
  try {
    // Tentar decodificar usando decodeURIComponent
    const decoded = decodeURIComponent(escape(text));
    console.log('🔤 Decodificado via decodeURIComponent:', decoded);
    return decoded;
  } catch (e) {
    console.log('⚠️ Erro ao decodificar UTF-8, tentando método alternativo');
    
    try {
      // Método alternativo usando TextDecoder
      const encoder = new TextEncoder();
      const decoder = new TextDecoder('utf-8');
      const bytes = encoder.encode(text);
      const decoded = decoder.decode(bytes);
      console.log('🔤 Decodificado via TextDecoder:', decoded);
      return decoded;
    } catch (e2) {
      console.log('⚠️ Erro no TextDecoder, retornando texto original');
      return text;
    }
  }
} 