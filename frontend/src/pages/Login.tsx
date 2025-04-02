import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export function Login() {
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    
    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ 
          email: formData.email, 
          password: formData.password 
        }),
      });

      if (response.ok) {
        const data = await response.json();
        login(data.token);
        navigate('/');
      } else {
        alert('Credenciais inválidas');
      }
    } catch (error) {
      console.error('Erro ao fazer login:', error);
      alert('Erro ao tentar fazer login');
    } finally {
      setIsLoading(false);
    }
  };

  // CSS estilizado com a paleta de cores enviada
  const styles = `
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    }
    
    .login-page {
      min-height: 100vh;
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      background-color: #f1f5f9;
      background-image: linear-gradient(135deg, #f8fafc, #f1f5f9);
    }
    
    .login-container {
      display: flex;
      width: 100%;
      max-width: 1000px;
      height: 600px;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
    }
    
    .login-image-side {
      width: 50%;
      position: relative;
      overflow: hidden;
      background-color: #2d2a5d;
      background-image: linear-gradient(135deg, #514EE7, #6E65ED);
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      padding: 2.5rem;
    }
    
    .chart-container {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-size: cover;
      background-position: center;
      opacity: 0.25;
    }
    
    @media (max-width: 768px) {
      .login-container {
        height: auto;
        max-width: 500px;
        flex-direction: column;
      }
      
      .login-image-side {
        width: 100%;
        min-height: 200px;
        padding: 1.5rem;
      }
      
      .login-form-side {
        width: 100%;
      }
    }
    
    .login-image-title {
      color: white;
      font-size: 1.75rem;
      font-weight: 700;
      margin-bottom: 0.75rem;
      position: relative;
      z-index: 1;
    }
    
    .login-image-subtitle {
      color: rgba(255, 255, 255, 0.9);
      font-size: 0.9rem;
      font-weight: 400;
      max-width: 350px;
      line-height: 1.6;
      margin-bottom: 1rem;
      position: relative;
      z-index: 1;
    }
    
    .login-form-side {
      width: 50%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      background-color: white;
      padding: 2.5rem;
    }
    
    .login-form-content {
      width: 100%;
      max-width: 360px;
      margin: 0 auto;
    }
    
    .login-logo {
      color: #514EE7;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.1em;
      margin-bottom: 1.5rem;
    }
    
    .login-heading {
      color: #2d2a5d;
      font-size: 1.75rem;
      font-weight: 800;
      line-height: 1.2;
      margin-bottom: 0.5rem;
      letter-spacing: -0.025em;
    }
    
    .login-subheading {
      color: #6b7280;
      font-size: 0.875rem;
      line-height: 1.5;
      margin-bottom: 1.5rem;
    }
    
    .login-feature-list {
      margin-bottom: 1.5rem;
      list-style: none;
    }
    
    .login-feature-item {
      display: flex;
      align-items: flex-start;
      margin-bottom: 0.5rem;
      color: #4b5563;
      font-size: 0.875rem;
    }
    
    .login-feature-icon {
      color: #8A7CF3;
      font-size: 1.25rem;
      margin-right: 0.5rem;
      line-height: 1;
    }
    
    .login-form {
      width: 100%;
    }
    
    .login-form-group {
      margin-bottom: 1rem;
    }
    
    .login-label {
      display: block;
      font-size: 0.75rem;
      font-weight: 600;
      color: #4b5563;
      margin-bottom: 0.375rem;
    }
    
    .login-input {
      width: 100%;
      padding: 0.625rem 0.875rem;
      border: 1px solid #e5e7eb;
      border-radius: 0.375rem;
      font-size: 0.875rem;
      transition: all 0.2s ease;
      background-color: white;
    }
    
    .login-input:focus {
      outline: none;
      border-color: #8A7CF3;
      box-shadow: 0 0 0 3px rgba(138, 124, 243, 0.25);
    }
    
    .login-password-container {
      position: relative;
    }
    
    .login-password-toggle {
      position: absolute;
      right: 0.75rem;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      cursor: pointer;
      color: #9ca3af;
      font-size: 0.875rem;
    }
    
    .login-password-toggle:hover {
      color: #6b7280;
    }
    
    .login-checkbox-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.25rem;
      font-size: 0.75rem;
    }
    
    .login-remember-me {
      display: flex;
      align-items: center;
    }
    
    .login-checkbox {
      width: 0.875rem;
      height: 0.875rem;
      margin-right: 0.375rem;
      accent-color: #514EE7;
    }
    
    .login-checkbox-label {
      font-size: 0.75rem;
      color: #6b7280;
    }
    
    .login-forgot-password {
      font-size: 0.75rem;
      color: #6E65ED;
      text-decoration: none;
      font-weight: 500;
      transition: all 0.2s ease;
    }
    
    .login-forgot-password:hover {
      color: #514EE7;
      text-decoration: underline;
    }
    
    .login-button {
      width: 100%;
      padding: 0.625rem 0.875rem;
      background: #514EE7;
      color: white;
      border: none;
      border-radius: 0.375rem;
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      box-shadow: 0 1px 3px rgba(81, 78, 231, 0.25);
    }
    
    .login-button:hover {
      background: #6E65ED;
      box-shadow: 0 2px 4px rgba(81, 78, 231, 0.3);
      transform: translateY(-1px);
    }
    
    .login-button:active {
      background: #514EE7;
      transform: translateY(0);
    }
    
    .login-button:disabled {
      background: #A793F9;
      cursor: not-allowed;
      box-shadow: none;
      transform: none;
    }
    
    .login-loading {
      display: inline-block;
      width: 0.875rem;
      height: 0.875rem;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-radius: 50%;
      border-top-color: white;
      animation: spin 1s ease-in-out infinite;
      margin-right: 0.375rem;
    }
    
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    
    .login-signup {
      margin-top: 1.5rem;
      text-align: center;
      font-size: 0.75rem;
      color: #6b7280;
    }
    
    .login-signup-link {
      color: #6E65ED;
      text-decoration: none;
      font-weight: 600;
      transition: all 0.2s ease;
    }
    
    .login-signup-link:hover {
      color: #514EE7;
      text-decoration: underline;
    }
    
    .login-dots {
      position: absolute;
      top: 1.5rem;
      right: 1.5rem;
      display: flex;
      gap: 0.5rem;
    }
    
    .login-dot {
      width: 0.5rem;
      height: 0.5rem;
      border-radius: 50%;
      background-color: rgba(255, 255, 255, 0.5);
    }
    
    .login-chart-lines {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      height: 60%;
      opacity: 0.2;
      z-index: 0;
      stroke: white;
      stroke-width: 1;
    }
  `;

  return (
    <>
      <style>{styles}</style>
      <div className="login-page">
        <div className="login-container">
          {/* Lado esquerdo - Imagem e texto */}
          <div className="login-image-side">
            <div className="chart-container">
              {/* SVG simples de gráfico de candlestick para o fundo em tons de cinza */}
              <svg viewBox="0 0 500 300" width="100%" height="100%" style={{position: 'absolute', top: 0, left: 0}}>
                <g fill="none" stroke="white" strokeWidth="2" opacity="0.4">
                  <path d="M50,250 L450,250" /> {/* linha base */}
                  <path d="M50,200 L450,200" /> {/* linha horizontal */}
                  <path d="M50,150 L450,150" /> {/* linha horizontal */}
                  <path d="M50,100 L450,100" /> {/* linha horizontal */}
                  <path d="M50,50 L450,50" /> {/* linha horizontal */}
                  
                  {/* Candlesticks em tons de cinza */}
                  <line x1="80" y1="80" x2="80" y2="220" stroke="white" />
                  <rect x="70" y="130" width="20" height="90" fill="white" opacity="0.7" />
                  
                  <line x1="120" y1="60" x2="120" y2="200" stroke="white" />
                  <rect x="110" y="60" width="20" height="70" fill="white" opacity="0.6" />
                  
                  <line x1="160" y1="100" x2="160" y2="240" stroke="white" />
                  <rect x="150" y="170" width="20" height="70" fill="white" opacity="0.8" />
                  
                  <line x1="200" y1="40" x2="200" y2="180" stroke="white" />
                  <rect x="190" y="40" width="20" height="60" fill="white" opacity="0.7" />
                  
                  <line x1="240" y1="120" x2="240" y2="220" stroke="white" />
                  <rect x="230" y="120" width="20" height="40" fill="white" opacity="0.6" />
                  
                  <line x1="280" y1="90" x2="280" y2="190" stroke="white" />
                  <rect x="270" y="120" width="20" height="70" fill="white" opacity="0.8" />
                  
                  <line x1="320" y1="70" x2="320" y2="190" stroke="white" />
                  <rect x="310" y="70" width="20" height="50" fill="white" opacity="0.7" />
                  
                  <line x1="360" y1="40" x2="360" y2="150" stroke="white" />
                  <rect x="350" y="40" width="20" height="40" fill="white" opacity="0.6" />
                  
                  <line x1="400" y1="100" x2="400" y2="220" stroke="white" />
                  <rect x="390" y="150" width="20" height="70" fill="white" opacity="0.8" />
                  
                  {/* Curva para simular uma linha de tendência */}
                  <path d="M50,200 C150,180 250,120 450,80" stroke="white" strokeWidth="3" opacity="0.8" />
                </g>
              </svg>
            </div>
            
            <div className="login-dots">
              <div className="login-dot"></div>
              <div className="login-dot"></div>
              <div className="login-dot"></div>
            </div>
            
            <h1 className="login-image-title">Olisystem Trading</h1>
            <p className="login-image-subtitle">
              Ferramentas avançadas de análise e acompanhamento para o seu sucesso no mercado de opções.
            </p>
          </div>
          
          {/* Lado direito - Formulário */}
          <div className="login-form-side">
            <div className="login-form-content">
              <div className="login-logo">OLISYSTEM</div>
              
              <h1 className="login-heading">Olá, bem-vindo!</h1>
              <p className="login-subheading">
                Acesse a melhor plataforma para gerenciamento de opções financeiras e investimentos.
              </p>
              
              <ul className="login-feature-list">
                <li className="login-feature-item">
                  <span className="login-feature-icon">•</span>
                  <span>Análise avançada de mercados de opções</span>
                </li>
                <li className="login-feature-item">
                  <span className="login-feature-icon">•</span>
                  <span>Gestão completa do seu portfólio</span>
                </li>
                <li className="login-feature-item">
                  <span className="login-feature-icon">•</span>
                  <span>Alertas em tempo real e notificações</span>
                </li>
              </ul>
              
              <form className="login-form" onSubmit={handleSubmit}>
                <div className="login-form-group">
                  <label htmlFor="email" className="login-label">Email</label>
                  <input
                    type="email"
                    id="email"
                    placeholder="Digite seu email"
                    className="login-input"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    required
                  />
                </div>
                
                <div className="login-form-group">
                  <label htmlFor="password" className="login-label">Senha</label>
                  <div className="login-password-container">
                    <input
                      type={showPassword ? "text" : "password"}
                      id="password"
                      placeholder="Digite sua senha"
                      className="login-input"
                      value={formData.password}
                      onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="login-password-toggle"
                    >
                      {showPassword ? "👁️" : "👁️‍🗨️"}
                    </button>
                  </div>
                </div>
                
                <div className="login-checkbox-row">
                  <div className="login-remember-me">
                    <input
                      type="checkbox"
                      id="remember-me"
                      className="login-checkbox"
                    />
                    <label htmlFor="remember-me" className="login-checkbox-label">
                      Remember me
                    </label>
                  </div>
                  <a href="#" className="login-forgot-password">
                    Forgot password?
                  </a>
                </div>
                
                <button
                  type="submit"
                  className="login-button"
                  disabled={isLoading}
                >
                  {isLoading ? (
                    <>
                      <span className="login-loading"></span>
                      <span>Entrando...</span>
                    </>
                  ) : "Entrar"}
                </button>
                
                <div className="login-signup">
                  <p>
                    Não tem uma conta?{' '}
                    <Link to="/register" className="login-signup-link">
                      Cadastre-se agora
                    </Link>
                  </p>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
