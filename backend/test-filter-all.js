// Teste do filtro ALL para invoices
const http = require('http');

// Dados de teste para diferentes filtros
const testCases = [
    {
        name: "Teste 1 - Filtro ALL (todas as invoices)",
        url: "/api/invoices-v2?page=0&size=10&processingStatus=ALL"
    },
    {
        name: "Teste 2 - Filtro PENDING (apenas pendentes)",
        url: "/api/invoices-v2?page=0&size=10&processingStatus=PENDING"
    },
    {
        name: "Teste 3 - Filtro SUCCESS (apenas processadas com sucesso)",
        url: "/api/invoices-v2?page=0&size=10&processingStatus=SUCCESS"
    },
    {
        name: "Teste 4 - Sem filtro (deve retornar todas)",
        url: "/api/invoices-v2?page=0&size=10"
    }
];

function testFilter(testCase) {
    return new Promise((resolve, reject) => {
        const options = {
            hostname: 'localhost',
            port: 8080,
            path: testCase.url,
            method: 'GET',
            headers: {
                'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmZXJuYW5kb0BnbWFpbC5jb20iLCJpYXQiOjE3MzE5NzI4MDAsImV4cCI6MTczMTk3NjQwMH0.test-token',
                'Content-Type': 'application/json'
            }
        };

        const req = http.request(options, (res) => {
            console.log(`\n🔍 ${testCase.name}`);
            console.log(`📡 Status: ${res.statusCode}`);
            console.log(`🔗 URL: ${testCase.url}`);

            let data = '';
            res.on('data', (chunk) => {
                data += chunk;
            });

            res.on('end', () => {
                try {
                    const response = JSON.parse(data);
                    console.log(`✅ Total de invoices: ${response.totalElements || 0}`);
                    console.log(`📄 Página atual: ${response.number || 0}`);
                    console.log(`📋 Tamanho da página: ${response.size || 0}`);
                    console.log(`📊 Total de páginas: ${response.totalPages || 0}`);
                    
                    if (response.content && response.content.length > 0) {
                        console.log(`📝 Primeira invoice: ${response.content[0].invoiceNumber || 'N/A'}`);
                    }
                    
                    resolve(response);
                } catch (e) {
                    console.log(`❌ Erro ao parsear resposta: ${e.message}`);
                    console.log(`📄 Resposta bruta: ${data}`);
                    reject(e);
                }
            });
        });

        req.on('error', (e) => {
            console.log(`❌ Erro na requisição: ${e.message}`);
            reject(e);
        });

        req.end();
    });
}

async function runTests() {
    console.log('🚀 Iniciando testes do filtro ALL...\n');
    
    for (const testCase of testCases) {
        try {
            await testFilter(testCase);
            console.log('✅ Teste concluído\n');
        } catch (error) {
            console.log(`❌ Teste falhou: ${error.message}\n`);
        }
        
        // Aguardar um pouco entre os testes
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
    
    console.log('🎉 Todos os testes concluídos!');
}

runTests(); 