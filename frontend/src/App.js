import React, { useState, useEffect } from 'react';
import { 
  queryBackend, 
  getTelemetry, 
  resetTelemetry, 
  getGoals, 
  addGoal, 
  getBriefing, 
  regenerateBriefing,
  getGraphNodes,
  queryGraph,
  getGraphAffected,
  getApprovedNotes,
  getApprovedNoteContent,
  deleteApprovedNote,
  getPendingNotes,
  deletePendingNote,
  lintPendingNote,
  approvePendingNote,
  queryKnowledgeBase,
  getFarmSoul,
  saveFarmSoul,
  getAgentTasks,
  runAgentTask,
  getAvailableModels,
  getModelRouting,
  updateModelRouting,
  getModelCosts
} from './services/api.js';
import './App.css';

const App = () => {
  // Tabs State
  const [activeTab, setActiveTab] = useState('operations');

  // Operations state
  const [question, setQuestion] = useState('');
  const [sql, setSql] = useState('');
  const [answer, setAnswer] = useState('');
  const [mode, setMode] = useState('');
  const [columns, setColumns] = useState([]);
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [telemetry, setTelemetry] = useState({ aiSuccessCount: 0, templateFallbackCount: 0, sqlFailureCount: 0, accumulatedCost: "0.00000" });
  const [goals, setGoals] = useState([]);
  const [briefing, setBriefing] = useState('');
  const [briefingLoading, setBriefingLoading] = useState(false);

  // Goal Form State
  const [newGoalName, setNewGoalName] = useState('');
  const [newGoalTarget, setNewGoalTarget] = useState('');
  const [newGoalQuery, setNewGoalQuery] = useState('');
  const [newGoalDate, setNewGoalDate] = useState('');

  // Code Graph State
  const [graphNodes, setGraphNodes] = useState([]);
  const [searchNodeTerm, setSearchNodeTerm] = useState('');
  const [bfsQuestion, setBfsQuestion] = useState('');
  const [bfsResult, setBfsResult] = useState('');
  const [bfsLoading, setBfsLoading] = useState(false);
  const [selectedImpactNode, setSelectedImpactNode] = useState('');
  const [impactResult, setImpactResult] = useState('');
  const [impactLoading, setImpactLoading] = useState(false);

  // Knowledge Base State
  const [approvedNotes, setApprovedNotes] = useState([]);
  const [selectedNote, setSelectedNote] = useState(null);
  const [selectedNoteContent, setSelectedNoteContent] = useState('');
  const [pendingNotes, setPendingNotes] = useState([]);
  const [selectedPendingNote, setSelectedPendingNote] = useState(null);
  const [pendingNoteTitle, setPendingNoteTitle] = useState('');
  const [pendingNoteContent, setPendingNoteContent] = useState('');
  const [lintedContent, setLintedContent] = useState('');
  const [kbQuery, setKbQuery] = useState('');
  const [kbAnswer, setKbAnswer] = useState('');
  const [kbSources, setKbSources] = useState('');
  const [kbLoading, setKbLoading] = useState(false);
  const [kbActionLoading, setKbActionLoading] = useState(false);

  // Agentic OS State
  const [farmSoul, setFarmSoul] = useState('');
  const [soulEditing, setSoulEditing] = useState(false);
  const [agentTasks, setAgentTasks] = useState([]);
  const [agentLoading, setAgentLoading] = useState(false);

  // Model Intel & Telemetry State
  const [availableModels, setAvailableModelsState] = useState([]);
  const [modelRouting, setModelRoutingState] = useState({});
  const [costReport, setCostReportState] = useState({ totalCost: 0.0, totalTokens: 0, byTask: [], byModel: [], history: [] });
  const [imagePath, setImagePath] = useState('');

  useEffect(() => {
    loadAllData();
  }, []);

  const loadKbData = async () => {
    try {
      const appNotes = await getApprovedNotes();
      setApprovedNotes(appNotes);
      const pendNotes = await getPendingNotes();
      setPendingNotes(pendNotes);
      if (pendNotes.length > 0 && !selectedPendingNote) {
        setSelectedPendingNote(pendNotes[0]);
        setPendingNoteContent(pendNotes[0].content);
        // Extract a clean title suggestion from the filename
        const cleanTitle = pendNotes[0].filename
          .replace(".txt", "")
          .replace(".md", "")
          .replace(/_/g, " ")
          .replace(/-/g, " ")
          .trim();
        setPendingNoteTitle(cleanTitle.charAt(0).toUpperCase() + cleanTitle.slice(1));
      }

      // Fetch Farm Soul Profile & Tasks
      const soulRes = await getFarmSoul();
      setFarmSoul(soulRes.content || '');
      const taskRes = await getAgentTasks();
      setAgentTasks(taskRes);
    } catch (e) {
      console.error("Failed to load KB data", e);
    }
  };

  const loadCostData = async () => {
    try {
      const models = await getAvailableModels();
      setAvailableModelsState(models);
      const routing = await getModelRouting();
      setModelRoutingState(routing);
      const costs = await getModelCosts();
      setCostReportState(costs);
    } catch (e) {
      console.error("Failed to load model and cost telemetry", e);
    }
  };

  const handleUpdateRouting = async (taskName, modelName) => {
    try {
      await updateModelRouting(taskName, modelName);
      const routing = await getModelRouting();
      setModelRoutingState(routing);
    } catch (e) {
      alert("Failed to update model routing: " + e.message);
    }
  };

  const loadAllData = async () => {
    try {
      const telData = await getTelemetry();
      setTelemetry(telData);

      const goalsData = await getGoals();
      setGoals(goalsData);

      const briefingData = await getBriefing();
      setBriefing(briefingData.briefing || '');

      const nodesData = await getGraphNodes();
      setGraphNodes(nodesData);
      if (nodesData.length > 0) {
        setSelectedImpactNode(nodesData[0]);
      }

      await loadKbData();
      await loadCostData();
    } catch (e) {
      console.error("Failed to load initial data", e);
    }
  };

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

      // Refresh telemetry and goals metrics
      const telData = await getTelemetry();
      setTelemetry(telData);
      const goalsData = await getGoals();
      setGoals(goalsData);
    } catch (e) {
      setError(e.message || 'Request error');
      // Refresh telemetry even on error
      const telData = await getTelemetry();
      setTelemetry(telData);
    }
    setLoading(false);
  };

  const handleResetTelemetry = async () => {
    if (window.confirm("Are you sure you want to reset telemetry and spend metrics?")) {
      try {
        await resetTelemetry();
        const telData = await getTelemetry();
        setTelemetry(telData);
      } catch (e) {
        console.error("Reset telemetry failed", e);
      }
    }
  };

  const handleRegenerateBriefing = async () => {
    setBriefingLoading(true);
    try {
      const briefingData = await regenerateBriefing();
      setBriefing(briefingData.briefing || '');
    } catch (e) {
      console.error("Briefing regeneration failed", e);
    }
    setBriefingLoading(false);
  };

  const handleAddGoal = async (e) => {
    e.preventDefault();
    if (!newGoalName || !newGoalTarget || !newGoalQuery || !newGoalDate) {
      alert("Please fill in all goal fields.");
      return;
    }
    try {
      await addGoal({
        name: newGoalName,
        targetValue: parseFloat(newGoalTarget),
        sqlMetricQuery: newGoalQuery,
        targetDate: newGoalDate
      });
      // Clear form
      setNewGoalName('');
      setNewGoalTarget('');
      setNewGoalQuery('');
      setNewGoalDate('');
      // Reload goals
      const goalsData = await getGoals();
      setGoals(goalsData);
    } catch (e) {
      alert("Failed to add goal: " + e.message);
    }
  };

  // --- Graph Handlers ---

  const handleBfsSearch = async () => {
    if (!bfsQuestion.trim()) return;
    setBfsLoading(true);
    setBfsResult('');
    try {
      const res = await queryGraph(bfsQuestion);
      setBfsResult(res.result || 'No traversal path found.');
    } catch (e) {
      setBfsResult("Error: " + e.message);
    }
    setBfsLoading(false);
  };

  const handleImpactCheck = async () => {
    if (!selectedImpactNode) return;
    setImpactLoading(true);
    setImpactResult('');
    try {
      const res = await getGraphAffected(selectedImpactNode);
      setImpactResult(res.result || 'No affected dependencies found.');
    } catch (e) {
      setImpactResult("Error: " + e.message);
    }
    setImpactLoading(false);
  };

  const handleNodeClick = (node) => {
    setSelectedImpactNode(node);
  };

  // --- Knowledge Base Handlers ---

  const handleSelectNote = async (note) => {
    setSelectedNote(note);
    setSelectedNoteContent('Loading...');
    try {
      const res = await getApprovedNoteContent(note.filename);
      setSelectedNoteContent(res.content || '');
    } catch (e) {
      setSelectedNoteContent('Error loading note: ' + e.message);
    }
  };

  const handleDeleteNote = async (filename) => {
    if (window.confirm(`Are you sure you want to delete "${filename}"?`)) {
      try {
        await deleteApprovedNote(filename);
        if (selectedNote && selectedNote.filename === filename) {
          setSelectedNote(null);
          setSelectedNoteContent('');
        }
        await loadKbData();
      } catch (e) {
        alert("Failed to delete note: " + e.message);
      }
    }
  };

  const handleSelectPendingNote = (note) => {
    setSelectedPendingNote(note);
    setPendingNoteContent(note.content);
    setLintedContent('');
    const cleanTitle = note.filename
      .replace(".txt", "")
      .replace(".md", "")
      .replace(/_/g, " ")
      .replace(/-/g, " ")
      .trim();
    setPendingNoteTitle(cleanTitle.charAt(0).toUpperCase() + cleanTitle.slice(1));
  };

  const handleDeletePendingNote = async (filename) => {
    if (window.confirm(`Are you sure you want to delete pending file "${filename}"?`)) {
      try {
        await deletePendingNote(filename);
        if (selectedPendingNote && selectedPendingNote.filename === filename) {
          setSelectedPendingNote(null);
          setPendingNoteContent('');
          setPendingNoteTitle('');
          setLintedContent('');
        }
        await loadKbData();
      } catch (e) {
        alert("Failed to delete pending file: " + e.message);
      }
    }
  };

  const handleLintNote = async () => {
    if (!selectedPendingNote) return;
    setKbActionLoading(true);
    setLintedContent('');
    try {
      const res = await lintPendingNote(selectedPendingNote.filename);
      setLintedContent(res.content || '');
      // Try to parse out the title from '# Title' header
      const titleMatch = res.content.match(/^#\s+(.+)$/m);
      if (titleMatch && titleMatch[1]) {
        setPendingNoteTitle(titleMatch[1].trim());
      }
    } catch (e) {
      alert("Linting failed: " + e.message);
    }
    setKbActionLoading(false);
  };

  const handleApproveNote = async () => {
    if (!selectedPendingNote) return;
    const finalContent = lintedContent || pendingNoteContent;
    if (!finalContent.trim()) {
      alert("Note content cannot be empty.");
      return;
    }
    if (!pendingNoteTitle.trim()) {
      alert("Please specify a note title.");
      return;
    }
    setKbActionLoading(true);
    try {
      await approvePendingNote(selectedPendingNote.filename, pendingNoteTitle, finalContent);
      setSelectedPendingNote(null);
      setPendingNoteContent('');
      setPendingNoteTitle('');
      setLintedContent('');
      
      await loadKbData();
      alert("Note successfully approved and saved to the farm wiki vault!");
      const telData = await getTelemetry();
      setTelemetry(telData);
    } catch (e) {
      alert("Approval failed: " + e.message);
    }
    setKbActionLoading(false);
  };

  const handleKbSearch = async () => {
    if (!kbQuery.trim()) return;
    setKbLoading(true);
    setKbAnswer('');
    setKbSources('');
    try {
      const res = await queryKnowledgeBase(kbQuery);
      setKbAnswer(res.answer || 'No answer generated.');
      setKbSources(res.sources || '');
      const telData = await getTelemetry();
      setTelemetry(telData);
    } catch (e) {
      setKbAnswer("Error: " + e.message);
    }
    setKbLoading(false);
  };

  // --- Agentic OS Handlers ---

  const handleSaveSoul = async () => {
    try {
      await saveFarmSoul(farmSoul);
      setSoulEditing(false);
      alert("Farm Soul Profile successfully updated!");
    } catch (e) {
      alert("Failed to save Farm Soul Profile: " + e.message);
    }
  };

  const handleTriggerAgent = async (taskName, extraParams = {}) => {
    setAgentLoading(true);
    try {
      await runAgentTask(taskName, extraParams);
      
      // Poll or reload after a short delay since it's async
      setTimeout(async () => {
        const taskRes = await getAgentTasks();
        setAgentTasks(taskRes);
        // Refresh pending notes too as the agent writes to pending folder
        const pendNotes = await getPendingNotes();
        setPendingNotes(pendNotes);
        // Load cost data
        await loadCostData();
        setAgentLoading(false);
      }, 3500); // 3.5 seconds matches the background sleep delay!
    } catch (e) {
      alert("Failed to run background agent: " + e.message);
      setAgentLoading(false);
    }
  };

  const handleChipClick = (text) => {
    setQuestion(text);
  };

  const isTableResult = rows && rows.length > 0 && (rows.length > 1 || columns.length > 1);

  // Filter project files list based on search term
  const filteredNodes = graphNodes.filter(node => 
    node.toLowerCase().includes(searchNodeTerm.toLowerCase())
  );

  return (
    <div className="dashboard-container">
      {/* HEADER SECTION */}
      <header className="dashboard-header">
        <div>
          <h1>Farm Manager AI</h1>
          <div className="subtitle">Agentic Farm OS & Database Operations Control</div>
          
          {/* TAB BUTTONS */}
          <div className="tabs">
            <button 
              className={`tab-btn ${activeTab === 'operations' ? 'active' : ''}`}
              onClick={() => setActiveTab('operations')}
            >
              Farm Operations
            </button>
            <button 
              className={`tab-btn ${activeTab === 'graph' ? 'active' : ''}`}
              onClick={() => setActiveTab('graph')}
            >
              Code Graph Explorer
            </button>
            <button 
              className={`tab-btn ${activeTab === 'kb' ? 'active' : ''}`}
              onClick={() => setActiveTab('kb')}
            >
              Farm Knowledge Base
            </button>
          </div>
        </div>

        <div className="telemetry-bar">
          <div className="tel-card">
            <span className="tel-label">AI Success</span>
            <span className="tel-val success">{telemetry.aiSuccessCount}</span>
          </div>
          <div className="tel-card">
            <span className="tel-label">Fallbacks</span>
            <span className="tel-val warn">{telemetry.templateFallbackCount}</span>
          </div>
          <div className="tel-card">
            <span className="tel-label">SQL Failures</span>
            <span className="tel-val error">{telemetry.sqlFailureCount}</span>
          </div>
          <div className="tel-card">
            <span className="tel-label">LLM Spend</span>
            <span className="tel-val cost">${telemetry.accumulatedCost}</span>
          </div>
          <button className="reset-btn" onClick={handleResetTelemetry} title="Reset telemetry metrics">Reset</button>
        </div>
      </header>

      {/* RENDER ACTIVE TAB */}
      {activeTab === 'operations' ? (
        /* TAB 1: OPERATIONS LAYOUT */
        <div className="dashboard-grid">
          {/* LEFT COLUMN */}
          <div className="left-panel">
            {/* DAILY BRIEFING CARD */}
            <div className="dreaming-card card">
              <div className="card-header">
                <h2>Daily Briefing (Overnight Dreaming)</h2>
                <button 
                  className="regen-btn" 
                  disabled={briefingLoading} 
                  onClick={handleRegenerateBriefing}
                >
                  {briefingLoading ? "Briefing..." : "Regenerate"}
                </button>
              </div>
              <div className="briefing-content">
                {briefing.split('\n\n').map((para, idx) => (
                  <p key={idx}>{para}</p>
                ))}
              </div>
            </div>

            {/* QUERY WIDGET */}
            <div className="query-card card">
              <h2>Natural Language Farm Query</h2>
              <div className="chips">
                <span className="chip" onClick={() => handleChipClick("How many eggs did I collect last week?")}>Eggs last week</span>
                <span className="chip" onClick={() => handleChipClick("What were my total expenses last month?")}>Expenses last month</span>
                <span className="chip" onClick={() => handleChipClick("Show milk production this month.")}>Milk this month</span>
                <span className="chip" onClick={() => handleChipClick("Which animals need attention?")}>Sick animals</span>
              </div>
              
              <div className="input-row">
                <input
                  type="text"
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  placeholder="Ask your farm database a question..."
                  className="question-input"
                />
                <button disabled={loading} onClick={handleAsk} className="ask-button">Ask</button>
              </div>
            </div>

            {/* QUERY RESULTS */}
            {loading && <div className="loading-label">Executing SQL pipeline...</div>}

            {error && (
              <div className="error-box" role="alert">
                <p>{error}</p>
              </div>
            )}

            {sql && (
              <div className="card sql-card">
                <h3>Generated SQLite Query</h3>
                <code>{sql}</code>
              </div>
            )}

            {answer && !isTableResult && (
              <div className="card answer-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <h2 style={{ margin: 0, fontSize: '1.1rem', color: '#0f6354' }}>Answer</h2>
                  {mode && (
                    <span className={`mode-badge mode-${mode}`}>
                      {mode === 'ai' ? 'AI Mode' : 'Template Mode'}
                    </span>
                  )}
                </div>
                <p style={{ margin: 0, fontSize: '1.25rem', fontWeight: '500' }}>{answer}</p>
              </div>
            )}

            {isTableResult && (
              <div className="card table-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.8rem' }}>
                  <h2 style={{ margin: 0, fontSize: '1.1rem', color: '#333' }}>Database Results</h2>
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

          {/* RIGHT COLUMN */}
          <div className="right-panel">
            <div className="goals-card card">
              <h2>Mission Control Goals</h2>
              <div className="goals-list">
                {goals.map((g) => {
                  const percent = Math.min(100, Math.round((g.currentValue / g.targetValue) * 100));
                  return (
                    <div key={g.id} className="goal-item">
                      <div className="goal-meta">
                        <span className="goal-title">{g.name}</span>
                        <span className={`status-badge status-${g.status.toLowerCase()}`}>{g.status}</span>
                      </div>
                      <div className="goal-progress-row">
                        <div className="progress-bar-bg">
                          <div className={`progress-bar-fill progress-${g.status.toLowerCase()}`} style={{ width: `${percent}%` }}></div>
                        </div>
                        <span className="progress-text">{percent}%</span>
                      </div>
                      <div className="goal-details">
                        <span>Target: {g.targetValue}</span>
                        <span>Current: {g.currentValue}</span>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* ADD GOAL FORM */}
              <div className="add-goal-box">
                <h3>Create Operational Target</h3>
                <form onSubmit={handleAddGoal}>
                  <input 
                    type="text" 
                    placeholder="Goal Title (e.g. Eggs Collected)" 
                    value={newGoalName}
                    onChange={(e) => setNewGoalName(e.target.value)}
                    className="goal-input"
                    required
                  />
                  <div className="goal-form-row">
                    <input 
                      type="number" 
                      placeholder="Target Value" 
                      value={newGoalTarget}
                      onChange={(e) => setNewGoalTarget(e.target.value)}
                      className="goal-input"
                      required
                    />
                    <input 
                      type="date" 
                      value={newGoalDate}
                      onChange={(e) => setNewGoalDate(e.target.value)}
                      className="goal-input"
                      required
                    />
                  </div>
                  <textarea 
                    placeholder="SQL Metric Query (SELECT count/sum from table)" 
                    value={newGoalQuery}
                    onChange={(e) => setNewGoalQuery(e.target.value)}
                    className="goal-textarea"
                    required
                  />
                  <button type="submit" className="add-goal-btn">Activate Goal</button>
                </form>
              </div>
            </div>
          </div>
        </div>
      ) : activeTab === 'graph' ? (
        /* TAB 2: CODE GRAPH EXPLORER LAYOUT */
        <div className="dashboard-grid">
          {/* LEFT COLUMN: Graph Queries & Impact Checks */}
          <div className="left-panel">
            {/* BFS SEMANTIC QUERY */}
            <div className="graph-query-card card">
              <h2>Graph BFS Semantic Query</h2>
              <div className="chips">
                <span className="chip" onClick={() => setBfsQuestion("How is SQL validated?")}>SQL validation</span>
                <span className="chip" onClick={() => setBfsQuestion("Where are metrics reset?")}>Reset telemetry</span>
                <span className="chip" onClick={() => setBfsQuestion("Show daily briefing generation")}>Briefing logic</span>
                <span className="chip" onClick={() => setBfsQuestion("Find database seeding")}>DB seeding</span>
              </div>
              <div className="input-row">
                <input 
                  type="text" 
                  value={bfsQuestion} 
                  onChange={(e) => setBfsQuestion(e.target.value)} 
                  placeholder="Ask a technical layout question (e.g., How does UI connect to SQLite?)"
                  className="question-input"
                />
                <button disabled={bfsLoading} onClick={handleBfsSearch} className="ask-button">Trace</button>
              </div>
              
              {bfsLoading && <div className="loading-label">Searching codebase semantic graph...</div>}
              {bfsResult && (
                <div className="graph-result-box">
                  <h3>Graph Traversal Result</h3>
                  <pre className="graph-terminal-output">{bfsResult}</pre>
                </div>
              )}
            </div>

            {/* IMPACT ANALYSIS */}
            <div className="impact-card card">
              <h2>Change Impact Analyzer</h2>
              <p className="subtitle" style={{marginBottom: "0.8rem"}}>Select a code file or SQL database component to evaluate downstream dependencies.</p>
              <div className="input-row">
                <select 
                  value={selectedImpactNode} 
                  onChange={(e) => setSelectedImpactNode(e.target.value)} 
                  className="question-input"
                  style={{paddingRight: "2rem"}}
                >
                  {graphNodes.map((node, idx) => (
                    <option key={idx} value={node}>{node}</option>
                  ))}
                </select>
                <button disabled={impactLoading} onClick={handleImpactCheck} className="ask-button">Analyze</button>
              </div>
              
              {impactLoading && <div className="loading-label">Analyzing dependency impact tree...</div>}
              {impactResult && (
                <div className="graph-result-box">
                  <h3>Impact Analysis Result</h3>
                  <pre className="graph-terminal-output">{impactResult}</pre>
                </div>
              )}
            </div>
          </div>

          {/* RIGHT COLUMN: Symbol Directory */}
          <div className="right-panel">
            <div className="node-dir-card card">
              <h2>Codebase Symbol Directory</h2>
              <input 
                type="text" 
                value={searchNodeTerm} 
                onChange={(e) => setSearchNodeTerm(e.target.value)} 
                placeholder="Filter files (e.g. Controller, schema.sql)" 
                className="goal-input"
                style={{marginBottom: "1rem"}}
              />
              <div className="node-list-container">
                {filteredNodes.length > 0 ? (
                  <ul className="node-list">
                    {filteredNodes.map((node, idx) => (
                      <li 
                        key={idx} 
                        className={`node-list-item ${selectedImpactNode === node ? 'selected' : ''}`}
                        onClick={() => handleNodeClick(node)}
                        title="Click to select for Change Impact Analyzer"
                      >
                        <span className="node-icon">📄</span>
                        <span className="node-name">{node}</span>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <div style={{color: "#888", fontSize: "0.9rem", textAlign: "center", padding: "1rem"}}>No matching files.</div>
                )}
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* TAB 3: KNOWLEDGE BASE LAYOUT */
        <div className="dashboard-grid">
          {/* LEFT COLUMN: Search & Ingest */}
          <div className="left-panel">
            {/* WIKI SEARCH (RAG) */}
            <div className="query-card card">
              <h2>Farm Wiki Search (RAG Mode)</h2>
              <p className="subtitle" style={{marginBottom: "0.8rem"}}>Ask natural language questions about farm operations, vet advice, or soil health guidelines.</p>
              <div className="input-row">
                <input
                  type="text"
                  value={kbQuery}
                  onChange={(e) => setKbQuery(e.target.value)}
                  placeholder="Ask the farm wiki (e.g. how to treat bovine mastitis?)..."
                  className="question-input"
                />
                <button disabled={kbLoading} onClick={handleKbSearch} className="ask-button">Search</button>
              </div>

              {kbLoading && <div className="loading-label">Executing RAG pipeline...</div>}

              {kbAnswer && (
                <div className="card answer-card" style={{ marginTop: '1rem', padding: '1rem', background: '#f0fbf9', border: '1px solid #ccece6' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                    <h3 style={{ margin: 0, fontSize: '1.05rem', color: '#0f6354' }}>Answer</h3>
                    <span className="mode-badge mode-ai">Wiki RAG</span>
                  </div>
                  <p style={{ margin: 0, fontSize: '1.05rem', fontWeight: '500', whiteSpace: 'pre-wrap' }}>{kbAnswer}</p>
                  {kbSources && (
                    <div style={{ marginTop: '0.8rem', fontSize: '0.85rem', color: '#666' }}>
                      <strong>Sources cited:</strong> {kbSources}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* PENDING DOCUMENTS / INGESTION AREA */}
            <div className="card" style={{ marginTop: '1.5rem' }}>
              <h2>Pending Ingestions (Raw Logs & Transcripts)</h2>
              <p className="subtitle" style={{marginBottom: "0.8rem"}}>Review raw logs captured by automated flows, edit and clean them using AI before saving to the permanent vault.</p>
              
              <div className="pending-section" style={{ display: 'flex', gap: '1rem' }}>
                {/* Pending Sidebar */}
                <div className="pending-sidebar" style={{ width: '30%', borderRight: '1px solid #eee', paddingRight: '1rem' }}>
                  {pendingNotes.length > 0 ? (
                    <ul className="node-list" style={{ maxHeight: '250px', overflowY: 'auto' }}>
                      {pendingNotes.map((note) => (
                        <li
                          key={note.filename}
                          className={`node-list-item ${selectedPendingNote && selectedPendingNote.filename === note.filename ? 'selected' : ''}`}
                          onClick={() => handleSelectPendingNote(note)}
                        >
                          <span className="node-icon">📥</span>
                          <span className="node-name" style={{ fontSize: '0.85rem' }}>{note.filename}</span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <div style={{ color: '#888', fontSize: '0.9rem', textAlign: 'center', padding: '1rem' }}>No pending logs.</div>
                  )}
                </div>

                {/* Editor & AI Preview */}
                <div className="pending-editor" style={{ width: '70%' }}>
                  {selectedPendingNote ? (
                    <div>
                      <div className="input-row" style={{ marginBottom: '0.8rem' }}>
                        <input
                          type="text"
                          value={pendingNoteTitle}
                          onChange={(e) => setPendingNoteTitle(e.target.value)}
                          placeholder="Note Title (e.g. Sheep Deworming Guide)"
                          className="goal-input"
                          style={{ flex: 1 }}
                        />
                      </div>
                      
                      <div style={{ display: 'flex', gap: '1rem', height: '220px' }}>
                        <textarea
                          value={pendingNoteContent}
                          onChange={(e) => setPendingNoteContent(e.target.value)}
                          placeholder="Raw log text..."
                          className="goal-textarea"
                          style={{ width: '50%', height: '100%', resize: 'none', fontSize: '0.85rem' }}
                        />
                        <div 
                          className="goal-textarea" 
                          style={{ 
                            width: '50%', 
                            height: '100%', 
                            background: '#fcfcfc', 
                            overflowY: 'auto', 
                            fontSize: '0.85rem',
                            border: '1px solid #ddd',
                            padding: '0.5rem',
                            whiteSpace: 'pre-wrap'
                          }}
                        >
                          {kbActionLoading ? (
                            <div style={{ color: '#0f6354', fontWeight: '500' }}>Linting raw content with local Ollama...</div>
                          ) : lintedContent ? (
                            <div>
                              <div style={{ color: '#0f6354', fontWeight: '600', borderBottom: '1px solid #eee', marginBottom: '0.3rem', fontSize: '0.75rem' }}>AI POLISHED MARKDOWN PREVIEW:</div>
                              {lintedContent}
                            </div>
                          ) : (
                            <span style={{ color: '#999', fontStyle: 'italic' }}>AI Polished preview will appear here...</span>
                          )}
                        </div>
                      </div>

                      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.8rem', justifyContent: 'flex-end' }}>
                        <button 
                          onClick={() => handleDeletePendingNote(selectedPendingNote.filename)} 
                          className="reset-btn" 
                          style={{ margin: 0, padding: '0.4rem 0.8rem' }}
                        >
                          Discard
                        </button>
                        <button 
                          disabled={kbActionLoading} 
                          onClick={handleLintNote} 
                          className="ask-button" 
                          style={{ padding: '0.4rem 0.8rem', background: '#4a7c59' }}
                        >
                          Lint with AI
                        </button>
                        <button 
                          disabled={kbActionLoading} 
                          onClick={handleApproveNote} 
                          className="ask-button" 
                          style={{ padding: '0.4rem 1.2rem', background: '#0f6354' }}
                        >
                          Approve & Commit
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div style={{ color: '#888', fontStyle: 'italic', textAlign: 'center', padding: '3rem' }}>Select a pending log to review.</div>
                  )}
                </div>
              </div>
            </div>

            {/* BACKGROUND AGENTS CONSOLE */}
            <div className="card" style={{ marginTop: '1.5rem' }}>
              <h2>Background Agent Orchestrator (Level 5)</h2>
              <p className="subtitle" style={{marginBottom: "0.8rem"}}>Spawn parallel diagnostic workers to scan database tables and parse internet feeds to write findings to the pending queue.</p>
              
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', border: '1px solid #e1e6eb', borderRadius: '6px', padding: '0.5rem', background: '#f8fafc' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 'bold', color: '#4a5568', whiteSpace: 'nowrap' }}>Visual Diagnostics Input:</span>
                <input 
                  type="text" 
                  placeholder="Absolute image file path (defaults to screenshots/list-animals.png)" 
                  value={imagePath} 
                  onChange={(e) => setImagePath(e.target.value)}
                  style={{ flex: 1, padding: '0.3rem 0.5rem', fontSize: '0.75rem', borderRadius: '4px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
                <button
                  disabled={agentLoading}
                  onClick={() => handleTriggerAgent('ANOMALY_DETECTOR')}
                  className="ask-button"
                  style={{ background: '#4a7c59', fontSize: '0.82rem', padding: '0.5rem 0.8rem' }}
                >
                  Run Anomaly Scanner
                </button>
                <button
                  disabled={agentLoading}
                  onClick={() => handleTriggerAgent('MARKET_ANALYZER')}
                  className="ask-button"
                  style={{ background: '#4a7c59', fontSize: '0.82rem', padding: '0.5rem 0.8rem' }}
                >
                  Run Market Advisor
                </button>
                <button
                  disabled={agentLoading}
                  onClick={() => handleTriggerAgent('WEB_INTELLIGENCE')}
                  className="ask-button"
                  style={{ background: '#0f6354', fontSize: '0.82rem', padding: '0.5rem 0.8rem' }}
                >
                  Run Web Intel (Agent Reach)
                </button>
                <button
                  disabled={agentLoading}
                  onClick={() => handleTriggerAgent('OUTREACH_SPECIALIST')}
                  className="ask-button"
                  style={{ background: '#0f6354', fontSize: '0.82rem', padding: '0.5rem 0.8rem' }}
                >
                  Run Outreach (Clay)
                </button>
                <button
                  disabled={agentLoading}
                  onClick={() => handleTriggerAgent('MODEL_ROUTER')}
                  className="ask-button"
                  style={{ background: '#1a73e8', fontSize: '0.82rem', padding: '0.5rem 0.8rem' }}
                >
                  Run Model Router
                </button>
                <button
                  disabled={agentLoading}
                  onClick={() => handleTriggerAgent('IMAGE_ANALYZER', { imagePath })}
                  className="ask-button"
                  style={{ background: '#1a73e8', fontSize: '0.82rem', padding: '0.5rem 0.8rem' }}
                >
                  Run Image Analyzer
                </button>
              </div>

              {agentLoading && <div className="loading-label" style={{textAlign: 'left', marginBottom: '0.5rem'}}>Agent running in background... executing task sweep...</div>}

              <div style={{ maxHeight: '180px', overflowY: 'auto', border: '1px solid #eee', borderRadius: '6px', background: '#fafafa', padding: '0.5rem' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 'bold', color: '#666', display: 'block', marginBottom: '0.3rem' }}>AGENT TASK EXECUTION HISTORY:</span>
                {agentTasks.length > 0 ? (
                  <table className="results-table" style={{ fontSize: '0.75rem', margin: 0 }}>
                    <thead>
                      <tr>
                        <th>Task ID</th>
                        <th>Agent Name</th>
                        <th>Routed Model</th>
                        <th>Cost</th>
                        <th>Status</th>
                        <th>Started At</th>
                        <th>Completed At</th>
                      </tr>
                    </thead>
                    <tbody>
                      {agentTasks.map((t, idx) => (
                        <tr key={idx}>
                          <td><code>{t.id}</code></td>
                          <td>{t.name}</td>
                          <td><span style={{ fontSize: '0.7rem', color: '#555', fontFamily: 'monospace' }}>{t.model || 'qwen2.5-coder:7b'}</span></td>
                          <td><span style={{ fontWeight: '500', color: '#0f6354' }}>{t.cost !== undefined ? `$${t.cost.toFixed(5)}` : '$0.00000'}</span></td>
                          <td>
                            <span className={`status-badge status-${t.status.toLowerCase() === 'completed' ? 'met' : t.status.toLowerCase() === 'running' ? 'active' : 'missed'}`} style={{fontSize: '0.65rem'}}>
                              {t.status}
                            </span>
                          </td>
                          <td>{t.startTime}</td>
                          <td>{t.endTime || '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <span style={{ color: '#999', fontSize: '0.8rem', fontStyle: 'italic' }}>No background tasks executed yet.</span>
                )}
              </div>
            </div>

            {/* MODEL INTELLIGENCE & COST CONTROL */}
            <div className="card" style={{ marginTop: '1.5rem' }}>
              <h2>Model Intelligence & Cost Telemetry</h2>
              <p className="subtitle" style={{marginBottom: "0.8rem"}}>Track token usage, cost analytics, and configure dynamic model routing for background workers.</p>

              {/* Aggregated Telemetry Summary */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '0.8rem', marginBottom: '1.2rem' }}>
                <div style={{ background: '#f0faf6', border: '1px solid #c7ebd4', borderRadius: '6px', padding: '0.6rem 0.8rem' }}>
                  <div style={{ fontSize: '0.7rem', color: '#0f6354', fontWeight: 'bold', textTransform: 'uppercase' }}>Total Model Spend</div>
                  <div style={{ fontSize: '1.3rem', fontWeight: 'bold', color: '#0f6354' }}>${costReport.totalCost.toFixed(5)}</div>
                </div>
                <div style={{ background: '#f5f7fa', border: '1px solid #e1e6eb', borderRadius: '6px', padding: '0.6rem 0.8rem' }}>
                  <div style={{ fontSize: '0.7rem', color: '#4f5e71', fontWeight: 'bold', textTransform: 'uppercase' }}>Total Tokens Processed</div>
                  <div style={{ fontSize: '1.3rem', fontWeight: 'bold', color: '#2d3748' }}>{costReport.totalTokens.toLocaleString()}</div>
                </div>
                <div style={{ background: '#fcf8e3', border: '1px solid #faebcc', borderRadius: '6px', padding: '0.6rem 0.8rem' }}>
                  <div style={{ fontSize: '0.7rem', color: '#8a6d3b', fontWeight: 'bold', textTransform: 'uppercase' }}>Active Task Models</div>
                  <div style={{ fontSize: '1.3rem', fontWeight: 'bold', color: '#8a6d3b' }}>{availableModels.filter(m => !m.includes("bge")).length}</div>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                {/* Routing Config */}
                <div style={{ border: '1px solid #eee', borderRadius: '6px', padding: '0.8rem', background: '#fafafa' }}>
                  <h4 style={{ margin: '0 0 0.5rem 0', color: '#4a7c59', fontSize: '0.85rem' }}>Dynamic Engine Routing</h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {Object.keys(modelRouting).length > 0 ? (
                      Object.entries(modelRouting).map(([task, model]) => (
                        <div key={task} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.75rem' }}>
                          <span style={{ fontWeight: '500', color: '#555' }}>{task}:</span>
                          <select 
                            value={model} 
                            onChange={(e) => handleUpdateRouting(task, e.target.value)}
                            style={{ padding: '0.2rem', borderRadius: '4px', border: '1px solid #ccc', fontSize: '0.75rem', width: '150px' }}
                          >
                            {availableModels.map(m => (
                              <option key={m} value={m}>{m}</option>
                            ))}
                          </select>
                        </div>
                      ))
                    ) : (
                      <span style={{ color: '#999', fontSize: '0.75rem', fontStyle: 'italic' }}>Loading routing map...</span>
                    )}
                  </div>
                </div>

                {/* Available Models */}
                <div style={{ border: '1px solid #eee', borderRadius: '6px', padding: '0.8rem', background: '#fafafa' }}>
                  <h4 style={{ margin: '0 0 0.5rem 0', color: '#4a7c59', fontSize: '0.85rem' }}>Discovered Ollama Instances</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.3rem', alignContent: 'flex-start', maxHeight: '100px', overflowY: 'auto' }}>
                    {availableModels.length > 0 ? (
                      availableModels.map(model => (
                        <span key={model} style={{ 
                          fontSize: '0.65rem', 
                          background: model.includes('coder') ? '#e6f4ea' : model.includes('deepseek') ? '#e8f0fe' : '#f1f3f4',
                          color: model.includes('coder') ? '#137333' : model.includes('deepseek') ? '#1a73e8' : '#3c4043',
                          padding: '0.2rem 0.4rem', 
                          borderRadius: '4px',
                          border: '1px solid rgba(0,0,0,0.05)',
                          fontWeight: '500'
                        }}>
                          {model}
                        </span>
                      ))
                    ) : (
                      <span style={{ color: '#999', fontSize: '0.75rem', fontStyle: 'italic' }}>No models discovered on localhost:11434</span>
                    )}
                  </div>
                </div>
              </div>

              {/* Cost Charts & Cost Breakdown */}
              <div style={{ border: '1px solid #eee', borderRadius: '6px', padding: '0.8rem', background: '#fafafa' }}>
                <h4 style={{ margin: '0 0 0.5rem 0', color: '#4a7c59', fontSize: '0.85rem' }}>Task Cost Allocation (Relative)</h4>
                {costReport.byTask.length > 0 ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {costReport.byTask.map(t => (
                      <div key={t.taskName} style={{ fontSize: '0.75rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.1rem' }}>
                          <span style={{ fontWeight: '500' }}>{t.taskName}</span>
                          <span style={{ color: '#666' }}>${t.cost.toFixed(5)} ({t.tokens.toLocaleString()} tkn)</span>
                        </div>
                        <div style={{ width: '100%', height: '8px', background: '#e1e6eb', borderRadius: '4px', overflow: 'hidden' }}>
                          <div style={{ height: '100%', width: `${t.percent}%`, background: '#4a7c59', borderRadius: '4px' }}></div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <span style={{ color: '#999', fontSize: '0.75rem', fontStyle: 'italic' }}>No cost telemetry recorded yet. Run background agents to compile.</span>
                )}
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN: Approved Notes Vault Browser */}
          <div className="right-panel">
            <div className="node-dir-card card" style={{ display: 'flex', flexDirection: 'column', marginBottom: '1.5rem' }}>
              <h2>Wiki Vault Notes</h2>
              <div className="node-list-container" style={{ maxHeight: '250px', overflowY: 'auto', borderBottom: '1px solid #eee', marginBottom: '1rem' }}>
                {approvedNotes.length > 0 ? (
                  <ul className="node-list">
                    {approvedNotes.map((note) => (
                      <li
                        key={note.filename}
                        className={`node-list-item ${selectedNote && selectedNote.filename === note.filename ? 'selected' : ''}`}
                        onClick={() => handleSelectNote(note)}
                      >
                        <span className="node-icon">📖</span>
                        <div style={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
                          <span className="node-name" style={{ fontWeight: '500' }}>{note.title}</span>
                          {note.tags && <span style={{ fontSize: '0.7rem', color: '#888' }}>{note.tags}</span>}
                        </div>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <div style={{ color: '#888', fontSize: '0.9rem', textAlign: 'center', padding: '1rem' }}>No approved notes in vault.</div>
                )}
              </div>

              {/* Note Detail Panel */}
              <div style={{ display: 'flex', flexDirection: 'column', minHeight: '250px' }}>
                {selectedNote ? (
                  <div style={{ display: 'flex', flexDirection: 'column', height: '100%', border: '1px solid #eee', borderRadius: '4px', padding: '0.8rem', background: '#fafafa' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #ddd', paddingBottom: '0.3rem', marginBottom: '0.5rem' }}>
                      <span style={{ fontWeight: 'bold', color: '#333', fontSize: '0.9rem' }}>{selectedNote.title}</span>
                      <button 
                        onClick={() => handleDeleteNote(selectedNote.filename)} 
                        className="reset-btn" 
                        style={{ margin: 0, padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}
                      >
                        Delete
                      </button>
                    </div>
                    <div style={{ flex: 1, overflowY: 'auto', fontSize: '0.85rem', whiteSpace: 'pre-wrap', fontFamily: 'monospace' }}>
                      {selectedNoteContent}
                    </div>
                  </div>
                ) : (
                  <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999', border: '1px dashed #ccc', borderRadius: '4px', fontStyle: 'italic' }}>
                    Select an approved wiki note to view content.
                  </div>
                )}
              </div>
            </div>

            {/* FARM SOUL PROFILE EDITOR */}
            <div className="card">
              <h2>Farm Soul Profile (Level 2)</h2>
              <p className="subtitle" style={{ marginBottom: "0.8rem" }}>
                Defines the identity, goals, and operational constraints of the farm. Injected automatically into all SQL and RAG queries.
              </p>
              
              {soulEditing ? (
                <div>
                  <textarea
                    value={farmSoul}
                    onChange={(e) => setFarmSoul(e.target.value)}
                    placeholder="# Farm Identity..."
                    className="goal-textarea"
                    style={{ width: '100%', height: '150px', resize: 'vertical', fontSize: '0.85rem', marginBottom: '0.8rem' }}
                  />
                  <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                    <button 
                      onClick={() => setSoulEditing(false)} 
                      className="reset-btn" 
                      style={{ margin: 0, padding: '0.4rem 0.8rem' }}
                    >
                      Cancel
                    </button>
                    <button 
                      onClick={handleSaveSoul} 
                      className="ask-button" 
                      style={{ padding: '0.4rem 1.2rem', background: '#0f6354' }}
                    >
                      Save Profile
                    </button>
                  </div>
                </div>
              ) : (
                <div>
                  <div 
                    style={{ 
                      background: '#f9f9f9', 
                      border: '1px solid #eee', 
                      borderRadius: '4px', 
                      padding: '0.8rem', 
                      maxHeight: '150px', 
                      overflowY: 'auto', 
                      fontSize: '0.85rem',
                      whiteSpace: 'pre-wrap',
                      fontFamily: 'monospace',
                      marginBottom: '0.8rem'
                    }}
                  >
                    {farmSoul ? farmSoul : <span style={{ color: '#999', fontStyle: 'italic' }}>No Farm Soul Profile defined. Click Edit to create one.</span>}
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <button 
                      onClick={() => setSoulEditing(true)} 
                      className="ask-button" 
                      style={{ padding: '0.4rem 1.2rem', background: '#4a7c59' }}
                    >
                      Edit Profile
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default App;
