// Teste do backend UTF-8
const http = require('http');

const options = {
    hostname: 'localhost',
    port: 8080,
    path: '/api/test/utf8/test',
    method: 'GET',
    headers: {
        'Accept': 'application/json; charset=utf-8',
        'Content-Type': 'application/json; charset=utf-8'
    }
};

const req = http.request(options, (res) => {
    console.log(`Status: ${res.statusCode}`);
    console.log(`Headers:`, res.headers);
    
    let data = '';
    res.on('data', (chunk) => {
        data += chunk;
    });
    
    res.on('end', () => {
        console.log('Response:', data);
        
        // Verificar se há caracteres Unicode
        if (data.includes('\\u')) {
            console.log('❌ Caracteres Unicode detectados no backend!');
        } else {
            console.log('✅ UTF-8 correto no backend!');
        }
    });
});

req.on('error', (e) => {
    console.error(`Erro: ${e.message}`);
});

req.end(); 