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
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleAsk = async () => {
    if (!question.trim()) return;
    setLoading(true);
    setError('');
    try {
      const data = await queryBackend(question);
      setSql(data.sql ?? '');
      setAnswer(data.answer ?? '');
    } catch (e) {
      setError(e.message || 'Request error');
    }
    setLoading(false);
  };

  const handleChipClick = (text) => {
    setQuestion(text);
  };

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

      {answer && (
        <div className="card answer-card">
          <h2>Answer:</h2>
          <p>{answer}</p>
        </div>
      )}
    </div>
  );
};

export default App;
