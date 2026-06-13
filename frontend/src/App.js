import React, { useState } from 'react';
import { queryBackend } from './services/api.js';
import './App.css';

const sampleQuestions = [
  "How many eggs did I collect last week?",
  "What were my total expenses last month?",
  "Show milk production this month.",
  "Which animals need attention?"
];

const App = () => {
  const [question, setQuestion] = useState('');
  const [sql, setSql] = useState('');
  const [answer, setAnswer] = useState('');
  const [mode, setMode] = useState('');
  const [columns, setColumns] = useState([]);
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleAsk = async () => {
    if (!question.trim()) return;
    setLoading(true);
    setError('');
    setSql('');
    setAnswer('');
    setMode('');
    setColumns([]);
    setRows([]);
    try {
      const data = await queryBackend(question);
      setSql(data.sql ?? '');
      setAnswer(data.answer ?? '');
      setMode(data.mode ?? '');
      setColumns(data.columns ?? []);
      setRows(data.rows ?? []);
    } catch (e) {
      setError(e.message || 'Request error');
    }
    setLoading(false);
  };

  const handleChipClick = (text) => {
    setQuestion(text);
  };

  const isTableResult = rows && rows.length > 0 && (rows.length > 1 || columns.length > 1);

  return (
    <div className="app-container">
      <h1>Farm Manager AI</h1>
      <div className="subtitle">Natural language farm insights powered by structured farm data</div>

      <div className="chips">
        {sampleQuestions.map((q, idx) => (
          <span key={idx} className="chip" onClick={() => handleChipClick(q)}>{q}</span>
        ))}
      </div>

      <input
        type="text"
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        placeholder="Ask a farm question..."
        className="question-input"
      />
      <button disabled={loading} onClick={handleAsk} className="ask-button">Ask</button>

      {loading && <div className="loading-label">Processing question...</div>}

      {error && (
        <div className="error-box" role="alert">
          <p>{error}</p>
        </div>
      )}

      {sql && (
        <div className="card sql-card">
          <h2>Generated SQL:</h2>
          <code>{sql}</code>
        </div>
      )}

      {answer && !isTableResult && (
        <div className="card answer-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <h2 style={{ margin: 0, fontSize: '1.2rem', color: '#0f6354' }}>Answer:</h2>
            {mode && (
              <span className={`mode-badge mode-${mode}`}>
                {mode === 'ai' ? 'AI Mode' : 'Template Mode'}
              </span>
            )}
          </div>
          <p style={{ margin: 0 }}>{answer}</p>
        </div>
      )}

      {isTableResult && (
        <div className="card table-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.8rem' }}>
            <h2 style={{ margin: 0, fontSize: '1.2rem', color: '#333' }}>Results:</h2>
            {mode && (
              <span className={`mode-badge mode-${mode}`}>
                {mode === 'ai' ? 'AI Mode' : 'Template Mode'}
              </span>
            )}
          </div>
          <div style={{ overflowX: 'auto' }}>
            <table className="results-table">
              <thead>
                <tr>
                  {columns.map((col, idx) => (
                    <th key={idx}>{col}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((row, rowIdx) => (
                  <tr key={rowIdx}>
                    {columns.map((col, colIdx) => (
                      <td key={colIdx}>
                        {row[col] !== null && row[col] !== undefined ? row[col].toString() : 'NULL'}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default App;
