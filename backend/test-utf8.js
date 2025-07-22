// Teste de UTF-8
const testData = {
    "type": "PROCESSING",
    "message": "Processando operação 1 de 7...",
    "current": 1,
    "total": 7,
    "status": "PROCESSING",
    "invoiceId": "eb003f95-0719-4f5b-a8c6-c021e65d27d8",
    "invoiceNumber": "74803590",
    "timestamp": "2025-07-21 15:05:21",
    "percentage": 14,
    "details": null
};

console.log("Teste UTF-8:");
console.log(JSON.stringify(testData, null, 2));

// Verificar se há caracteres Unicode
const jsonString = JSON.stringify(testData);
if (jsonString.includes("\\u")) {
    console.log("❌ Caracteres Unicode detectados!");
} else {
    console.log("✅ UTF-8 correto!");
} 