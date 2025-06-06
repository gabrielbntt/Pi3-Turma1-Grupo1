// https://us-central1-superid-23905.cloudfunctions.net/getLoginStatus
// https://us-central1-superid-23905.cloudfunctions.net/performAuth

let loginToken = '';
let intervalId = null;

async function gerarQRCode() {
    clearInterval(intervalId);
    document.getElementById('message').innerText = '';
    document.getElementById('qrcode').innerHTML = 'Gerando QR Code...';

    try {
        const response = await fetch(`https://performauth-numlyzkaiq-uc.a.run.app`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                partnerUrl: 'https://smile.com',
                apiKey: '6970352f-7940-4f17-813b-125090c14fb4'
            })
        });

        const data = await response.json();

        if (response.ok) {
            loginToken = data.loginToken;
            document.getElementById('qrcode').innerHTML = `
            <img src="${data.qrCode}" alt="QR Code para login" />
            <p>Token expira em 1 minuto</p>
          `;

            intervalId = setInterval(verificarStatus, 19000);

            setTimeout(() => {
                clearInterval(intervalId);
                verificarStatus()
                loginToken = '';
                document.getElementById('message').innerText = 'QR Code expirado. Clique para gerar um novo.';
                document.getElementById('qrcode').innerHTML = '';
            }, 60000);
        } else {
            document.getElementById('message').innerText = 'Erro: ' + data.error;
            document.getElementById('qrcode').innerHTML = '';
        }
    } catch (err) {
        document.getElementById('message').innerText = 'Erro ao gerar QR Code.';
        document.getElementById('qrcode').innerHTML = '';
        console.error(err);
    }
};

async function verificarStatus() {
    if (!loginToken) return;

    try {
        const response = await fetch(`https://getloginstatus-numlyzkaiq-uc.a.run.app`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ loginToken })
        });

        const data = await response.json();

        if (response.ok) {
            if (data.status === 'authorized') {
                clearInterval(intervalId);
                loginToken = '';
                document.getElementById('qrcode').innerHTML = '';
                document.getElementById('message').innerText = 'Usuário logado com sucesso!';

                alert('Usuário logado com sucesso! UID: ' + data.uid);
                setTimeout(() => {
                    window.location.href = 'dashboard.html';
                }, 5000);
            }
        } else {
            if (response.status === 410) { // token expirado
                clearInterval(intervalId);
                loginToken = '';
                document.getElementById('qrcode').innerHTML = '';
                document.getElementById('message').innerText = 'QR Code expirado. Clique para gerar um novo.';
            }
        }
    } catch (err) {
        console.error(err);
    }
}