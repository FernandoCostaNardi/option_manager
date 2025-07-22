// Teste da API de estimativa melhorada
const http = require('http');

// Dados de teste com diferentes invoices
const testCases = [
    {
        name: "Teste 1 - Invoice simples",
        invoiceIds: ["eb003f95-0719-4f5b-a8c6-c021e65d27d8"] // Invoice com poucos items
    },
    {
        name: "Teste 2 - Múltiplas invoices",
        invoiceIds: ["eb003f95-0719-4f5b-a8c6-c021e65d27d8", "eb003f95-0719-4f5b-a8c6-c021e65d27d8"] // Mesma invoice duas vezes
    },
    {
        name: "Teste 3 - Invoice inexistente",
        invoiceIds: ["00000000-0000-0000-0000-000000000000"] // UUID inválido
    }
];

function testEstimateAPI(testCase) {
    return new Promise((resolve, reject) => {
        const postData = JSON.stringify({
            invoiceIds: testCase.invoiceIds
        });

        const options = {
            hostname: 'localhost',
            port: 8080,
            path: '/api/processing/estimate',
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(postData),
                'Authorization': 'Bearer test-token' // Token de teste
            }
        };

        const req = http.request(options, (res) => {
            console.log(`\n🧪 ${testCase.name}`);
            console.log(`Status: ${res.statusCode}`);
            console.log(`Headers:`, res.headers);

            let data = '';
            res.on('data', (chunk) => {
                data += chunk;
            });

            res.on('end', () => {
                try {
                    const response = JSON.parse(data);
                    console.log('Response:', JSON.stringify(response, null, 2));
                    
                    if (response.success) {
                        console.log(`✅ ${testCase.name} - SUCESSO`);
                        console.log(`   📊 Invoices: ${response.totalInvoices}`);
                        console.log(`   📋 Items: ${response.totalItems || 'N/A'}`);
                        console.log(`   🔄 Operações: ${response.estimatedOperations}`);
                        console.log(`   ⏱️ Tempo: ${response.estimatedTimeFormatted}`);
                        console.log(`   🎯 Complexidade: ${response.complexity}`);
                        console.log(`   💬 Mensagem: ${response.message}`);
                    } else {
                        console.log(`❌ ${testCase.name} - ERRO: ${response.error}`);
                    }
                } catch (e) {
                    console.log(`❌ ${testCase.name} - Erro ao parsear JSON:`, e.message);
                    console.log('Raw response:', data);
                }
                resolve();
            });
        });

        req.on('error', (e) => {
            console.error(`❌ ${testCase.name} - Erro na requisição: ${e.message}`);
            reject(e);
        });

        req.write(postData);
        req.end();
    });
}

async function runTests() {
    console.log('🚀 Iniciando testes da API de estimativa melhorada...\n');
    
    for (const testCase of testCases) {
        try {
            await testEstimateAPI(testCase);
            // Aguardar um pouco entre os testes
            await new Promise(resolve => setTimeout(resolve, 1000));
        } catch (error) {
            console.error(`❌ Erro no teste ${testCase.name}:`, error.message);
        }
    }
    
    console.log('\n✅ Testes concluídos!');
}

// Executar os testes
runTests().catch(console.error); 